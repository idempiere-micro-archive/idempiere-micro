package software.hsharp.woocommerce.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.kittinunf.fuel.httpGet
import software.hsharp.woocommerce.oauth.OAuthSignature

interface IConfig {
    val url: String
    val consumerKey: String
    val consumerSecret: String
}

data class Config(
    override val url: String,
    override val consumerKey: String,
    override val consumerSecret: String
) : IConfig

/**
 * Enum that represents WooCommerce API versions
 */
enum class ApiVersionType constructor(val value: String) {
    V1("v1"),
    V2("v2")
}

open class WooCommerceBase(val config: IConfig, val apiVersion: ApiVersionType) {
    inline fun <reified T : Any> get(
        endpointBase: String,
        id: Int,
        factoryMethod: (Any) -> T
    ): T {
        val url = "${config.url}/wp-json/wc/$apiVersion/$endpointBase/$id"
        val signature = OAuthSignature.getAsQueryString(config)
        val securedUrl = "$url?$signature"

        val (_, _, result) = securedUrl.httpGet().responseString()
        val data = result.get()

        val mapper = ObjectMapper().registerModule(KotlinModule())
        val dataObj = mapper.readValue(data, T::class.java)

        return factoryMethod(dataObj)
    }

    inline fun <reified T : Any, reified U : Any> getAll(
        endpointBase: String,
        factoryMethod: (T) -> U
    ): Array<U> {
        val url = "${config.url}/wp-json/wc/$apiVersion/$endpointBase"
        val signature = OAuthSignature.getAsQueryString(config)
        val securedUrl = "$url?$signature"

        val (_, _, result) = securedUrl.httpGet().responseString()
        val data = result.get()

        val mapper = ObjectMapper().registerModule(KotlinModule())

        val dataObj = mapper.readValue(data, Array<T>::class.java)

        return dataObj.map { factoryMethod(it) }.toTypedArray()
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