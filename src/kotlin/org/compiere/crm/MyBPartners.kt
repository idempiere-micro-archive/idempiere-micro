package org.compiere.crm

import java.sql.PreparedStatement

class MyBPartners : BaseBPartnerSearch() {
    override fun setStatementParams(statement: PreparedStatement) {
        statement.setInt(1, AD_USER_ID)

        statement.setInt(2, AD_CLIENT_ID)
        statement.setInt(3, AD_ORG_ID)
        statement.setInt(4, AD_ORG_ID)
    }

    override fun getSql(): String {
        val columns =
            if (full) { "*, C_ContactActivity_ID as activity_C_ContactActivity_ID" } else { "c_bpartner_id,name,taxid" }

        return """select $columns from adempiere.bpartner_v
        where salesrep_id = ? -- param 1
        and ad_client_id IN (0, ?) and ( ad_org_id IN (0,?) or ? = 0) and isactive = 'Y' -- params 2..4
        order by name asc
        """
    }
}