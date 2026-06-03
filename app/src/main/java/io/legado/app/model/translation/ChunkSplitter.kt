package io.legado.app.model.translation

/**
 * 把长文本按段落切分成多个块
 *
 * 切分规则:
 *  1. 按段落(空行)切分
 *  2. 段落按 maxChars 再次切分
 *  3. 每次请求最多 maxParas 个段落
 */
object ChunkSplitter {

    data class Chunk(val text: String)

    fun split(text: String, maxChars: Int, maxParas: Int): List<Chunk> {
        if (text.isBlank()) return emptyList()
        val safeMaxChars = maxChars.coerceAtLeast(100)
        val safeMaxParas = maxParas.coerceAtLeast(1)
        val paragraphs = text.split(Regex("\\n\\s*\\n+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .flatMap { splitParagraph(it, safeMaxChars) }
        if (paragraphs.isEmpty()) return emptyList()
        return paragraphs.chunked(safeMaxParas).map { Chunk(it.joinToString("\n\n")) }
    }

    private fun splitParagraph(para: String, maxChars: Int): List<String> {
        if (para.length <= maxChars) return listOf(para)
        val out = mutableListOf<String>()
        var i = 0
        while (i < para.length) {
            val end = (i + maxChars).coerceAtMost(para.length)
            // 尝试在句号/换行处断开
            var cut = end
            if (end < para.length) {
                val slice = para.substring(i, end)
                val lastBoundary = slice.lastIndexOfAny(charArrayOf('。', '!', '?', '！', '?', '\n'))
                if (lastBoundary > maxChars / 2) {
                    cut = i + lastBoundary + 1
                }
            }
            out.add(para.substring(i, cut).trim())
            i = cut
        }
        return out.filter { it.isNotEmpty() }
    }
}
