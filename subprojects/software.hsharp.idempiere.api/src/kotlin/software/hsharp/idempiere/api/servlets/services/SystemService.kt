package software.hsharp.idempiere.api.servlets.services

import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import software.hsharp.core.services.ISystemImpl

@Component
class SystemService {
    companion object {
        var systemImpl: ISystemImpl? = null
        val system get() = systemImpl!!
    }

    @Reference
    fun setSystem(system : ISystemImpl) {
        systemImpl= system
    }
}