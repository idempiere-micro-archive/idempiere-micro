package software.hsharp.business.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.compiere.orm.DefaultModelFactory
import org.compiere.orm.IModelFactory
import org.compiere.orm.MTable
import org.compiere.process.ProcessInfo
import org.compiere.process.ProcessInfoParameter
import org.compiere.process.ProcessUtil
import org.idempiere.common.util.DB
import org.idempiere.common.util.Env
import software.hsharp.core.models.IGetDataResult
import software.hsharp.core.util.Paging
import software.hsharp.core.util.parse
import java.io.Serializable
import java.sql.Connection
import java.sql.ResultSet
import software.hsharp.core.models.IDataSource
import software.hsharp.core.models.IPaging
import software.hsharp.core.models.IGetRowResultData
import software.hsharp.core.models.IGetRowResultObject
import software.hsharp.core.models.IUpdateDataResult
import software.hsharp.core.models.ICreateDataResult
import software.hsharp.core.models.ITreeDataDescriptor
import software.hsharp.core.models.IGetTreeDataResult
import software.hsharp.core.models.IDataTable
import software.hsharp.core.models.IDataColumn
import software.hsharp.core.models.IGetRowResult

data class GetDataResult(
    override val rs: ResultSet?,
    override val __metadata: IDataSource?,
    override val __paging: IPaging?,
    override val rsmdColumnCount: Int,
    override val cnn: Connection?
) : IGetDataResult {
    companion object {
        val empty: IGetDataResult
            get() = GetDataResult(null, null, null, rsmdColumnCount = 0, cnn = null)
    }
}

data class GetRowResult(
    override val rs: ResultSet?,
    override val __metadata: IDataSource?,
    override val __paging: IPaging?,
    override val rsmdColumnCount: Int,
    override val cnn: Connection
) : IGetRowResultData

data class GetRowResultObj(
    override val o: Any?,
    override val __metadata: IDataSource?,
    override val __paging: IPaging?,
    override val rsmdColumnCount: Int,
    override val cnn: Connection
) : IGetRowResultObject

data class UpdateDataResult(
    override val id: Int?,
    override val __metadata: IDataSource?,
    override val __paging: IPaging?
) : IUpdateDataResult {
    companion object {
        val empty: IUpdateDataResult
            get() = UpdateDataResult(null, null, null)
    }
}

data class CreateDataResult(
    override val id: Int?,
    override val __metadata: IDataSource?,
    override val __paging: IPaging?
) : ICreateDataResult {
    companion object {
        val empty: ICreateDataResult
            get() = CreateDataResult(null, null, null)
    }
}

data class ExecuteJavaProcessResult(
    val message: String?,
    val success: Boolean,
    val result: Serializable?
)

open class DataService {
    fun getTreeData(connection: Connection, root: ITreeDataDescriptor, orderBy: String, orderByOrder: String, offset: Int, limit: Int, filterName1: String, filterValue1: String, filterName2: String, filterValue2: String): IGetTreeDataResult {
        TODO("not implemented")
    }

    fun execute(connection: Connection, procName: String, jsonBody: String): String? {
        val ctx = Env.getCtx()
        val ad_Client_ID = Env.getAD_Client_ID(ctx)
        val ad_Org_ID = Env.getAD_Org_ID(ctx)
        val ad_User_ID = Env.getAD_User_ID(ctx)
        val parameters: MutableList<ProcessInfoParameter> = mutableListOf(
                ProcessInfoParameter("AD_Client_ID", ad_Client_ID.toBigDecimal(), null, null, null),
                ProcessInfoParameter("AD_Org_ID", ad_Org_ID.toBigDecimal(), null, null, null),
                ProcessInfoParameter("AD_User_ID", ad_User_ID.toBigDecimal(), null, null, null)
        )
        val bodyParams = parse(jsonBody)
        parameters
                .addAll(3, bodyParams.map { ProcessInfoParameter(it.first, it.second, null, null, null) })

        val processInfo = ProcessInfo("Execute Java Process", 0)
        processInfo.aD_Client_ID = ad_Client_ID
        processInfo.aD_User_ID = ad_User_ID
        processInfo.parameter = parameters.toTypedArray()
        processInfo.className = procName
        val success = ProcessUtil.startJavaProcess(ctx, processInfo, null, true)
        ExecuteJavaProcessResult(processInfo.summary, success, processInfo.serializableObject)
        val mapper = ObjectMapper().registerModule(KotlinModule())
        return mapper.writeValueAsString(processInfo.serializableObject)
    }

    fun getSchemasSupported(connection: Connection): Array<String> {
        return arrayOf("adempiere", "idempiere")
    }
    val name: String
        get() = "iDempiere Data Service"

    private fun getColumn(next: Pair<String, Any>, table: IDataTable): IDataColumn? {
        return table.columns.find { it.columnName.toLowerCase() == next.first.toLowerCase() }
    }

    private fun getTypeCast(next: Pair<String, Any>, table: IDataTable): String {
        val column = getColumn(next, table)
        return if (column == null) { "?" } else "CAST(? AS ${column.columnType})"
    }

    fun createData(
        connection: Connection,
        tableName: String,
        table: IDataTable?,
        fields: MutableList<Pair<String, Any>>,
        anonymous_call: Boolean
    ): ICreateDataResult {
        val ctx = Env.getCtx()
        val ad_Client_ID = Env.getAD_Client_ID(ctx)
        val ad_Org_ID = Env.getAD_Org_ID(ctx)
        val ad_User_ID = Env.getAD_User_ID(ctx)
        val cnn = DB.getConnectionRW()

        val tableName_lowerCase = tableName.toLowerCase()

        val sql =
                (fields.fold("INSERT INTO \"${tableName_lowerCase}\" ( \"${tableName_lowerCase}_id\",") { total, next -> total + next.first + "," }) +
                        (fields.fold(
                                "ad_client_id, ad_org_id, updatedby, updated, createdby, created ) VALUES ( (SELECT COALESCE(MAX(\"${tableName_lowerCase}_id\"),0) + 1 FROM \"${tableName_lowerCase}\" ),"
                        ) { total, next -> "${total}${getTypeCast(next, table!!)}," }) + " ?, ?, ?, statement_timestamp(), ?, statement_timestamp()) RETURNING ${tableName_lowerCase}_id;"

        val statement = cnn.prepareStatement(sql)
        fields.forEachIndexed { index, value ->
            try {
                statement.setObject(index + 1, value.second)
            } catch (ex: Exception) {
                throw ex
            }
        }
        val fieldsCount = fields.count()
        statement.setInt(fieldsCount + 1, ad_Client_ID)
        statement.setInt(fieldsCount + 2, ad_Org_ID)
        statement.setInt(fieldsCount + 3, ad_User_ID)
        statement.setInt(fieldsCount + 4, ad_User_ID)

        val rs = statement.executeQuery()
        connection.commit()

        var result: Int? = null
        while (rs.next()) {
            result = rs.getInt(1)
        }
        cnn.close()

        return CreateDataResult(result, null, null)
    }

    fun updateData(
        connection: Connection,
        tableName: String,
        table: IDataTable?,
        id: Int,
        fields: MutableList<Pair<String, Any>>,
        anonymous_call: Boolean
    ): IUpdateDataResult {
        val ctx = Env.getCtx()
        val ad_Client_ID = Env.getAD_Client_ID(ctx)
        val ad_Org_ID = Env.getAD_Org_ID(ctx)
        val cnn = DB.getConnectionRW()

        val tableName_lowerCase = tableName.toLowerCase()

        val sql =
                (fields.fold("UPDATE \"${tableName_lowerCase}\" SET ", { total, next -> total + "${next.first}=${getTypeCast(next, table!!)}," })) +
                        "ad_client_id = ?, ad_org_id = ? WHERE ${tableName_lowerCase}_id = ? RETURNING ${tableName}_id;"

        val statement = cnn.prepareStatement(sql)
        fields.forEachIndexed { index, value -> statement.setObject(index + 1, value.second) }

        val fieldsCount = fields.count()
        statement.setInt(fieldsCount + 1, ad_Client_ID)
        statement.setInt(fieldsCount + 2, ad_Org_ID)
        statement.setInt(fieldsCount + 3, id)

        val rs = statement.executeQuery()
        // cnn.commit() <- auto commit

        var result: Int? = null
        while (rs.next()) {
            result = rs.getInt(1)
        }
        cnn.close()

        return UpdateDataResult(result, null, null)
    }

    fun getRow(
        connection: Connection,
        tableName: String,
        id: Int,
        anonymous_call: Boolean
    ): IGetRowResult {
        val ctx = Env.getCtx()
        val ad_Client_ID = Env.getAD_Client_ID(ctx)
        val ad_Org_ID = Env.getAD_Org_ID(ctx)
        val ad_User_ID = Env.getAD_User_ID(ctx)
        val cnn = DB.getConnectionRO()

        val tableNameLowerCase = tableName.toLowerCase()
        val selectPart = "SELECT *"
        val sql = selectPart + ", ? as ad_user_id FROM \"" + tableNameLowerCase + "\" WHERE (ad_client_id = ? OR ad_client_id=0) AND (ad_org_id = ? OR ad_org_id=0 OR ?=0) AND \"" + tableNameLowerCase + "_id\" = ? "
        val statement = cnn.prepareStatement(sql)
        statement.setInt(1, ad_User_ID)
        statement.setInt(2, ad_Client_ID)
        statement.setInt(3, ad_Org_ID)
        statement.setInt(4, ad_Org_ID)
        statement.setInt(5, id)
        val rs = statement.executeQuery()

        val table = MTable.get(ctx, tableName)
        if (table == null) {
            return GetRowResult(rs = rs, __metadata = null, __paging = null, rsmdColumnCount = rs.metaData.columnCount, cnn = cnn)
        } else {
            rs.next()
            val modelFactory: IModelFactory = DefaultModelFactory()
            val result = modelFactory.getPO(tableName, rs, "getRow")
            return GetRowResultObj(o = result, __metadata = null, __paging = null, rsmdColumnCount = 0, cnn = cnn)
        }
    }

    fun getData(
        connection: Connection,
        tableName: String,
        columnsRequested: Array<String>?, // null => *
        orderBy: String, // Name
        orderByOrder: String, // ASC | DESC
        offset: Int, // 0
        limit: Int, // 100
        filterName1: String, // Name
        filterValue1: String, // Franta
        filterName2: String, // LastName
        filterValue2: String, // Vokurka
        anonymous_call: Boolean
    ): IGetDataResult {
        val ctx = Env.getCtx()
        val ad_Client_ID = Env.getAD_Client_ID(ctx)
        val ad_Org_ID = Env.getAD_Org_ID(ctx)
        val ad_User_ID = Env.getAD_User_ID(ctx)
        val cnn = DB.getConnectionRO()
        var count = 0

        val where_clause =
                if (filterName1 != "") {
                    " AND \"$filterName1\"=? " +

                            if (filterName2 != "") {
                                " AND \"$filterName2\"=? "
                            } else {
                                ""
                            }
                } else {
                    ""
                }

        val tableName_lowerCase = tableName.toLowerCase()

        val sql_count = "SELECT COUNT(*), ? as ad_user_id FROM \"$tableName_lowerCase\" WHERE (ad_client_id = ? OR ad_client_id=0) AND (ad_org_id = ? OR ad_org_id=0 OR ?=0) $where_clause"
        val statement_count = cnn.prepareStatement(sql_count)
        statement_count.setInt(1, ad_User_ID)
        statement_count.setInt(2, ad_Client_ID)
        statement_count.setInt(3, ad_Org_ID)
        statement_count.setInt(4, ad_Org_ID)
        if (filterName1 != "") {
            statement_count.setString(5, filterValue1)
            if (filterName2 != "") { statement_count.setString(6, filterValue2) }
        }

        val rs_count = statement_count.executeQuery()
        while (rs_count.next()) {
            count = rs_count.getInt(1)
        }

        val selectPart =
            if (columnsRequested == null || columnsRequested.count() == 0) { "SELECT * " } else { columnsRequested.fold("SELECT ", { total, next -> "$total \"$next\"," }).trimEnd(',') }

        val sql =
                "$selectPart, ? as ad_user_id  FROM \"${tableName_lowerCase}\" WHERE (ad_client_id = ? OR ad_client_id=0) AND (ad_org_id = ? OR ad_org_id=0 OR ?=0) $where_clause" +
                if (orderBy != "") {
                    " ORDER BY \"$orderBy\"" + if (orderByOrder.toLowerCase() == "desc") { " desc" } else { "" }
                } else { " ORDER BY 1" } +
                " LIMIT $limit OFFSET $offset;"
        val statement = cnn.prepareStatement(sql)
        statement.setInt(1, ad_User_ID)
        statement.setInt(2, ad_Client_ID)
        statement.setInt(3, ad_Org_ID)
        statement.setInt(4, ad_Org_ID)
        if (filterName1 != "") {
            statement.setString(5, filterValue1)
            if (filterName2 != "") { statement.setString(6, filterValue2) }
        }
        val rs = statement.executeQuery()

        val result = GetDataResult(rs = rs, __metadata = null, __paging = Paging(count), rsmdColumnCount = rs.metaData.columnCount, cnn = cnn)
        return result
    }
}
