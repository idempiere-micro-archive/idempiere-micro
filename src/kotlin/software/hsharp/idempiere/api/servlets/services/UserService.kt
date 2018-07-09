package software.hsharp.idempiere.api.servlets.services

import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import software.hsharp.core.services.IUserServiceImpl

@Component
class UserService {
    companion object {
        var userServiceImpl: IUserServiceImpl? = null
        val userService get() = userServiceImpl!!
    }

    @Reference
    fun setSystem(userService : IUserServiceImpl) {
        userServiceImpl = userService
    }
}