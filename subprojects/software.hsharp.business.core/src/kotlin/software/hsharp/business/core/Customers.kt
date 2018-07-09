package software.hsharp.business.core

import org.compiere.crm.MBPartner
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
import software.hsharp.core.services.IServiceRegisterRegister
import java.util.*

object crm_customer_category : IntIdTable(columnName = "customer_category_id") {
    val ad_client_id = integer("ad_client_id")
    val ad_org_id = integer("ad_org_id")
    val isactive = varchar("isactive", 1)
    val created = datetime("created")
    val createdby = integer("createdby")
    val updated = datetime("updated")
    val updatedby = integer("updatedby")
    val customer_category_uu= varchar("customer_category_uu", 36)

    val name = varchar("name", 60)
    val searchKey = varchar("value", 60)

    val customer_id = reference("c_bpartner_id", c_bpartner)
    val category_id = reference("category_id", crm_category)
}

class CustomerCategoryModel(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CustomerCategoryModel>(crm_customer_category)

    var category_Id by crm_customer_category.id
    var AD_Client_Id by crm_customer_category.ad_client_id
    var AD_Org_Id by crm_customer_category.ad_org_id
    var IsActive by crm_customer_category.isactive
    var Created by crm_customer_category.created
    var CreatedBy by crm_customer_category.createdby
    var Updated by crm_customer_category.updated
    var UpdatedBy by crm_customer_category.updatedby
    var customer_category_uu by crm_customer_category.customer_category_uu
    var name by crm_customer_category.name
    var searchKey by crm_customer_category.searchKey
    var customer_id by crm_customer_category.customer_id
    var category_id by crm_customer_category.category_id

    val categories by CategoryModel referencedOn crm_customer_category.category_id
    val customers by CustomerModel referencedOn crm_customer_category.customer_id
}

class CustomerModel(id: EntityID<Int>) : BusinessPartnerModel(id) {
    companion object : IntEntityClass<CustomerModel>(c_bpartner)

    val categories by CustomerCategoryModel referrersOn crm_customer_category.customer_id
}

data class Customer(
        override val Key : Int,
        override val name : String,
        override val value : String,
        override val categories : Array<ICategory>,
        override val Locations: Array<IBusinessPartnerLocation>    
 ) : ICustomer {
    override val ID: String
        get() = ""+Key
}
data class CustomersResult(
        override val customers : Array<ICustomer>,
        override val __paging: IPaging?) : ICustomersResult {
    companion object {
        val metadata: IDataSource? get () {
            return null
        }
    }

    override val __metadata: IDataSource?
        get() = CustomersResult.metadata
}
data class CustomerResult(
        override val customer : ICustomer?,
        override val __paging: IPaging? ) : ICustomerResult {
    override val __metadata: IDataSource?
        get() = CustomersResult.metadata
}

@Component
class Customers : ICustomersImpl {
    override val name: String
        get() = "Customers Service"

    private fun convert(it:CustomerModel, ctx: Properties) : Customer {
        val bpartner = MBPartner.get(ctx, it.id.value)
        return Customer( 
            it.id.value, it.name, it.searchKey, 
            it.categories.map { Category( it.category_Id.value, it.name ) as ICategory }.toTypedArray(),
                BusinessPartners.convertLocations( bpartner ) )
    }

    override fun getAllCustomers(): ICustomersResult {
        val ctx = Env.getCtx()
        val AD_Org_ID = Env.getAD_Org_ID(ctx)
        val AD_Client_ID = Env.getAD_Client_ID(ctx)
        var result = listOf<ICustomer>()

        Database.connect( { DB.getConnectionRO() } )
        transaction {
            result =
                CustomerModel.find {
                    (c_bpartner.ad_client_id eq AD_Client_ID)
                            .and(c_bpartner.ad_org_id eq AD_Org_ID)
                            .and(c_bpartner.iscustomer eq "Y")
                }.map {
                    convert(it, ctx )
                }
        }

        return CustomersResult( result.toTypedArray(), Paging(result.count()))
    }

    override fun getCustomerById(id: Int): ICustomerResult {
        val ctx = Env.getCtx()
        val AD_Org_ID = Env.getAD_Org_ID(ctx)
        val AD_Client_ID = Env.getAD_Client_ID(ctx)
        var result : ICustomer? = null

        Database.connect( { DB.getConnectionRO() } )
        transaction {
            result =
                    CustomerModel.find{ (c_bpartner.ad_client_id eq AD_Client_ID)
                            .and( c_bpartner.ad_org_id eq AD_Org_ID )
                            .and( c_bpartner.iscustomer eq "Y" )
                            .and( c_bpartner.id eq id )
                    }
                    .map {
                        convert(it, ctx)
                    }.firstOrNull { MBPartner.get(ctx, it.Key) != null }
        }
        return CustomerResult(result, if(result==null) {
            Paging(0)
        } else {
            Paging(1)
        })
    }

    override fun getCustomersByAnyCategory(categories: Array<ICategory>): ICustomersResult {
        val ctx = Env.getCtx()
        val AD_Org_ID = Env.getAD_Org_ID(ctx)
        val AD_Client_ID = Env.getAD_Client_ID(ctx)
        var result = listOf<ICustomer>()

        Database.connect( { DB.getConnectionRO() } )
        transaction {
            result =
                    CustomerModel.find {
                        (c_bpartner.ad_client_id eq AD_Client_ID)
                                .and(c_bpartner.ad_org_id eq AD_Org_ID)
                                .and(c_bpartner.iscustomer eq "Y")
                    }.map {
                        convert(it, ctx)
                    }.filter { it.categories.intersect(categories.toList()).isNotEmpty() }
        }

        return CustomersResult( result.toTypedArray(), Paging(result.count()))
    }

}

@Component
class CustomersRegisterHolder {
    companion object {
        var CustomersServiceRegister: ICustomersServiceRegister? = null
        var customers : Customers = Customers()
    }

    @Reference
    fun setCustomersServiceRegister(customersServiceRegister: ICustomersServiceRegister) {
        CustomersServiceRegister = customersServiceRegister
        customersServiceRegister.registerService( customers )
    }

}