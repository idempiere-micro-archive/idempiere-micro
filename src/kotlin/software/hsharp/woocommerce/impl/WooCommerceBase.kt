package software.hsharp.woocommerce.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.kittinunf.fuel.httpGet
import software.hsharp.woocommerce.oauth.HttpMethod
import software.hsharp.woocommerce.oauth.OAuthSignature

interface IConfig {
    val url : String
    val consumerKey : String
    val consumerSecret : String
}

data class Config(
        override val url : String,
        override val consumerKey : String,
        override val consumerSecret : String
) : IConfig


/**
 * Enum that represents WooCommerce API versions
 */
enum class ApiVersionType constructor(val value: String) {
    V1("v1"),
    V2("v2")
}

open class WooCommerceBase(val config : IConfig, val apiVersion : ApiVersionType) {

    fun create(endpointBase: String, context: Map<String, Any>): Map<*, *> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    inline fun <reified T:Any> get(
            endpointBase: String,
            id: Int,
            factoryMethod: (Any) -> T
    ): T {
        val url = "${config.url}/wp-json/wc/$apiVersion/$endpointBase/$id"
        val signature = OAuthSignature.getAsQueryString(config, url, HttpMethod.GET)
        val securedUrl = "$url?$signature"

        val (request, response, result) = securedUrl.httpGet().responseString()
        val data = result.get()

        val mapper = ObjectMapper().registerModule(KotlinModule())
        val dataObj = mapper.readValue(data, T::class.java)

        return factoryMethod( dataObj )

    }

    inline fun <reified T:Any>  getAll(
            endpointBase: String,
            params: Map<String, String>,
            factoryMethod: (Any) -> T
    ): Array<T> {
        val url = "${config.url}/wp-json/wc/$apiVersion/$endpointBase"
        val signature = OAuthSignature.getAsQueryString(config, url, HttpMethod.GET, params)
        val securedUrl = "$url?$signature"

        val (request, response, result) = securedUrl.httpGet().responseString()
        val data = result.get()

        val mapper = ObjectMapper().registerModule(KotlinModule())

        val dataObj = mapper.readValue(data, Array<T>::class.java)

        return dataObj.map { factoryMethod(it) }.toTypedArray()
    }

    fun update(endpointBase: String, id: Int, `object`: Map<String, Any>): Map<*, *> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun delete(endpointBase: String, id: Int): Map<*, *> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

/**
 * Enum with basic WooCommerce endpoints
 */
enum class EndpointBaseType constructor(val value: String) {

    COUPONS("coupons"),
    CUSTOMERS("customers"),
    ORDERS("orders"),
    PRODUCTS("products"),
    PRODUCTS_ATTRIBUTES("products/attributes"),
    PRODUCTS_CATEGORIES("products/categories"),
    PRODUCTS_SHIPPING_CLASSES("products/shipping_classes"),
    PRODUCTS_TAGS("products/tags"),
    REPORTS("reports"),
    REPORTS_SALES("reports/sales"),
    REPORTS_TOP_SELLERS("reports/top_sellers"),
    TAXES("taxes"),
    TAXES_CLASSES("taxes/classes"),
    WEBHOOKS("webhooks")
}