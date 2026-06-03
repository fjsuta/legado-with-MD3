package io.legado.app.model.translation

import java.util.concurrent.ConcurrentHashMap

/**
 * 有状态分块翻译装配器。
 *
 * 多个协程并发调用 [setResult] 写入不同索引的结果,调用方通过 [build] 获取
 * 已完成部分的拼接结果。[isComplete] 仅在所有索引都填满后才为 true。
 *
 * 使用 [ConcurrentHashMap] 保证多协程并发写入安全。
 */
class TranslationAssembler(private val totalChunks: Int) {

    private val results = ConcurrentHashMap<Int, String>(totalChunks)

    fun setResult(index: Int, content: String) {
        results[index] = content
    }

    fun hasResult(index: Int): Boolean = results.containsKey(index)

    fun completedCount(): Int = results.size

    val isComplete: Boolean get() = results.size >= totalChunks

    /**
     * 按索引顺序拼接所有已存在的结果。尚未填入的索引会被跳过。
     * 段落间用 `\n\n` 分隔,保持可读性。
     */
    fun build(): String {
        if (totalChunks == 0) return ""
        val sb = StringBuilder()
        for (i in 0 until totalChunks) {
            val text = results[i] ?: continue
            if (sb.isNotEmpty()) sb.append("\n\n")
            sb.append(text)
        }
        return sb.toString()
    }
}
