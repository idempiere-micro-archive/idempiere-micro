package org.compiere.crm

import org.compiere.process.SvrProcess
import org.idempiere.common.util.Env

class Import : SvrProcess() {
    var i_bpartner_id : Int = 0
    var AD_CLIENT_ID = 0 //AD_Client_ID
    var AD_ORG_ID = 0 //AD_Org_ID

    override fun prepare() {
        for (para in parameter) {
            if ( para.parameterName == "i_bpartner_id" ) {
                i_bpartner_id = para.parameterAsInt
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

        return "OK"
    }

}