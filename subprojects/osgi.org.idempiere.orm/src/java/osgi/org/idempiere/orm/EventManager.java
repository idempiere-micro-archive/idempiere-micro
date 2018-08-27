package osgi.org.idempiere.orm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import osgi.org.idempiere.common.base.BaseActivator;

@Component
public class EventManager extends org.idempiere.orm.EventManager {
    protected Map<EventHandler, List<ServiceRegistration<?>>> registrations = new HashMap<EventHandler, List<ServiceRegistration<?>>>();

    /* (non-Javadoc)
     * @see org.idempiere.app.event.IEventManager#register(java.lang.String[], java.lang.String, org.osgi.service.event.EventHandler)
     */
    @Override
    public boolean register(String[] topics, String filter, EventHandler eventHandler) {
        BundleContext bundleContext = BaseActivator.getBundleContext();
        if (bundleContext == null) {
            log.severe("No bundle context. Topic="+ Arrays.toString(topics));
            return false;
        }
        Dictionary<String, Object> d = new Hashtable<String, Object>();
        d.put(EventConstants.EVENT_TOPIC, topics);
        if (filter != null)
            d.put(EventConstants.EVENT_FILTER, filter);
        ServiceRegistration<?> registration = bundleContext.registerService(EventHandler.class.getName(), eventHandler, d);
        synchronized(registrations) {
            List<ServiceRegistration<?>> list = registrations.get(eventHandler);
            if (list == null) {
                list = new ArrayList<ServiceRegistration<?>>();
                registrations.put(eventHandler, list);
            }
            list.add(registration);
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.idempiere.app.event.IEventManager#unregister(org.osgi.service.event.EventHandler)
     */
    @Override
    public boolean unregister(EventHandler eventHandler) {
        List<ServiceRegistration<?>> serviceRegistrations = null;
        synchronized(registrations) {
            serviceRegistrations = registrations.remove(eventHandler);
        }
        if (serviceRegistrations == null)
            return false;
        for (ServiceRegistration<?> registration : serviceRegistrations)
            registration.unregister();
        return true;
    }

    static void setup() {
        org.idempiere.orm.EventManager.instance = new EventManager();
    }
}
