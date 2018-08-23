/******************************************************************************
 * Copyright (C) 2012 Heng Sin Low                                            *
 * Copyright (C) 2012 Trek Global                 							  *
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
package org.idempiere.common.base.ds;

import org.idempiere.common.base.BaseActivator;
import org.idempiere.common.base.IServiceHolder;
import org.idempiere.common.base.IServiceLocator;
import org.idempiere.common.base.IServicesHolder;
import org.idempiere.common.base.ServiceQuery;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentConstants;
import org.osgi.util.tracker.ServiceTracker;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hengsin
 *
 */
public class DynamicServiceLocator implements IServiceLocator {

	/**
	 * 
	 */
	public DynamicServiceLocator() {
	}

	/**
	 * @see org.idempiere.common.base.IServiceLocator#locate(java.lang.Class)
	 */
	@Override
	public <T> IServiceHolder<T> locate(Class<T> type) {
		Filter filter = filter(type, null, null);
		if ( filter == null ) { return new DummyDynamicServiceHolder<>(); }
		ServiceTracker<T, T> tracker = BaseActivator.getServiceTracker(type, filter);
		return new DynamicServiceHolder<>(tracker);
	}

	/**
	 * @see org.idempiere.common.base.IServiceLocator#locate(java.lang.Class, org.idempiere.common.base.ServiceQuery)
	 */
	@Override
	public <T> IServiceHolder<T> locate(Class<T> type, ServiceQuery query) {
		if (query == null || query.isEmpty())
			return locate(type);
		
		Filter filter = filter(type, null, query);
		ServiceTracker<T, T> tracker = BaseActivator.getServiceTracker(type, filter);
		return new DynamicServiceHolder<>(tracker);
	}

	/**
	 * @see org.idempiere.common.base.IServiceLocator#locate(java.lang.Class, java.lang.String, org.idempiere.common.base.ServiceQuery)
	 */
	@Override
	public <T> IServiceHolder<T> locate(Class<T> type, String serviceId, ServiceQuery query) {
		if ((query == null || query.isEmpty()) && (serviceId == null || serviceId.trim().length() == 0))
			return locate(type);
		
		Filter filter = filter(type, serviceId, query);
		ServiceTracker<T, T> tracker = BaseActivator.getServiceTracker(type, filter);
		
		return new DynamicServiceHolder<>(tracker);
	}

	public class DummyDynamicServiceHolder<T> extends DynamicServiceHolder<T> {
        DummyDynamicServiceHolder() {
            super(null);
        }

        @Override
        public T getService() {
            return null;
        }

        @Override
        public List<T> getServices() {
            return new ArrayList<>();
        }
    }

	/**
	 * @see org.idempiere.common.base.IServiceLocator#list(java.lang.Class)
	 */
	@Override
	public <T> IServicesHolder<T> list(Class<T> type) {
		Filter filter = filter(type, null, null);
		if ( filter == null ) { return new DummyDynamicServiceHolder<>(); }
		ServiceTracker<T, T> tracker = BaseActivator.getServiceTracker(type, filter);
		
		return new DynamicServiceHolder<>(tracker);
	}

	/**
	 * @see org.idempiere.common.base.IServiceLocator#list(java.lang.Class, org.idempiere.common.base.ServiceQuery)
	 */
	@Override
	public <T> IServicesHolder<T> list(Class<T> type, ServiceQuery query) {
		if (query == null || query.isEmpty())
			return list(type);
		
		Filter filter = filter(type, null, query);
        if ( filter == null ) { return new DummyDynamicServiceHolder<>(); }
		ServiceTracker<T, T> tracker = BaseActivator.getServiceTracker(type, filter);
		return new DynamicServiceHolder<>(tracker);
	}

	/**
	 * @see org.idempiere.common.base.IServiceLocator#list(java.lang.Class, java.lang.String, org.idempiere.common.base.ServiceQuery)
	 */
	@Override
	public <T> IServicesHolder<T> list(Class<T> type, String serviceId, ServiceQuery query) {
		if ((query == null || query.isEmpty()) && (serviceId == null || serviceId.trim().length() == 0))
			return list(type);
		
		Filter filter = filter(type, serviceId, query);
		ServiceTracker<T, T> tracker = BaseActivator.getServiceTracker(type, filter);
		return new DynamicServiceHolder<>(tracker);
	}

	private Filter filter(Class<?> type, String serviceId, ServiceQuery query) {
		StringBuilder builder = new StringBuilder("(&(objectclass=");
		builder.append(type.getName()).append(")");
		if (query != null) {
			for(String key : query.keySet()) {
				String value = query.get(key);
				builder.append("(").append(key).append("=").append(value).append(")");
			}
		}
		if (serviceId != null && serviceId.trim().length() > 0) {
			builder.append("(").append(ComponentConstants.COMPONENT_NAME).append("=").append(serviceId.trim()).append(")");
		}
		builder.append(")");
		try {
			BundleContext bundleContext = BaseActivator.getBundleContext();
			if ( bundleContext== null ) {
				return null;
			} else {
				return bundleContext.createFilter(builder.toString());
			}
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}
}
