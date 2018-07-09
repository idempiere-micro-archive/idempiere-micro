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
package org.adempiere.process;

import java.util.logging.Level;

import org.adempiere.base.Core;
import org.compiere.impl.StandardTaxProvider;
import org.compiere.model.IProcessInfoParameter;
import org.compiere.tax.ITaxProvider;
import org.idempiere.common.exceptions.AdempiereException;
import org.compiere.tax.MTaxProvider;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Msg;

/**
 * Online validate tax provider connection
 * @author Elaine
 *
 */
public class ValidateTaxProviderConnection extends SvrProcess 
{
	@Override
	protected void prepare() 
	{
		IProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
		}
	}
	
	@Override
	protected String doIt() throws Exception 
	{
		MTaxProvider provider = new MTaxProvider(getCtx(), getRecord_ID(), get_TrxName());			
		ITaxProvider calculator = MTaxProvider.getTaxProvider(provider, new StandardTaxProvider());
		if (calculator == null)
			throw new AdempiereException(Msg.getMsg(getCtx(), "TaxNoProvider"));
		return calculator.validateConnection(provider, getProcessInfo());
	}	
}
