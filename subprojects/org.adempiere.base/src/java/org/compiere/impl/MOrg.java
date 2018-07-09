package org.compiere.impl;

import org.compiere.model.I_AD_Org;
import org.compiere.orm.MClient;
import org.compiere.orm.MOrgInfo;
import org.compiere.orm.MRole;
import org.compiere.orm.MRoleOrgAccess;
import org.compiere.orm.MTree_Base;
import org.idempiere.common.util.CCache;
import org.idempiere.common.util.Env;

import java.util.Properties;

public class MOrg extends org.compiere.orm.MOrg {
    /**
     * 	After Save
     *	@param newRecord new Record
     *	@param success save success
     *	@return success
     */
    protected boolean afterSave (boolean newRecord, boolean success)
    {
        if (!success)
            return success;
        if (newRecord)
        {
            //	Info
            MOrgInfo info = new MOrgInfo (this);
            info.saveEx();
            //	Access
            MRoleOrgAccess.createForOrg (this);
            MRole role = MRole.getDefault(getCtx(), true);	//	reload
            role.set_TrxName(get_TrxName());
            role.loadAccess(true); // reload org access within transaction
            //	TreeNode
            insert_Tree(MTree_Base.TREETYPE_Organization);
        }
        if (newRecord || is_ValueChanged(I_AD_Org.COLUMNNAME_Value))
            update_Tree(MTree_Base.TREETYPE_Organization);
        //	Value/Name change
        if (!newRecord && (is_ValueChanged("Value") || is_ValueChanged("Name")))
        {
            MAccount.updateValueDescription(getCtx(), "AD_Org_ID=" + getAD_Org_ID(), get_TrxName());
            if ("Y".equals(Env.getContext(getCtx(), "$Element_OT")))
                MAccount.updateValueDescription(getCtx(), "AD_OrgTrx_ID=" + getAD_Org_ID(), get_TrxName());
        }

        return true;
    }	//	afterSave

    private static CCache<Integer,MOrg> s_cache	= new CCache<Integer, MOrg>(I_AD_Org.Table_Name, 50);

    public MOrg (Properties ctx, int AD_Org_ID, String trxName) {
        super(ctx, AD_Org_ID, trxName);
    }

    public MOrg (MClient client, String value, String name) {
        super (client, value, name);
    }

    /**
     * 	Get Org from Cache
     *	@param ctx context
     *	@param AD_Org_ID id
     *	@return MOrg
     */
    public static MOrg get (Properties ctx, int AD_Org_ID)
    {
        MOrg retValue = s_cache.get (AD_Org_ID);
        if (retValue != null)
            return retValue;
        retValue = new MOrg(ctx, AD_Org_ID, null);
        if (retValue.get_ID () != 0)
            s_cache.put (AD_Org_ID, retValue);
        return retValue;
    }	//	get

}
