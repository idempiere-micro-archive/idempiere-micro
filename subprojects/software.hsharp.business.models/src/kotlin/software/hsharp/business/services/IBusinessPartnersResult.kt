package software.hsharp.business.services

import software.hsharp.business.models.IBusinessPartner
import software.hsharp.core.models.IResult

interface IBusinessPartnersResult : IResult {
   val businessPartners: Array<IBusinessPartner>
}