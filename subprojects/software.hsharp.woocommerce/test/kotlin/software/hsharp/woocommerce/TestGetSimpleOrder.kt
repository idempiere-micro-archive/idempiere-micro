package software.hsharp.woocommerce

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
import org.junit.Test
import pg.org.compiere.db.DB_PostgreSQL
import software.hsharp.woocommerce.impl.*
import org.compiere.process.ProcessInfoParameter
import org.compiere.product.X_I_Product
import org.compiere.crm.ImportBPartner
import org.junit.Ignore
import kotlin.test.assertTrue

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
        db.setDatabase(DatabaseImpl())
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
        assertTrue( id > 0 )
    }

    @Ignore
    fun createNewImportOrderAndProcess() {
        Ini.getIni().isClient = false
        CLogger.getCLogger(TestGetSimpleOrder::class.java)
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
        val AD_ORG_ID = 11
        val AD_ORG_ID_s = AD_ORG_ID.toString()
        ctx.setProperty(Env.AD_ORG_ID, AD_ORG_ID_s )
        Env.setContext(ctx, Env.AD_ORG_ID, AD_ORG_ID_s )

        val newOrder = X_I_Order( ctx, 0, null )
        newOrder.bPartnerValue = "TreeFarm"
        newOrder.productValue = "Oak"
        newOrder.qtyOrdered = 1.toBigDecimal()
        newOrder.docTypeName = "Standard Order"
        newOrder.c_Currency_ID = 100 // 100 = USD, 148= CZK
        newOrder.aD_Org_ID = AD_ORG_ID
        newOrder.setIsSOTrx( true )
        newOrder.salesRep_ID = 103
        newOrder.save()
        val id = newOrder._ID
        println( "id:${id}" )
        assertTrue( id > 0 )

        /* TODO: move import order
        val importOrder = ImportOrder()
        val pinfo = ProcessInfo("Import Test Order", 206);

        val parameters : Array<ProcessInfoParameter> = arrayOf(
                ProcessInfoParameter( "AD_Client_ID", AD_CLIENT_ID.toBigDecimal(), null, null, null ),
                ProcessInfoParameter( "AD_Org_ID", AD_ORG_ID.toBigDecimal(), null, null, null )
        )

        pinfo.aD_Client_ID = AD_CLIENT_ID
        pinfo.parameter = parameters

        importOrder.startProcess(ctx, pinfo, null)

        println( "pinfo:$pinfo" )
         */
    }

    @Ignore
    fun importFromWooCommerceAndProcess() {
        Ini.getIni().isClient = false
        CLogger.getCLogger(TestGetSimpleOrder::class.java)
        Ini.getIni().properties
        val db = Database()
        db.setDatabase(DatabaseImpl())
        DB.setDBTarget(CConnection.get(null))
        DB.isConnected()

        val ctx = Env.getCtx()
        val AD_CLIENT_ID = 1000000
        val AD_CLIENT_ID_s = AD_CLIENT_ID.toString()
        ctx.setProperty(Env.AD_CLIENT_ID, AD_CLIENT_ID_s )
        Env.setContext(ctx, Env.AD_CLIENT_ID, AD_CLIENT_ID_s )
        val AD_ORG_ID = 1000000
        val AD_ORG_ID_s = AD_ORG_ID.toString()
        ctx.setProperty(Env.AD_ORG_ID, AD_ORG_ID_s )
        Env.setContext(ctx, Env.AD_ORG_ID, AD_ORG_ID_s )

        val config = Secrets()
        val wooCommerce = WooCommerceAPI(config, ApiVersionType.V2)
        val orders : Array<SingleOrder> = wooCommerce.getOrders()
        orders.forEach {
            println("Processing Order:$it")

            val order = it
            val billing = order.billing
            val shipping = order.shipping

            val newBPartner = X_I_BPartner(ctx, 0, null)
            newBPartner.countryCode = billing.country
            newBPartner.value = billing.email
            newBPartner.name = "${billing.firstName} ${billing.lastName} ${billing.company}".trim()
            newBPartner.address1 = billing.address
            newBPartner.address2 = billing.address
            newBPartner.postal = billing.postcode
            newBPartner.city = billing.city
            newBPartner.contactName = "${billing.firstName} ${billing.lastName}".trim()
            if (newBPartner.contactName.isEmpty()) newBPartner.contactName = null
            newBPartner.phone = billing.phone
            newBPartner.setIsCustomer(true)
            newBPartner.setIsBillTo(true)
            newBPartner.setIsShipTo(false)
            newBPartner.save()

            val newBPartner1 = X_I_BPartner(ctx, 0, null)
            newBPartner1.countryCode = shipping.country
            newBPartner1.value = billing.email
            newBPartner1.name = "${shipping.firstName} ${shipping.lastName} ${shipping.company}".trim()
            newBPartner1.address1 = shipping.address
            newBPartner1.address2 = shipping.address
            newBPartner1.postal = shipping.postcode
            newBPartner1.city = shipping.city
            newBPartner1.contactName = "${shipping.firstName} ${shipping.lastName}".trim()
            if (newBPartner1.contactName.isEmpty()) newBPartner1.contactName = null
            newBPartner1.phone = billing.phone
            newBPartner1.setIsCustomer( true )
            newBPartner1.setIsBillTo(false)
            newBPartner1.setIsShipTo(true)
            newBPartner1.save()

            if (it.lineItems!=null) {

                it.lineItems!!.forEach {
                    val newOrder = X_I_Order(ctx, 0, null)
                    newOrder.eMail = billing.email
                    newOrder.bPartnerValue = billing.email
                    newOrder.name = "${billing.firstName} ${billing.lastName} ${billing.company}".trim()
                    newOrder.address1 = billing.address
                    newOrder.address2 = billing.address
                    newOrder.postal = billing.postcode
                    newOrder.city = billing.city
                    newOrder.contactName = "${billing.firstName} ${billing.lastName}".trim()
                    if (newOrder.contactName.isEmpty()) newOrder.contactName = null
                    newOrder.phone = billing.phone
                    newOrder.productValue = it.productId.toString()
                    newOrder.qtyOrdered = it.quantity.toBigDecimal()
                    newOrder.docTypeName = "Standard Order"
                    newOrder.c_Currency_ID = 148 // 100 = USD, 148= CZK
                    newOrder.aD_Org_ID = AD_ORG_ID
                    newOrder.setIsSOTrx(true)
                    newOrder.salesRep_ID = 1000001
                    newOrder.countryCode = order.billing.country
                    newOrder.documentNo = order.number
                    newOrder.dateOrdered = parseDate(order.dateCreated)
                    newOrder.save()
                    val id = newOrder._ID
                    println("id:${id}")
                    assertTrue(id > 0)
                }
            }

            if ( it.shippingLines != null ) {
                it.shippingLines!!.forEach {
                    val newOrder = X_I_Order(ctx, 0, null)
                    newOrder.eMail = billing.email
                    newOrder.bPartnerValue = billing.email
                    newOrder.name = "${billing.firstName} ${billing.lastName} ${billing.company}".trim()
                    newOrder.address1 = billing.address
                    newOrder.address2 = billing.address
                    newOrder.postal = billing.postcode
                    newOrder.city = billing.city
                    newOrder.contactName = "${billing.firstName} ${billing.lastName}".trim()
                    if (newOrder.contactName.isEmpty()) newOrder.contactName = null
                    newOrder.phone = billing.phone
                    newOrder.productValue = it.methodId
                    newOrder.qtyOrdered = 1.toBigDecimal()
                    newOrder.docTypeName = "Standard Order"
                    newOrder.c_Currency_ID = 148 // 100 = USD, 148= CZK
                    newOrder.aD_Org_ID = AD_ORG_ID
                    newOrder.setIsSOTrx(true)
                    newOrder.salesRep_ID = 1000001
                    newOrder.countryCode = order.billing.country
                    newOrder.documentNo = order.number
                    newOrder.save()
                    val id = newOrder._ID
                    println("id:${id}")
                    assertTrue(id > 0)
                }
            }

        }


        // change the they ImportBPartner will create BOTH addresses as locations
        val importBPartner = ImportBPartner()
        val parameters1 : Array<ProcessInfoParameter> = arrayOf(
                ProcessInfoParameter( "AD_Client_ID", AD_CLIENT_ID.toBigDecimal(), null, null, null ),
                ProcessInfoParameter( "CreateAllLocations", "Y", null, null, null )
        )
        val pinfo1 = ProcessInfo("Import Test BPartner", 206)
        pinfo1.aD_Client_ID = AD_CLIENT_ID
        pinfo1.parameter = parameters1
        importBPartner.startProcess(ctx, pinfo1, null)

        println( "pinfo1:$pinfo1" )

        /* TODO fix
        val importOrder = ImportOrder()

        val parameters : Array<ProcessInfoParameter> = arrayOf(
                ProcessInfoParameter( "AD_Client_ID", AD_CLIENT_ID.toBigDecimal(), null, null, null ),
                ProcessInfoParameter( "AD_Org_ID", AD_ORG_ID.toBigDecimal(), null, null, null )
        )

        val pinfo = ProcessInfo("Import Test Order", 206);
        pinfo.aD_Client_ID = AD_CLIENT_ID
        pinfo.parameter = parameters

        importOrder.startProcess(ctx, pinfo, null)

        println( "pinfo:$pinfo" )
         */
    }

    @Ignore("Unfinished")
    fun createNewImportProductAndProcess() {
        Ini.getIni().isClient = false
        CLogger.getCLogger(TestGetSimpleOrder::class.java)
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
        val AD_ORG_ID = 11
        val AD_ORG_ID_s = AD_ORG_ID.toString()
        ctx.setProperty(Env.AD_ORG_ID, AD_ORG_ID_s )
        Env.setContext(ctx, Env.AD_ORG_ID, AD_ORG_ID_s )

        val newProduct = X_I_Product( ctx, 0, null )

        newProduct.bPartner_Value = "Wood, Inc"
        TODO( "add all the required values" )

        /* TODO: use
        newProduct.save()
        val id = newProduct._ID
        println( "id:${id}" )
        assertTrue( id > 0 )

        val importProduct = ImportProduct()
        val pinfo = ProcessInfo("Import Test Product", 206);

        val parameters : Array<ProcessInfoParameter> = arrayOf(
                ProcessInfoParameter( "AD_Client_ID", AD_CLIENT_ID.toBigDecimal(), null, null, null ),
                ProcessInfoParameter( "M_PriceList_Version_ID", 101, null, null, null )
        )

        pinfo.aD_Client_ID = AD_CLIENT_ID
        pinfo.parameter = parameters

        importProduct.startProcess(ctx, pinfo, null)

        println( "pinfo:$pinfo" )
         */
    }

    @Test
    fun exportOrderToXml() {
        DummyEventManager.setup()
        DummyService.setup()
        Ini.getIni().isClient = false
        CLogger.getCLogger(TestGetSimpleOrder::class.java)
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