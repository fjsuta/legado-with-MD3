package io.legado.app.domain.usecase

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.domain.gateway.TranslationCacheGateway
import io.legado.app.help.book.BookHelp
import io.legado.app.model.translation.ChunkSplitter
import io.legado.app.model.translation.ProviderConfigData
import io.legado.app.model.translation.RateLimiter
import io.legado.app.model.translation.TranslationAssembler
import io.legado.app.model.translation.TranslationProvider
import io.legado.app.ui.config.translation.TranslationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class TranslationProgress(
    val currentChunk: Int = 0,
    val totalChunks: Int = 0,
    val mixedContent: String? = null
)

class TranslateChapterUseCase : KoinComponent {

    private val translationCacheGateway: TranslationCacheGateway by inject()

    /**
     * 翻译整章
     * 失败:Result.failure;成功:Result.success(最终内容)
     */
    suspend fun execute(
        book: Book,
        bookChapter: BookChapter,
        targetLanguage: String,
        onProgress: (TranslationProgress) -> Unit = {},
        onTranslateStarted: () -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        val active = TranslationConfig.firstValidConfig()
            ?: return@withContext Result.failure(IllegalStateException("未配置翻译服务"))
        val (provider, config) = active
        val maxChars = (config.fields["maxChars"]?.toIntOrNull() ?: 1800).coerceAtLeast(100)
        val maxParas = (config.fields["maxParas"]?.toIntOrNull() ?: 8).coerceAtLeast(1)
        val rateLimit = (config.fields["rateLimit"]?.toIntOrNull() ?: 1).coerceAtLeast(1)

        val originalContent = BookHelp.getContent(book, bookChapter)
            ?: return@withContext Result.failure(IllegalStateException("原文为空"))

        val contentHash = translationCacheGateway.computeContentHash(originalContent)
        val providerId = config.id
        val existingChunks = translationCacheGateway.getCachedChunks(
            book, bookChapter, targetLanguage, contentHash
        )
        val completedChunks = existingChunks
            .filter { it.provider == providerId && it.status == STATUS_OK }
            .associateBy { it.chunkIndex }
            .mapValues { it.value.translatedChunkContent.orEmpty() }

        val rawChunks = ChunkSplitter.split(originalContent, maxChars, maxParas)
        val totalChunks = rawChunks.size
        if (totalChunks == 0) return@withContext Result.success("")

        val assembler = TranslationAssembler(totalChunks)
        completedChunks.forEach { (idx, text) -> assembler.setResult(idx, text) }
        onProgress(
            TranslationProgress(
                currentChunk = assembler.completedCount(),
                totalChunks = totalChunks,
                mixedContent = assembler.build().ifEmpty { null }
            )
        )

        if (assembler.isComplete) {
            val text = assembler.build()
            translationCacheGateway.writeTranslation(book, bookChapter, targetLanguage, text)
            return@withContext Result.success(text)
        }

        onTranslateStarted()

        val rateLimiter = RateLimiter(rateLimit)
        val semaphore = Semaphore(maxParas)
        try {
            coroutineScope {
                rawChunks.mapIndexed { idx, chunk ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            if (assembler.hasResult(idx)) return@async
                            rateLimiter.acquire()
                            val result = provider.translate(config, chunk.text, "", targetLanguage)
                            result.onSuccess { translated ->
                                assembler.setResult(idx, translated)
                                translationCacheGateway.saveChunk(
                                    book = book,
                                    bookChapter = bookChapter,
                                    targetLanguage = targetLanguage,
                                    chunkIndex = idx,
                                    originalChunkContent = chunk.text,
                                    originalContentHash = contentHash,
                                    provider = providerId,
                                    status = STATUS_OK,
                                    translatedContent = translated,
                                    errorMessage = null
                                )
                            }.onFailure { e ->
                                translationCacheGateway.saveChunk(
                                    book = book,
                                    bookChapter = bookChapter,
                                    targetLanguage = targetLanguage,
                                    chunkIndex = idx,
                                    originalChunkContent = chunk.text,
                                    originalContentHash = contentHash,
                                    provider = providerId,
                                    status = STATUS_FAILED,
                                    translatedContent = null,
                                    errorMessage = e.message
                                )
                            }
                            onProgress(
                                TranslationProgress(
                                    currentChunk = assembler.completedCount(),
                                    totalChunks = totalChunks,
                                    mixedContent = assembler.build().ifEmpty { null }
                                )
                            )
                        }
                    }
                }.awaitAll()
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }

        if (!assembler.isComplete) {
            return@withContext Result.failure(
                RuntimeException("翻译部分失败: 成功 ${assembler.completedCount()}/$totalChunks")
            )
        }

        val finalText = assembler.build()
        translationCacheGateway.writeTranslation(book, bookChapter, targetLanguage, finalText)
        Result.success(finalText)
    }

    companion object {
        private const val STATUS_OK = 2
        private const val STATUS_FAILED = 3
    }
}
