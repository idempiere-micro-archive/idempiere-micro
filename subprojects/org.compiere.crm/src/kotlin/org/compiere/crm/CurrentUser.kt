package org.compiere.crm

import software.hsharp.business.models.IDTOReady

data class CurrentUserResult(val AD_USER_ID: Int) : IDTOReady

class CurrentUser : SvrProcessBase() {
    override fun getResult(): IDTOReady {
        return CurrentUserResult(AD_USER_ID)
    }
}