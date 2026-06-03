package io.legado.app.model.translation.signer

import java.security.MessageDigest

/**
 * 百度翻译签名
 *
 * sign = MD5(appid + q + salt + secretKey)
 */
object BaiduSigner {
    fun sign(appId: String, q: String, salt: String, secretKey: String): String {
        val raw = appId + q + salt + secretKey
        return md5(raw)
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xff
            if (v < 16) sb.append('0')
            sb.append(Integer.toHexString(v))
        }
        return sb.toString()
    }

    fun randomSalt(): String = (10000..99999).random().toString()
}
