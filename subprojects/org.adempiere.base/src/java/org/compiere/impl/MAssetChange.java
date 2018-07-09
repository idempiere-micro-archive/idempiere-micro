package org.compiere.impl;

import org.compiere.orm.MRefList;

import java.util.Properties;

public class MAssetChange extends org.compiere.product.MAssetChange {
    public MAssetChange (Properties ctx, int A_Asset_Change_ID, String trxName)
    {
        super (ctx, A_Asset_Change_ID, trxName);
    }	//

    /**
     * 	Before Save
     *	@param newRecord new
     *	@return true
     */
    @Override
    protected boolean beforeSave (boolean newRecord)
    {
        String textDetails = getTextDetails();
        if (textDetails == null || textDetails.length() == 0) {
            setTextDetails(MRefList.getListDescription (getCtx(),"A_Update_Type" , getChangeType()));
        }
        return true;
    }	//	beforeSave

    public static org.compiere.product.MAssetChange createAddition(MAssetAddition assetAdd, MDepreciationWorkfile assetwk) {
        org.compiere.product.MAssetChange change = new org.compiere.product.MAssetChange(assetAdd.getCtx(), 0, assetAdd.get_TrxName());
        change.setAD_Org_ID(assetAdd.getAD_Org_ID()); //@win added
        change.setA_Asset_ID(assetAdd.getA_Asset_ID());
        change.setA_QTY_Current(assetAdd.getA_QTY_Current());
        change.setChangeType("ADD");
        change.setTextDetails(MRefList.getListDescription (assetAdd.getCtx(),"A_Update_Type" , "ADD"));
        change.setPostingType(assetwk.getPostingType());
        change.setAssetValueAmt(assetAdd.getAssetValueAmt());
        change.setA_QTY_Current(assetAdd.getA_QTY_Current());
        change.saveEx();

        return change;
    }

}
