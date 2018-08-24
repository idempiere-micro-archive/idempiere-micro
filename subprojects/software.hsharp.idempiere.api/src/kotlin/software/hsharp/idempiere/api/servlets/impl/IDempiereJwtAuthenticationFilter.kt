package software.hsharp.idempiere.api.servlets.impl

import com.fasterxml.jackson.databind.ObjectMapper
import software.hsharp.api.helpers.jwt.JwtAuthenticationFilter
import software.hsharp.idempiere.api.servlets.jwt.LoginManager
import software.hsharp.idempiere.api.servlets.jwt.UserLoginModel
import javax.annotation.Priority
import javax.ws.rs.Priorities
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.ext.Provider

@Provider
@Priority(Priorities.AUTHENTICATION)
class IDempiereJwtAuthenticationFilter : JwtAuthenticationFilter() {
    override fun decodeUserLoginModel(requestContext: ContainerRequestContext, userLoginModel: String) {
        if (userLoginModel == "") {
            return
        }
        val mapper = ObjectMapper()
        val result = mapper.readValue(userLoginModel, UserLoginModel::class.java)
        val loginManager = LoginManager()
        loginManager.doLogin(result)
    }
}