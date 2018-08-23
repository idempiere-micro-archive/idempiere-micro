package software.hsharp.core.models

import software.hsharp.core.services.IService
import software.hsharp.core.services.IServiceRegister
import java.sql.Connection
import java.sql.ResultSet

interface HasRsMdColumnCount {
    val rsmdColumnCount: Int
    val cnn: Connection?
}

interface IGetDataResult : IResult, HasRsMdColumnCount {
    val rs: ResultSet?
}

interface IGetRowResult : IResult, HasRsMdColumnCount

interface IGetRowResultData : IGetRowResult {
    val rs: ResultSet?
}
interface IGetRowResultObject : IGetRowResult {
    val o: Any?
}

interface ICreateDataResult : IResult {
    val id: Int?
}

interface IUpdateDataResult : IResult {
    val id: Int?
}

interface IGetTreeDataResult : IResult, HasRsMdColumnCount {
    val rs: ResultSet?
}

interface ITreeDataDescriptor {
    val tableName: String
    val children: Pair<String, Array<ITreeDataDescriptor>>
}

interface IDataService : IService {
    fun getSchemasSupported(connection: Connection): Array<String>

    fun getRow(
        connection: Connection,
        tableName: String,
        id: Int,
        anonymous_call: Boolean
    ): IGetRowResult

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
    ): IGetDataResult

    fun createData(
        connection: Connection,
        tableName: String,
        table: IDataTable?,
        fields: MutableList<Pair<String, Any>>,
        anonymous_call: Boolean
    ): ICreateDataResult

    fun updateData(
        connection: Connection,
        tableName: String,
        table: IDataTable?,
        id: Int,
        fields: MutableList<Pair<String, Any>>,
        anonymous_call: Boolean
    ): IUpdateDataResult

    fun execute(
        connection: Connection,
        procName: String,
        jsonBody: String
    ): String?

    fun getTreeData(
        connection: Connection,
        root: ITreeDataDescriptor,
        orderBy: String, // Name
        orderByOrder: String, // ASC | DESC
        offset: Int, // 0
        limit: Int, // 100
        filterName1: String, // Name
        filterValue1: String, // Franta
        filterName2: String, // LastName
        filterValue2: String // Vokurka
    ): IGetTreeDataResult
}

interface IDataServiceRegister : IServiceRegister<IDataService> {
    fun serviceBySchema(connection: Connection, schema: String): IDataService?
}
