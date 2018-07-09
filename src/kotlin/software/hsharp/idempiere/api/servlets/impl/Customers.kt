package software.hsharp.idempiere.api.servlets.impl

import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import software.hsharp.api.icommon.IDatabase
import software.hsharp.business.models.ICategory
import software.hsharp.business.models.ICustomer
import software.hsharp.business.services.ICustomerResult
import software.hsharp.business.services.ICustomersEndpoint
import software.hsharp.business.services.ICustomersResult
import software.hsharp.idempiere.api.servlets.services.CustomerService
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("customers")
class Customers : ICustomersEndpoint {
    override val name: String
        get() = "Customers EndPoint"

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("all")
    override fun getAllCustomers(): ICustomersResult {
        return CustomerService.customers.getAllCustomers()
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    override fun getCustomerById(@PathParam("id") id: Int): ICustomerResult {
        return CustomerService.customers.getCustomerById( id )
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("search")
    override fun getCustomersByAnyCategory(categories: Array<ICategory>): ICustomersResult {
        return CustomerService.customers.getCustomersByAnyCategory( categories )
    }
}
