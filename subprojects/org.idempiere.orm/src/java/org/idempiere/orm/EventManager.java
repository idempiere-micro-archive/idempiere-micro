/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2010 Heng Sin Low                							  *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package org.idempiere.orm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.idempiere.common.util.CLogger;

/**
 * Simple wrapper for the osgi event admin service.
 * Usage: EventManager.getInstance().sendEvent/postEvent
 * @author hengsin
 *
 */
public abstract class EventManager implements IEventManager {

	protected static IEventManager instance = null;
	protected final static CLogger log = CLogger.getCLogger(EventManager.class);

	protected final static Object mutex = new Object();

	/**
	 * Get the singleton instance created by the osgi service framework
	 * @return EventManager
	 */
	public static IEventManager getInstance() {
		//this never NEVER EVER works now
		// read more http://blog.vogella.com/2017/05/16/osgi-event-admin-publish-subscribe/
		// TODO DAP HACK
		synchronized (mutex) {
			while (instance == null) {
				try {
					log.info("Waiting for the IEventManager instance...");
					mutex.wait(10000);
				} catch (InterruptedException e) {
				}
			}
		}
		return instance;
	}



	/* (non-Javadoc)
	 * @see org.idempiere.app.event.IEventManager#register(java.lang.String, org.osgi.service.event.EventHandler)
	 */
	@Override
	public boolean register(String topic, IEventHandler eventHandler) {
		return register(topic, null, eventHandler);
	}

	/* (non-Javadoc)
	 * @see org.idempiere.app.event.IEventManager#register(java.lang.String[], org.osgi.service.event.EventHandler)
	 */
	@Override
	public boolean register(String[] topics, IEventHandler eventHandler) {
		return register(topics, null, eventHandler);
	}

	/* (non-Javadoc)
	 * @see org.idempiere.app.event.IEventManager#register(java.lang.String, java.lang.String, org.osgi.service.event.EventHandler)
	 */
	@Override
	public boolean register(String topic, String filter, IEventHandler eventHandler) {
		String[] topics = new String[] {topic};
		return register(topics, filter, eventHandler);
	}

	/**
	 * @param topic
	 * @param parameter
	 */
	public static IEvent newEvent(String topic, Object data) {
		return getInstance().createNewEvent(topic, data);
	}

	/**
	 *
	 * @param topic
	 * @param properties
	 * @return event object
	 */
	public static IEvent newEvent(String topic, EventProperty ...properties) {
		return getInstance().createNewEvent(topic, properties);
	}
}
