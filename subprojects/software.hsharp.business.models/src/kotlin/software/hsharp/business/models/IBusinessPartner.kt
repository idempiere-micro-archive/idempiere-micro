package software.hsharp.business.models

import software.hsharp.core.models.INamedEntity
import software.hsharp.core.models.ISearchableByKey
import java.math.BigDecimal

interface IBusinessPartner : IDatabaseEntity, INamedEntity, ISearchableByKey {
    val Locations: Array<IBusinessPartnerLocation>
    val flatDiscount: BigDecimal
}