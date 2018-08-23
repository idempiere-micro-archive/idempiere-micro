package org.compiere.crm

import software.hsharp.business.models.IContactActivity
import software.hsharp.business.models.IDTOReady
import software.hsharp.business.models.IBusinessPartner
import java.sql.PreparedStatement

data class BPartnerFindResult(val id: Int, val name: String, val searchName: String, val taxid: String?)

data class FindResult(val rows: List<Any>) : IDTOReady

data class BPartnerWithActivity(val BPartner: IBusinessPartner, val ContactActivity: IContactActivity?, val BPartner_Category: String?)

class Find : BaseBPartnerSearch() {
    override fun setStatementParams(statement: PreparedStatement) {
        val sqlSearch = "%$search%".toLowerCase()

        statement.setString(1, sqlSearch)
        statement.setString(2, sqlSearch)
        statement.setString(3, sqlSearch)
        statement.setString(4, sqlSearch)
        statement.setString(5, sqlSearch)

        statement.setInt(6, AD_CLIENT_ID)
        statement.setInt(7, AD_ORG_ID)
        statement.setInt(8, AD_ORG_ID)
    }

    override fun getSql(): String {
        val columns =
            if (full) { "*, C_ContactActivity_ID as activity_C_ContactActivity_ID" } else { "c_bpartner_id,name,taxid" }

        val sql =
            """
select $columns from adempiere.bpartner_v
where (value ilike ? or name ilike ? or referenceno ilike ? or duns ilike ? or taxid ilike ?) -- params 1..5
and ad_client_id IN (0, ?) and ( ad_org_id IN (0,?) or ? = 0) and isactive = 'Y' -- params 6..8
order by name asc
                """.trimIndent()
        return sql
    }
}