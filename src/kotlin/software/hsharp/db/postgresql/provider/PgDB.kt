package software.hsharp.db.postgresql.provider

import org.osgi.service.component.annotations.Component
import software.hsharp.api.icommon.ICConnection
import software.hsharp.api.icommon.IDatabase
import software.hsharp.api.icommon.IDatabaseSetup
import java.sql.Connection
import java.sql.Driver
import java.sql.DriverManager
import java.sql.DriverManager.registerDriver
import java.sql.DriverManager.setLoginTimeout
import java.util.*

@Component
open class PgDB : IDatabase
{
    protected val DRIVER : String = "org.postgresql.Driver"
    protected val DEFAULT_CONN_TEST_SQL : String = "SELECT 1"

    /** Connection Timeout in seconds   */
    protected val CONNECTION_TIMEOUT = 10;

    /** Driver                  */
    private val driverObj : org.postgresql.Driver = registerIfNeeded( org.postgresql.Driver() )
    private var driverRegistered : Boolean = false

    private val rand = Random()

    private fun registerIfNeeded( driverInst: org.postgresql.Driver ): org.postgresql.Driver {
        if (!driverRegistered)
        {
            registerDriver (driverInst);
            setLoginTimeout (CONNECTION_TIMEOUT);
            driverRegistered = true;
        }
        return driverInst;
    }

    override val status: String
    get() = ""
    override val driver: Driver
    get() = driverObj

    override val defaultSetupParameters : IDatabaseSetup
    get() = PgDatabaseSetup( dataSourceName = "default", checkoutTimeout = 10, unreturnedConnectionTimeout = 10 )

    override fun setup(parameters: IDatabaseSetup) {
    }

    private var cnnString : String? = null

    override fun connect(connection: ICConnection) {
        cnnString= getConnectionURL(connection) + "&user=" +  connection.dbUid + "&password=" + connection.dbPwd
    }

    /**
     *  Get Database Connection String.
     *  Requirements:
     *      - createdb -E UNICODE compiere
     *  @param connection Connection Descriptor
     *  @return connection String
     */
    open fun getConnectionURL (connection:ICConnection ) : String
    {
        dbName = connection.dbName;
        //  jdbc:postgresql://hostname:portnumber/databasename?encoding=UNICODE
        val sb = StringBuilder("jdbc:postgresql://")
                .append(connection.dbHost)
                .append(":").append(connection.dbPort)
                .append("/").append(connection.dbName)
                .append("?encoding=UNICODE");
        if (connection.ssl)
            sb.append( "&ssl=true&sslmode=require" )

        return sb.toString();
    }   //  getConnectionString

    var dbName = ""


    open fun getNumBusyConnections() : Int {
        return 0
    }

    open fun getJdbcUrl() : String {
        return cnnString!!
    }

    /**
     * Close
     */
    open fun close() {

        try {
            //dataSource.close()
        } catch (e: Exception) {
        }
    }    //	close

    protected var maxRetries : Int = 5
    protected var minWaitSecs : Int = 2
    protected var maxWaitSecs : Int = 10

    override val CachedConnection: Connection
    get() {
        //Class.forName("org.postgresql.Driver");
        val url = cnnString!!
        val conn = DriverManager.getConnection(url)
        return conn
    }

    override fun cleanup(connection: Connection) {
        // try to kill the old idle connections first
        val killCommand = "SELECT count(pg_terminate_backend(pid)) FROM pg_stat_activity WHERE datname = '$dbName' AND pid <> pg_backend_pid() AND state = 'idle' AND state_change < current_timestamp - INTERVAL '1' MINUTE;"
        val stmt = connection.createStatement()
        val rs = stmt.executeQuery( killCommand )
        while (rs.next()) {
            val num = rs.getInt(1)
            println("**** KILLED $num idle transactions")
        }
        rs.close()
    }
}
