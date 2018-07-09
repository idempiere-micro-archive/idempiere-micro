package org.compiere.impl;

import org.compiere.lookups.MLookupFactory;
import org.compiere.model.I_AD_Table;
import org.idempiere.common.util.CCache;
import org.idempiere.common.util.Env;
import org.idempiere.orm.Lookup;

import java.util.Properties;

public class POInfo extends org.idempiere.orm.POInfo {
    private POInfo(Properties ctx, int AD_Table_ID, boolean baseLanguageOnly) {
        super(ctx, AD_Table_ID, baseLanguageOnly);
    }

    protected POInfo(Properties ctx, int AD_Table_ID, boolean baseLanguageOnly, String trxName) {
        super(ctx, AD_Table_ID, baseLanguageOnly, trxName);
    }

    /**
     *  Get Lookup
     *  @param index index
     *  @return Lookup
     */
    public Lookup getColumnLookup (int index)
    {
        if (!isColumnLookup(index))
            return null;
        //
        int WindowNo = 0;
        //  List, Table, TableDir
        Lookup lookup = null;
        try
        {
            lookup = MLookupFactory.get (m_ctx, WindowNo,
                    m_columns[index].AD_Column_ID, m_columns[index].DisplayType,
                    Env.getLanguage(m_ctx), m_columns[index].ColumnName,
                    m_columns[index].AD_Reference_Value_ID,
                    m_columns[index].IsParent, m_columns[index].ValidationCode);
        }
        catch (Exception e)
        {
            lookup = null;          //  cannot create Lookup
        }
        return lookup;
        /** @todo other lookup types */
    }   //  getColumnLookup

    /** Cache of POInfo     */
    private static CCache<Integer, POInfo> s_cache = new CCache<Integer, POInfo>(I_AD_Table.Table_Name, "POInfo", 200);

    /**
     *  POInfo Factory
     *  @param ctx context
     *  @param AD_Table_ID AD_Table_ID
     *  @param trxName Transaction name
     *  @return POInfo
     */
    public static synchronized POInfo getPOInfo (Properties ctx, int AD_Table_ID, String trxName)
    {
        Integer key = Integer.valueOf(AD_Table_ID);
        POInfo retValue = s_cache.get(key);
        if (retValue == null)
        {
            retValue = new POInfo(ctx, AD_Table_ID, false, trxName);
            if (retValue.getColumnCount() == 0)
                //	May be run before Language verification
                retValue = new POInfo(ctx, AD_Table_ID, true, trxName);
            else
                s_cache.put(key, retValue);
        }
        return retValue;
    }   //  getPOInfo



}
