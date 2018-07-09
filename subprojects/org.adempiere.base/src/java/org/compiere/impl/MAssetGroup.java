package org.compiere.impl;

import org.compiere.model.I_A_Asset_Group;
import org.compiere.orm.SetGetUtil;
import org.idempiere.common.util.CCache;

import java.util.Properties;

public class MAssetGroup extends org.compiere.product.MAssetGroup {
    /**
     * 	Default Constructor
     *	@param ctx context
     *	@param A_Asset_Group_ID
     */
    public MAssetGroup (Properties ctx, int A_Asset_Group_ID, String trxName)
    {
        super (ctx,A_Asset_Group_ID, trxName);
    }

    @Override
    protected boolean afterSave (boolean newRecord, boolean success)
    {
        if(!success)
        {
            return false;
        }
        //
        if (newRecord)
        {
            // If this is not the default group, then copy accounting settings from default group
            int default_id = getDefault_ID(SetGetUtil.wrap(this));
            if (default_id > 0 && default_id != get_ID())
            {
                for (MAssetGroupAcct acct : MAssetGroupAcct.forA_Asset_Group_ID(getCtx(), default_id))
                {
                    MAssetGroupAcct newAcct = acct.copy(this);
                    newAcct.saveEx(get_TrxName());
                }
            }
        }
        //
        return true;
    }

    private static CCache<Integer, MAssetGroup> s_cache = new CCache<Integer, MAssetGroup>(I_A_Asset_Group.Table_Name, 10, 0);

    /**
     * Get Asset Group [CACHE]
     * @param ctx context
     * @param A_Asset_Group_ID	asset group id
     * @return asset group or null
     */
    public static MAssetGroup get(Properties ctx, int A_Asset_Group_ID)
    {
        if (A_Asset_Group_ID <= 0)
            return null;
        // Try cache
        MAssetGroup ag = s_cache.get(A_Asset_Group_ID);
        if (ag != null)
            return ag;
        // Load
        ag = new MAssetGroup(ctx, A_Asset_Group_ID, null);
        if (ag != null && ag.get_ID() != A_Asset_Group_ID)
            ag = null;
        else
            s_cache.put(A_Asset_Group_ID, ag);
        //
        return ag;
    }

}
