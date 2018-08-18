package software.hsharp.business.services

import software.hsharp.business.models.IBusinessPartner
import software.hsharp.core.models.IResult

interface IBusinessPartnerResult : IResult {
    val businessPartner: IBusinessPartner?
}