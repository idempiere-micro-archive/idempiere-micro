import org.compiere.bo.CustomerProcessBase
import org.compiere.bo.CustomerProcessBaseResult
import org.compiere.crm.MBPartner
import org.idempiere.common.db.CConnection
import org.idempiere.common.db.Database
import org.idempiere.common.util.CLogger
import org.idempiere.common.util.DB
import org.idempiere.common.util.Env
import org.idempiere.common.util.Ini
import org.junit.Assert
import pg.org.compiere.db.DB_PostgreSQL
import java.util.Properties
import java.util.Random
import org.compiere.bo.updateCustomerCategory

abstract class BaseCustomerTest: BaseProcessTest() {
    abstract fun preparePartnerId(ctx: Properties, AD_CLIENT_ID: Int): Int?
    abstract fun getProcess(): CustomerProcessBase
    abstract fun runFinallyCleanup()

    // generates random string with small letters of a given length
    fun randomString(length: Int): String {
        // helper extension function to generate random string based on a set
        fun ClosedRange<Char>.randomString(length: Int) =
            (1..length)
                .map { (Random().nextInt(endInclusive.toInt() - start.toInt()) + start.toInt()).toChar() }
                .joinToString("")
        return ('a'..'z').randomString(length)
    }

    fun doTheTest() {
        Ini.getIni().isClient = false
        CLogger.getCLogger(TestUpdateCustomer::class.java)
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

        val bPartnerId = preparePartnerId(ctx, AD_CLIENT_ID)

        val bodyParams = arrayOf(
            "id" to if (bPartnerId==null) {0} else {bPartnerId},
            // legal name
            Pair( "bpName", "bp-" + randomString(10)), // name
            // description
            Pair( "description", "description"), // description
            // friendly name
            Pair( "value", "v-"+randomString(10) ), // value
            // DUNS (IC)
            Pair( "duns", "duns"),
            // Tax ID (VAT ID)
            Pair( "taxid", "taxid"),
            // place of business name
            Pair( "locationName", "locationName"),
            // place of business address
            Pair( "locationAddress", "locationAddress"), // address1
            // place of business city
            Pair( "locationCity", "locationCity"), // city
            // place of business postal
            Pair( "locationPostal", "1234567890"), // postal
            // place of business country code
            Pair( "locationCountryCode", "CZ" ), // countryCode
            // place of business phone
            Pair( "locationPhone", "locationPhone"), // phone
            // legal address
            Pair( "legalAddress", "legalAddress"),
            // legal city
            Pair( "legalCity", "legalCity"),
            // legal postal
            Pair( "legalPostal", "1234567890"),
            // telefon na sídlo
            Pair( "legalPhone", "legalPhone"),
            // order contact person
            Pair( "orderContactPerson", "orderContactPerson"),
            // decision maker
            Pair( "decisionMaker", "decisionMaker"),
            // invoicing contact
            Pair( "invoicingContact", "invoicingContact"),
            // already a customer
            Pair( "isCustomer", true),
            // customer category
            Pair( "customerCategoryId", 1),
            // flat discount
            Pair( "discount", 7),
            // account manager
            Pair( "salesRepId", 101),
            Pair( "locationCountryId", 166 ), // country_id "CZ"
            Pair( "legalCountryId", 166 ) // country_id "CZ"
        )
        val processResult = runProcess(DB_PostgreSQL(), getProcess(), bodyParams) as CustomerProcessBaseResult

        val newPartner2 = MBPartner.get( Env.getCtx(), processResult.C_BPartner_Id )

        try {
            Assert.assertEquals(bodyParams.first { it.first == "bpName" }.second, newPartner2.name)
            Assert.assertEquals(bodyParams.first { it.first == "discount" }.second.toString(), newPartner2.flatDiscount.toString())
            Assert.assertEquals(bodyParams.first { it.first == "description" }.second, newPartner2.description)
            Assert.assertEquals(bodyParams.first { it.first == "isCustomer" }.second, newPartner2.isCustomer)

            Assert.assertEquals(3, newPartner2.contacts.count())
            Assert.assertEquals(2, newPartner2.locations.count())
        } finally {
            updateCustomerCategory( null, newPartner2, DB.getConnectionRW() )
            newPartner2.delete(true)
            runFinallyCleanup()
        }
    }
}