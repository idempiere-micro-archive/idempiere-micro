package software.hsharp.db.postgresql.provider

import software.hsharp.api.icommon.IDatabaseSetup

data class PgDatabaseSetup(
        val dataSourceName: String,
        val idleConnectionTestPeriod: Int = 1200,
        val maxIdleTimeExcessConnections: Int = 1200,
        val maxIdleTime: Int = 1200,
        val testConnectionOnCheckin: Boolean = false,
        val testConnectionOnCheckout: Boolean = false,
        val acquireRetryAttempts: Int = 10,
        val checkoutTimeout: Int,
        val initialPoolSize: Int = 10,
        val minPoolSize: Int = 5,
        val maxPoolSize: Int = 90,
        val maxStatementsPerConnection: Int = 2,
        val unreturnedConnectionTimeout : Int,
        val maxRetries : Int = 5,
        val minWaitSecs : Int = 2,
        val maxWaitSecs : Int = 10
) : IDatabaseSetup