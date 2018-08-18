package software.hsharp.core.models

interface IDataTable : IDataSource {
    val tableName: String
    val schemaName: String
    val isFunction: Boolean
}