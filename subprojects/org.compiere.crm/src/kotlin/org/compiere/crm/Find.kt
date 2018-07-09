package org.compiere.crm

import org.compiere.orm.DefaultModelFactory
import org.compiere.orm.IModelFactory
import org.compiere.process.SvrProcess
import org.idempiere.common.util.DB
import org.idempiere.common.util.Env
import org.idempiere.common.util.KeyNamePair
import org.idempiere.icommon.model.IPO
import org.compiere.model.I_C_BPartner
import org.compiere.model.I_C_ContactActivity
import java.math.BigDecimal

data class BPartnerFindResult(val id:Int, val name:String, val searchName : String, val taxid : String? )

data class FindResult( val rows : List<Any> ) : java.io.Serializable

data class BPartnerWithActivity(val BPartner : I_C_BPartner, val ContactActivity : I_C_ContactActivity?, val BPartner_Category : String? )

class Find : SvrProcess() {
    var search : String = ""
    var full : Boolean = false
    var opensearch : Boolean = false
    var AD_CLIENT_ID = 0 //AD_Client_ID
    var AD_ORG_ID = 0 //AD_Org_ID

    override fun prepare() {
        for (para in parameter) {
            if ( para.parameterName == "Search" ) {
                search = para.parameterAsString
            } else if ( para.parameterName == "OpenSearch" ) {
                opensearch = para.parameterAsBoolean
            } else if ( para.parameterName == "Full" ) {
                full = para.parameterAsBoolean
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

        val tableName = "C_BPartner";

        val columns =
                if ( full ) { "*" } else { "c_bpartner_id,name,taxid" }

        val sql =
                """
select $columns from adempiere.bpartner_v
where (value ilike ? or name ilike ? or referenceno ilike ? or duns ilike ? or taxid ilike ?) -- params 1..5
and iscustomer = 'Y' and ad_client_id IN (0, ?) and ( ad_org_id IN (0,?) or ? = 0) and isactive = 'Y' -- params 6..8
order by 1 desc
                """.trimIndent()

        val sqlSearch = "%$search%".toLowerCase()

        val cnn = DB.getConnectionRO()
        val statement = cnn.prepareStatement(sql)
        statement.setString(1, sqlSearch)
        statement.setString(2, sqlSearch)
        statement.setString(3, sqlSearch)
        statement.setString(4, sqlSearch)
        statement.setString(5, sqlSearch)

        statement.setInt(6, AD_CLIENT_ID)
        statement.setInt(7, AD_ORG_ID)
        statement.setInt(8, AD_ORG_ID)
        val rs = statement.executeQuery()

        val modelFactory : IModelFactory = DefaultModelFactory()
        val result = mutableListOf<Any>()

        while(rs.next()) {
            if ( full ) {
                val bpartner : I_C_BPartner = modelFactory.getPO( "C_BPartner", rs, "pokus") as I_C_BPartner
                val c_contactactivity_id = rs.getObject("c_contactactivity_id") as BigDecimal?
                val row = BPartnerWithActivity( bpartner,
                        if (c_contactactivity_id ==null) { null } else {
                            modelFactory.getPO( "C_ContactActivity", rs, "pokus", "activity_") as I_C_ContactActivity
                        },
                        rs.getString("category_name")
                )
                result.add(row)
            } else {
                val name = rs.getString( "name" )
                val foundIdx = name.toLowerCase().indexOf(search.toLowerCase())
                val subName = if ( foundIdx > 0 ) { name.substring(foundIdx) } else { name }
                val keyName = BPartnerFindResult(rs.getInt("c_bpartner_id"), name, subName, rs.getString("taxid"))
                result.add(keyName)
            }
        }

        pi.serializableObject =
                if (full ) {
                    FindResult(result)
                } else {
                    if (opensearch) {
                        arrayOf<Any>(search, result.map { it as BPartnerFindResult }.map { it.name }.toTypedArray())
                    } else {
                        FindResult(result)
                    }
                }

        return "OK"
    }

}