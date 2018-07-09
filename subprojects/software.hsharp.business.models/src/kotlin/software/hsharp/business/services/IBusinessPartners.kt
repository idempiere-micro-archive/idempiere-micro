package software.hsharp.business.services

import software.hsharp.business.models.IBusinessPartner
import software.hsharp.core.services.IService
import software.hsharp.core.services.IServiceRegister

interface IBusinessPartners : IService {
    fun getAllBusinessPartners() : IBusinessPartnersResult
    fun getBusinessPartnerById( id : Int ) : IBusinessPartnerResult
}

interface IBusinessPartnersImpl : IBusinessPartners
interface IBusinessPartnersEndpoint : IBusinessPartners

interface IBusinessPartnersServiceRegister : IServiceRegister<IBusinessPartners> {
}
