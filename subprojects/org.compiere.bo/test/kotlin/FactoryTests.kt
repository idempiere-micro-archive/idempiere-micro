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
    fun getUsingDefaultModelFactoryFromRSSuperComplex() {
        Ini.getIni().isClient = false
        CLogger.getCLogger(FactoryTests::class.java)
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

        val tableName = "C_BPartner"
        val id = 118

        val sql = """select *, C_ContactActivity_ID as activity_C_ContactActivity_ID from adempiere.bpartner_v
            where c_contactactivity_id is not null
            order by 1, c_contactactivity_id asc"""

        println ( "SQL:$sql" )
        val cnn = DB.getConnectionRO()
        val statement = cnn.prepareStatement(sql)
        val rs = statement.executeQuery()
        rs.next()

        val modelFactory : IModelFactory = DefaultModelFactory()
        val result = modelFactory.getPO( tableName, rs, "pokus")
        val result2 = modelFactory.getPO("C_ContactActivity", rs, "pokus", "activity_") as I_C_ContactActivity
        println( result )
        println( result2 );
        Assert.assertNotNull(result);
        Assert.assertNotNull(result2);
        Assert.assertEquals(id, result._ID)
        Assert.assertEquals(123, result2.c_ContactActivity_ID)

        cnn.close()
    }


    @Test
    fun getUsingDefaultModelFactoryFromRSComplex() {
        Ini.getIni().isClient = false
        CLogger.getCLogger(FactoryTests::class.java)
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

        val tableName = "C_BPartner"
        val AD_ORG_ID = 0
        val id = 118

        val sql =
                ("SELECT * FROM adempiere.\"${tableName}\", adempiere.M_PriceList " +
                        "WHERE (\"${tableName}\".ad_client_id = ? OR \"${tableName}\".ad_client_id=0) " +
                        "AND (\"${tableName}\".ad_org_id = ? OR \"${tableName}\".ad_org_id=0) " +
                        "AND (\"${tableName}_ID\"=?) AND (M_PriceList.M_PriceList_ID = C_BPartner.M_PriceList_ID);"
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
        val result2 = modelFactory.getPO( "M_PriceList", rs, "pokus")
        println( result )
        println( result2 )
        Assert.assertNotNull(result)
        Assert.assertNotNull(result2)
        Assert.assertEquals(id, result._ID)
        Assert.assertEquals(101, result2._ID)

        cnn.close()
    }

}