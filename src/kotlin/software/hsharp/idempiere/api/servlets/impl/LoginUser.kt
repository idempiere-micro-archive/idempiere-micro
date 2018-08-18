package software.hsharp.idempiere.api.servlets.impl

import com.fasterxml.jackson.databind.ObjectMapper
import org.idempiere.common.util.Env
import software.hsharp.api.helpers.jwt.JwtManager
import software.hsharp.idempiere.api.servlets.Error
import software.hsharp.idempiere.api.servlets.Result
import software.hsharp.idempiere.api.servlets.jwt.LoginManager
import software.hsharp.idempiere.api.servlets.jwt.UserLoginModel
import software.hsharp.idempiere.api.servlets.jwt.UserLoginModelResponse
import software.hsharp.idempiere.api.servlets.services.SystemService
import java.io.PrintWriter
import java.io.StringWriter
import javax.annotation.security.PermitAll
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@PermitAll
@Path("authentication")
class LoginUser {

    protected fun doLogin(
        username: String,
        password: String,
        clientId: Int?,
        roleId: Int?,
        orgId: Int?,
        warehouseId: Int?,
        language: String?
    ): UserLoginModelResponse {
        val mapper = ObjectMapper()

        SystemService.system.startup()

        val loginManager = LoginManager()
        val userLoginModel =
            UserLoginModel(
                username, password,
                clientId,
                roleId,
                orgId,
                warehouseId,
                if (language == null) { "en-US" } else { language }
            )

        val result = loginManager.doLogin(userLoginModel)
        if (result.logged) {
            val AD_User_ID = Env.getAD_User_ID(Env.getCtx())
            val token = JwtManager.createToken(AD_User_ID.toString(), "", mapper.writeValueAsString(userLoginModel))
            return result.copy(token = token)
        } else {
            return result
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    fun login1(
        @QueryParam("username") username: String,
        @QueryParam("password") password: String,
        @QueryParam("clientId") clientId: Int?,
        @QueryParam("roleId") roleId: Int?,
        @QueryParam("orgId") orgId: Int?,
        @QueryParam("warehouseId") warehouseId: Int?,
        @QueryParam("language") language: String?

    ): String {
        try {
            val mapper = ObjectMapper()
            return mapper.writeValueAsString(
                doLogin(
                    username, password,
                    clientId,
                    roleId,
                    orgId,
                    warehouseId,
                    language
            ))
        } catch (e: Exception) {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))

            val result = Result(Error("Login failed:" + e.toString() + "[" + sw.toString() + "]"))
            return result.toString()
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun login2(
        @FormParam("username") username: String,
        @FormParam("password") password: String,
        @FormParam("clientId") clientId: Int?,
        @FormParam("roleId") roleId: Int?,
        @FormParam("orgId") orgId: Int?,
        @FormParam("warehouseId") warehouseId: Int?,
        @FormParam("language") language: String?
    ): String {
        try {
            val mapper = ObjectMapper()
            return mapper.writeValueAsString(
                doLogin(
                    username, password,
                    clientId,
                    roleId,
                    orgId,
                    warehouseId,
                    language
                    ))
        } catch (e: Exception) {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))

            val result = Result(Error("Login failed:" + e.toString() + "[" + sw.toString() + "]"))
            return result.toString()
        }
    }
}

/* Sample POST body to test
 		
{ "userName":"System", "password":"System", "clientId" : 1000002, "roleId":1000004, "orgId":1000002, "warehouseId" : 1000000 }
  		
*/
