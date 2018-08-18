package software.hsharp.api.icommon

interface ICConnection {
    val dbHost: String
    val dbPort: Int
    val dbName: String
    val dbUid: String
    val dbPwd: String
    val ssl: Boolean
}