import org.compiere.bo.CustomerProcessBase
import org.compiere.bo.UpdateCustomer
import org.compiere.crm.MBPartner
import org.compiere.crm.MBPartnerLocation
import org.compiere.crm.MCountry
import org.compiere.crm.MLocation
import org.compiere.crm.MRegion
import org.junit.Test
import java.util.Properties

class TestUpdateCustomer : BaseCustomerTest() {
    var location: MLocation? = null

    override fun preparePartnerId(ctx: Properties, AD_CLIENT_ID: Int): Int? {
        val newPartner = MBPartner.getTemplate(ctx, AD_CLIENT_ID)
        newPartner.setName("bp-o-" + randomString(10))
        newPartner.setValue("v-o-"+randomString(10))
        newPartner.save()

        val defaultCountry = MCountry.getDefault(ctx)
        val defaultRegion = MRegion.getDefault(ctx)
        location = MLocation( defaultCountry, defaultRegion )
        location!!.save()
        val partnerLocation = MBPartnerLocation( newPartner )
        partnerLocation.c_Location_ID = location!!.c_Location_ID
        partnerLocation.save()

        return newPartner.c_BPartner_ID
    }

    override fun getProcess(): CustomerProcessBase {
        return UpdateCustomer()
    }

    override fun runFinallyCleanup() {
        location!!.delete(true)
    }

    @Test
    fun test1() {
        doTheTest()
    }
}
