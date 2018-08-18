package software.hsharp.api.icommon

import java.sql.Connection
import javax.sql.DataSource

interface IDatabase {
    /**
     * Get Status
     * @return status info or null if no local datasource available
     */
    val status: String

    /**
     * Get and register Database Driver
     * @return Driver
     */
    val driver: java.sql.Driver

    /**
     * Setup DataSource
     * @param parameters connection pool parameters
     */
    fun setup(parameters: IDatabaseSetup)

    val defaultSetupParameters: IDatabaseSetup

    /**
     * Connect
     * @param connection connection
     */
    fun connect(connection: ICConnection): DataSource?

    /**
     * 	Cleanup connections that are not used using the connection provided
     * @param connection connection
     */
    fun cleanup(connection: Connection)

    /**
     * Unable to obtain connection, try to do whatever you can to get one.
     * This must not ever fail.
     */
    fun fubar()
}
