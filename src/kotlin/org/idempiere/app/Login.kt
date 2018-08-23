package org.idempiere.app

import org.compiere.crm.MUser
import org.compiere.util.Msg
import org.osgi.service.component.annotations.Component
import software.hsharp.core.models.INameKeyPair
import software.hsharp.core.services.ILoginUtility
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.util.logging.Level
import org.compiere.model.I_M_Warehouse
import org.compiere.crm.MCountry
import org.compiere.orm.MRole
import org.compiere.orm.MSysConfig
import org.compiere.orm.MSystem
import org.compiere.orm.Query
import org.compiere.validation.ModelValidationEngine
import org.idempiere.common.util.CLogger
import org.idempiere.common.util.DB
import org.idempiere.common.util.Env
import org.idempiere.common.util.KeyNamePair
import org.idempiere.common.util.Ini
import org.idempiere.common.util.Util
import java.util.Date
import java.util.Properties
import org.compiere.orm.MTree_Base

@Component
class Login : ILoginUtility {
    private val log = CLogger.getCLogger(Login::class.java)
    private var loginErrMsg: String? = null
    private var isPasswordExpired: Boolean = false

    override fun getClients(role: INameKeyPair): Array<INameKeyPair> {
        val m_ctx = Env.getCtx()
        val list = mutableListOf<KeyNamePair>()
        val sql = ("SELECT DISTINCT r.UserLevel, r.ConnectionProfile, " + // 	1/2

                " c.AD_Client_ID,c.Name " + // 	3/4

                "FROM AD_Role r" +
                " INNER JOIN AD_Client c ON (r.AD_Client_ID=c.AD_Client_ID) " +
                "WHERE r.AD_Role_ID=?" + // 	#1

                " AND r.IsActive='Y' AND c.IsActive='Y'")

        var pstmt: PreparedStatement? = null
        var rs: ResultSet? = null
        // 	get Role details
        try {
            pstmt = DB.prepareStatement(sql, null)
            pstmt!!.setInt(1, role.Key)
            rs = pstmt.executeQuery()

            if (!rs!!.next()) {
                log.log(Level.SEVERE, "No Clients for Role: " + role.toString())
                return arrayOf()
            }

            //  Role Info
            Env.setContext(m_ctx, "#AD_Role_ID", role.Key)
            Env.setContext(m_ctx, "#AD_Role_Name", role.name)
            Ini.getIni().setProperty(Ini.getIni().P_ROLE, role.name)
            // 	User Level
            Env.setContext(m_ctx, "#User_Level", rs.getString(1)) // 	Format 'SCO'

            //  load Clients
            do {
                val AD_Client_ID = rs.getInt(3)
                val Name = rs.getString(4)
                val p = KeyNamePair(AD_Client_ID, Name)
                list.add(p)
            } while (rs.next())
            //
            if (log.isLoggable(Level.FINE)) log.fine("Role: " + role.toString() + " - clients #" + list.count())
        } catch (ex: SQLException) {
            log.log(Level.SEVERE, sql, ex)
        } finally {
            DB.close(rs, pstmt)
        }
        return list.toTypedArray()
    }

    override fun getClients(app_user: String, app_pwd: String): Array<INameKeyPair> {
        val m_ctx = Env.getCtx()
        if (log.isLoggable(Level.INFO)) log.info("User=$app_user")

        if (Util.isEmpty(app_user)) {
            log.warning("No Apps User")
            return arrayOf()
        }

        // 	Authentication
        var authenticated = false
        MSystem.get(m_ctx) ?: throw IllegalStateException("No System Info")

        if (app_pwd == "") {
            log.warning("No Apps Password")
            return arrayOf()
        }

        val hash_password = MSysConfig.getBooleanValue(MSysConfig.USER_PASSWORD_HASH, false)
        val email_login = MSysConfig.getBooleanValue(MSysConfig.USE_EMAIL_FOR_LOGIN, false)
        val clientList = ArrayList<KeyNamePair>()
        val clientsValidated = ArrayList<Int>()

        val where = StringBuilder("Password IS NOT NULL AND ")
        if (email_login)
            where.append("EMail=?")
        else
            where.append("COALESCE(LDAPUser,Name)=?")
        where.append(" AND")
                .append(" EXISTS (SELECT * FROM AD_User_Roles ur")
                .append("         INNER JOIN AD_Role r ON (ur.AD_Role_ID=r.AD_Role_ID)")
                .append("         WHERE ur.AD_User_ID=AD_User.AD_User_ID AND ur.IsActive='Y' AND r.IsActive='Y') AND ")
                .append(" EXISTS (SELECT * FROM AD_Client c")
                .append("         WHERE c.AD_Client_ID=AD_User.AD_Client_ID")
                .append("         AND c.IsActive='Y') AND ")
                .append(" AD_User.IsActive='Y'")

        val users: List<MUser> = Query(m_ctx, MUser.Table_Name, where.toString(), null)
                .setParameters(app_user)
                .setOrderBy(MUser.COLUMNNAME_AD_User_ID)
                .list()

        if (users.size == 0) {
            log.saveError("UserPwdError", app_user, false)
            return arrayOf()
        }

        val MAX_ACCOUNT_LOCK_MINUTES = MSysConfig.getIntValue(MSysConfig.USER_LOCKING_MAX_ACCOUNT_LOCK_MINUTES, 0)
        val MAX_INACTIVE_PERIOD_DAY = MSysConfig.getIntValue(MSysConfig.USER_LOCKING_MAX_INACTIVE_PERIOD_DAY, 0)
        val MAX_PASSWORD_AGE = MSysConfig.getIntValue(MSysConfig.USER_LOCKING_MAX_PASSWORD_AGE_DAY, 0)
        val now = Date().time
        for (user in users) {
            if (MAX_ACCOUNT_LOCK_MINUTES > 0 && user.isLocked() && user.getDateAccountLocked() != null) {
                val minutes = (now - user.getDateAccountLocked().getTime()) / (1000 * 60)
                if (minutes > MAX_ACCOUNT_LOCK_MINUTES) {
                    var inactive = false
                    if (MAX_INACTIVE_PERIOD_DAY > 0 && user.getDateLastLogin() != null) {
                        val days = (now - user.getDateLastLogin().getTime()) / (1000 * 60 * 60 * 24)
                        if (days > MAX_INACTIVE_PERIOD_DAY)
                            inactive = true
                    }

                    if (!inactive) {
                        user.setIsLocked(false)
                        user.setDateAccountLocked(null)
                        user.setFailedLoginCount(0)
                        if (!user.save())
                            log.severe("Failed to unlock user account")
                    }
                }
            }

            if (MAX_INACTIVE_PERIOD_DAY > 0 && !user.isLocked() && user.getDateLastLogin() != null) {
                val days = (now - user.getDateLastLogin().getTime()) / (1000 * 60 * 60 * 24)
                if (days > MAX_INACTIVE_PERIOD_DAY) {
                    user.setIsLocked(true)
                    user.setDateAccountLocked(Timestamp(now))
                    if (!user.save())
                        log.severe("Failed to lock user account")
                }
            }
        }

        var validButLocked = false
        for (user in users) {
            if (clientsValidated.contains(user.getAD_Client_ID())) {
                log.severe("Two users with password with the same name/email combination on same tenant: $app_user")
                return arrayOf()
            }
            clientsValidated.add(user.getAD_Client_ID())
            val valid = when {
                authenticated -> true
                hash_password -> user.authenticateHash(app_pwd)
                else -> // password not hashed
                    user.password != null && user.password == app_pwd
            }
            // authenticated by ldap

            if (valid) {
                if (user.isLocked()) {
                    validButLocked = true
                    continue
                }

                if (authenticated) {
                    // use Ldap because don't check password age
                } else if (user.isExpired())
                    isPasswordExpired = true
                else if (MAX_PASSWORD_AGE > 0 && !user.isNoPasswordReset()) {
                    if (user.getDatePasswordChanged() == null)
                        user.setDatePasswordChanged(Timestamp(now))

                    val days = (now - user.getDatePasswordChanged().getTime()) / (1000 * 60 * 60 * 24)
                    if (days > MAX_PASSWORD_AGE) {
                        user.setIsExpired(true)
                        isPasswordExpired = true
                    }
                }

                val sql = StringBuilder("SELECT  DISTINCT cli.AD_Client_ID, cli.Name, u.AD_User_ID, u.Name")
                sql.append(" FROM AD_User_Roles ur")
                        .append(" INNER JOIN AD_User u on (ur.AD_User_ID=u.AD_User_ID)")
                        .append(" INNER JOIN AD_Client cli on (ur.AD_Client_ID=cli.AD_Client_ID)")
                        .append(" WHERE ur.IsActive='Y'")
                        .append(" AND u.IsActive='Y'")
                        .append(" AND cli.IsActive='Y'")
                        .append(" AND ur.AD_User_ID=? ORDER BY cli.Name")
                var pstmt: PreparedStatement? = null
                var rs: ResultSet? = null
                try {
                    pstmt = DB.prepareStatement(sql.toString(), null)
                    pstmt!!.setInt(1, user.getAD_User_ID())
                    rs = pstmt.executeQuery()

                    while (rs!!.next()) {
                        val AD_Client_ID = rs.getInt(1)
                        val Name = rs.getString(2)
                        val p = KeyNamePair(AD_Client_ID, Name)
                        clientList.add(p)
                    }
                } catch (ex: SQLException) {
                    log.log(Level.SEVERE, sql.toString(), ex)
                } finally {
                    DB.close(rs, pstmt)
                }
            }
        }
        if (clientList.size > 0)
            authenticated = true

        if (authenticated) {
            if (log.isLoggable(Level.FINE)) log.fine("User=" + app_user + " - roles #" + clientList.count())

            for (user in users) {
                user.setFailedLoginCount(0)
                user.setDateLastLogin(Timestamp(now))
                if (!user.save())
                    log.severe("Failed to update user record with date last login (" + user + " / clientID = " + user.getAD_Client_ID() + ")")
            }
        } else if (validButLocked) {
            // User account ({0}) is locked, please contact the system administrator
            loginErrMsg = Msg.getMsg(m_ctx, "UserAccountLocked", arrayOf<Any>(app_user))
        } else {
            var foundLockedAccount = false
            for (user in users) {
                if (user.isLocked()) {
                    foundLockedAccount = true
                    continue
                }

                val count = user.getFailedLoginCount() + 1

                var reachMaxAttempt = false
                val MAX_LOGIN_ATTEMPT = MSysConfig.getIntValue(MSysConfig.USER_LOCKING_MAX_LOGIN_ATTEMPT, 0)
                if (MAX_LOGIN_ATTEMPT in 1..count) {
                    // Reached the maximum number of login attempts, user account ({0}) is locked
                    loginErrMsg = Msg.getMsg(m_ctx, "ReachedMaxLoginAttempts", arrayOf<Any>(app_user))
                    reachMaxAttempt = true
                } else if (MAX_LOGIN_ATTEMPT > 0) {
                    if (count == MAX_LOGIN_ATTEMPT - 1) {
                        // Invalid User ID or Password (Login Attempts: {0} / {1})
                        loginErrMsg = Msg.getMsg(m_ctx, "FailedLoginAttempt", arrayOf<Any>(count, MAX_LOGIN_ATTEMPT))
                        reachMaxAttempt = false
                    } else {
                        loginErrMsg = Msg.getMsg(m_ctx, "FailedLogin", true)
                    }
                } else {
                    reachMaxAttempt = false
                }

                user.setFailedLoginCount(count)
                user.setIsLocked(reachMaxAttempt)
                user.setDateAccountLocked(if (user.isLocked()) Timestamp(now) else null)
                if (!user.save())
                    log.severe("Failed to update user record with increase failed login count")
            }

            if (loginErrMsg == null && foundLockedAccount) {
                // User account ({0}) is locked, please contact the system administrator
                loginErrMsg = Msg.getMsg(m_ctx, "UserAccountLocked", arrayOf<Any>(app_user))
            }
        }
        return clientList.toTypedArray()
    }

    override fun getRoles(app_user: String, client: INameKeyPair): Array<INameKeyPair> {
        val m_ctx = Env.getCtx()

        val rolesList = ArrayList<KeyNamePair>()
        val sql = StringBuffer("SELECT u.AD_User_ID, r.AD_Role_ID,r.Name ")
                .append("FROM AD_User u")
                .append(" INNER JOIN AD_User_Roles ur ON (u.AD_User_ID=ur.AD_User_ID AND ur.IsActive='Y')")
                .append(" INNER JOIN AD_Role r ON (ur.AD_Role_ID=r.AD_Role_ID AND r.IsActive='Y') ")
        sql.append("WHERE u.Password IS NOT NULL AND ur.AD_Client_ID=? AND ")
        val email_login = MSysConfig.getBooleanValue(MSysConfig.USE_EMAIL_FOR_LOGIN, false)
        if (email_login)
            sql.append("u.EMail=?")
        else
            sql.append("COALESCE(u.LDAPUser,u.Name)=?")
        sql.append(" AND r.IsMasterRole='N'")
        sql.append(" AND u.IsActive='Y' AND EXISTS (SELECT * FROM AD_Client c WHERE u.AD_Client_ID=c.AD_Client_ID AND c.IsActive='Y')")
        // don't show roles without org access
        sql.append(" AND (")
        sql.append(" (r.isaccessallorgs='Y' OR EXISTS (SELECT 1 FROM AD_Role_OrgAccess ro WHERE ro.AD_Role_ID=r.AD_Role_ID AND ro.IsActive='Y'))")
        // show roll with isuseuserorgaccess = "Y" when Exist org in AD_User_Orgaccess
        sql.append(" OR ")
        sql.append(" (r.isuseuserorgaccess='Y' AND EXISTS (SELECT 1 FROM AD_User_Orgaccess uo WHERE uo.AD_User_ID=u.AD_User_ID AND uo.IsActive='Y')) ")
        sql.append(")")
        sql.append(" ORDER BY r.Name")

        var pstmt: PreparedStatement? = null
        var rs: ResultSet? = null
        // 	get Role details
        try {
            pstmt = DB.prepareStatement(sql.toString(), null)
            pstmt!!.setInt(1, client.Key)
            pstmt.setString(2, app_user)
            rs = pstmt.executeQuery()

            if (!rs!!.next()) {
                log.log(Level.SEVERE, "No Roles for Client: " + client.toString())
                return arrayOf()
            }

            //  load Roles
            do {
                val AD_Role_ID = rs.getInt(2)
                val Name = rs.getString(3)
                val p = KeyNamePair(AD_Role_ID, Name)
                rolesList.add(p)
            } while (rs.next())
            //
            if (log.isLoggable(Level.FINE)) log.fine("Role: " + client.toString() + " - clients #" + rolesList.count())
        } catch (ex: SQLException) {
            log.log(Level.SEVERE, sql.toString(), ex)
        } finally {
            DB.close(rs, pstmt)
        }
        // Client Info
        Env.setContext(m_ctx, "#AD_Client_ID", client.Key)
        Env.setContext(m_ctx, "#AD_Client_Name", client.name)
        Ini.getIni().setProperty(Ini.getIni().P_CLIENT, client.name)
        return rolesList.toTypedArray()
    }

    override fun getOrgs(rol: INameKeyPair): Array<INameKeyPair> {
        val m_ctx = Env.getCtx()
        if (Env.getContext(m_ctx, "#AD_Client_ID").length == 0)
        // 	could be number 0
            throw UnsupportedOperationException("Missing Context #AD_Client_ID")

        val AD_Client_ID = Env.getContextAsInt(m_ctx, "#AD_Client_ID")
        val AD_User_ID = Env.getContextAsInt(m_ctx, "#AD_User_ID")
        // 	s_log.fine("Client: " + client.toStringX() + ", AD_Role_ID=" + AD_Role_ID);

        // 	get Client details for role
        val list = ArrayList<KeyNamePair>()
        //
        val sql = (" SELECT DISTINCT r.UserLevel, r.ConnectionProfile,o.AD_Org_ID,o.Name,o.IsSummary " +
                " FROM AD_Org o" +
                " INNER JOIN AD_Role r on (r.AD_Role_ID=?)" +
                " INNER JOIN AD_Client c on (c.AD_Client_ID=?)" +
                " WHERE o.IsActive='Y' " +
                " AND o.AD_Client_ID IN (0, c.AD_Client_ID)" +
                " AND (r.IsAccessAllOrgs='Y'" +
                " OR (r.IsUseUserOrgAccess='N' AND o.AD_Org_ID IN (SELECT AD_Org_ID FROM AD_Role_OrgAccess ra" +
                " WHERE ra.AD_Role_ID=r.AD_Role_ID AND ra.IsActive='Y')) " +
                " OR (r.IsUseUserOrgAccess='Y' AND o.AD_Org_ID IN (SELECT AD_Org_ID FROM AD_User_OrgAccess ua" +
                " WHERE ua.AD_User_ID=?" +
                " AND ua.IsActive='Y')))" +
                "ORDER BY o.Name")
        //
        var pstmt: PreparedStatement? = null
        var role: MRole? = null
        var rs: ResultSet? = null
        try {
            pstmt = DB.prepareStatement(sql, null)
            pstmt!!.setInt(1, rol.Key)
            pstmt.setInt(2, AD_Client_ID)
            pstmt.setInt(3, AD_User_ID)
            rs = pstmt.executeQuery()
            //  load Orgs
            if (!rs!!.next()) {
                log.log(Level.SEVERE, "No org for Role: " + rol.toString())
                return arrayOf()
            }
            //  Role Info
            Env.setContext(m_ctx, "#AD_Role_ID", rol.Key)
            Env.setContext(m_ctx, "#AD_Role_Name", rol.name)
            Ini.getIni().setProperty(Ini.getIni().P_ROLE, rol.name)
            // 	User Level
            Env.setContext(m_ctx, "#User_Level", rs.getString(1)) // 	Format 'SCO'
            //  load Orgs

            do {
                val AD_Org_ID = rs.getInt(3)
                val Name = rs.getString(4)
                val summary = "Y" == rs.getString(5)
                if (summary) {
                    if (role == null)
                        role = MRole.get(m_ctx, rol.Key)
                    getOrgsAddSummary(list, AD_Org_ID, Name, role)
                } else {
                    val p = KeyNamePair(AD_Org_ID, Name)
                    if (!list.contains(p))
                        list.add(p)
                }
            } while (rs.next())

            if (log.isLoggable(Level.FINE)) log.fine("Client: " + AD_Client_ID + ", AD_Role_ID=" + rol.name + ", AD_User_ID=" + AD_User_ID + " - orgs #" + list.count())
        } catch (ex: SQLException) {
            log.log(Level.SEVERE, sql, ex)
        } finally {
            DB.close(rs, pstmt)
        }

        if (list.count() == 0) {
            log.log(Level.WARNING, "No Org for Client: " + AD_Client_ID +
                    ", AD_Role_ID=" + rol.Key +
                    ", AD_User_ID=" + AD_User_ID)
            return arrayOf()
        }
        return list.toTypedArray()
    }

    private fun getOrgsAddSummary(
        list: ArrayList<KeyNamePair>,
        Summary_Org_ID: Int,
        Summary_Name: String,
        role: MRole?
    ) {
        val m_ctx = Env.getCtx()
        if (role == null) {
            log.warning("Summary Org=$Summary_Name($Summary_Org_ID) - No Role")
            return
        }
        // 	Do we look for trees?
        if (role.aD_Tree_Org_ID == 0) {
            if (log.isLoggable(Level.CONFIG)) log.config("Summary Org=$Summary_Name($Summary_Org_ID) - No Org Tree: $role")
            return
        }
        // 	Summary Org - Get Dependents
        val tree = MTree_Base.get(m_ctx, role.aD_Tree_Org_ID, null)
        val sql = ("SELECT AD_Client_ID, AD_Org_ID, Name, IsSummary FROM AD_Org " +
                "WHERE IsActive='Y' AND AD_Org_ID IN (SELECT Node_ID FROM " +
                tree.nodeTableName +
                " WHERE AD_Tree_ID=? AND Parent_ID=? AND IsActive='Y') " +
                "ORDER BY Name")
        var pstmt: PreparedStatement? = null
        var rs: ResultSet? = null
        try {
            pstmt = DB.prepareStatement(sql, null)
            pstmt!!.setInt(1, tree.aD_Tree_ID)
            pstmt.setInt(2, Summary_Org_ID)
            rs = pstmt.executeQuery()
            while (rs!!.next()) {
                // int AD_Client_ID = rs.getInt(1);
                val AD_Org_ID = rs.getInt(2)
                val Name = rs.getString(3)
                val summary = "Y" == rs.getString(4)
                //
                if (summary)
                    getOrgsAddSummary(list, AD_Org_ID, Name, role)
                else {
                    val p = KeyNamePair(AD_Org_ID, Name)
                    if (!list.contains(p))
                        list.add(p)
                }
            }
        } catch (e: Exception) {
            log.log(Level.SEVERE, sql, e)
        } finally {
            DB.close(rs, pstmt)
        }
    } // 	getOrgAddSummary

    override fun getWarehouses(org: INameKeyPair): Array<INameKeyPair> {
        val list = ArrayList<KeyNamePair>()
        val sql = ("SELECT M_Warehouse_ID, Name FROM M_Warehouse " +
                "WHERE AD_Org_ID=? AND IsActive='Y' " +
                " AND " + I_M_Warehouse.COLUMNNAME_IsInTransit + "='N' " + // do not show in tranzit warehouses - teo_sarca [ 2867246 ]

                "ORDER BY Name")
        var pstmt: PreparedStatement? = null
        var rs: ResultSet? = null
        try {
            pstmt = DB.prepareStatement(sql, null)
            pstmt!!.setInt(1, org.Key)
            rs = pstmt.executeQuery()

            if (!rs!!.next()) {
                if (log.isLoggable(Level.INFO)) log.info("No Warehouses for Org: " + org.toString())
                return arrayOf()
            }

            //  load Warehousess
            do {
                val AD_Warehouse_ID = rs.getInt(1)
                val Name = rs.getString(2)
                val p = KeyNamePair(AD_Warehouse_ID, Name)
                list.add(p)
            } while (rs.next())

            if (log.isLoggable(Level.FINE))
                log.fine("Org: " + org.toString() +
                        " - warehouses #" + list.count())
        } catch (ex: SQLException) {
            log.log(Level.SEVERE, "getWarehouses", ex)
        } finally {
            DB.close(rs, pstmt)
        }
        return list.toTypedArray()
    }

    override fun init(ctx: Properties): ILoginUtility {
        return this
    }

    private fun loadUserPreferences() {
        Env.getCtx()
        /* DAP commented out for now
        val userPreference = MUserPreference.getUserPreference(Env.getAD_User_ID(m_ctx), Env.getAD_Client_ID(m_ctx))
        userPreference.fillPreferences()*/
    }

    override fun loadPreferences(org: INameKeyPair, warehouse: INameKeyPair?, timestamp: Timestamp?, printerName: String?): String {
        val m_ctx = Env.getCtx()
        if (log.isLoggable(Level.INFO)) log.info("Org: " + org.toString())

        if (m_ctx == null)
            throw IllegalArgumentException("Required parameter missing")
        if (Env.getContext(m_ctx, Env.AD_CLIENT_ID).length == 0)
            throw UnsupportedOperationException("Missing Context #AD_Client_ID")
        if (Env.getContext(m_ctx, Env.AD_USER_ID).length == 0)
            throw UnsupportedOperationException("Missing Context #AD_User_ID")
        if (Env.getContext(m_ctx, Env.AD_ROLE_ID).length == 0)
            throw UnsupportedOperationException("Missing Context #AD_Role_ID")

        //  Org Info - assumes that it is valid
        Env.setContext(m_ctx, Env.AD_ORG_ID, org.Key)
        Env.setContext(m_ctx, Env.AD_ORG_NAME, org.name)
        Ini.getIni().setProperty(Ini.getIni().P_ORG, org.name)

        //  Warehouse Info
        if (warehouse != null) {
            Env.setContext(m_ctx, Env.M_WAREHOUSE_ID, warehouse.Key)
            Ini.getIni().setProperty(Ini.getIni().P_WAREHOUSE, warehouse.name)
        }

        // 	Date (default today)
        var today = System.currentTimeMillis()
        if (timestamp != null)
            today = timestamp.time
        Env.setContext(m_ctx, "#Date", java.sql.Timestamp(today))

        // 	Optional Printer
        val printerName2 = printerName ?: ""

        Env.setContext(m_ctx, "#Printer", printerName2)
        Ini.getIni().setProperty(Ini.getIni().P_PRINTER, printerName2)

        // 	Load Role Info
        MRole.getDefault(m_ctx, true)

        // 	Other
        loadUserPreferences()

        if (MRole.getDefault(m_ctx, false).isShowAcct)
            Env.setContext(m_ctx, "#ShowAcct", Ini.getIni().getProperty(Ini.getIni().P_SHOW_ACCT))
        else
            Env.setContext(m_ctx, "#ShowAcct", "N")
        Env.setContext(m_ctx, "#ShowTrl", Ini.getIni().getProperty(Ini.getIni().P_SHOW_TRL))
        Env.setContext(m_ctx, "#ShowAdvanced", MRole.getDefault().isAccessAdvanced)

        var retValue = ""
        val AD_Client_ID = Env.getContextAsInt(m_ctx, "#AD_Client_ID")

        // 	Other Settings
        Env.setContext(m_ctx, "#YYYY", "Y")
        Env.setContext(m_ctx, "#StdPrecision", 2)

        // 	AccountSchema Info (first)
        var sql = ("SELECT * " +
                "FROM C_AcctSchema a, AD_ClientInfo c " +
                "WHERE a.C_AcctSchema_ID=c.C_AcctSchema1_ID " +
                "AND c.AD_Client_ID=?")
        var pstmt: PreparedStatement? = null
        var rs: ResultSet? = null
        try {

            var C_AcctSchema_ID = 0

            pstmt = DB.prepareStatement(sql, null)
            pstmt!!.setInt(1, AD_Client_ID)
            rs = pstmt.executeQuery()

            if (!rs!!.next()) {
                //  No Warning for System
                if (AD_Client_ID != 0)
                    retValue = "NoValidAcctInfo"
            } else {
                // 	Accounting Info
                C_AcctSchema_ID = rs.getInt("C_AcctSchema_ID")
                Env.setContext(m_ctx, "\$C_AcctSchema_ID", C_AcctSchema_ID)
                Env.setContext(m_ctx, "\$C_Currency_ID", rs.getInt("C_Currency_ID"))
                Env.setContext(m_ctx, "\$HasAlias", rs.getString("HasAlias"))
            }
            DB.close(rs, pstmt)

            /**Define AcctSchema , Currency, HasAlias for Multi AcctSchema */
            /* DAP we do not have it here
            val ass = MAcctSchema.getClientAcctSchema(Env.getCtx(), AD_Client_ID)
            if (ass != null && ass!!.size > 1) {
                for (`as` in ass!!) {
                    C_AcctSchema_ID = MClientInfo.get(Env.getCtx(), AD_Client_ID).c_AcctSchema1_ID
                    if (`as`.getAD_OrgOnly_ID() != 0) {
                        if (`as`.isSkipOrg(AD_Org_ID)) {
                            continue
                        } else {
                            C_AcctSchema_ID = `as`.getC_AcctSchema_ID()
                            Env.setContext(m_ctx, "\$C_AcctSchema_ID", C_AcctSchema_ID)
                            Env.setContext(m_ctx, "\$C_Currency_ID", `as`.getC_Currency_ID())
                            Env.setContext(m_ctx, "\$HasAlias", `as`.isHasAlias())
                            break
                        }
                    }
                }
            }*/

            // 	Accounting Elements
            sql = ("SELECT ElementType " +
                    "FROM C_AcctSchema_Element " +
                    "WHERE C_AcctSchema_ID=?" +
                    " AND IsActive='Y'")
            pstmt = DB.prepareStatement(sql, null)
            pstmt!!.setInt(1, C_AcctSchema_ID)
            rs = pstmt.executeQuery()
            while (rs!!.next())
                Env.setContext(m_ctx, "\$Element_" + rs.getString("ElementType"), "Y")
            DB.close(rs, pstmt)

            // 	This reads all relevant window neutral defaults
            // 	overwriting superseeded ones.  Window specific is read in Mainain
            sql = ("SELECT Attribute, Value, AD_Window_ID, AD_Process_ID, AD_InfoWindow_ID, PreferenceFor " +
                    "FROM AD_Preference " +
                    "WHERE AD_Client_ID IN (0, @#AD_Client_ID@)" +
                    " AND AD_Org_ID IN (0, @#AD_Org_ID@)" +
                    " AND (AD_User_ID IS NULL OR AD_User_ID=0 OR AD_User_ID=@#AD_User_ID@)" +
                    " AND IsActive='Y' " +
                    "ORDER BY Attribute, AD_Client_ID, AD_User_ID DESC, AD_Org_ID")
            // 	the last one overwrites - System - Client - User - Org - Window
            sql = Env.parseContext(m_ctx, 0, sql, false)
            if (sql.length == 0)
                log.log(Level.SEVERE, "loadPreferences - Missing Environment")
            else {
                pstmt = DB.prepareStatement(sql, null)
                rs = pstmt!!.executeQuery()
                while (rs!!.next()) {
                    val AD_Window_ID = rs.getInt(3)
                    val isAllWindow = rs.wasNull()
                    val AD_Process_ID = rs.getInt(4)
                    val AD_InfoWindow_ID = rs.getInt(5)
                    val PreferenceFor = rs.getString(6)
                    var at = ""

                    // preference for window
                    when (PreferenceFor) {
                        "W" -> at = if (isAllWindow)
                            "P|" + rs.getString(1)
                        else
                            "P" + AD_Window_ID + "|" + rs.getString(1)
                        "P" -> // preference for processs
                            // when apply for all window or all process format is "P0|0|m_Attribute;
                            at = "P" + AD_Window_ID + "|" + AD_InfoWindow_ID + "|" + AD_Process_ID + "|" + rs.getString(1)
                        "I" -> // preference for infoWindow
                            at = "P" + AD_Window_ID + "|" + AD_InfoWindow_ID + "|" + rs.getString(1)
                    }

                    val va = rs.getString(2)
                    Env.setContext(m_ctx, at, va)
                }
                DB.close(rs, pstmt)
            }

            // 	Default Values
            if (log.isLoggable(Level.INFO)) log.info("Default Values ...")
            sql = ("SELECT t.TableName, c.ColumnName " +
                    "FROM AD_Column c " +
                    " INNER JOIN AD_Table t ON (c.AD_Table_ID=t.AD_Table_ID) " +
                    "WHERE c.IsKey='Y' AND t.IsActive='Y' AND t.IsView='N'" +
                    " AND EXISTS (SELECT * FROM AD_Column cc " +
                    " WHERE ColumnName = 'IsDefault' AND t.AD_Table_ID=cc.AD_Table_ID AND cc.IsActive='Y')")
            pstmt = DB.prepareStatement(sql, null)
            rs = pstmt!!.executeQuery()
            while (rs!!.next())
                loadDefault(rs.getString(1), rs.getString(2))
        } catch (e: SQLException) {
            log.log(Level.SEVERE, "loadPreferences", e)
        } finally {
            DB.close(rs, pstmt)
        }
        // 	Country
        Env.setContext(m_ctx, "#C_Country_ID", MCountry.getDefault(m_ctx).c_Country_ID)
        // Call ModelValidators afterLoadPreferences - teo_sarca FR [ 1670025 ]
        ModelValidationEngine.get().afterLoadPreferences(m_ctx)
        return retValue
    }

    private fun loadDefault(TableName: String, ColumnName: String) {
        val m_ctx = Env.getCtx()
        if (TableName.startsWith("AD_Window") ||
                TableName.startsWith("AD_PrintFormat") ||
                TableName.startsWith("AD_Workflow") ||
                TableName.startsWith("M_Locator"))
            return
        var value: String? = null
        //
        var sql = ("SELECT " + ColumnName + " FROM " + TableName + // 	most specific first

                " WHERE IsDefault='Y' AND IsActive='Y' ORDER BY AD_Client_ID DESC, AD_Org_ID DESC")
        sql = MRole.getDefault(m_ctx, false).addAccessSQL(sql,
                TableName, MRole.SQL_NOTQUALIFIED, MRole.SQL_RO)
        var pstmt: PreparedStatement? = null
        var rs: ResultSet? = null
        try {
            pstmt = DB.prepareStatement(sql, null)
            rs = pstmt!!.executeQuery()
            if (rs!!.next())
                value = rs.getString(1)
        } catch (e: SQLException) {
            log.log(Level.SEVERE, "$TableName ($sql)", e)
            return
        } finally {
            DB.close(rs, pstmt)
        }
        // 	Set Context Value
        if (value != null && value.isNotEmpty()) {
            if (TableName == "C_DocType")
                Env.setContext(m_ctx, "#C_DocTypeTarget_ID", value)
            else
                Env.setContext(m_ctx, "#$ColumnName", value)
        }
    }
}