package org.compiere.bo.tests

import org.idempiere.common.base.IServiceHolder
import org.idempiere.common.base.IServiceLocator
import org.idempiere.common.base.IServicesHolder
import org.idempiere.common.base.ServiceQuery

class DummyService : org.idempiere.common.base.Service() {
    class DummyServiceLocator: IServiceLocator {
        override fun <T : Any?> locate(type: Class<T>?): IServiceHolder<T>? {
            return null
        }

        override fun <T : Any?> locate(type: Class<T>?, query: ServiceQuery?): IServiceHolder<T>? {
            return null
        }

        override fun <T : Any?> locate(type: Class<T>?, componentName: String?, query: ServiceQuery?): IServiceHolder<T>? {
            return null
        }

        override fun <T : Any?> list(type: Class<T>?): IServicesHolder<T>? {
            return null
        }

        override fun <T : Any?> list(type: Class<T>?, query: ServiceQuery?): IServicesHolder<T>? {
            return null
        }

        override fun <T : Any?> list(type: Class<T>?, componentName: String?, query: ServiceQuery?): IServicesHolder<T>? {
            return null
        }

    }

    override fun getLocator(): IServiceLocator {
        return DummyServiceLocator()
    }

    companion object {
        fun setup() {
            org.idempiere.common.base.Service.instance = DummyService()
        }
    }

}