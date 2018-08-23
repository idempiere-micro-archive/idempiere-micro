package software.hsharp.core.services

import software.hsharp.core.models.INameKeyPair
import java.util.Properties

interface ILoginUtility {
    fun getClients(role: INameKeyPair): Array<INameKeyPair>
    fun getClients(app_user: String, app_pwd: String): Array<INameKeyPair>
    fun getRoles(app_user: String, client: INameKeyPair): Array<INameKeyPair>
    fun getOrgs(rol: INameKeyPair): Array<INameKeyPair>
    fun getWarehouses(org: INameKeyPair): Array<INameKeyPair>
    fun init(ctx: Properties): ILoginUtility
    fun loadPreferences(
        org: INameKeyPair,
        warehouse: INameKeyPair?,
        timestamp: java.sql.Timestamp?,
        printerName: String?
    ): String
}