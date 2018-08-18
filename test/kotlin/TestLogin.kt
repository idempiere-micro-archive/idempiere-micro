import com.gt2base.api.Configuration
import com.gt2base.api.DB
import com.gt2base.api.servlets.impl.LoginUser
import junit.framework.Assert
import org.junit.Test
import software.hsharp.db.postgresql.provider.PgDB

class TestLogin {
    @Test
    fun testLoginREST() {
        org.postgresql.Driver()
        val config = Configuration()
        config.load()
        val db = DB()
        db.setDatabase(PgDB())
        val loginUser = LoginUser()
        val result = loginUser.login1("gardenuser", "Garden_User", null, null, null, null )
        Assert.assertTrue(
            result.contains("true")
        )
    }

    @Test
    fun testLoginFailREST() {
        org.postgresql.Driver()
        val config = Configuration()
        config.load()
        val db = DB()
        db.setDatabase(PgDB())
        val loginUser = LoginUser()
        val result = loginUser.login1("aaa", "aaa123", null, null, null, null )
        Assert.assertTrue(
            result.contains("false")
        )
    }

    @Test
    fun testBoth() {
        org.postgresql.Driver()
        val config = Configuration()
        config.load()
        val db = DB()
        db.setDatabase(PgDB())
        val loginUser = LoginUser()
        val result = loginUser.login1("gardenuser", "Garden_User", null, null, null, null )
        Assert.assertTrue(
            result.contains("true")
        )
        val result2 = loginUser.login1("aaa", "aaa123", null, null, null, null )
        Assert.assertTrue(
            result2.contains("false")
        )
    }
}