package org.compiere.impl;

import java.util.Properties;

public class MRole extends org.compiere.orm.MRole {
    /**
     * 	Overwrite Client Org if different
     *	@param po persistent object
     */
    protected void setClientOrg (org.idempiere.orm.PO po)
    {
        super.setClientOrg (po);
    }	//	setClientOrg

    public MRole (Properties ctx, int AD_Role_ID, String trxName) {
        super( ctx, AD_Role_ID, trxName);
    }

}
