package org.compiere.crm

import org.compiere.model.I_C_BPartner
import org.compiere.model.I_C_ContactActivity
import org.compiere.orm.DefaultModelFactory
import org.compiere.orm.IModelFactory
import software.hsharp.business.models.IDTOReady
import java.math.BigDecimal
import java.sql.Connection

data class DetailResult(
    val bPartner: I_C_BPartner?,
    val categoryName: String?,
    val activities: MutableList<I_C_ContactActivity>
) : IDTOReady

class Detail : SvrProcessBaseSql() {
    override val isRO: Boolean
        get() = true
    var businessPartnerId: Int = 0

    override fun prepare() {
        super.prepare()
        for (para in parameter) {
            if (para.parameterName == "BusinessPartnerId") {
                businessPartnerId = para.parameterAsInt
            }
        }
    }

    override fun getSqlResult(cnn: Connection): IDTOReady {
        val sql =
            """
select *, C_ContactActivity_ID as activity_C_ContactActivity_ID from bpartner_detail_v where c_bpartner_id = ?
and ad_client_id IN (0, ?) and ( ad_org_id IN (0,?) or ? = 0) and isactive = 'Y' order by activity_startdate desc
""".trimIndent()

        val statement = cnn.prepareStatement(sql)
        statement.setInt(1, businessPartnerId)
        statement.setInt(2, AD_CLIENT_ID)
        statement.setInt(3, AD_ORG_ID)
        statement.setInt(4, AD_ORG_ID)

        val rs = statement.executeQuery()

        val modelFactory: IModelFactory = DefaultModelFactory()
        var bpartner: I_C_BPartner? = null
        var categoryName: String? = null

        val activities = mutableListOf<I_C_ContactActivity>()

        while (rs.next()) {
            if (bpartner == null) {
                bpartner = modelFactory.getPO(I_C_BPartner.Table_Name, rs, null) as I_C_BPartner
                categoryName = rs.getString("category_name")
            }

            val c_contactactivity_id = rs.getObject("c_contactactivity_id") as BigDecimal?
            if (c_contactactivity_id != null) {
                val activity = modelFactory.getPO(I_C_ContactActivity.Table_Name, rs, null, "activity_") as I_C_ContactActivity
                activities.add(activity)
            }
        }

        return DetailResult(bpartner, categoryName, activities)
    }
}