import software.hsharp.woocommerce.impl.IConfig

class Secrets  : IConfig {
    override val url: String
        get() = Secrets.URL
    override val consumerKey: String
        get() = Secrets.consumerKey
    override val consumerSecret: String
        get() = Secrets.consumerSecret

    companion object {
        val URL : String = System.getenv("WOOCOMMERCE_URL")
        val consumerKey : String = System.getenv("WOOCOMMERCE_KEY")
        val consumerSecret : String = System.getenv("WOOCOMMERCE_SECRET")
    }
}