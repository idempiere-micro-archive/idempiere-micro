package org.compiere.product.test

import org.idempiere.orm.EventProperty
import org.idempiere.orm.IEvent
import org.idempiere.orm.IEventHandler

class DummyEventManager : org.idempiere.orm.EventManager() {
    override fun postEvent(event: IEvent?): Boolean {
        return true
    }

    override fun sendEvent(event: IEvent?): Boolean {
        return true
    }

    override fun createNewEvent(topic: String?, data: Any?): IEvent? {
        return null
    }

    override fun createNewEvent(topic: String?, vararg properties: EventProperty?): IEvent? {
        return null
    }

    override fun unregister(eventHandler: IEventHandler?): Boolean {
        return true
    }

    override fun register(topics: Array<out String>?, filter: String?, eventHandler: IEventHandler?): Boolean {
        return true
    }

    companion object {
        fun setup() {
            org.idempiere.orm.EventManager.instance = DummyEventManager()
        }
    }
}