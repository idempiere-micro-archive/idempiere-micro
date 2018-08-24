package org.compiere.bo.tests

import org.compiere.process.ProcessCall
import org.compiere.process.ProcessInfo
import org.compiere.process.ProcessInfoParameter
import org.compiere.process.ProcessUtil
import org.idempiere.common.db.CConnection
import org.idempiere.common.db.Database
import org.idempiere.common.util.CLogger
import org.idempiere.common.util.DB
import org.idempiere.common.util.Env
import org.idempiere.common.util.Ini
import org.idempiere.icommon.db.AdempiereDatabase
import java.io.Serializable

abstract class BaseProcessTest {
    fun runProcess(databaseService: AdempiereDatabase, process: ProcessCall, bodyParams: Array<Pair<String,Any>>): Serializable {
        Ini.getIni().isClient = false
        CLogger.getCLogger(BaseProcessTest::class.java)
        Ini.getIni().properties
        val db = Database()
        db.setDatabase(databaseService)
        DB.setDBTarget(CConnection.get(null))
        DB.isConnected()

        val ctx = Env.getCtx()
        val AD_CLIENT_ID = 11
        val AD_CLIENT_ID_s = AD_CLIENT_ID.toString()
        ctx.setProperty(Env.AD_CLIENT_ID, AD_CLIENT_ID_s )
        Env.setContext(ctx, Env.AD_CLIENT_ID, AD_CLIENT_ID_s )

        val ad_Client_ID = Env.getAD_Client_ID(ctx)
        val ad_Org_ID = Env.getAD_Org_ID(ctx)
        val ad_User_ID = Env.getAD_User_ID(ctx)
        val parameters: MutableList<ProcessInfoParameter> = mutableListOf(
            ProcessInfoParameter("AD_Client_ID", ad_Client_ID.toBigDecimal(), null, null, null),
            ProcessInfoParameter("AD_Org_ID", ad_Org_ID.toBigDecimal(), null, null, null),
            ProcessInfoParameter("AD_User_ID", ad_User_ID.toBigDecimal(), null, null, null)
        )

        parameters
            .addAll(3, bodyParams.map {
                ProcessInfoParameter(
                    it.first,
                    it.second,
                    null,
                    null,
                    null
                )
            })

        val processInfo = ProcessInfo("Execute Java Process", 0)
        processInfo.aD_Client_ID = ad_Client_ID
        processInfo.aD_User_ID = ad_User_ID
        processInfo.parameter = parameters.toTypedArray()
        processInfo.className = process.javaClass.canonicalName
        ProcessUtil.startJavaProcess(ctx, processInfo, null, false, null, process)
        val processResult = processInfo.serializableObject
        return processResult
    }
}