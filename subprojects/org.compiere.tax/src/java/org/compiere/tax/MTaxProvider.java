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
package org.compiere.tax;

import org.compiere.orm.PO;
import org.idempiere.common.base.Service;

import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Tax provider model
 * @author Elaine
 *
 */
public class MTaxProvider extends X_C_TaxProvider 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 6621828279540899973L;

	public MTaxProvider(Properties ctx, int C_TaxProvider_ID, String trxName) 
	{
		super(ctx, C_TaxProvider_ID, trxName);
	}
	
	public MTaxProvider(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}
	
	public String getTaxProviderClass()
	{
		return getC_TaxProviderCfg().getTaxProviderClass();
	}
	
	public String getURL()
	{
		return getC_TaxProviderCfg().getURL();
	}

	/**
	 * Get tax provider instance
	 * @param provider
	 * @return tax provider instance or null if not found
	 */
	public static ITaxProvider getTaxProvider(MTaxProvider provider, ITaxProvider standardTaxProvider)
	{
		ITaxProvider calculator = null;
		if (provider != null)
		{
			if (provider.getC_TaxProvider_ID() == 0)
				return standardTaxProvider;

			if (!provider.isActive())
			{
				s_log.log(Level.SEVERE, "Tax provider is inactive: " + provider);
				return null;
			}

			String className = provider.getTaxProviderClass();
			if (className == null || className.length() == 0)
			{
				s_log.log(Level.SEVERE, "Tax provider class not defined: " + provider);
				return null;
			}

			List<ITaxProviderFactory> factoryList = Service.locator().list(ITaxProviderFactory.class).getServices();
			if (factoryList == null)
				return null;
			for (ITaxProviderFactory factory : factoryList)
			{
				calculator = factory.newTaxProviderInstance(className);
				if (calculator != null)
					return calculator;
			}
		}

		return null;
	}

	/**
	 * Get tax provider instance
	 * @param provider
	 * @return tax provider instance or null if not found
	 */
	public static IInvoiceTaxProvider getTaxProvider(MTaxProvider provider, IInvoiceTaxProvider standardTaxProvider)
	{
		IInvoiceTaxProvider calculator = null;
		if (provider != null)
		{
			if (provider.getC_TaxProvider_ID() == 0)
				return standardTaxProvider;

			if (!provider.isActive())
			{
				s_log.log(Level.SEVERE, "Tax provider is inactive: " + provider);
				return null;
			}

			String className = provider.getTaxProviderClass();
			if (className == null || className.length() == 0)
			{
				s_log.log(Level.SEVERE, "Tax provider class not defined: " + provider);
				return null;
			}

			List<ITaxProviderFactory> factoryList = Service.locator().list(ITaxProviderFactory.class).getServices();
			if (factoryList == null)
				return null;
			for (ITaxProviderFactory factory : factoryList)
			{
				calculator =  PO.as( IInvoiceTaxProvider.class, factory.newTaxProviderInstance(className) );
				if (calculator != null)
					return calculator;
			}
		}

		return null;
	}

}
