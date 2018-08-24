package org.compiere.bo

import org.compiere.model.I_C_BPartner
import org.idempiere.common.util.Trx

class UpdateCustomer : CustomerProcessBase() {
    override fun getData(m_trx: Trx): I_C_BPartner {
        if (id < 1) { throw IllegalArgumentException() }
        return modelFactory.getPO(I_C_BPartner.Table_Name, id, m_trx.trxName) as I_C_BPartner
    }

    override val trxName: String
        get() = "UpdateCustomer"

    var id: Int = 0

    override fun prepare() {
        super.prepare()
        for (para in parameter) {
            if (para.parameterName == "id") {
                id = para.parameterAsInt
            }
        }
    }
}