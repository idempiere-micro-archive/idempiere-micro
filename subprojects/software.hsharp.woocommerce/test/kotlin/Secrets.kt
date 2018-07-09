import software.hsharp.woocommerce.impl.IConfig

class Secrets  : IConfig {
    override val url: String
        get() = Secrets.URL
    override val consumerKey: String
        get() = Secrets.consumerKey
    override val consumerSecret: String
        get() = Secrets.consumerSecret

    companion object {
        val URL : String = "http://localhost"
        val consumerKey : String = "ck_xxx123"
        val consumerSecret : String = "cs_abcdef567"
    }
}