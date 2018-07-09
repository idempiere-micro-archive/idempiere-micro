package org.compiere.model;

import org.compiere.model.IDoc;
import org.idempiere.icommon.model.IPO;

public interface IPODoc extends IPO {
    /**
     *      Set the accounting document associated to the PO - for use in POST ModelValidator
     *      @param doc Document
     */
    void setDoc(IDoc doc);

    int get_ID();

    boolean load(String trxName);

    int get_ColumnIndex(String docStatus);

    Object get_Value(int index);

    void set_TrxName(String m_trxName);

    boolean isActive();

    int getUpdatedBy();

    void setAD_Org_ID(int ad_org_id);

    void saveEx();
}
