package org.compiere.orm;

import org.compiere.util.DisplayType;
import org.idempiere.common.db.CConnection;
import org.idempiere.common.db.Database;
import org.idempiere.common.exceptions.DBException;
import org.idempiere.common.util.CCache;
import org.idempiere.common.util.DB;
import org.idempiere.common.util.Env;
import org.idempiere.common.util.Ini;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;

public class MSystem extends X_AD_System {
    /**************************************************************************
     * 	Default Constructor
     *	@param ctx context
     *	@param ignored id
     *	@param mtrxName transaction
     */
    public MSystem (Properties ctx, int ignored, String mtrxName)
    {
        super(ctx, 0, mtrxName);
        String trxName = null;
        load(trxName);	//	load ID=0
        if (s_system.get(0) == null)
            s_system.put(0, this);
    }	//	MSystem

    /**
     * 	Load Constructor
     * 	@param ctx context
     * 	@param rs result set
     * 	@param trxName transaction
     */
    public MSystem (Properties ctx, ResultSet rs, String trxName)
    {
        super (ctx, rs, trxName);
        if (s_system.get(0) == null)
            s_system.put(0, this);
    }	//	MSystem

    /**
     * 	Constructor
     */
    public MSystem ()
    {
        this (new Properties(), 0, null);
    }	//	MSystem

    /** System - cached					*/
    private static CCache<Integer,MSystem> s_system = new CCache<Integer,MSystem>(Table_Name, 1, -1, true);

    /**
     * 	Load System Record
     *	@param ctx context
     *	@return System
     */
    public synchronized static MSystem get (Properties ctx)
    {
        if (s_system.get(0) != null)
            return s_system.get(0);
        //
        MSystem system = new Query(ctx, Table_Name, null, null)
                .setOrderBy(COLUMNNAME_AD_System_ID)
                .firstOnly();
        if (system == null)
            return null;
        //
        if (!Ini.getIni().isClient() && system.setInfo())
        {
            system.saveEx();
        }
        s_system.put(0, system);
        return system;
    }	//	get

    /**************************************************************************
     * 	Set/Derive Info if more then a day old
     * 	@return true if set
     */
    public boolean setInfo()
    {
        if (!TimeUtil.getDay(getUpdated()).before(TimeUtil.getDay(null)))
            return false;
        try
        {
            setDBInfo();
            setInternalUsers();
            if (isAllowStatistics())
            {
                setStatisticsInfo(getStatisticsInfo(true));
                setProfileInfo(getProfileInfo(true));
            }
        }
        catch (Exception e)
        {
            setSupportUnits(9999);
            setInfo(e.getLocalizedMessage());
            log.log(Level.SEVERE, "", e);
        }
        return true;
    }	//	setInfo

    /**
     * 	Set DB Info
     */
    private void setDBInfo()
    {
        String dbAddress = CConnection.get().getConnectionURL();
        setDBAddress(dbAddress.toLowerCase());
        //
        if (!Ini.getIni().isClient())
        {
            int noProcessors = Runtime.getRuntime().availableProcessors();
            setNoProcessors(noProcessors);
        }
        //
        String dbName = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sql = null;
        try
        {
            String dbType = CConnection.get().getDatabase().getName();
            sql = getDBInfoSQL(dbType);
            pstmt = DB.prepareStatement (sql, null);
            rs = pstmt.executeQuery ();
            if (rs.next())
            {
                //	dbAddress = rs.getString(1);
                dbName = rs.getString(2);
                setDBInstance(dbName.toLowerCase());
            }
        }
        catch (SQLException e)
        {
            throw new DBException(e, sql);
        }
        finally
        {
            DB.close(rs, pstmt);
            rs = null; pstmt = null;
        }
    }	//	setDBInfo

    /**
     * 	Get DB Info SQL
     *	@param dbType database type
     *	@return sql
     */
    public static String getDBInfoSQL (String dbType)
    {
        if (Database.DB_ORACLE.equals(dbType))
            return "SELECT SYS_CONTEXT('USERENV','HOST') || '/' || SYS_CONTEXT('USERENV','IP_ADDRESS') AS DBAddress,"
                    + "	SYS_CONTEXT('USERENV','CURRENT_USER') || '.' || SYS_CONTEXT('USERENV','DB_NAME')"
                    + " || '.' || SYS_CONTEXT('USERENV','DB_DOMAIN') AS DBName "
                    + "FROM DUAL";
        //
        return "SELECT NULL,NULL FROM AD_System WHERE AD_System_ID=-1";
    }	//	getDBInfoSQL

    /**
     * 	Set Internal User Count
     */
    private void setInternalUsers()
    {
        final String sql = "SELECT COUNT(DISTINCT (u.AD_User_ID)) AS iu "
                + "FROM AD_User u"
                + " INNER JOIN AD_User_Roles ur ON (u.AD_User_ID=ur.AD_User_ID) "
                + "WHERE u.AD_Client_ID<>11"			//	no Demo
                + " AND u.AD_User_ID NOT IN (0,100)";	//	no System/SuperUser
        int internalUsers = DB.getSQLValue(null, sql);
        setSupportUnits(internalUsers);
    }	//	setInternalUsers


    /**
     * 	Get Statistics Info
     * 	@param recalc recalculate
     *	@return statistics
     */
    public String getStatisticsInfo (boolean recalc)
    {
        String s = super.getStatisticsInfo ();
        if (s == null || recalc)
        {
            String sql = "SELECT 'C'||(SELECT " + DB.TO_CHAR("COUNT(*)", DisplayType.Number, Env.getAD_Language(Env.getCtx())) + " FROM AD_Client)"
                    + "||'U'|| (SELECT " + DB.TO_CHAR("COUNT(*)", DisplayType.Number, Env.getAD_Language(Env.getCtx())) + " FROM AD_User)"
                    + "||'B'|| (SELECT " + DB.TO_CHAR("COUNT(*)", DisplayType.Number, Env.getAD_Language(Env.getCtx())) + " FROM C_BPartner)"
                    + "||'P'|| (SELECT " + DB.TO_CHAR("COUNT(*)", DisplayType.Number, Env.getAD_Language(Env.getCtx())) + " FROM M_Product)"
                    + "||'I'|| (SELECT " + DB.TO_CHAR("COUNT(*)", DisplayType.Number, Env.getAD_Language(Env.getCtx())) + " FROM C_Invoice)"
                    + "||'L'|| (SELECT " + DB.TO_CHAR("COUNT(*)", DisplayType.Number, Env.getAD_Language(Env.getCtx())) + " FROM C_InvoiceLine)"
                    + "||'M'|| (SELECT " + DB.TO_CHAR("COUNT(*)", DisplayType.Number, Env.getAD_Language(Env.getCtx())) + " FROM M_Transaction)"
                    + " FROM AD_System";
            s = DB.getSQLValueString(null, sql);
        }
        return s;
    }	//	getStatisticsInfo

    /**
     * 	Get Profile Info
     * 	@param recalc recalculate
     *	@return profile
     */
    public String getProfileInfo (boolean recalc)
    {
        String s = super.getProfileInfo ();
        if (s == null || recalc)
        {
            final String sql = "SELECT Value FROM AD_Client "
                    + " WHERE IsActive='Y' ORDER BY AD_Client_ID DESC";
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            StringBuilder sb = new StringBuilder();
            try
            {
                pstmt = DB.prepareStatement (sql, null);
                rs = pstmt.executeQuery ();
                while (rs.next ())
                {
                    sb.append(rs.getString(1)).append('|');
                }
            }
            catch (SQLException e)
            {
                throw new DBException(e, sql);
            }
            finally
            {
                DB.close(rs, pstmt);
                rs = null; pstmt = null;
            }
            s = sb.toString();
        }
        return s;
    }	//	getProfileInfo
}
