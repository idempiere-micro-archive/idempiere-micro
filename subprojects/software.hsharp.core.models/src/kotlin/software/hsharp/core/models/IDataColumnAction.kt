package software.hsharp.core.models

interface INameValueString {
    val name: String
    val value: String
}

interface IDataColumnAction {
    val type: String
    val url: String
    val params: Array<INameValueString>?
}
