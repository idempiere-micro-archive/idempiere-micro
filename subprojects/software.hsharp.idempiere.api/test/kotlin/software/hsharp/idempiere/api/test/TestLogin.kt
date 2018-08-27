package software.hsharp.idempiere.api.test

import org.idempiere.common.db.Database
import org.junit.Before
import org.junit.Test
import osgi.org.compiere.db.postgresql.provider.DB_PostgreSQLComponent
import software.hsharp.api.helpers.jwt.ILogin
import software.hsharp.api.helpers.jwt.ILoginService
import software.hsharp.idempiere.api.servlets.jwt.LoginManager
import software.hsharp.idempiere.api.servlets.services.LoginService
import software.hsharp.idempiere.api.servlets.services.SystemService
import software.hsharp.idempiere.api.servlets.services.UserService
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestLogin {

    companion object {
        val gardenUser = LoginParams("GardenUser", "GardenUser", orgId = 50001, language="en-US")
        val fail = LoginParams("aaa", "aaa123")
    }

    data class LoginParams(
            override val loginName: String,
            override val password: String,
            override val clientId: Int? = null,
            override val roleId: Int? = null,
            override val orgId: Int? = null,
            override val warehouseId: Int? = null,
            override val language: String? = null
    ) : ILogin

    @Before
    fun prepare() {
        DummyService.setup()
        DummyEventManager.setup()
        SystemService().setSystem(DummyApp())
        Database().setDatabase(DB_PostgreSQLComponent())
        LoginService().setLoginUtility(DummyLogin())
        UserService().setUserService(DummyUserService())
    }

    @Test
    fun testLoginREST() {
        val loginUser: ILoginService = LoginManager()
        val result = loginUser.login(gardenUser)
        assertTrue(
            result.logged
        )
        assertTrue(
                result.token != ""
        )
    }

    @Test
    fun testLoginFailREST() {
        val loginUser: ILoginService = LoginManager()
        val result = loginUser.login(fail)
        assertFalse(
                result.logged
        )
    }

    @Test
    fun testBoth() {
        testLoginREST()
        testLoginFailREST()
    }
}