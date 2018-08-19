import org.compiere.crm.X_I_BPartner
import org.compiere.order.MOrder
import org.compiere.order.X_I_Order
import org.compiere.process.ProcessInfo
import org.idempiere.common.db.CConnection
import org.idempiere.common.db.Database
import org.idempiere.common.util.CLogger
import org.idempiere.common.util.DB
import org.idempiere.common.util.Env
import org.idempiere.common.util.Ini
import org.junit.Assert
import org.junit.Test
import pg.org.compiere.db.DB_PostgreSQL
import software.hsharp.woocommerce.impl.*
import org.compiere.process.ProcessInfoParameter
import org.compiere.product.X_I_Product
import org.compiere.crm.ImportBPartner
import org.junit.Ignore
import software.hsharp.woocommerce.*


class TestGetSimpleOrder {
    @Ignore
    fun getSimpleOrder() {
        val config = Secrets()
        val wooCommerce = WooCommerceAPI(config, ApiVersionType.V2)
        val order : SingleOrder = wooCommerce.getOrder(1290)
        println("Order:$order")
    }

    @Ignore
    fun getAllOrders() {
        val config = Secrets()
        val wooCommerce = WooCommerceAPI(config, ApiVersionType.V2)
        val orders : Array<SingleOrder> = wooCommerce.getOrders()
        orders.forEach { println("Order:$it") }
    }

    @Ignore
    fun getAllProducts() {
        val config = Secrets()
        val wooCommerce = WooCommerceAPI(config, ApiVersionType.V2)
        val products: Array<IProduct> = wooCommerce.getProducts()
        products.forEach { println("Product:$it") }
    }

    @Ignore
    fun createNewOrder() {
        Ini.getIni().isClient = false
        CLogger.getCLogger(TestGetSimpleOrder::class.java)
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

        val newOrder = MOrder( ctx, 0, null )
        newOrder.aD_Org_ID = 11
        newOrder.m_Warehouse_ID = 50000
        newOrder.c_BPartner_ID = 114
        newOrder.save()
        val id = newOrder._ID
        println( "id:${id}" )
        Assert.assertTrue( id > 0 )
    }

    @Test
    fun exportOrderToXml() {
        Ini.getIni().isClient = false
        CLogger.getCLogger(TestGetSimpleOrder::class.java)
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
        val AD_ORG_ID = 11
        val AD_ORG_ID_s = AD_ORG_ID.toString()
        ctx.setProperty(Env.AD_ORG_ID, AD_ORG_ID_s )
        Env.setContext(ctx, Env.AD_ORG_ID, AD_ORG_ID_s )

        val order = MOrder( ctx, 108, "test" )
        val exportOrder = ExportOrder(
                order
        )
        val order1 = MOrder( ctx, 106, "test" )
        val exportOrder1 = ExportOrder(
                order1
        )

        val orders : Array<ExportOrder> = arrayOf(exportOrder, exportOrder1)

        val xml = write2XMLString(orders)

        println( "XML:$xml" )
    }
}