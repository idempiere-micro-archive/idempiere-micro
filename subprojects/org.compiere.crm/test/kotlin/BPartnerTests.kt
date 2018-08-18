import org.compiere.crm.*
import org.compiere.model.I_C_BPartner
import org.junit.Assert
import org.junit.Test
import org.idempiere.common.db.CConnection
import org.idempiere.common.db.Database
import org.idempiere.common.util.CLogger
import org.idempiere.common.util.DB
import org.idempiere.common.util.Env
import org.idempiere.common.util.Ini
import pg.org.compiere.db.DB_PostgreSQL
import java.util.Random

// generates random string with small letters of a given length
fun randomString(length: Int): String {
    // helper extension function to generate random string based on a set
    fun ClosedRange<Char>.randomString(length: Int) =
        (1..length)
            .map { (Random().nextInt(endInclusive.toInt() - start.toInt()) + start.toInt()).toChar() }
            .joinToString("")
    return ('a'..'z').randomString(length)
}

class BPartnerTests : BaseProcessTest() {

    @Test
    fun loading_saving_finding_business_partner_work() {
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
        val AD_USER_ID = 104
        val AD_USER_ID_s = AD_USER_ID.toString()
        ctx.setProperty(Env.AD_USER_ID, AD_USER_ID_s )
        Env.setContext(ctx, Env.AD_USER_ID, AD_USER_ID_s )

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
        Assert.assertEquals( 1, newPartner2.locations.count() )

        val bodyParams = arrayOf(
            "Full" to true,
            "Search" to value
        )
        val result = runProcess(DB_PostgreSQL(), Find(), bodyParams) as FindResult
        Assert.assertEquals(1,result.rows.count())
        val bp = result.rows[0] as BPartnerWithActivity
        Assert.assertNotNull(bp)
        Assert.assertNotNull(bp.BPartner)
        val bpp = bp.BPartner
        Assert.assertEquals(name, bpp.name)
        Assert.assertEquals(1,bpp.Locations.count())

        val result2 = runProcess(DB_PostgreSQL(), AbandonedBPartners(), bodyParams) as FindResult
        Assert.assertTrue(result2.rows.count() > 0)
        val bp2 = result2.rows.first { (it as BPartnerWithActivity).BPartner.value == value } as BPartnerWithActivity
        Assert.assertNotNull(bp2)
        Assert.assertNotNull(bp2.BPartner)
        val bpp2 = bp2.BPartner
        Assert.assertEquals(name, bpp2.name)
        Assert.assertEquals(1,bpp2.Locations.count())

        newPartner2.salesRep_ID = AD_USER_ID
        newPartner2.save()

        val result3 = runProcess(DB_PostgreSQL(), MyBPartners(), bodyParams) as FindResult
        Assert.assertTrue(result3.rows.count() > 0)
        val bp3 = result3.rows.first { (it as BPartnerWithActivity).BPartner.value == value } as BPartnerWithActivity
        Assert.assertNotNull(bp3)
        Assert.assertNotNull(bp3.BPartner)
        val bpp3 = bp3.BPartner
        Assert.assertEquals(name, bpp3.name)
        Assert.assertEquals(1,bpp3.Locations.count())

        val result4 = runProcess(DB_PostgreSQL(), ForgottenBPartners(), bodyParams) as FindResult
        Assert.assertTrue(result4.rows.count() > 0)
        val bp4 = result4.rows.first { (it as BPartnerWithActivity).BPartner.value == value } as BPartnerWithActivity
        Assert.assertNotNull(bp4)
        Assert.assertNotNull(bp4.BPartner)
        val bpp4 = bp4.BPartner
        Assert.assertEquals(name, bpp4.name)
        Assert.assertEquals(1,bpp4.Locations.count())

        newPartner.delete(true)

        location.delete(true)
    }
}