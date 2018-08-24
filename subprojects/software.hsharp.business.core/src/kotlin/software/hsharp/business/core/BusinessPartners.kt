package software.hsharp.business.core

import org.compiere.model.I_C_BPartner_Location
import org.compiere.model.I_C_Location
import software.hsharp.business.models.IBusinessPartner
import software.hsharp.core.models.IDataSource
import software.hsharp.core.models.IPaging
import software.hsharp.business.models.IBusinessPartnerLocation
import software.hsharp.business.models.ILocation
import software.hsharp.business.services.IBusinessPartnerResult
import software.hsharp.business.services.IBusinessPartnersResult
import java.math.BigDecimal

data class Location(
    override val CountryName: String?,
    override val City: String?,
    override val Postal: String?,
    override val Address1: String?,
    override val Address2: String?,
    override val Address3: String?,
    override val Address4: String?,
    override val Address5: String?
) : ILocation {
    constructor(a: I_C_Location): this(a.countryName, a.city, a.postal, a.address1, a.address2, a.address3, a.address4, a.address5)
}

data class BusinessPartnerLocation(
    override val Location: ILocation
) : IBusinessPartnerLocation {
    constructor (a: I_C_BPartner_Location): this(Location(a.location))
}

data class BusinessPartner(
    override val Key: Int,
    override val name: String,
    override val value: String,
    override val Locations: Array<IBusinessPartnerLocation>,
    override val flatDiscount: BigDecimal
) : IBusinessPartner {
    override val ID: String
        get() = "" + Key
}

data class BusinessPartnersResult(
    override val businessPartners: Array<IBusinessPartner>,
    override val __paging: IPaging?
) : IBusinessPartnersResult {
    companion object {
        val metadata: IDataSource? get() {
            /*return DataTable(
                    tableName = I_C_BPartner.Table_Name,
                    columns = arrayOf(),
                    defaultSortBy = DataColumn(
                            isRequired = true,

                    )
            )*/
            return null
        }
    }

    override val __metadata: IDataSource?
        get() = BusinessPartnersResult.metadata
}

data class BusinessPartnerResult(
    override val businessPartner: IBusinessPartner?,
    override val __paging: IPaging?
) : IBusinessPartnerResult {
    override val __metadata: IDataSource?
        get() = BusinessPartnersResult.metadata
}
