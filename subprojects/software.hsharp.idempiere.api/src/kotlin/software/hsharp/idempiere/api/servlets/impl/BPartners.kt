package software.hsharp.idempiere.api.servlets.impl

import org.osgi.service.component.annotations.Reference
import software.hsharp.business.services.*
import software.hsharp.idempiere.api.servlets.services.BPartnersService
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("bpartners")
class BPartners : IBusinessPartnersEndpoint {
    override val name: String
        get() = "Business Partners Endpoint"

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("all")
    override fun getAllBusinessPartners() : IBusinessPartnersResult {
        return BPartnersService.bPartners.getAllBusinessPartners()
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    override fun getBusinessPartnerById( @PathParam("id") id : Int ) : IBusinessPartnerResult {
        return BPartnersService.bPartners.getBusinessPartnerById( id )
    }
}
