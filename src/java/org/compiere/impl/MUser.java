package org.compiere.impl;

import org.compiere.crm.EMail;
import org.compiere.model.I_AD_User;
import org.compiere.orm.MSysConfig;
import org.compiere.orm.Query;
import org.compiere.util.Msg;
import org.idempiere.common.exceptions.DBException;
import org.idempiere.common.util.CCache;
import org.idempiere.common.util.DB;
import org.idempiere.common.util.Util;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class MUser extends org.compiere.crm.MUser {

    public MUser (Properties ctx, int AD_User_ID, String trxName) {
        super(ctx, AD_User_ID, trxName);
    }

    /**
     * 	Before Save
     *	@param newRecord new
     *	@return true
     */
    protected boolean beforeSave (boolean newRecord)
    {
        //	New Address invalidates verification
        if (!newRecord && is_ValueChanged("EMail"))
            setEMailVerifyDate(null);

        // IDEMPIERE-1409
        if (!Util.isEmpty(getEMail()) && (newRecord || is_ValueChanged("EMail"))) {
            if (! EMail.validate(getEMail())) {
                log.saveError("SaveError", Msg.getMsg(getCtx(), "InvalidEMailFormat") + Msg.getElement(getCtx(), I_AD_User.COLUMNNAME_EMail) + " - [" + getEMail() + "]");
                return false;
            }
        }

        if (newRecord || super.getValue() == null || is_ValueChanged("Value"))
            setValue(super.getValue());

        if (getPassword() != null && getPassword().length() > 0) {
            boolean email_login = MSysConfig.getBooleanValue(MSysConfig.USE_EMAIL_FOR_LOGIN, false);
            if (email_login) {
                // email is mandatory for users with password
                if (getEMail() == null || getEMail().length() == 0) {
                    log.saveError("SaveError", Msg.getMsg(getCtx(), "FillMandatory") + Msg.getElement(getCtx(), I_AD_User.COLUMNNAME_EMail) + " - " + toString());
                    return false;
                }
                // email with password must be unique on the same tenant
                int cnt = DB.getSQLValue(get_TrxName(),
                        "SELECT COUNT(*) FROM AD_User WHERE Password IS NOT NULL AND EMail=? AND AD_Client_ID=? AND AD_User_ID!=?",
                        getEMail(), getAD_Client_ID(), getAD_User_ID());
                if (cnt > 0) {
                    log.saveError("SaveError", Msg.getMsg(getCtx(), DBException.SAVE_ERROR_NOT_UNIQUE_MSG, true) + Msg.getElement(getCtx(), I_AD_User.COLUMNNAME_EMail));
                    return false;
                }
            } else {
                // IDEMPIERE-1672 check duplicate name in client
                String nameToValidate = getLDAPUser();
                if (Util.isEmpty(nameToValidate))
                    nameToValidate = getName();
                int cnt = DB.getSQLValue(get_TrxName(),
                        "SELECT COUNT(*) FROM AD_User WHERE Password IS NOT NULL AND COALESCE(LDAPUser,Name)=? AND AD_Client_ID=? AND AD_User_ID!=?",
                        nameToValidate, getAD_Client_ID(), getAD_User_ID());
                if (cnt > 0) {
                    log.saveError("SaveError", Msg.getMsg(getCtx(), DBException.SAVE_ERROR_NOT_UNIQUE_MSG, true) + Msg.getElement(getCtx(), I_AD_User.COLUMNNAME_Name) + " / " + Msg.getElement(getCtx(), I_AD_User.COLUMNNAME_LDAPUser));
                    return false;
                }
            }
        }

        if (getPassword() != null && getPassword().length() > 0 && (newRecord || is_ValueChanged("Password"))) {
            // Validate password policies / IDEMPIERE-221
            if (get_ValueOld("Salt") == null && get_Value("Salt") != null) { // being hashed
                ;
            } else {
                MPasswordRule pwdrule = MPasswordRule.getRules(getCtx(), get_TrxName());
                if (pwdrule != null){
                    List<MPasswordHistory> passwordHistorys = MPasswordHistory.getPasswordHistoryForCheck(pwdrule.getDays_Reuse_Password(), this.getAD_User_ID());
                    // for long time user don't use this system, because all password in history table is out of check range. but we will want new password must difference latest password
                    if (passwordHistorys.size() == 0 && !this.is_new() && this.get_ValueOld(org.compiere.crm.MUser.COLUMNNAME_Password) != null){
                        Object oldSalt = this.get_ValueOld(org.compiere.crm.MUser.COLUMNNAME_Salt);
                        Object oldPassword = this.get_ValueOld(org.compiere.crm.MUser.COLUMNNAME_Password);

                        MPasswordHistory latestPassword = new MPasswordHistory(oldSalt == null?null:oldSalt.toString(), oldPassword == null?null:oldPassword.toString());
                        passwordHistorys.add(latestPassword);
                    }
                    pwdrule.validate((getLDAPUser() != null ? getLDAPUser() : getName()), getPassword(), passwordHistorys);
                }

            }

            // Hash password - IDEMPIERE-347
            boolean hash_password = MSysConfig.getBooleanValue(MSysConfig.USER_PASSWORD_HASH, false);
            if (hash_password)
                setPassword(getPassword());

            setDatePasswordChanged(new Timestamp(new Date().getTime()));
        }

        return true;
    }	//	beforeSave

    @Override
    public String getEMailUser() {
        // IDEMPIERE-722
        if (MClient.isSendCredentialsSystem()) {
            MClient sysclient = MClient.get(getCtx(), 0);
            return sysclient.getRequestUser();
        } else if (MClient.isSendCredentialsClient()) {
            MClient client = MClient.get(getCtx());
            return client.getRequestUser();
        } else {
            return super.getEMailUser();
        }
    }

    @Override
    public String getEMailUserPW() {
        // IDEMPIERE-722
        if (MClient.isSendCredentialsSystem()) {
            MClient sysclient = MClient.get(getCtx(), 0);
            return sysclient.getRequestUserPW();
        } else if (MClient.isSendCredentialsClient()) {
            MClient client = MClient.get(getCtx());
            return client.getRequestUserPW();
        } else {
            return super.getEMailUserPW();
        }
    }

    /**
     * save new pass to history
     */
    @Override
    protected boolean afterSave(boolean newRecord, boolean success) {
        if (getPassword() != null && getPassword().length() > 0 && (newRecord || is_ValueChanged("Password"))) {
            MPasswordHistory passwordHistory = new MPasswordHistory(this.getCtx(), 0, this.get_TrxName());
            passwordHistory.setSalt(this.getSalt());
            passwordHistory.setPassword(this.getPassword());
            // http://wiki.idempiere.org/en/System_user
            if (!this.is_new() && this.getAD_User_ID() == 0){
                passwordHistory.set_Value(MPasswordHistory.COLUMNNAME_AD_User_ID, 0);
            }else{
                passwordHistory.setAD_User_ID(this.getAD_User_ID());
            }
            passwordHistory.setDatePasswordChanged(this.getUpdated());
            passwordHistory.saveEx();
        }
        return super.afterSave(newRecord, success);
    }

    /**************************************************************************
     * 	Set AD_Client
     * 	@param AD_Client_ID client
     */
    protected void setAD_Client_ID (int AD_Client_ID)
    {
        super.setAD_Client_ID(AD_Client_ID);
    }	//	setAD_Client_ID

    /**
     * Get active Users of BPartner
     * @param ctx
     * @param C_BPartner_ID
     * @param trxName
     * @return array of users
     */
    public static MUser[] getOfBPartner (Properties ctx, int C_BPartner_ID, String trxName)
    {
        List<MUser> list = new Query(ctx, I_AD_User.Table_Name, "C_BPartner_ID=?", trxName)
                .setParameters(C_BPartner_ID)
                .setOnlyActiveRecords(true)
                .list();

        MUser[] retValue = new MUser[list.size ()];
        list.toArray (retValue);
        return retValue;
    }	//	getOfBPartner

    /**
     * 	Parent Constructor
     *	@param partner partner
     */
    public MUser (X_C_BPartner partner)
    {
        this (partner.getCtx(), 0, partner.get_TrxName());
        setClientOrg(partner);
        setC_BPartner_ID (partner.getC_BPartner_ID());
        setName(partner.getName());
    }	//	MUser

    /**	Cache					*/
    static private CCache<Integer,MUser> s_cache = new CCache<Integer,MUser>(I_AD_User.Table_Name, 30, 60);

    /**
     * 	Get User (cached)
     * 	Also loads Admninistrator (0)
     *	@param ctx context
     *	@param AD_User_ID id
     *	@return user
     */
    public static MUser get (Properties ctx, int AD_User_ID)
    {
        Integer key = new Integer(AD_User_ID);
        MUser retValue = (MUser)s_cache.get(key);
        if (retValue == null)
        {
            retValue = new MUser (ctx, AD_User_ID, null);
            if (AD_User_ID == 0)
            {
                String trxName = null;
                retValue.load(trxName);	//	load System Record
            }
            s_cache.put(key, retValue);
        }
        return retValue;
    }	//	get

    public MUser (Properties ctx, ResultSet rs, String trxName)
    {
        super(ctx, rs, trxName);
    }

}
