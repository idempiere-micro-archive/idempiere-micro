package software.hsharp.business.core

import org.idempiere.common.util.DB
import org.idempiere.common.util.Env
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import software.hsharp.core.util.Paging
import software.hsharp.business.models.ICategory
import software.hsharp.business.models.ICustomer
import software.hsharp.core.models.IDataSource
import software.hsharp.core.models.IPaging
import software.hsharp.business.models.IBusinessPartnerLocation
import software.hsharp.business.services.*
import java.math.BigDecimal
import java.util.*

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
