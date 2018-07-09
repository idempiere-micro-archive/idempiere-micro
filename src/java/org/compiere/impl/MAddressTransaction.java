package org.compiere.impl;

import org.adempiere.base.Core;
import org.adempiere.model.IAddressValidation;
import org.compiere.order.MOnlineTrxHistory;
import org.compiere.util.Msg;
import org.idempiere.common.util.Env;

import java.util.Properties;
import java.util.logging.Level;


public class MAddressTransaction extends org.compiere.crm.MAddressTransaction {

    public MAddressTransaction(Properties ctx, int C_AddressTransaction_ID, String trxName)
    {
        super(ctx, C_AddressTransaction_ID, trxName);
    }

    /**
     * Get address validation
     * @return address validation
     */
    public MAddressValidation getMAddressValidation()
    {
        return new MAddressValidation(getCtx(), getC_AddressValidation_ID(), get_TrxName());
    }

    /**
     * Online process address validation
     * @return true if valid
     */
    public boolean processOnline()
    {
        setErrorMessage(null);

        boolean processed = false;
        try
        {
            IAddressValidation validation = Core.getAddressValidation(getMAddressValidation());
            if (validation == null)
                setErrorMessage(Msg.getMsg(Env.getCtx(), "AddressNoValidation"));
            else
            {
                processed = validation.onlineValidate(getCtx(), this, get_TrxName());
                if (!processed || !isValid())
                    setErrorMessage("From " + getMAddressValidation().getName() + ": " + getResult());
            }
        }
        catch (Exception e)
        {
            log.log(Level.SEVERE, "processOnline", e);
            setErrorMessage(Msg.getMsg(Env.getCtx(), "AddressNotProcessed") + ":\n" + e.getMessage());
        }

        MOnlineTrxHistory history = new MOnlineTrxHistory(getCtx(), 0, get_TrxName());
        history.setAD_Table_ID(org.compiere.crm.MAddressTransaction.Table_ID);
        history.setRecord_ID(getC_AddressTransaction_ID());
        history.setIsError(!(processed && isValid()));
        history.setProcessed(processed);

        StringBuilder msg = new StringBuilder();
        if (processed)
            msg.append(getResult());
        else
            msg.append("ERROR: " + getErrorMessage());
        history.setTextMsg(msg.toString());

        history.saveEx();

        setProcessed(processed);
        return processed;
    }
}
