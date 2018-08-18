package org.compiere.crm

import org.compiere.process.SvrProcess
import software.hsharp.business.models.IDTOReady
import java.io.Serializable

data class CurrentUserResult( val AD_USER_ID: Int ) : IDTOReady

class CurrentUser : SvrProcessBase() {
    override fun getResult(): IDTOReady {
        return CurrentUserResult( AD_USER_ID )
    }
}