package org.compiere.bo

import org.compiere.crm.ContactActivity
import org.compiere.crm.SvrProcessBaseSql
import org.compiere.model.I_C_ContactActivity
import org.compiere.orm.DefaultModelFactory
import org.compiere.orm.IModelFactory
import software.hsharp.business.models.IContactActivity
import software.hsharp.business.models.IDTOReady
import java.math.BigDecimal
import java.sql.Connection

class MyFutureContactActivities : SvrProcessBaseSql() {

    data class Result(val activities: List<IContactActivity>) : IDTOReady

    override val isRO: Boolean
        get() = true

    override fun getSqlResult(cnn: Connection): IDTOReady {
        val sql =
            """
select * from(
select * from (
select 0 as loc_ord, * from 

(select 
    c_contactactivity_id,
    A.ad_client_id,
    A.ad_org_id,
    A.created,
    A.createdby,
    A.description,
    A.isactive ,
    A.updated,
    A.updatedby,
    A.startdate,
    BTRIM(to_json(startdate)::varchar,'"') as startdate_f, 
    A.enddate,
    BTRIM(to_json(enddate)::varchar,'"') as enddate_f,
    A.comments ,
    A.salesrep_id ,
    A.ad_user_id ,
    A.contactactivitytype ,
    A.iscomplete ,
    A.c_opportunity_id ,
    A.c_contactactivity_uu,
    O.description as o_description,
    C.value as c_value,
    C.name as c_name
from adempiere.c_contactactivity A
inner join adempiere.c_opportunity O ON O.c_opportunity_id = A.c_opportunity_id
inner join adempiere.c_bpartner C ON O.c_bpartner_id = C.c_bpartner_id) X

where EXTRACT(WEEK FROM startdate) = EXTRACT(WEEK FROM current_date )
and abs(EXTRACT(day FROM startdate) - EXTRACT(day FROM current_date ) ) = 0
and salesrep_id = ? and ad_client_id IN (0, ?) and ( ad_org_id IN (0,?) or ? = 0) and isactive = 'Y'
union
select 1 as loc_ord, * from 

(select 
    c_contactactivity_id,
    A.ad_client_id,
    A.ad_org_id,
    A.created,
    A.createdby,
    A.description,
    A.isactive ,
    A.updated,
    A.updatedby,
    A.startdate,
    BTRIM(to_json(startdate)::varchar,'"') as startdate_f, 
    A.enddate,
    BTRIM(to_json(enddate)::varchar,'"') as enddate_f,
    A.comments ,
    A.salesrep_id ,
    A.ad_user_id ,
    A.contactactivitytype ,
    A.iscomplete ,
    A.c_opportunity_id ,
    A.c_contactactivity_uu,
    O.description as o_description,
    C.value as c_value,
    C.name as c_name
from adempiere.c_contactactivity A
inner join adempiere.c_opportunity O ON O.c_opportunity_id = A.c_opportunity_id
inner join adempiere.c_bpartner C ON O.c_bpartner_id = C.c_bpartner_id) X

where EXTRACT(WEEK FROM startdate) = EXTRACT(WEEK FROM current_date )
and abs(EXTRACT(day FROM startdate) - EXTRACT(day FROM current_date ) ) = 1
and salesrep_id = ? and ad_client_id IN (0, ?) and ( ad_org_id IN (0,?) or ? = 0) and isactive = 'Y'
union
select 2 as loc_ord, * from 

(select
    c_contactactivity_id,
    A.ad_client_id,
    A.ad_org_id,
    A.created,
    A.createdby,
    A.description,
    A.isactive ,
    A.updated,
    A.updatedby,
    A.startdate,
    BTRIM(to_json(startdate)::varchar,'"') as startdate_f, 
    A.enddate,
    BTRIM(to_json(enddate)::varchar,'"') as enddate_f,
    A.comments ,
    A.salesrep_id ,
    A.ad_user_id ,
    A.contactactivitytype ,
    A.iscomplete ,
    A.c_opportunity_id ,
    A.c_contactactivity_uu,
    O.description as o_description,
    C.value as c_value,
    C.name as c_name
from adempiere.c_contactactivity A
inner join adempiere.c_opportunity O ON O.c_opportunity_id = A.c_opportunity_id
inner join adempiere.c_bpartner C ON O.c_bpartner_id = C.c_bpartner_id) X

where EXTRACT(WEEK FROM startdate) = EXTRACT(WEEK FROM current_date )
and abs(EXTRACT(day FROM startdate) - EXTRACT(day FROM current_date ) ) > 1
and salesrep_id = ? and ad_client_id IN (0, ?) and ( ad_org_id IN (0,?) or ? = 0) and isactive = 'Y'
) x order by loc_ord limit 50
) y order by startDate limit 100
""".trimIndent()

        val statement = cnn.prepareStatement(sql)
        statement.setInt(1, AD_USER_ID)
        statement.setInt(2, AD_CLIENT_ID)
        statement.setInt(3, AD_ORG_ID)
        statement.setInt(4, AD_ORG_ID)
        statement.setInt(5, AD_USER_ID)
        statement.setInt(6, AD_CLIENT_ID)
        statement.setInt(7, AD_ORG_ID)
        statement.setInt(8, AD_ORG_ID)
        statement.setInt(9, AD_USER_ID)
        statement.setInt(10, AD_CLIENT_ID)
        statement.setInt(11, AD_ORG_ID)
        statement.setInt(12, AD_ORG_ID)

        val rs = statement.executeQuery()

        val modelFactory: IModelFactory = DefaultModelFactory()

        try {
            val activities =
                generateSequence {
                    if (rs.next()) {
                        val c_contactactivity_id = rs.getObject("c_contactactivity_id") as BigDecimal?
                        if (c_contactactivity_id != null) {
                            val activity = modelFactory.getPO(I_C_ContactActivity.Table_Name, rs, null) as I_C_ContactActivity
                            ContactActivity(activity)
                        } else null
                    } else null
                }.toList()
            return Result(activities)
        } finally {
            rs.close()
        }
    }
}