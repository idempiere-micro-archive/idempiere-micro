package org.compiere.impl;

import org.compiere.crm.MBPartnerLocation;
import org.compiere.orm.MSysConfig;
import org.compiere.orm.PO;
import org.compiere.util.Msg;
import org.idempiere.common.util.DB;
import org.idempiere.common.util.Env;
import org.idempiere.common.util.Trx;

import java.util.Properties;
import java.util.logging.Level;

public class MLocation extends org.compiere.crm.MLocation {

    /**************************************************************************
     * 	Standard Constructor
     *	@param ctx context
     *	@param C_Location_ID id
     *	@param trxName transaction
     */
    public MLocation (Properties ctx, int C_Location_ID, String trxName) {
        super (ctx, C_Location_ID, trxName);
    }

    /**
     * 	Full Constructor
     *	@param ctx context
     *	@param C_Country_ID country
     *	@param C_Region_ID region
     *	@param city city
     *	@param trxName transaction
     */
    public MLocation (Properties ctx, int C_Country_ID, int C_Region_ID, String city, String trxName)
    {
        super(ctx, C_Country_ID, C_Region_ID, city, trxName);
    }	//	MLocation

    /**
     * 	After Save
     *	@param newRecord new
     *	@param success success
     *	@return success
     */
    protected boolean afterSave (boolean newRecord, boolean success)
    {
        if (!success)
            return success;
        //	Value/Name change in Account
        if (!newRecord
                && ("Y".equals(Env.getContext(getCtx(), "$Element_LF"))
                || "Y".equals(Env.getContext(getCtx(), "$Element_LT")))
                && (is_ValueChanged("Postal") || is_ValueChanged("City"))
                ){
            StringBuilder msgup = new StringBuilder(
                    "(C_LocFrom_ID=").append(getC_Location_ID())
                    .append(" OR C_LocTo_ID=").append(getC_Location_ID()).append(")");
            MAccount.updateValueDescription(getCtx(), msgup.toString(), get_TrxName());
        }

        //Update BP_Location name IDEMPIERE 417
        if (get_TrxName().startsWith(PO.LOCAL_TRX_PREFIX)) { // saved without trx
            int bplID = DB.getSQLValueEx(get_TrxName(), "SELECT C_BPartner_Location_ID FROM C_BPartner_Location WHERE C_Location_ID = " + getC_Location_ID());
            if (bplID>0)
            {
                // just trigger BPLocation name change when the location change affects the name:
                // START_VALUE_BPLOCATION_NAME
                // 0 - City
                // 1 - City + Address1
                // 2 - City + Address1 + Address2
                // 3 - City + Address1 + Address2 + Region
                // 4 - City + Address1 + Address2 + Region + ID
                int bplocname = MSysConfig.getIntValue(MSysConfig.START_VALUE_BPLOCATION_NAME, 0, getAD_Client_ID(), getAD_Org_ID());
                if (bplocname < 0 || bplocname > 4)
                    bplocname = 0;
                if (   is_ValueChanged(COLUMNNAME_City)
                        || is_ValueChanged(COLUMNNAME_C_City_ID)
                        || (bplocname >= 1 && is_ValueChanged(COLUMNNAME_Address1))
                        || (bplocname >= 2 && is_ValueChanged(COLUMNNAME_Address2))
                        || (bplocname >= 3 && (is_ValueChanged(COLUMNNAME_RegionName) || is_ValueChanged(COLUMNNAME_C_Region_ID)))
                        ) {
                    MBPartnerLocation bpl = new MBPartnerLocation(getCtx(), bplID, get_TrxName());
                    bpl.setName(bpl.getBPLocName(this));
                    bpl.saveEx();
                }
            }
        }
        return success;
    }	//	afterSave

    /**
     * Perform online address validation
     * @param C_AddressValidation_ID
     * @return true if valid
     */
    public boolean processOnline(int C_AddressValidation_ID)
    {
        setErrorMessage(null);

        Trx trx = Trx.get(Trx.createTrxName("avt-"), true);
        trx.setDisplayName(getClass().getName()+"_processOnline");
        boolean ok = false;
        try
        {
            MAddressTransaction at = createAddressTransaction(getCtx(), this, C_AddressValidation_ID, trx.getTrxName());
            ok = at.processOnline();
            at.saveEx();

            setC_AddressValidation_ID(at.getC_AddressValidation_ID());
            setIsValid(at.isValid());
            setResult(at.getResult());

            if (!ok)
                setErrorMessage(at.getErrorMessage());
        }
        catch (Exception e)
        {
            log.log(Level.SEVERE, "processOnline", e);
            setErrorMessage(Msg.getMsg(Env.getCtx(), "AddressNotProcessed") + ": " + e.getMessage());
        }
        finally
        {
            if (trx != null)
            {
                trx.commit();
                trx.close();
            }
        }

        return ok;
    }

    /**
     * Create address transaction instance
     * @param ctx
     * @param location
     * @param C_AddressValidation_ID
     * @param trxName
     * @return address transaction instance
     */
    private static MAddressTransaction createAddressTransaction(Properties ctx, MLocation location, int C_AddressValidation_ID, String trxName)
    {
        MAddressTransaction at = new MAddressTransaction(ctx, 0, trxName);
        at.setAD_Org_ID(location.getAD_Org_ID());
        at.setAddress1(location.getAddress1());
        at.setAddress2(location.getAddress2());
        at.setAddress3(location.getAddress3());
        at.setAddress4(location.getAddress4());
        at.setAddress5(location.getAddress5());
        at.setComments(location.getComments());
        at.setC_AddressValidation_ID(C_AddressValidation_ID);
        at.setC_Location_ID(location.getC_Location_ID());
        at.setCity(location.getCity());
        if (location.getCountry() != null)
            at.setCountry(location.getCountry().getCountryCode());
        at.setIsActive(location.isActive());
        at.setPostal(location.getPostal());
        if (location.getRegion() != null)
            at.setRegion(location.getRegion().getName());
        else
            at.setRegion(location.getRegionName());
        at.saveEx();
        return at;
    }

}
