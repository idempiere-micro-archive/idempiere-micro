package software.hsharp.woocommerce

import software.hsharp.woocommerce.impl.IConfig

class Secrets  : IConfig {
    override val url: String
        get() = URL
    override val consumerKey: String
        get() = Companion.consumerKey
    override val consumerSecret: String
        get() = Companion.consumerSecret

    companion object {
        val URL : String = System.getenv("WOOCOMMERCE_URL")
        val consumerKey : String = System.getenv("WOOCOMMERCE_KEY")
        val consumerSecret : String = System.getenv("WOOCOMMERCE_SECRET")
    }
}