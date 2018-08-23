package software.hsharp.core.util

import software.hsharp.core.models.IDataColumn
import software.hsharp.core.models.IDataColumnAction
import software.hsharp.core.models.INameValueString

data class NameValueString(
    override val name: String,
    override val value: String
) : INameValueString {
    constructor() : this("", "")
}

data class DataColumnAction(
    override val type: String,
    override val url: String,
    override val params: Array<INameValueString>?
) : IDataColumnAction

data class DataColumn(
    override val isRequired: Boolean,
    override val isReadOnly: Boolean,
    override val columnName: String,
    override val columnType: String,
    override val action: IDataColumnAction?
) : IDataColumn, Comparable<DataColumn> {
        override fun compareTo(other: DataColumn): Int {
                return columnName.compareTo(other.columnName)
        }
}