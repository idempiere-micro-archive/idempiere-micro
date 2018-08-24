package software.hsharp.business.models

interface IContactActivity : IDatabaseEntity {
    val start: Long // represent the value of java.sql.Timestamp.time
    val bpartnerName: String
    val completed: Boolean
    val activityOwnerName: String
}