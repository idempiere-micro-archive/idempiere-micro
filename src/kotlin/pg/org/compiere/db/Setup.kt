package pg.org.compiere.db

import com.mchange.v2.c3p0.ComboPooledDataSource
import software.hsharp.api.icommon.ICConnection
import software.hsharp.api.icommon.IDatabaseSetup
import software.hsharp.db.postgresql.provider.PgDB
import software.hsharp.db.postgresql.provider.PgDatabaseSetup
import java.sql.Driver
import java.sql.DriverManager
import java.util.*

open class PooledPgDB: PgDB() {
    private var dataSourceObj : ComboPooledDataSource? = null

    /** Driver                  */
    private val driverObj : org.postgresql.Driver = registerIfNeeded( org.postgresql.Driver() )
    private var driverRegistered : Boolean = false

    private val rand = Random()

    private fun registerIfNeeded( driverInst: org.postgresql.Driver ): org.postgresql.Driver {
        if (!driverRegistered)
        {
            DriverManager.registerDriver(driverInst);
            DriverManager.setLoginTimeout(CONNECTION_TIMEOUT);
            driverRegistered = true;
        }
        return driverInst;
    }

    override val status: String
        get() = ""
    override val driver: Driver
        get() = driverObj


    protected val dataSource: ComboPooledDataSource
        get() {
            if ( dataSourceObj == null ) dataSourceObj = ComboPooledDataSource()
            return dataSourceObj!!
        }

    override fun setup(parameters: IDatabaseSetup) {
        val params : PgDatabaseSetup = parameters as PgDatabaseSetup
        dataSource.dataSourceName = params.dataSourceName
        dataSource.driverClass = DRIVER
        dataSource.preferredTestQuery = DEFAULT_CONN_TEST_SQL
        dataSource.idleConnectionTestPeriod = params.idleConnectionTestPeriod
        dataSource.maxIdleTimeExcessConnections = params.maxIdleTimeExcessConnections
        dataSource.maxIdleTime = params.maxIdleTime
        dataSource.isTestConnectionOnCheckin = params.testConnectionOnCheckin
        dataSource.isTestConnectionOnCheckout = params.testConnectionOnCheckout
        dataSource.acquireRetryAttempts = params.acquireRetryAttempts
        if (params.checkoutTimeout > 0)
            dataSource.checkoutTimeout = params.checkoutTimeout

        dataSource.initialPoolSize = params.initialPoolSize
        dataSource.initialPoolSize = params.initialPoolSize
        dataSource.minPoolSize = params.minPoolSize
        dataSource.maxPoolSize = params.maxPoolSize

        dataSource.maxStatementsPerConnection = params.maxStatementsPerConnection

        if (params.unreturnedConnectionTimeout > 0) {
            dataSource.unreturnedConnectionTimeout = 1200
            dataSource.isDebugUnreturnedConnectionStackTraces = true
        }

        maxRetries = params.maxRetries
        minWaitSecs = params.minWaitSecs
        maxWaitSecs = params.maxWaitSecs
    }

    override fun connect(connection: ICConnection) {
        dataSource.jdbcUrl = getConnectionURL(connection)
        dataSource.user = connection.dbUid
        dataSource.password = connection.dbPwd
    }

    override fun getNumBusyConnections() : Int {
        return dataSource.numBusyConnections
    }

    override fun getJdbcUrl() : String {
        return dataSource.jdbcUrl
    }
}