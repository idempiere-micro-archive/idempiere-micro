package osgi.org.idempiere.common.db

import org.idempiere.icommon.db.AdempiereDatabase
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference

@Component
class Database : org.idempiere.common.db.Database() {
    @Reference
    fun setDatabaseImpl(databaseService: AdempiereDatabase) {
        org.idempiere.common.db.Database.databaseService = databaseService
    }
}