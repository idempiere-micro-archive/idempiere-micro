import org.junit.Test
import software.hsharp.api.icommon.ICConnection
import software.hsharp.db.h2.provider.H2DB

data class TestH2DBConnection(override val dbName: String) : ICConnection {
    override val dbHost: String
        get() = ""
    override val dbPort: Int
        get() = 0
    override val dbUid: String
        get() = ""
    override val dbPwd: String
        get() = ""
    override val ssl: Boolean
        get() = false

}

class TestH2DB {
    @Test
    fun test1() {
        val db= H2DB()
        val login = TestH2DBConnection( "./../../tmp/test" )

        db.setup( db.defaultSetupParameters )
        db.connect( login )
        val conn = db.CachedConnection

        //STEP 3: Execute a query
        println("Creating table in given database...")
        val stmt = conn.createStatement()
        val sql = "CREATE TABLE   REGISTRATION " +
                "(id INTEGER not NULL, " +
                " first VARCHAR(255), " +
                " last VARCHAR(255), " +
                " age INTEGER, " +
                " PRIMARY KEY ( id ))"
        stmt.executeUpdate(sql)
        println("Created table in given database...")

        // STEP 4: Clean-up environment
        stmt.close()
        conn.close()
    }
}