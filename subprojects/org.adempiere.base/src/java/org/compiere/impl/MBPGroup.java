package org.compiere.impl;

import java.util.Properties;

public class MBPGroup extends X_C_BP_Group {
    /**
     * 	After Save
     *	@param newRecord new record
     *	@param success success
     *	@return success
     */
    @Override
    protected boolean afterSave (boolean newRecord, boolean success)
    {
        if (newRecord && success)
            return insert_Accounting("C_BP_Group_Acct", "C_AcctSchema_Default", null);
        return success;
    }	//	afterSave

    public MBPGroup (Properties ctx, int C_BP_Group_ID, String trxName) {
        super (ctx, C_BP_Group_ID, trxName);
    }

}
