package software.hsharp.core.models

interface IDataColumn : IDataColumnDefinition {
    val isRequired: Boolean
    val isReadOnly: Boolean
    // val format : IDataFormat
    val action: IDataColumnAction?
}
