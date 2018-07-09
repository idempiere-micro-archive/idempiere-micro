import org.compiere.order.MOrder
import org.compiere.orm.DefaultModelFactory
import org.compiere.orm.IModelFactory
import org.idempiere.common.db.CConnection
import org.idempiere.common.db.Database
import org.idempiere.common.util.CLogger
import org.idempiere.common.util.DB
import org.idempiere.common.util.Env
import org.idempiere.common.util.Ini
import org.junit.Assert
import org.junit.Test
import pg.org.compiere.db.DB_PostgreSQL

class OrderTests {
    @Test
    fun getUsingDefaultModelFactoryById() {
        val p : org.compiere.product.MProduct? = null

        Ini.getIni().isClient = false
        CLogger.getCLogger(OrderTests::class.java)
        Ini.getIni().properties
        val db = Database()
        db.setDatabase(DB_PostgreSQL())
        DB.setDBTarget(CConnection.get(null))
        DB.isConnected()

        val ctx = Env.getCtx()
        val AD_CLIENT_ID = 11
        val AD_CLIENT_ID_s = AD_CLIENT_ID.toString()
        ctx.setProperty(Env.AD_CLIENT_ID, AD_CLIENT_ID_s )
        Env.setContext(ctx, Env.AD_CLIENT_ID, AD_CLIENT_ID_s )
        val order_id = 104

        val modelFactory : IModelFactory = DefaultModelFactory()
        val result = modelFactory.getPO( "C_Order", order_id, "pokus")
        println( result );
        Assert.assertNotNull(result)
        val order = result as MOrder
        Assert.assertNotNull(order)
        Assert.assertEquals( order_id, order._ID )
        val lines = order.lines
        Assert.assertNotNull(lines)
        Assert.assertEquals( 6, lines.count() )
    }
}