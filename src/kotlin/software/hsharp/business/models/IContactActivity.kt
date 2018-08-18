package software.hsharp.business.models

import java.sql.Timestamp

interface IContactActivity : IDatabaseEntity {
    val start: Timestamp
    val bpartnerName : String
    val completed: Boolean
    val activityOwnerName: String
}