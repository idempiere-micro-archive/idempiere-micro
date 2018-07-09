package org.compiere.impl;

import org.compiere.crm.MLocationLookup;
import org.compiere.lookups.MLookup;
import org.compiere.lookups.MLookupFactory;
import org.compiere.lookups.MLookupInfo;
import org.compiere.model.I_AD_Process_Para;
import org.compiere.process.MProcess;
import org.compiere.util.DisplayType;
import org.idempiere.common.util.CCache;
import org.idempiere.common.util.Env;

import java.sql.ResultSet;
import java.util.Properties;
import java.util.logging.Level;

public class MProcessPara extends org.compiere.process.MProcessPara {
    public MProcessPara(Properties ctx, int AD_Process_Para_ID, String trxName) {
        super(ctx, AD_Process_Para_ID, trxName);
    }

    public MProcessPara(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }

    public MProcessPara(MProcess parent) {
        super(parent);
    }

    /**
     *  Set Lookup for columns with lookup
     */
    @Override
    public void loadLookup()
    {
        if (!isLookup())
            return;
        if (log.isLoggable(Level.FINE)) log.fine("(" + getColumnName() + ")");
        int displayType = getAD_Reference_ID();
        if (DisplayType.isLookup(displayType))
        {
            MLookupInfo lookupInfo = MLookupFactory.getLookupInfo(getCtx(), 0,
                    getAD_Process_Para_ID(), getAD_Reference_ID(),
                    Env.getLanguage(getCtx()), getColumnName(),
                    getAD_Reference_Value_ID(), false, "");
            if (lookupInfo == null)
            {
                log.log(Level.SEVERE, "(" + getColumnName() + ") - No LookupInfo");
                return;
            }
            //	Prevent loading of CreatedBy/UpdatedBy
            if (displayType == DisplayType.Table
                    && (getColumnName().equals("CreatedBy") || getColumnName().equals("UpdatedBy")) )
            {
                lookupInfo.IsCreadedUpdatedBy = true;
                lookupInfo.DisplayType = DisplayType.Search;
            }
            //
            MLookup ml = new MLookup (lookupInfo, TAB_NO);
            m_lookup = ml;
        }
        else if (displayType == DisplayType.Location)   //  not cached
        {
            MLocationLookup ml = new MLocationLookup (getCtx(), WINDOW_NO);
            m_lookup = ml;
        }
        else if (displayType == DisplayType.Locator)
        {
            MLocatorLookup ml = new MLocatorLookup (getCtx(), WINDOW_NO);
            m_lookup = ml;
        }
        else if (displayType == DisplayType.Account)    //  not cached
        {
            MAccountLookup ma = new MAccountLookup (getCtx(), WINDOW_NO);
            m_lookup = ma;
        }
        else if (displayType == DisplayType.PAttribute)    //  not cached
        {
            MPAttributeLookup pa = new MPAttributeLookup (getCtx(), WINDOW_NO);
            m_lookup = pa;
        }
        //
        if (m_lookup != null)
            m_lookup.loadComplete();
    }   //  loadLookup

    /**
     * 	Get MProcessPara from Cache
     *	@param ctx context
     *	@param AD_Process_Para_ID id
     *	@return MProcessPara
     */
    public static MProcessPara get (Properties ctx, int AD_Process_Para_ID)
    {
        Integer key = new Integer (AD_Process_Para_ID);
        MProcessPara retValue = (MProcessPara)s_cache.get (key);
        if (retValue != null)
            return retValue;
        retValue = new MProcessPara (ctx, AD_Process_Para_ID, null);
        if (retValue.get_ID () != 0)
            s_cache.put (key, retValue);
        return retValue;
    }	//	get

    /**	Cache						*/
    private static CCache<Integer, MProcessPara> s_cache
            = new CCache<Integer, MProcessPara> (I_AD_Process_Para.Table_Name, 20);
}
