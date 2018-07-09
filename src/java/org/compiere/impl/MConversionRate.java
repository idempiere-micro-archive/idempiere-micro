package org.compiere.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Properties;

public class MConversionRate extends org.compiere.conversionrate.MConversionRate {

    public MConversionRate (Properties ctx, int C_Conversion_Rate_ID, String trxName) {
        super( ctx, C_Conversion_Rate_ID,  trxName);
    }

    /**
     *	Convert an amount to base Currency
     *	@param ctx context
     *  @param CurFrom_ID  The C_Currency_ID FROM
     *  @param ConvDate conversion date - if null - use current date
     *  @param C_ConversionType_ID conversion rate type - if 0 - use Default
     *  @param Amt amount to be converted
     * 	@param AD_Client_ID client
     * 	@param AD_Org_ID organization
     *  @return converted amount
     */
    public static BigDecimal convertBase (Properties ctx,
                                          BigDecimal Amt, int CurFrom_ID,
                                          Timestamp ConvDate, int C_ConversionType_ID,
                                          int AD_Client_ID, int AD_Org_ID)
    {
        return convert (ctx, Amt, CurFrom_ID, MClient.get(ctx).getC_Currency_ID(),
            ConvDate, C_ConversionType_ID, AD_Client_ID, AD_Org_ID);
    }	//	convertBase
}
