package software.hsharp.core.models

enum class ACCESSRIGHTS {
    SELECT, INSERT, UPDATE
}

interface IDataSource {
    val columns: Array<IDataColumn>
    val access_rights: Set<ACCESSRIGHTS>
    val allow_anonymous: Boolean
}
