package software.hsharp.idempiere.api.servlets.services

import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import software.hsharp.business.services.ICustomersImpl

@Component
class CustomerService {
    companion object {
        var customersImpl: ICustomersImpl? = null
        val customers get() = customersImpl!!
    }

    @Reference
    fun setCustomers(customers : ICustomersImpl) {
        customersImpl= customers
    }
}
