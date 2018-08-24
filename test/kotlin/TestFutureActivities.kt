import org.junit.Assert
import org.compiere.bo.MyFutureContactActivities
import org.compiere.crm.*
import org.compiere.model.I_C_BPartner
import org.idempiere.common.db.CConnection
import org.idempiere.common.db.Database
import org.idempiere.common.util.CLogger
import org.idempiere.common.util.DB
import org.idempiere.common.util.Env
import org.idempiere.common.util.Ini
import org.junit.Test
import pg.org.compiere.db.DB_PostgreSQL
import java.util.*

// generates random string with small letters of a given length
fun randomString(length: Int): String {
    // helper extension function to generate random string based on a set
    fun ClosedRange<Char>.randomString(length: Int) =
            (1..length)
                    .map { (Random().nextInt(endInclusive.toInt() - start.toInt()) + start.toInt()).toChar() }
                    .joinToString("")
    return ('a'..'z').randomString(length)
}

class TestFutureActivities : BaseProcessTest() {
    @Test
    fun `gardenuser has at least one contact activity`() {
        Ini.getIni().isClient = false
        CLogger.getCLogger(TestFutureActivities::class.java)
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
        val AD_USER_ID = 102 // gardenuser
        val AD_USER_ID_s = AD_USER_ID.toString()
        ctx.setProperty(Env.AD_USER_ID, AD_USER_ID_s )
        Env.setContext(ctx, Env.AD_USER_ID, AD_USER_ID_s )

        val processResult = runProcess(DB_PostgreSQL(), MyFutureContactActivities(), arrayOf()) as MyFutureContactActivities.Result
        Assert.assertTrue( processResult.activities.count() > 0 )
    }

    @Test
    fun `CRM processes runs` () {
        Ini.getIni().isClient = false
        CLogger.getCLogger(TestFutureActivities::class.java)
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
        val AD_USER_ID = 104
        val AD_USER_ID_s = AD_USER_ID.toString()
        ctx.setProperty(Env.AD_USER_ID, AD_USER_ID_s )
        Env.setContext(ctx, Env.AD_USER_ID, AD_USER_ID_s )

        val id = 118
        val partner = MBPartner.get( Env.getCtx(), id )

        org.junit.Assert.assertEquals( id, partner.c_BPartner_ID)
        org.junit.Assert.assertEquals( "JoeBlock", partner.value)
        org.junit.Assert.assertEquals( "Joe Block", partner.name)

        val partner2 : I_C_BPartner = partner as I_C_BPartner

        val newValue = "JoeBlock*"
        partner2.setValue( newValue )
        partner2.save()

        val partner3 = MBPartner.get( Env.getCtx(), id )

        org.junit.Assert.assertEquals( id, partner3.c_BPartner_ID)
        org.junit.Assert.assertEquals( newValue, partner3.value)
        org.junit.Assert.assertEquals( "Joe Block", partner3.name)

        partner2.setValue( "JoeBlock" )
        partner2.save()

        val newPartner = MBPartner.getTemplate(ctx, AD_CLIENT_ID)
        val name = "Test "+randomString(10)
        newPartner.setName(name)
        val value = "t-"+randomString(5)
        newPartner.setValue(value)
        newPartner.save()

        val defaultCountry = MCountry.getDefault(ctx)
        val defaultRegion = MRegion.getDefault(ctx)
        val location = MLocation( defaultCountry, defaultRegion )
        location.save()
        val partnerLocation = MBPartnerLocation( newPartner )
        partnerLocation.c_Location_ID = location.c_Location_ID
        partnerLocation.save()

        val newPartner2 = MBPartner.get( Env.getCtx(), newPartner.c_BPartner_ID )
        org.junit.Assert.assertEquals( 1, newPartner2.locations.count() )

        val bodyParams = arrayOf(
                "Full" to true,
                "Search" to value
        )
        val result = runProcess(DB_PostgreSQL(), Find(), bodyParams) as FindResult
        org.junit.Assert.assertEquals(1,result.rows.count())
        val bp = result.rows[0] as BPartnerWithActivity
        org.junit.Assert.assertNotNull(bp)
        org.junit.Assert.assertNotNull(bp.BPartner)
        val bpp = bp.BPartner
        org.junit.Assert.assertEquals(name, bpp.name)
        org.junit.Assert.assertEquals(1,bpp.Locations.count())

        val result2 = runProcess(DB_PostgreSQL(), AbandonedBPartners(), bodyParams) as FindResult
        org.junit.Assert.assertTrue(result2.rows.count() > 0)
        val bp2 = result2.rows.first { (it as BPartnerWithActivity).BPartner.value == value } as BPartnerWithActivity
        org.junit.Assert.assertNotNull(bp2)
        org.junit.Assert.assertNotNull(bp2.BPartner)
        val bpp2 = bp2.BPartner
        org.junit.Assert.assertEquals(name, bpp2.name)
        org.junit.Assert.assertEquals(1,bpp2.Locations.count())

        newPartner2.salesRep_ID = AD_USER_ID
        newPartner2.save()

        val result3 = runProcess(DB_PostgreSQL(), MyBPartners(), bodyParams) as FindResult
        org.junit.Assert.assertTrue(result3.rows.count() > 0)
        val bp3 = result3.rows.first { (it as BPartnerWithActivity).BPartner.value == value } as BPartnerWithActivity
        org.junit.Assert.assertNotNull(bp3)
        org.junit.Assert.assertNotNull(bp3.BPartner)
        val bpp3 = bp3.BPartner
        org.junit.Assert.assertEquals(name, bpp3.name)
        org.junit.Assert.assertEquals(1,bpp3.Locations.count())

        val result4 = runProcess(DB_PostgreSQL(), ForgottenBPartners(), bodyParams) as FindResult
        org.junit.Assert.assertTrue(result4.rows.count() > 0)
        val bp4 = result4.rows.first { (it as BPartnerWithActivity).BPartner.value == value } as BPartnerWithActivity
        org.junit.Assert.assertNotNull(bp4)
        org.junit.Assert.assertNotNull(bp4.BPartner)
        val bpp4 = bp4.BPartner
        org.junit.Assert.assertEquals(name, bpp4.name)
        org.junit.Assert.assertEquals(1,bpp4.Locations.count())

        newPartner.delete(true)

        location.delete(true)
    }
}