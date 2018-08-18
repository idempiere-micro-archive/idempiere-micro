package software.hsharp.api.icommon

interface IDatabaseSetup {
    val connectionTimeout: Long
    val validationTimeout: Long
    val idleTimeout: Long
    val leakDetectionThreshold: Long
    val maxLifetime: Long
    val maximumPoolSize: Int
    val minimumIdle: Int
    val cachePrepStmts: Boolean
    val prepStmtCacheSize: Int
    val prepStmtCacheSqlLimit: Int
}
