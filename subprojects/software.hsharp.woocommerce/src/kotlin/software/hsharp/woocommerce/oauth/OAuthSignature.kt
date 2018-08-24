package software.hsharp.woocommerce.oauth

import org.apache.commons.codec.binary.Base64
import software.hsharp.woocommerce.impl.Config
import software.hsharp.woocommerce.impl.IConfig
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.HashMap
import java.util.TreeMap
import java.util.UUID
import java.util.stream.Collectors

enum class HttpMethod {

    GET, POST, PUT, DELETE
}

enum class OAuthHeader private constructor(val value: String) {

    OAUTH_CONSUMER_KEY("oauth_consumer_key"),
    OAUTH_TIMESTAMP("oauth_timestamp"),
    OAUTH_NONCE("oauth_nonce"),
    OAUTH_SIGNATURE_METHOD("oauth_signature_method"),
    OAUTH_SIGNATURE("oauth_signature")
}

enum class SpecialSymbol private constructor(val plain: String, val encoded: String) {

    AMP("&", "%26"),
    EQUAL("=", "%3D"),
    PLUS("+", "%2B"),
    STAR("*", "%2A"),
    TILDE("~", "%7E")
}

/**
 * WooCommerce specific OAuth signature generator
 */
object OAuthSignature {

    private val UTF_8 = "UTF-8"
    private val HMAC_SHA256 = "HmacSHA256"
    private val SIGNATURE_METHOD_HMAC_SHA256 = "HMAC-SHA256"
    private val BASE_SIGNATURE_FORMAT = "%s&%s&%s"
    private val DELETE_PARAM_FORCE = "force"

    @JvmOverloads
    fun getAsMap(_config: Config?, endpoint: String?, httpMethod: HttpMethod?, params: Map<String, String> = emptyMap()): MutableMap<String, String> {
        if (_config == null || endpoint == null || httpMethod == null) {
            return emptyMap<String, String>().toMutableMap()
        }
        val config = _config
        val authParams = HashMap<String, String>()
        authParams[OAuthHeader.OAUTH_CONSUMER_KEY.value] = config.consumerKey
        authParams[OAuthHeader.OAUTH_TIMESTAMP.value] = (System.currentTimeMillis() / 1000L).toString()
        authParams[OAuthHeader.OAUTH_NONCE.value] = UUID.randomUUID().toString()
        authParams[OAuthHeader.OAUTH_SIGNATURE_METHOD.value] = SIGNATURE_METHOD_HMAC_SHA256
        authParams.putAll(params)

        // WooCommerce specified param
        if (HttpMethod.DELETE == httpMethod) {
            authParams[DELETE_PARAM_FORCE] = java.lang.Boolean.TRUE.toString()
        }
        val oAuthSignature = generateOAuthSignature(config.consumerSecret, endpoint, httpMethod, authParams)
        authParams[OAuthHeader.OAUTH_SIGNATURE.value] = oAuthSignature
        return authParams
    }

    fun getAsQueryString(config: IConfig?): String {
        return "consumer_key=${config!!.consumerKey}&consumer_secret=${config.consumerSecret}&per_page=100"
    }

    private fun generateOAuthSignature(customerSecret: String, endpoint: String, httpMethod: HttpMethod, parameters: Map<String, String>): String {
        val signatureBaseString = getSignatureBaseString(endpoint, httpMethod.name, parameters)
        // v1, v2
        val secret = customerSecret + SpecialSymbol.AMP.plain
        return signBaseString(secret, signatureBaseString)
    }

    private fun signBaseString(secret: String, signatureBaseString: String): String {
        val macInstance: Mac
        try {
            macInstance = Mac.getInstance(HMAC_SHA256)
            val secretKey = SecretKeySpec(secret.toByteArray(charset(UTF_8)), HMAC_SHA256)
            macInstance.init(secretKey)
            return Base64.encodeBase64String(macInstance.doFinal(signatureBaseString.toByteArray(charset(UTF_8))))
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: InvalidKeyException) {
            throw RuntimeException(e)
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
    }

    private fun urlEncode(s: String): String {
        try {
            return URLEncoder.encode(s, UTF_8)
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
    }

    private fun getSignatureBaseString(url: String, method: String, parameters: Map<String, String>): String {
        val requestURL = urlEncode(url)
        // 1. Percent encode every key and value that will be signed.
        var encodedParameters = percentEncodeParameters(parameters)

        // 2. Sort the list of parameters alphabetically by encoded key.
        encodedParameters = getSortedParameters(encodedParameters)
        val paramsString = mapToString(encodedParameters, SpecialSymbol.EQUAL.encoded, SpecialSymbol.AMP.encoded)
        return String.format(BASE_SIGNATURE_FORMAT, method, requestURL, paramsString)
    }

    private fun mapToString(paramsMap: Map<String, String>, keyValueDelimiter: String, paramsDelimiter: String): String {
        return paramsMap.entries.stream()
                .map { entry -> entry.key + keyValueDelimiter + entry.value }
                .collect(Collectors.joining(paramsDelimiter))
    }

    private fun percentEncodeParameters(parameters: Map<String, String>): Map<String, String> {
        val encodedParamsMap = HashMap<String, String>()

        for ((key, value) in parameters) {
            encodedParamsMap[percentEncode(key)] = percentEncode(value)
        }

        return encodedParamsMap
    }

    private fun percentEncode(s: String): String {
        try {
            return URLEncoder.encode(s, UTF_8)
                    // OAuth encodes some characters differently:
                    .replace(SpecialSymbol.PLUS.plain, SpecialSymbol.PLUS.encoded)
                    .replace(SpecialSymbol.STAR.plain, SpecialSymbol.STAR.encoded)
                    .replace(SpecialSymbol.TILDE.encoded, SpecialSymbol.TILDE.plain)
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e.message, e)
        }
    }

    private fun getSortedParameters(parameters: Map<String, String>): Map<String, String> {
        return TreeMap(parameters)
    }
}