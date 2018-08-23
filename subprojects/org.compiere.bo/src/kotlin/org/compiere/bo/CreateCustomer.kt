package org.compiere.bo

import org.compiere.crm.MBPartner
import org.compiere.model.I_C_BPartner
import org.idempiere.common.util.Env
import org.idempiere.common.util.Trx

class CreateCustomer : CustomerProcessBase() {
    override fun getData(m_trx: Trx): I_C_BPartner {
        return MBPartner.getTemplate(Env.getCtx(), AD_CLIENT_ID)
    }

    override val trxName: String
        get() = "CreateCustomer"
}