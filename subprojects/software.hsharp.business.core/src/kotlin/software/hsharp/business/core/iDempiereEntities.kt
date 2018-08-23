package software.hsharp.business.core

import org.idempiere.common.util.DB
import org.idempiere.common.util.Env
import java.util.Properties

abstract class iDempiereEntities<T, I> {
    protected abstract val tableName: String

    protected val allIdsSelect: String
        get() = """SELECT ${tableName}_id FROM adempiere.$tableName WHERE (ad_client_id = ? OR ad_client_id=0) AND (ad_org_id = ? OR ad_org_id=0)"""
    protected val countSelect: String
        get() = "SELECT COUNT(*) FROM adempiere.$tableName WHERE (ad_client_id = ? OR ad_client_id=0) AND (ad_org_id = ? OR ad_org_id=0)"

    protected abstract fun getEntityById(ctx: Properties, id: Int): T?
    protected abstract fun convertToDTO(t: T): I

    protected fun getAllData(): List<I> {
        var dataFromDB: ArrayList<T> = arrayListOf()
        val connection = DB.getConnectionRO()
        val ctx = Env.getCtx()
        val ad_Client_ID = Env.getAD_Client_ID(ctx)
        val ad_Org_ID = Env.getAD_Org_ID(ctx)
        val queryIdCommand = allIdsSelect
        val stmt = connection.prepareStatement(queryIdCommand)
        stmt.setInt(1, ad_Client_ID)
        stmt.setInt(2, ad_Org_ID)
        val rs = stmt.executeQuery()
        while (rs.next()) {
            val id = rs.getInt(1)
            val data: T? = getEntityById(ctx, id)
            if (data != null) dataFromDB.add(data)
        }
        rs.close()
        return dataFromDB.map { convertToDTO(it) }
    }

    protected fun getById(id: Int): I? {
        val ctx = Env.getCtx()
        val data = getEntityById(ctx, id) ?: return null
        return convertToDTO(data)
    }

    protected fun getCount(): Int {
        var result = 0
        val connection = DB.getConnectionRO()
        val ctx = Env.getCtx()
        val ad_Client_ID = Env.getAD_Client_ID(ctx)
        val ad_Org_ID = Env.getAD_Org_ID(ctx)
        val queryIdCommand = countSelect
        val stmt = connection.prepareStatement(queryIdCommand)
        stmt.setInt(1, ad_Client_ID)
        stmt.setInt(2, ad_Org_ID)
        val rs = stmt.executeQuery()
        while (rs.next()) {
            result = rs.getInt(1)
        }
        rs.close()
        return result
    }
}