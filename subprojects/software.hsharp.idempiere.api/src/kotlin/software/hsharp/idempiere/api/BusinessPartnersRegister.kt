package software.hsharp.idempiere.api

import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import software.hsharp.business.services.IBusinessPartners
import software.hsharp.business.services.IBusinessPartnersServiceRegister
import software.hsharp.core.services.IServiceRegisterRegister

@Component
class BusinessPartnersRegister : IBusinessPartnersServiceRegister {
    override val name: String
        get() = "Register of Business Partners Service"

    companion object {
        private var businessPartnerServices = mutableListOf<IBusinessPartners>()
    }

    override fun registerService(service: IBusinessPartners) {
        businessPartnerServices.add(service)
    }

    override val services: Array<IBusinessPartners>
        get() = businessPartnerServices.toTypedArray()

    @Reference
    fun setServiceRegisterRegister(serviceRegisterRegister: IServiceRegisterRegister) {
        serviceRegisterRegister.registerService( this )
    }
}