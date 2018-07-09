package org.compiere.order;

import java.sql.ResultSet;
import java.util.Properties;

public class MShippingTransactionLine extends X_M_ShippingTransactionLine 
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8148869412130244360L;

	public MShippingTransactionLine(Properties ctx, int M_ShippingTransactionLine_ID, String trxName) 
	{
		super(ctx, M_ShippingTransactionLine_ID, trxName);
	}
	
	public MShippingTransactionLine(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	public void setAD_Client_ID (int AD_Client_ID) {
		super.setAD_Client_ID(AD_Client_ID);
	}
	
}
