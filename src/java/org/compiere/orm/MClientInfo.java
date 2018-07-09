package org.compiere.orm;

import org.idempiere.common.util.CCache;
import org.idempiere.common.util.DB;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;

public class MClientInfo extends X_AD_ClientInfo {
    /**	Cache						*/
    protected static CCache<Integer,MClientInfo> s_cache = new CCache<Integer,MClientInfo>(Table_Name, 2);

    /**************************************************************************
     *	Standard Constructor
     *	@param ctx context
     *	@param ignored ignored
     *	@param trxName transaction
     */
    public MClientInfo (Properties ctx, int ignored, String trxName)
    {
        super (ctx, ignored, trxName);
        if (ignored != 0)
            throw new IllegalArgumentException("Multi-Key");
    }	//	MClientInfo

    /**
     * 	Load Constructor
     *	@param ctx context
     *	@param rs result set
     *	@param trxName transaction
     */
    public MClientInfo (Properties ctx, ResultSet rs, String trxName)
    {
        super(ctx, rs, trxName);
    }	//	MClientInfo

    /**
     * 	Parent Constructor
     *	@param client client
     *	@param AD_Tree_Org_ID org tree
     *	@param AD_Tree_BPartner_ID bp tree
     *	@param AD_Tree_Project_ID project tree
     *	@param AD_Tree_SalesRegion_ID sr tree
     *	@param AD_Tree_Product_ID product tree
     *	@param AD_Tree_Campaign_ID campaign tree
     *	@param AD_Tree_Activity_ID activity tree
     *	@param trxName transaction
     */
    public MClientInfo (MClient client, int AD_Tree_Org_ID, int AD_Tree_BPartner_ID,
                        int AD_Tree_Project_ID, int AD_Tree_SalesRegion_ID, int AD_Tree_Product_ID,
                        int AD_Tree_Campaign_ID, int AD_Tree_Activity_ID, String trxName)
    {
        super (client.getCtx(), 0, trxName);
        setAD_Client_ID(client.getAD_Client_ID());	//	to make sure
        setAD_Org_ID(0);
        setIsDiscountLineAmt (false);
        //
        setAD_Tree_Menu_ID(10);		//	HARDCODED
        //
        setAD_Tree_Org_ID(AD_Tree_Org_ID);
        setAD_Tree_BPartner_ID(AD_Tree_BPartner_ID);
        setAD_Tree_Project_ID(AD_Tree_Project_ID);
        setAD_Tree_SalesRegion_ID(AD_Tree_SalesRegion_ID);
        setAD_Tree_Product_ID(AD_Tree_Product_ID);
        setAD_Tree_Campaign_ID(AD_Tree_Campaign_ID);
        setAD_Tree_Activity_ID(AD_Tree_Activity_ID);
        //
        m_createNew = true;
    }	//	MClientInfo

    /**
     * 	Get Client Info
     * 	@param ctx context
     * 	@param AD_Client_ID id
     * 	@return Client Info
     */
    public static MClientInfo get (Properties ctx, int AD_Client_ID)
    {
        return get(ctx, AD_Client_ID, null);
    }	//	get

    /**
     * 	Get Client Info
     * 	@param ctx context
     * 	@param AD_Client_ID id
     * 	@param trxName optional trx
     * 	@return Client Info
     */
    public static MClientInfo get (Properties ctx, int AD_Client_ID, String trxName)
    {
        Integer key = new Integer (AD_Client_ID);
        MClientInfo info = (MClientInfo)s_cache.get(key);
        if (info != null)
            return info;
        //
        String sql = "SELECT * FROM AD_ClientInfo WHERE AD_Client_ID=?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try
        {
            pstmt = DB.prepareStatement (sql, trxName);
            pstmt.setInt (1, AD_Client_ID);
            rs = pstmt.executeQuery ();
            if (rs.next ())
            {
                info = new MClientInfo (ctx, rs, null);
                if (trxName == null)
                    s_cache.put (key, info);
            }
        }
        catch (SQLException ex)
        {
            s_log.log(Level.SEVERE, sql, ex);
        }
        finally
        {
            DB.close(rs, pstmt);
            rs = null;
            pstmt = null;
        }
        //
        return info;
    }	//	get


}
