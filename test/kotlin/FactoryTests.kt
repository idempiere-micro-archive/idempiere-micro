import org.compiere.model.I_C_BPartner
import org.compiere.model.I_C_ContactActivity
import org.compiere.orm.DefaultModelFactory
import org.compiere.orm.IModelFactory
import org.idempiere.common.db.CConnection
import org.idempiere.common.db.Database
import org.idempiere.common.util.CLogger
import org.idempiere.common.util.DB
import org.idempiere.common.util.Env
import org.idempiere.common.util.Ini
import org.junit.Assert
import org.junit.Test
import pg.org.compiere.db.DB_PostgreSQL

class FactoryTests {
    @Test
    fun getUsingDefaultModelFactoryById() {
        Ini.getIni().isClient = false
        CLogger.getCLogger(BPartnerTests::class.java)
        Ini.getIni().properties
        val db = Database()
        db.setDatabase(DB_PostgreSQL())
        DB.setDBTarget(CConnection.get(null))
        DB.isConnected()

        val ctx = Env.getCtx()
        val AD_CLIENT_ID = 11
        val AD_CLIENT_ID_s = AD_CLIENT_ID.toString()
        ctx.setProperty(Env.AD_CLIENT_ID, AD_CLIENT_ID_s )
        Env.setContext(ctx, Env.AD_CLIENT_ID, AD_CLIENT_ID_s )

        val modelFactory : IModelFactory = DefaultModelFactory()
        val result = modelFactory.getPO( I_C_BPartner.Table_Name, 118, "pokus")
        println( result )
        Assert.assertNotNull(result);
    }

    @Test
    fun getUsingDefaultModelFactoryFromRS() {
        Ini.getIni().isClient = false
        CLogger.getCLogger(BPartnerTests::class.java)
        Ini.getIni().properties
        val db = Database()
        db.setDatabase(DB_PostgreSQL())
        DB.setDBTarget(CConnection.get(null))
        DB.isConnected()

        val ctx = Env.getCtx()
        val AD_CLIENT_ID = 11
        val AD_CLIENT_ID_s = AD_CLIENT_ID.toString()
        ctx.setProperty(Env.AD_CLIENT_ID, AD_CLIENT_ID_s )
        Env.setContext(ctx, Env.AD_CLIENT_ID, AD_CLIENT_ID_s )

        val tableName = I_C_BPartner.Table_Name
        val AD_ORG_ID = 0
        val id = 118

        val sql =
            ("SELECT * FROM adempiere.\"${tableName}\" " +
                "WHERE (\"${tableName}\".ad_client_id = ? OR \"${tableName}\".ad_client_id=0) " +
                "AND (\"${tableName}\".ad_org_id = ? OR \"${tableName}\".ad_org_id=0) " +
                "AND (\"${tableName}_ID\"=?);"
                ).toLowerCase()
        println ( "SQL:$sql" )
        val cnn = DB.getConnectionRO()
        val statement = cnn.prepareStatement(sql)
        statement.setInt(1, AD_CLIENT_ID)
        statement.setInt(2, AD_ORG_ID)
        statement.setInt(3, id)
        val rs = statement.executeQuery()
        rs.next()

        val modelFactory : IModelFactory = DefaultModelFactory()
        val result = modelFactory.getPO( tableName, rs, "pokus")
        println( result )
        Assert.assertNotNull(result)
        Assert.assertEquals(id, result._ID)

        cnn.close()
    }
}