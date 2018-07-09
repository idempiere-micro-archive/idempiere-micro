import org.compiere.crm.*
import org.compiere.model.I_C_BPartner
import org.compiere.orm.DefaultModelFactory
import org.compiere.orm.IModelFactory
import org.junit.Assert
import org.junit.Test
import org.idempiere.common.db.CConnection
import org.idempiere.common.db.Database
import org.idempiere.common.util.CLogger
import org.idempiere.common.util.DB
import org.idempiere.common.util.Env
import org.idempiere.common.util.Ini
import pg.org.compiere.db.DB_PostgreSQL

class BPartnerTests {
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
        val result = modelFactory.getPO( "C_BPartner", 118, "pokus")
        println( result );
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

        val tableName = "C_BPartner";
        val AD_ORG_ID = 0;
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
        println( result );
        Assert.assertNotNull(result);
        Assert.assertEquals(id, result._ID)

        cnn.close()
    }

    @Test
    fun getUsingDefaultModelFactoryFromRSComplex() {
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

        val tableName = "C_BPartner";
        val AD_ORG_ID = 0;
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
        println( result );
        println( result2 );
        Assert.assertNotNull(result);
        Assert.assertNotNull(result2);
        Assert.assertEquals(id, result._ID)
        Assert.assertEquals(101, result2._ID)

        cnn.close()
    }

    @Test
    fun loading_saving_business_partner_work() {
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

        val id = 118
        val partner = MBPartner.get( Env.getCtx(), id )

        Assert.assertEquals( id, partner.c_BPartner_ID)
        Assert.assertEquals( "JoeBlock", partner.value)
        Assert.assertEquals( "Joe Block", partner.name)

        val partner2 : I_C_BPartner = partner as I_C_BPartner

        val newValue = "JoeBlock*"
        partner2.setValue( newValue )
        partner2.save()

        val partner3 = MBPartner.get( Env.getCtx(), id )

        Assert.assertEquals( id, partner3.c_BPartner_ID)
        Assert.assertEquals( newValue, partner3.value)
        Assert.assertEquals( "Joe Block", partner3.name)

        partner2.setValue( "JoeBlock" )
        partner2.save()

        val newPartner = MBPartner.getTemplate(ctx, AD_CLIENT_ID)
        newPartner.setName("Test 123")
        newPartner.setValue("Test123")
        newPartner.save()

        val defaultCountry = MCountry.getDefault(ctx)
        val defaultRegion = MRegion.getDefault(ctx)
        val location = MLocation( defaultCountry, defaultRegion )
        location.save()
        val partnerLocation = MBPartnerLocation( newPartner )
        partnerLocation.c_Location_ID = location.c_Location_ID
        partnerLocation.save()

        val newPartner2 = MBPartner.get( Env.getCtx(), newPartner.c_BPartner_ID )
        Assert.assertEquals( 1, newPartner2.locations.count() )

        newPartner.delete(true)

        location.delete(true)
    }
}