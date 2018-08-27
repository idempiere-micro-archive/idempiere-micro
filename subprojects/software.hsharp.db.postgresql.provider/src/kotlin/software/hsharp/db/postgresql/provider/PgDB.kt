package software.hsharp.db.postgresql.provider

import software.hsharp.api.icommon.ICConnection
import software.hsharp.api.icommon.IDatabaseSetup
import java.sql.Connection
import java.sql.Driver
import java.sql.DriverManager.registerDriver
import java.sql.DriverManager.setLoginTimeout
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.pool.HikariPool
import java.sql.DriverManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import javax.sql.DataSource

open class PgDB {
    fun fubar() {
        println("Trying to restart local PostgreSQL.")
        val pb = ProcessBuilder("bin/restartpgsql.sh")
        try {
            pb.start()
            println("Succeeded.")
        } catch (e: Exception) {
            println("Failed:")
            e.printStackTrace()
        }
    }

    /** Connection Timeout in seconds   */
    protected val CONNECTION_TIMEOUT = 10

    /** Driver                  */
    private val driverObj: org.postgresql.Driver = registerIfNeeded(org.postgresql.Driver())
    private var driverRegistered: Boolean = false

    private fun registerIfNeeded(driverInst: org.postgresql.Driver): org.postgresql.Driver {
        if (!driverRegistered) {
            registerDriver(driverInst)
            setLoginTimeout(CONNECTION_TIMEOUT)
            driverRegistered = true
        }
        return driverInst
    }

    val status: String
    get() = ""
    val driver: Driver
    get() = driverObj

    final val defaultSetupParameters: IDatabaseSetup
    get() = PgDatabaseSetup()

    private var parameters: IDatabaseSetup = defaultSetupParameters

    fun setup(parameters: IDatabaseSetup) {
        this.parameters = parameters
    }

    private var connectionparams: ICConnection? = null
    companion object {
        val cachedDs: ConcurrentMap<String, HikariDataSource> = ConcurrentHashMap()
    }

    fun connect(connection: ICConnection): DataSource? {
        connectionparams = connection
        val jdbcUrl = getConnectionURL(connection)
        val username = connection.dbUid
        val password = connection.dbPwd
        val key = "$jdbcUrl|$username|$password"
        val result = cachedDs[key]
        if (result == null) {
            try {
                // Test first
                val cnnString = jdbcUrl + "&user=" + connection.dbUid + "&password=" + connection.dbPwd
                try {
                    val conn = DriverManager.getConnection(cnnString)
                    conn.close()
                } catch (e: org.postgresql.util.PSQLException) {
                    // not logged in, go out
                    return null
                }
                // if works return the real pooled DB
                val config = HikariConfig()
                config.jdbcUrl = getConnectionURL(connection)
                config.username = connection.dbUid
                config.password = connection.dbPwd

                config.connectionTimeout = parameters.connectionTimeout
                config.validationTimeout = parameters.validationTimeout
                config.idleTimeout = parameters.idleTimeout
                config.leakDetectionThreshold = parameters.leakDetectionThreshold
                config.maxLifetime = parameters.maxLifetime
                config.maximumPoolSize = parameters.maximumPoolSize
                config.minimumIdle = parameters.minimumIdle

                config.addDataSourceProperty("cachePrepStmts", parameters.cachePrepStmts.toString())
                config.addDataSourceProperty("prepStmtCacheSize", parameters.prepStmtCacheSize.toString())
                config.addDataSourceProperty("prepStmtCacheSqlLimit", parameters.prepStmtCacheSqlLimit.toString())

                val ds = HikariDataSource(config)
                cachedDs[key] = ds
                return ds
            } catch (ex: HikariPool.PoolInitializationException) {
                // invalid username or password
                return null
            }
        } else {
            return result
        }
    }

    /**
     *  Get Database Connection String.
     *  Requirements:
     *      - createdb -E UNICODE compiere
     *  @param connection Connection Descriptor
     *  @return connection String
     */
    open fun getConnectionURL(connection: ICConnection): String
    {
        dbName = connection.dbName
        //  jdbc:postgresql://hostname:portnumber/databasename?encoding=UNICODE
        val sb = StringBuilder("jdbc:postgresql://")
                .append(connection.dbHost)
                .append(":").append(connection.dbPort)
                .append("/").append(connection.dbName)
                .append("?encoding=UNICODE")
        if (connection.ssl)
            sb.append("&ssl=true&sslmode=require")

        return sb.toString()
    } //  getConnectionString

    var dbName = ""

    open fun getNumBusyConnections(): Int {
        return 0
    }

    open fun getJdbcUrl(): String {
        return getConnectionURL(connectionparams!!)
    }

    /**
     * Close
     */
    open fun close() {

        try {
            // dataSource.close()
        } catch (e: Exception) {
        }
    } // 	close

    fun cleanup(connection: Connection) {
        // try to kill the old idle connections first
        // we take the number that should be handled by HikariCP and just add 10% of minutes on the top of that
        val maxLifeTimeInMinutes = (parameters.maxLifetime / 1000 / 60 * 1.1).toInt()
        val killCommand = "SELECT count(pg_terminate_backend(pid)) FROM pg_stat_activity WHERE datname = '$dbName' AND pid <> pg_backend_pid() AND state = 'idle' AND state_change < current_timestamp - INTERVAL '$maxLifeTimeInMinutes' MINUTE;"
        val stmt = connection.createStatement()
        val rs = stmt.executeQuery(killCommand)
        while (rs.next()) {
            val num = rs.getInt(1)
            println("**** KILLED $num idle transactions")
        }
        rs.close()
    }
}
