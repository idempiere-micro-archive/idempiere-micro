package org.compiere.crm

import org.compiere.model.I_C_BPartner
import org.compiere.model.I_C_ContactActivity
import org.compiere.orm.DefaultModelFactory
import org.compiere.orm.IModelFactory
import org.compiere.process.SvrProcess
import org.idempiere.common.util.DB
import org.idempiere.common.util.Env
import java.math.BigDecimal

data class DetailResult( val bPartner : I_C_BPartner?, val categoryName : String?, val activities : MutableList<I_C_ContactActivity> ) : java.io.Serializable

class Detail : SvrProcess() {
    var businessPartnerId : Int = 0
    var AD_CLIENT_ID = 0 //AD_Client_ID
    var AD_ORG_ID = 0 //AD_Org_ID

    override fun prepare() {
        for (para in parameter) {
            if ( para.parameterName == "BusinessPartnerId" ) {
                businessPartnerId = para.parameterAsInt
            } else if ( para.parameterName == "AD_Client_ID" ) {
                AD_CLIENT_ID = para.parameterAsInt
            } else if ( para.parameterName == "AD_Org_ID" ) {
                AD_ORG_ID = para.parameterAsInt
            } else println( "unknown parameter ${para.parameterName}" )
        }
    }

    override fun doIt(): String {
        val pi = processInfo

        val ctx = Env.getCtx()

        val sql =
                """
select * from bpartner_detail_v where c_bpartner_id = ?
and ad_client_id IN (0, ?) and ( ad_org_id IN (0,?) or ? = 0) and isactive = 'Y'
""".trimIndent()

        val cnn = DB.getConnectionRO()
        val statement = cnn.prepareStatement(sql)
        statement.setInt(1, businessPartnerId)
        statement.setInt(2, AD_CLIENT_ID)
        statement.setInt(3, AD_ORG_ID)
        statement.setInt(4, AD_ORG_ID)

        val rs = statement.executeQuery()

        val modelFactory : IModelFactory = DefaultModelFactory()
        var bpartner : I_C_BPartner? = null
        var categoryName : String? = null

        val activities = mutableListOf<I_C_ContactActivity>()

        while(rs.next()) {
            if ( bpartner == null) {
                bpartner = modelFactory.getPO("C_BPartner", rs, "pokus") as I_C_BPartner
                categoryName = rs.getString("category_name")
            }

            val c_contactactivity_id = rs.getObject("c_contactactivity_id") as BigDecimal?
            if (c_contactactivity_id != null) {
                val activity = modelFactory.getPO( "C_ContactActivity", rs, "pokus", "activity_") as I_C_ContactActivity
                activities.add(activity)
            }
        }

        pi.serializableObject = DetailResult( bpartner, categoryName, activities )

        return "OK"
    }

}