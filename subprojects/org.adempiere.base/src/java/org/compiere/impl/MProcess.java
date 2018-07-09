package org.compiere.impl;

import org.compiere.model.I_AD_Process;
import org.compiere.orm.MRole;
import org.compiere.process.MProcessAccess;
import org.compiere.wf.MWFNode;
import org.idempiere.common.util.CCache;

import java.sql.ResultSet;
import java.util.Properties;

public class MProcess extends org.compiere.process.MProcess {

    private static CCache<Integer,MProcess> s_cache	= new CCache<Integer,MProcess>(I_AD_Process.Table_Name, 20);

    public MProcess(Properties ctx, int AD_Process_ID, String trxName) {
        super(ctx, AD_Process_ID, trxName);
    }

    public MProcess(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }

    /**
     * 	Get MProcess from Cache
     *	@param ctx context
     *	@param AD_Process_ID id
     *	@return MProcess
     */
    public static MProcess get (Properties ctx, int AD_Process_ID)
    {
        Integer key = new Integer (AD_Process_ID);
        MProcess retValue = (MProcess) s_cache.get (key);
        if (retValue != null)
            return retValue;
        retValue = new MProcess (ctx, AD_Process_ID, null);
        if (retValue.get_ID () != 0)
            s_cache.put (key, retValue);
        return retValue;
    }	//	get

    /**
     * 	After Save
     *	@param newRecord new
     *	@param success success
     *	@return success
     */
    @Override
    protected boolean afterSave (boolean newRecord, boolean success)
    {
        if (!success)
            return success;
        if (newRecord)	//	Add to all automatic roles
        {
            org.compiere.orm.MRole[] roles = MRole.getOf(getCtx(), "IsManual='N'");
            for (int i = 0; i < roles.length; i++)
            {

                MProcessAccess pa = new MProcessAccess(this, roles[i].getAD_Role_ID());
                pa.saveEx();
            }
        }
        //	Menu/Workflow
        else if (is_ValueChanged("IsActive") || is_ValueChanged("Name")
                || is_ValueChanged("Description") || is_ValueChanged("Help"))
        {
            MMenu[] menues = MMenu.get(getCtx(), "AD_Process_ID=" + getAD_Process_ID(), get_TrxName());
            for (int i = 0; i < menues.length; i++)
            {
                menues[i].setIsActive(isActive());
                menues[i].setName(getName());
                menues[i].setDescription(getDescription());
                menues[i].saveEx();
            }
            MWFNode[] nodes = MWindow.getWFNodes(getCtx(), "AD_Process_ID=" + getAD_Process_ID(), get_TrxName());
            for (int i = 0; i < nodes.length; i++)
            {
                boolean changed = false;
                if (nodes[i].isActive() != isActive())
                {
                    nodes[i].setIsActive(isActive());
                    changed = true;
                }
                if (nodes[i].isCentrallyMaintained())
                {
                    nodes[i].setName(getName());
                    nodes[i].setDescription(getDescription());
                    nodes[i].setHelp(getHelp());
                    changed = true;
                }
                if (changed)
                    nodes[i].saveEx();
            }
        }
        return success;
    }	//	afterSave
}
