package org.compiere.crm

import org.idempiere.common.util.DB
import software.hsharp.business.models.IDTOReady
import java.sql.Connection

abstract class SvrProcessBaseSql : SvrProcessBase() {
    protected abstract fun getSqlResult(cnn: Connection): IDTOReady
    protected abstract val isRO: Boolean

    override fun getResult(): IDTOReady {
        val cnn = if (isRO) {
            DB.getConnectionRO() } else { DB.getConnectionRW() }
        try {
            val result = getSqlResult(cnn)
            return result
        } finally {
            cnn.close()
        }
    }
}