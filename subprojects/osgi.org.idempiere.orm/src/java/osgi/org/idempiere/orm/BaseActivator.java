package osgi.org.idempiere.orm;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class BaseActivator implements BundleActivator {
    @Override
    public void start(BundleContext context) throws Exception {
        EventManager.setup();
    }

    @Override
    public void stop(BundleContext context) throws Exception {

    }
}
