package software.hsharp.db.h2.provider

import org.osgi.service.component.annotations.Component
import software.hsharp.api.icommon.ICConnection
import software.hsharp.api.icommon.IDatabase
import software.hsharp.api.icommon.IDatabaseSetup
import java.sql.Connection
import java.sql.Driver
import java.sql.DriverManager
import java.util.*

@Component
open class H2DB : IDatabase {
    protected val DRIVER : String = "org.h2.Driver"
    protected val DEFAULT_CONN_TEST_SQL : String = "SELECT 1+1"

    /** Connection Timeout in seconds   */
    protected val CONNECTION_TIMEOUT = 10;

    /** Driver                  */
    private val driverObj : org.h2.Driver = registerIfNeeded( org.h2.Driver() )
    private var driverRegistered : Boolean = false

    private val rand = Random()

    private fun registerIfNeeded( driverInst: org.h2.Driver ): org.h2.Driver {
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

    override val defaultSetupParameters : IDatabaseSetup
        get() = H2DatabaseSetup( "jdbc:h2:mem:idempiere;DB_CLOSE_DELAY=-1", "", "" )

    override fun setup(parameters: IDatabaseSetup) {
    }

    private var cnnString : String? = null

    override fun connect(connection: ICConnection) {
        cnnString= getConnectionURL(connection)
    }

    /**
     *  Get Database Connection String.
     *  Requirements:
     *      - createdb -E UNICODE compiere
     *  @param connection Connection Descriptor
     *  @return connection String
     */
    open fun getConnectionURL (connection: ICConnection) : String
    {
        dbName = connection.dbName;
        //  jdbc:h2:file:<filepath>

        return "jdbc:h2:file:$dbName"
    }

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

    override val CachedConnection: Connection
        get() {
            val url = cnnString!!
            val conn = DriverManager.getConnection(url)
            return conn
        }

    override fun cleanup(connection: Connection) {
    }

}
