package software.hsharp.idempiere.api.servlets.services

import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import software.hsharp.core.services.ILoginUtility

@Component
class LoginService {
    companion object {
        var loginUtilityImpl: ILoginUtility? = null
        val loginUtility get() = loginUtilityImpl!!
    }

    @Reference
    fun setLoginUtility(loginUtility : ILoginUtility) {
        loginUtilityImpl= loginUtility
    }
}