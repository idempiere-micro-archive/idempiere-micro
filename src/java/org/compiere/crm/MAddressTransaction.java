/******************************************************************************
 * Copyright (C) 2013 Elaine Tan                                              *
 * Copyright (C) 2013 Trek Global
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package org.compiere.crm;

import java.sql.ResultSet;
import java.util.Properties;
import java.util.logging.Level;

import org.idempiere.common.util.Env;
import org.compiere.util.Msg;

/**
 * Address transaction model
 * @author Elaine
 *
 */
public class MAddressTransaction extends X_C_AddressTransaction 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8572809249265680649L;

	public MAddressTransaction(Properties ctx, int C_AddressTransaction_ID, String trxName) 
	{
		super(ctx, C_AddressTransaction_ID, trxName);
	}
	
	public MAddressTransaction(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}
	
	/** Error Message						*/
	private String				m_errorMessage = null;
	
	/**
	 * Set error message
	 * @param errorMessage
	 */
	public void setErrorMessage(String errorMessage)
	{
		m_errorMessage = errorMessage;
	}
	
	/**
	 * Get error message
	 * @return error message
	 */
	public String getErrorMessage()
	{
		return m_errorMessage;
	}
	

}