package org.compiere.crm

import java.sql.PreparedStatement

class ForgottenBPartners : BaseBPartnerSearch() {
    override fun setStatementParams(statement: PreparedStatement) {
        statement.setInt(1, AD_CLIENT_ID)
        statement.setInt(2, AD_ORG_ID)
        statement.setInt(3, AD_ORG_ID)
    }

    override fun getSql(): String {
        val columns =
            if (full) { "*, C_ContactActivity_ID as activity_C_ContactActivity_ID" } else { "c_bpartner_id,name,taxid" }

        return """select $columns from adempiere.bpartner_v
        where ( activity_startdate < current_date or c_contactactivity_id is null )
        and ad_client_id IN (0, ?) and ( ad_org_id IN (0,?) or ? = 0) and isactive = 'Y' -- params 1..3
        order by name asc
        """
    }
}