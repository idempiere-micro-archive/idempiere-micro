package osgi.org.compiere.db.postgresql.provider

import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext

class PostgreSQLBundleActivator : BundleActivator {
    companion object {
        var bundleContext: BundleContext? = null
    }

    override fun start(context: BundleContext?) {
        bundleContext = context
    }

    override fun stop(context: BundleContext?) {
        bundleContext = null
    }
}