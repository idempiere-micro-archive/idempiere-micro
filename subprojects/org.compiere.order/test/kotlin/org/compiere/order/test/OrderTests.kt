package org.compiere.order.test

import org.compiere.order.MOrder
import org.compiere.orm.DefaultModelFactory
import org.compiere.orm.IModelFactory
import org.idempiere.common.db.CConnection
import org.idempiere.common.db.Database
import org.idempiere.common.util.CLogger
import org.idempiere.common.util.DB
import org.idempiere.common.util.Env
import org.idempiere.common.util.Ini
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OrderTests {
    @Test
    fun getUsingDefaultModelFactoryById() {
        DummyService.setup()
        DummyEventManager.setup()
        Ini.getIni().isClient = false
        CLogger.getCLogger(OrderTests::class.java)
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
        val order_id = 104

        val modelFactory : IModelFactory = DefaultModelFactory()
        val result = modelFactory.getPO( "C_Order", order_id, "pokus")
        println( result );
        assertNotNull(result)
        val order = result as MOrder
        assertNotNull(order)
        assertEquals( order_id, order._ID )
        val lines = order.lines
        assertNotNull(lines)
        assertEquals( 6, lines.count() )
    }
}