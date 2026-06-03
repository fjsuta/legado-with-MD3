package io.legado.app.model.translation

import kotlinx.coroutines.delay

/**
 * 简单的令牌桶限速器:每秒最多 N 个请求
 */
class RateLimiter(private val perSecond: Int) {
    private val intervalMs: Long = if (perSecond <= 0) 0L else 1000L / perSecond
    private var lastAcquireMs: Long = 0L
    private val lock = Any()

    suspend fun acquire() {
        if (intervalMs == 0L) return
        val wait: Long = synchronized(lock) {
            val now = System.currentTimeMillis()
            val next = lastAcquireMs + intervalMs
            val w = if (now < next) next - now else 0L
            lastAcquireMs = if (w == 0L) now else next
            w
        }
        if (wait > 0) delay(wait)
    }
}
