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

import org.compiere.model.*;
import org.compiere.process.ProcessInfo;

/**
 * Tax provider interface
 * @author Elaine
 *
 * @contributor Murilo H. Torquato <muriloht@devcoffee.com.br>
 *
 */
public interface ITaxProvider {
		
	public boolean calculateOrderTaxTotal(I_C_TaxProvider provider, I_C_Order order);
	
	public boolean updateOrderTax(I_C_TaxProvider provider, I_C_OrderLine line);

	public boolean recalculateTax(I_C_TaxProvider provider, I_C_OrderLine line, boolean newRecord);

	public boolean updateHeaderTax(I_C_TaxProvider provider, I_C_OrderLine line);


	public boolean calculateRMATaxTotal(I_C_TaxProvider provider, I_M_RMA rma);
	
	public boolean updateRMATax(I_C_TaxProvider provider, I_M_RMALine line);

	public boolean recalculateTax(I_C_TaxProvider provider, I_M_RMALine line, boolean newRecord);

	public boolean updateHeaderTax(I_C_TaxProvider provider, I_M_RMALine line);

	public String validateConnection(I_C_TaxProvider provider, IProcessInfo pi) throws Exception;
	
}