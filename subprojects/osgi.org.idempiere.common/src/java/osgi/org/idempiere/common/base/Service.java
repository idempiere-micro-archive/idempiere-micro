package osgi.org.idempiere.common.base;

import org.idempiere.common.base.IServiceLocator;
import org.osgi.service.component.annotations.Component;
import osgi.org.idempiere.common.base.ds.DynamicServiceLocator;

@Component
public class Service extends org.idempiere.common.base.Service {
    @Override
    protected IServiceLocator getLocator() {
        return new DynamicServiceLocator();
    }

    static void setup() {
        org.idempiere.common.base.Service.instance = new Service();
    }
}
