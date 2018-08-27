package org.compiere.bo.tests

import org.compiere.model.I_C_BPartner
import org.compiere.model.I_C_ContactActivity
import org.compiere.orm.DefaultModelFactory
import org.compiere.orm.IModelFactory
import org.idempiere.common.db.CConnection
import org.idempiere.common.db.Database
import org.idempiere.common.util.CLogger
import org.idempiere.common.util.DB
import org.idempiere.common.util.Env
import org.idempiere.common.util.Ini
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FactoryTests {
    @Ignore
    fun getUsingDefaultModelFactoryFromRSSuperComplex() {
        DummyService.setup()
        DummyEventManager.setup()
        Ini.getIni().isClient = false
        CLogger.getCLogger(FactoryTests::class.java)
        Ini.getIni().properties
        val db = Database()
        db.setDatabase(DatabaseImpl())
        DB.setDBTarget(CConnection.get(null))
        DB.isConnected()

        val ctx = Env.getCtx()
        val AD_CLIENT_ID = 11
        val AD_CLIENT_ID_s = AD_CLIENT_ID.toString()
        ctx.setProperty(Env.AD_CLIENT_ID, AD_CLIENT_ID_s )
        Env.setContext(ctx, Env.AD_CLIENT_ID, AD_CLIENT_ID_s )

        val tableName = I_C_BPartner.Table_Name
        val id = 118

        val sql = """select *, C_ContactActivity_ID as activity_C_ContactActivity_ID from 
        
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


            where c_contactactivity_id is not null
            order by 1, c_contactactivity_id asc"""

        println ( "SQL:$sql" )
        val cnn = DB.getConnectionRO()
        val statement = cnn.prepareStatement(sql)
        val rs = statement.executeQuery()
        rs.next()

        val modelFactory : IModelFactory = DefaultModelFactory()
        val result = modelFactory.getPO( tableName, rs, "pokus")
        val result2 = modelFactory.getPO(I_C_ContactActivity.Table_Name, rs, "pokus", "activity_") as I_C_ContactActivity
        println( result )
        println( result2 )
        assertNotNull(result)
        assertNotNull(result2)
        assertEquals(id, result._ID)
        assertEquals(123, result2.c_ContactActivity_ID)

        cnn.close()
    }


    @Test
    fun getUsingDefaultModelFactoryFromRSComplex() {
        DummyService.setup()
        DummyEventManager.setup()
        Ini.getIni().isClient = false
        CLogger.getCLogger(FactoryTests::class.java)
        Ini.getIni().properties
        val db = Database()
        db.setDatabase(DatabaseImpl())
        DB.setDBTarget(CConnection.get(null))
        DB.isConnected()

        val ctx = Env.getCtx()
        val AD_CLIENT_ID = 11
        val AD_CLIENT_ID_s = AD_CLIENT_ID.toString()
        ctx.setProperty(Env.AD_CLIENT_ID, AD_CLIENT_ID_s )
        Env.setContext(ctx, Env.AD_CLIENT_ID, AD_CLIENT_ID_s )

        val tableName = I_C_BPartner.Table_Name
        val AD_ORG_ID = 0
        val id = 118

        val sql =
                ("SELECT * FROM adempiere.\"${tableName}\", adempiere.M_PriceList " +
                        "WHERE (\"${tableName}\".ad_client_id = ? OR \"${tableName}\".ad_client_id=0) " +
                        "AND (\"${tableName}\".ad_org_id = ? OR \"${tableName}\".ad_org_id=0) " +
                        "AND (\"${tableName}_ID\"=?) AND (M_PriceList.M_PriceList_ID = C_BPartner.M_PriceList_ID);"
                        ).toLowerCase()
        println ( "SQL:$sql" )
        val cnn = DB.getConnectionRO()
        val statement = cnn.prepareStatement(sql)
        statement.setInt(1, AD_CLIENT_ID)
        statement.setInt(2, AD_ORG_ID)
        statement.setInt(3, id)
        val rs = statement.executeQuery()
        rs.next()

        val modelFactory : IModelFactory = DefaultModelFactory()
        val result = modelFactory.getPO( tableName, rs, "pokus")
        val result2 = modelFactory.getPO( "M_PriceList", rs, "pokus")
        println( result )
        println( result2 )
        assertNotNull(result)
        assertNotNull(result2)
        assertEquals(id, result._ID)
        assertEquals(101, result2._ID)

        cnn.close()
    }

}