package software.hsharp.idempiere.api.servlets.impl

import org.idempiere.common.util.Env
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("me")
class Me {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    fun getStatus(): String {
        return "I am user ID " + Env.getAD_User_ID(Env.getCtx()) + ". Me works!"
    }
}
