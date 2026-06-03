package io.legado.app.model.translation

/**
 * 有状态的分块翻译装配器。
 *
 * 用于并行翻译场景:每个分块完成时调用 [setResult] 写入译文,
 * 任何时刻调用 [build] 即可得到当前已翻译的"原/译"混排文本。
 */
class TranslationAssembler(private val totalChunks: Int) {

    private val results = HashMap<Int, String>(totalChunks)

    fun setResult(index: Int, content: String) {
        results[index] = content
    }

    fun hasResult(index: Int): Boolean = results.containsKey(index)

    fun completedCount(): Int = results.size

    val isComplete: Boolean get() = results.size >= totalChunks

    fun build(): String {
        if (results.isEmpty()) return ""
        val sb = StringBuilder()
        for (i in 0 until totalChunks) {
            val text = results[i] ?: continue
            if (sb.isNotEmpty()) sb.append("\n\n")
            sb.append(text)
        }
        return sb.toString()
    }
}
