package software.hsharp.business.models

import software.hsharp.core.models.INamedEntity
import software.hsharp.core.models.ISearchableByKey

interface IBusinessPartner : IDatabaseEntity, INamedEntity, ISearchableByKey {
    val Locations : Array<IBusinessPartnerLocation>
}