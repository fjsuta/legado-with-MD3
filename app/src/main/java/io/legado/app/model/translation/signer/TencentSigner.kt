package io.legado.app.model.translation.signer

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 腾讯云 TC3-HMAC-SHA256 签名
 *
 * 文档:https://cloud.tencent.com/document/api/551/30636
 */
object TencentSigner {

    fun buildHeaders(
        secretId: String,
        secretKey: String,
        service: String = "tmt",
        host: String = "tmt.tencentcloudapi.com",
        action: String = "TextTranslate",
        version: String = "2018-03-21",
        region: String = "ap-guangzhou",
        payload: String
    ): Map<String, String> {
        val timestamp = System.currentTimeMillis() / 1000
        val date = java.time.Instant.ofEpochSecond(timestamp)
            .atZone(java.time.ZoneOffset.UTC)
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val canonicalHeaders = "content-type:application/json; charset=utf-8\n" +
            "host:$host\n" +
            "x-tc-action:${action.toLowerCase()}\n"
        val signedHeaders = "content-type;host;x-tc-action"

        val payloadHash = sha256Hex(payload)
        val canonicalRequest = "POST\n/\n\n$canonicalHeaders\n$signedHeaders\n$payloadHash"

        val credentialScope = "$date/$service/tc3_request"
        val stringToSign = "TC3-HMAC-SHA256\n$timestamp\n$credentialScope\n${sha256Hex(canonicalRequest)}"

        val secretDate = hmac(("TC3$secretKey").toByteArray(), date)
        val secretService = hmac(secretDate, service)
        val secretSigning = hmac(secretService, "tc3_request")
        val signature = hmac(secretSigning, stringToSign).toHex()

        return mapOf(
            "Authorization" to "TC3-HMAC-SHA256 Credential=$secretId/$credentialScope, " +
                "SignedHeaders=$signedHeaders, Signature=$signature",
            "Content-Type" to "application/json; charset=utf-8",
            "Host" to host,
            "X-TC-Action" to action,
            "X-TC-Timestamp" to timestamp.toString(),
            "X-TC-Version" to version,
            "X-TC-Region" to region
        )
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
