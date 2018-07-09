package software.hsharp.business.models

import software.hsharp.core.models.INamedEntity
import software.hsharp.core.models.ISearchableByKey

interface ILocation  {
    val CountryName : String?
    val City : String?
    val Postal : String?
    val Address1 : String?
    val Address2 : String?
    val Address3 : String?
    val Address4 : String?
    val Address5 : String?
}