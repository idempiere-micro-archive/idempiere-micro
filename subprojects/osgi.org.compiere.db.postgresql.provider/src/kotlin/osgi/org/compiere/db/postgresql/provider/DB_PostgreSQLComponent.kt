package osgi.org.compiere.db.postgresql.provider

import org.idempiere.icommon.db.AdempiereDatabase
import org.osgi.service.component.annotations.Component
import pg.org.compiere.db.DB_PostgreSQL
import java.net.URL

@Component
class DB_PostgreSQLComponent : DB_PostgreSQL(), AdempiereDatabase {
    override fun getServerPoolDefaultPropertiesUrl(): URL {
        return PostgreSQLBundleActivator.bundleContext!!.getBundle().getEntry("server.pool.default.properties")
    }
}