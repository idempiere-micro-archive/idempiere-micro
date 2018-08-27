import org.compiere.orm.DefaultModelFactory
import org.compiere.orm.IModelFactory
import org.compiere.product.MProduct
import org.compiere.product.test.DatabaseImpl
import org.compiere.product.test.DummyEventManager
import org.compiere.product.test.DummyService
import org.idempiere.common.db.CConnection
import org.idempiere.common.db.Database
import org.idempiere.common.util.CLogger
import org.idempiere.common.util.DB
import org.idempiere.common.util.Env
import org.idempiere.common.util.Ini
import org.junit.Assert
import org.junit.Test
import pg.org.compiere.db.DB_PostgreSQL
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ProductTests {
    @Test
    fun getUsingDefaultModelFactoryById() {
        DummyService.setup()
        DummyEventManager.setup()
        Ini.getIni().isClient = false
        CLogger.getCLogger(ProductTests::class.java)
        Ini.getIni().properties
        val db = Database()
        db.setDatabase(DatabaseImpl())
        DB.setDBTarget(CConnection.get(null))
        DB.isConnected()

        val ctx = Env.getCtx()
        val AD_CLIENT_ID = 11
        val AD_CLIENT_ID_s = AD_CLIENT_ID.toString()
        ctx.setProperty(Env.AD_CLIENT_ID, AD_CLIENT_ID_s )
        Env.setContext(ctx, Env.AD_CLIENT_ID, AD_CLIENT_ID_s )
        val product_id = 123

        val modelFactory : IModelFactory = DefaultModelFactory()
        val result = modelFactory.getPO( "M_Product", product_id, "pokus")
        println( result );
        assertNotNull(result)
        val product = result as MProduct
        assertNotNull(product)
        assertEquals( product_id, product._ID )
    }
}