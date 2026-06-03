package io.legado.app.model.translation.signer

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 火山引擎(字节跳动)机器翻译签名
 *
 * 服务名:translate
 * 区域:cn-north-1
 * 签名算法:Volcengine 签名
 * 文档:https://www.volcengine.com/docs/4640/65007
 */
object VolcanoSigner {

    fun buildHeaders(
        accessKeyId: String,
        accessKeySecret: String,
        method: String = "POST",
        host: String = "open.volcengineapi.com",
        path: String = "/",
        body: String
    ): Map<String, String> {
        val region = "cn-north-1"
        val service = "translate"
        val timestamp = System.currentTimeMillis() / 1000
        val date = java.time.Instant.ofEpochSecond(timestamp)
            .atZone(java.time.ZoneOffset.UTC)
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))
        val shortDate = date.substring(0, 8)

        val headers = linkedMapOf(
            "X-Date" to date,
            "Host" to host,
            "Content-Type" to "application/json"
        )

        val canonicalHeaders = "$shortDate\n$region\n$service\n"
        val signedHeaders = "x-date"
        val bodyHash = sha256Hex(body)

        val canonicalRequest = "$method\n$path\n\n$canonicalHeaders\n$signedHeaders\n$bodyHash"
        val credentialScope = "$shortDate/$region/$service/request"
        val stringToSign = "HMAC-SHA256\n$date\n$credentialScope\n${sha256Hex(canonicalRequest)}"

        val kDate = hmac("AWS4$accessKeySecret".toByteArray(), shortDate)
        val kRegion = hmac(kDate, region)
        val kService = hmac(kRegion, service)
        val kSigning = hmac(kService, "request")
        val signature = hmac(kSigning, stringToSign).toHex()

        val auth = "HMAC-SHA256 Credential=$accessKeyId/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"
        headers["Authorization"] = auth
        return headers
    }

    private fun sha256Hex(s: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(s.toByteArray(Charsets.UTF_8)).toHex()
    }

    private fun hmac(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
