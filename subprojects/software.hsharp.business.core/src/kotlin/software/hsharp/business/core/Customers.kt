package software.hsharp.business.core

import software.hsharp.business.models.ICategory
import software.hsharp.business.models.ICustomer
import software.hsharp.core.models.IDataSource
import software.hsharp.core.models.IPaging
import software.hsharp.business.models.IBusinessPartnerLocation
import software.hsharp.business.services.ICustomerResult
import software.hsharp.business.services.ICustomersResult
import java.math.BigDecimal

data class Customer(
    override val Key: Int,
    override val name: String,
    override val value: String,
    override val categories: Array<ICategory>,
    override val Locations: Array<IBusinessPartnerLocation>,
    override val flatDiscount: BigDecimal
) : ICustomer {
    override val ID: String
        get() = "" + Key
}
data class CustomersResult(
    override val customers: Array<ICustomer>,
    override val __paging: IPaging?
) : ICustomersResult {
    companion object {
        val metadata: IDataSource? get() {
            return null
        }
    }

    override val __metadata: IDataSource?
        get() = CustomersResult.metadata
}
data class CustomerResult(
    override val customer: ICustomer?,
    override val __paging: IPaging?
) : ICustomerResult {
    override val __metadata: IDataSource?
        get() = CustomersResult.metadata
}
