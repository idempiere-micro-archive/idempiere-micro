package org.compiere.bo.tests

import org.compiere.bo.CreateCustomer
import org.compiere.bo.CustomerProcessBase
import org.junit.Test
import java.util.Properties

class TestCreateCustomer :  BaseCustomerTest() {
    override fun preparePartnerId(ctx: Properties, AD_CLIENT_ID: Int): Int? {
        return null
    }

    override fun getProcess(): CustomerProcessBase {
        return CreateCustomer()
    }

    override fun runFinallyCleanup() {

    }

    @Test
    fun test1() {
        doTheTest()
    }
}