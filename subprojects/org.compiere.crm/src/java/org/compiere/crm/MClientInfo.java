package org.compiere.crm;

import org.compiere.orm.MClient;
import org.idempiere.common.util.CLogger;
import org.idempiere.common.util.DB;
import org.idempiere.common.util.Env;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;

public class MClientInfo extends org.compiere.orm.MClientInfo
{
    /**
     *
     */
    private static final long serialVersionUID = 4861006368856890116L;


    /**
     * 	Get optionally cached client
     *	@param ctx context
     *	@return client
     */
    public static MClientInfo get (Properties ctx)
    {
        return get (ctx, Env.getAD_Client_ID(ctx), null);
    }	//	get

    /**	Logger						*/
    private static CLogger s_log = CLogger.getCLogger (MClientInfo.class);




    /** New Record					*/
    private boolean				m_createNew = false;


    /**
     * 	Overwrite Save
     * 	@overwrite
     *	@return true if saved
     */
    public boolean save ()
    {
        if (getAD_Org_ID() != 0)
            setAD_Org_ID(0);
        if (m_createNew)
            return super.save ();
        return saveUpdate();
    }	//	save

    /**
     * 	Load Constructor
     *	@param ctx context
     *	@param rs result set
     *	@param trxName transaction
     */
    public MClientInfo (Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }

    public MClientInfo (MClient client, int AD_Tree_Org_ID, int AD_Tree_BPartner_ID,
                        int AD_Tree_Project_ID, int AD_Tree_SalesRegion_ID, int AD_Tree_Product_ID,
                        int AD_Tree_Campaign_ID, int AD_Tree_Activity_ID, String trxName) {
        super(client, AD_Tree_Org_ID, AD_Tree_BPartner_ID,
                AD_Tree_Project_ID, AD_Tree_SalesRegion_ID, AD_Tree_Product_ID,
                AD_Tree_Campaign_ID, AD_Tree_Activity_ID, trxName);
    }

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
                info = new MClientInfo(ctx, rs, null);
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

}	//	MClientInfo
