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

        return """select $columns from 
        
        (select B.*, 
    A.c_contactactivity_id as c_contactactivity_id,
    A.ad_client_id as activity_ad_client_id,
    A.ad_org_id as activity_ad_org_id,
    A.created as activity_created,
    A.createdby as activity_createdby,
    A.description as activity_description,
    A.isactive as activity_isactive,
    A.updated as activity_updated,
    A.updatedby as activity_updatedby,
    A.startdate as activity_startdate,
    A.enddate as activity_enddate,
    A.comments as activity_comments,
    A.salesrep_id as activity_salesrep_id,
    A.ad_user_id as activity_ad_user_id,
    A.contactactivitytype as activity_contactactivitytype,
    A.iscomplete as activity_iscomplete,
    A.c_opportunity_id as activity_c_opportunity_id,
    A.c_contactactivity_uu as activity_c_contactactivity_uu,
    '' as category_name --C.name as category_name

from adempiere.c_bpartner B
left join c_opportunity O on  B.c_bpartner_id = O.c_bpartner_id
left join c_contactactivity A on A.c_opportunity_id = O.c_opportunity_id
-- left join crm_customer_category CC on CC.c_bpartner_id = B.c_bpartner_id
-- left join crm_category C on C.category_id = CC.category_id
where (B.c_bpartner_id, coalesce(A.c_contactactivity_id,0)) IN 
(
select B.c_bpartner_id, coalesce(max(A.c_contactactivity_id),0) as c_contactactivity_id from adempiere.c_bpartner B
left join c_opportunity O on  B.c_bpartner_id = O.c_bpartner_id
left join c_contactactivity A on A.c_opportunity_id = O.c_opportunity_id
where coalesce(A.isactive,'Y') = 'Y'
group by B.c_bpartner_id
)) X
        
        where ( activity_startdate < current_date or c_contactactivity_id is null )
        and ad_client_id IN (0, ?) and ( ad_org_id IN (0,?) or ? = 0) and isactive = 'Y' -- params 1..3
        order by name asc
        """
    }
}