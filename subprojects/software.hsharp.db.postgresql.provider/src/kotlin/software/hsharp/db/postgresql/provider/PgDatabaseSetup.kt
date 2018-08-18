package software.hsharp.db.postgresql.provider

import software.hsharp.api.icommon.IDatabaseSetup
import java.util.concurrent.TimeUnit

data class PgDatabaseSetup(
    override val connectionTimeout: Long = TimeUnit.SECONDS.toMillis(10), // 3 sec.
    override val validationTimeout: Long = TimeUnit.SECONDS.toMillis(5), // 1 sec.
    override val idleTimeout: Long = TimeUnit.MINUTES.toMillis(1), // 5 min.
    override val leakDetectionThreshold: Long = TimeUnit.SECONDS.toMillis(60), // 60 sec.
    override val maxLifetime: Long = TimeUnit.MINUTES.toMillis(9), // 9 min.
    override val maximumPoolSize: Int = 10,
    override val minimumIdle: Int = 2, // minimum number connection to leave open even if idle,
    override val cachePrepStmts: Boolean = true,
    override val prepStmtCacheSize: Int = 250,
    override val prepStmtCacheSqlLimit: Int = 2048
) : IDatabaseSetup