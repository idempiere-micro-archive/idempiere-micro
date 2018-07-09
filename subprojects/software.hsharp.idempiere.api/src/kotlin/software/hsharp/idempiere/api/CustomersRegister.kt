package software.hsharp.idempiere.api

import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import software.hsharp.business.services.ICustomers
import software.hsharp.business.services.ICustomersServiceRegister
import software.hsharp.core.services.IServiceRegisterRegister


@Component
class CustomersRegister : ICustomersServiceRegister {
    override val name: String
        get() = "Register of Customers Service"

    companion object {
        private var customersServices = mutableListOf<ICustomers>()
    }

    override fun registerService(service: ICustomers) {
        customersServices.add(service)
    }

    override val services: Array<ICustomers>
        get() = customersServices.toTypedArray()

    @Reference
    fun setServiceRegisterRegister(serviceRegisterRegister: IServiceRegisterRegister) {
        serviceRegisterRegister.registerService( this )
    }
}