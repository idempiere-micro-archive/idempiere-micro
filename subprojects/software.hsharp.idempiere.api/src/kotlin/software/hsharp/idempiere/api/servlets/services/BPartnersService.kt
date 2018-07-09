package software.hsharp.idempiere.api.servlets.services

import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import software.hsharp.business.services.IBusinessPartnersImpl

@Component
class BPartnersService {
    companion object {
        var bPartnersImpl: IBusinessPartnersImpl? = null
        val bPartners get() = bPartnersImpl!!
    }

    @Reference
    fun setCustomers(customers : IBusinessPartnersImpl) {
        bPartnersImpl= customers
    }
}
