package osgi.org.idempiere.orm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import org.idempiere.orm.EventProperty;
import org.idempiere.orm.IEvent;
import org.idempiere.orm.IEventHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import osgi.org.idempiere.common.base.BaseActivator;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

@Component
public class EventManager extends org.idempiere.orm.EventManager {
    private EventAdmin eventAdmin;

    public class EventHolder implements IEvent {
        private Event event;
    }


    protected Map<IEventHandler, List<ServiceRegistration<?>>> registrations = new HashMap<IEventHandler, List<ServiceRegistration<?>>>();

    /**
     * @param eventAdmin
     */
    public void bindEventAdmin(EventAdmin eventAdmin) {
        synchronized (mutex) {
            if (instance == null) {
                instance  = this;
                mutex.notifyAll();
            }
        }
        this.eventAdmin = eventAdmin;
    }

    /**
     * @param eventAdmin
     */
    public void unbindEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = null;
    }

    /* (non-Javadoc)
     * @see org.idempiere.app.event.IEventManager#register(java.lang.String[], java.lang.String, org.osgi.service.event.EventHandler)
     */
    @Override
    public boolean register(String[] topics, String filter, IEventHandler eventHandler) {
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
    public boolean unregister(IEventHandler eventHandler) {
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

    /* (non-Javadoc)
     * @see org.idempiere.app.event.IEventManager#postEvent(org.osgi.service.event.Event)
     */
    @Override
    public boolean postEvent(IEvent event) {
        if (eventAdmin != null) {
            EventHolder holder = (EventHolder)event;
            eventAdmin.postEvent(holder.event);
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.idempiere.app.event.IEventManager#sendEvent(org.osgi.service.event.Event)
     */
    @Override
    public boolean sendEvent(IEvent event) {
        if (eventAdmin != null) {
            EventHolder holder = (EventHolder)event;
            eventAdmin.sendEvent(holder.event);
            return true;
        }
        return false;
    }

    static void setup() {
        org.idempiere.orm.EventManager.instance = new EventManager();
    }

    /**
     * @param topic
     * @param parameter
     */
    @SuppressWarnings("unchecked")
    public IEvent createNewEvent(String topic, Object data) {
        Event event = null;
        if (data instanceof Dictionary<?,?>) {
            Dictionary<String,Object>dict = (Dictionary<String,Object>)data;
            if (dict.get(EVENT_ERROR_MESSAGES) == null)
                dict.put(EVENT_ERROR_MESSAGES, new ArrayList<String>());
            event = new Event(topic, dict);
        } else if (data instanceof Map<?, ?>) {
            Map<String, Object> map = (Map<String, Object>)data;
            if (!map.containsKey(EVENT_ERROR_MESSAGES))
                map.put(EVENT_ERROR_MESSAGES, new ArrayList<String>());
            event = new Event(topic, map);
        } else {
            Map<String, Object> map = new HashMap<String, Object>(3);
            map.put(EventConstants.EVENT_TOPIC, topic);
            if (data != null)
                map.put(EVENT_DATA, data);
            map.put(EVENT_ERROR_MESSAGES, new ArrayList<String>());
            event = new Event(topic, map);
        }
        EventHolder result = new EventHolder();
        result.event = event;
        return result;
    }

    /**
     *
     * @param topic
     * @param properties
     * @return event object
     */
    public IEvent createNewEvent(String topic, EventProperty...properties) {
        Event event = null;
        Map<String, Object> map = new HashMap<String, Object>(3);
        if (properties != null) {
            for(int i = 0; i < properties.length; i++) {
                map.put(properties[i].name, properties[i].value);
            }
            if (!map.containsKey(EventConstants.EVENT_TOPIC))
                map.put(EventConstants.EVENT_TOPIC, topic);
            if (!map.containsKey(EVENT_ERROR_MESSAGES))
                map.put(EVENT_ERROR_MESSAGES, new ArrayList<String>());
        }
        event = new Event(topic, map);
        EventHolder result = new EventHolder();
        result.event = event;
        return result;
    }
}
