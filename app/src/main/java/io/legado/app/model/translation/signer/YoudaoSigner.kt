package io.legado.app.model.translation.signer

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 有道智云 v3 签名
 *
 * sign = SHA256(appKey + truncate(q) + salt + curtime + appSecret)
 * truncate: len(q) <= 20  -> q
 *           else          -> q[:10] + len(q) + q[-10:]
 */
object YoudaoSigner {
    fun sign(appKey: String, q: String, salt: String, curtime: String, appSecret: String): String {
        val truncated = if (q.length <= 20) q else q.substring(0, 10) + q.length + q.substring(q.length - 10)
        val raw = appKey + truncated + salt + curtime + appSecret
        return sha256Hex(raw)
    }

    fun currentTime(): String = (System.currentTimeMillis() / 1000).toString()

    fun randomSalt(): String = (1..65536).random().toString()

    private fun sha256Hex(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.toHex()
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }
}
