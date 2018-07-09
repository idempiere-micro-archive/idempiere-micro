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
package org.compiere.order;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.compiere.model.*;
import org.compiere.tax.ITaxProvider;
import org.compiere.process.ProcessInfo;
import org.compiere.tax.MTax;
import org.compiere.tax.MTaxProvider;
import org.idempiere.common.util.CLogger;
import org.idempiere.common.util.DB;
import org.idempiere.common.util.Env;
import org.compiere.util.Msg;

/**
 * Standard tax provider
 * @author Elaine
 *
 * @contributor Murilo H. Torquato <muriloht@devcoffee.com.br>
 *
 */
public class StandardTaxProvider implements ITaxProvider {

	/**	Logger							*/
	protected transient CLogger	log = CLogger.getCLogger (getClass());

	public boolean calculateOrderTaxTotal(I_C_TaxProvider provider, I_C_Order order) {
		//	Lines
		BigDecimal totalLines = Env.ZERO;
		ArrayList<Integer> taxList = new ArrayList<Integer>();
		I_C_OrderLine[] lines = order.getLines();
		for (int i = 0; i < lines.length; i++)
		{
			I_C_OrderLine line = lines[i];
			totalLines = totalLines.add(line.getLineNetAmt());
			Integer taxID = new Integer(line.getC_Tax_ID());
			if (!taxList.contains(taxID))
			{
				MTax tax = new MTax(order.getCtx(), taxID, order.get_TrxName());
				if (tax.getC_TaxProvider_ID() != 0)
					continue;
				MOrderTax oTax = MOrderTax.get (line, order.getPrecision(), false, order.get_TrxName());	//	current Tax
				oTax.setIsTaxIncluded(order.isTaxIncluded());
				if (!oTax.calculateTaxFromLines())
					return false;
				if (!oTax.save(order.get_TrxName()))
					return false;
				taxList.add(taxID);
			}
		}
		
		//	Taxes
		BigDecimal grandTotal = totalLines;
		I_C_OrderTax[] taxes = order.getTaxes(true);
		for (int i = 0; i < taxes.length; i++)
		{
			I_C_OrderTax oTax = taxes[i];
			if (oTax.getC_TaxProvider_ID() != 0) {
				if (!order.isTaxIncluded())
					grandTotal = grandTotal.add(oTax.getTaxAmt());
				continue;
			}
			I_C_Tax tax = oTax.getTax();
			if (tax.isSummary())
			{
				I_C_Tax[] cTaxes = tax.getChildTaxes(false);
				for (int j = 0; j < cTaxes.length; j++)
				{
					I_C_Tax cTax = cTaxes[j];
					BigDecimal taxAmt = cTax.calculateTax(oTax.getTaxBaseAmt(), false, order.getPrecision());
					//
					MOrderTax newOTax = new MOrderTax(order.getCtx(), 0, order.get_TrxName());
					newOTax.setClientOrg(order);
					newOTax.setC_Order_ID(order.getC_Order_ID());
					newOTax.setC_Tax_ID(cTax.getC_Tax_ID());
					newOTax.setPrecision(order.getPrecision());
					newOTax.setIsTaxIncluded(order.isTaxIncluded());
					newOTax.setTaxBaseAmt(oTax.getTaxBaseAmt());
					newOTax.setTaxAmt(taxAmt);
					if (!newOTax.save(order.get_TrxName()))
						return false;
					//
					if (!order.isTaxIncluded())
						grandTotal = grandTotal.add(taxAmt);
				}
				if (!oTax.delete(true, order.get_TrxName()))
					return false;
				if (!oTax.save(order.get_TrxName()))
					return false;
			}
			else
			{
				if (!order.isTaxIncluded())
					grandTotal = grandTotal.add(oTax.getTaxAmt());
			}
		}		
		//
		order.setTotalLines(totalLines);
		order.setGrandTotal(grandTotal);
		return true;
	}

	public boolean updateOrderTax(I_C_TaxProvider provider, I_C_OrderLine line) {
		MTax mtax = new MTax(line.getCtx(), line.getC_Tax_ID(), line.get_TrxName());
    	if (mtax.getC_TaxProvider_ID() == 0)
    		return line.updateOrderTax(false);
    	return true;
	}

	public boolean recalculateTax(I_C_TaxProvider provider, I_C_OrderLine line, boolean newRecord)
	{
		if (!newRecord && line.is_ValueChanged(MOrderLine.COLUMNNAME_C_Tax_ID) && !line.getParent().isProcessed())
		{
			MTax mtax = new MTax(line.getCtx(), line.getC_Tax_ID(), line.get_TrxName());
	    	if (mtax.getC_TaxProvider_ID() == 0)
	    	{
				//	Recalculate Tax for old Tax
				if (!line.updateOrderTax(true))
					return false;
	    	}
		}
		return line.updateHeaderTax();
	}

	public boolean updateHeaderTax(I_C_TaxProvider provider, I_C_OrderLine line)
	{
		//		Update Order Header
		String sql = "UPDATE C_Order i"
			+ " SET TotalLines="
				+ "(SELECT COALESCE(SUM(LineNetAmt),0) FROM C_OrderLine il WHERE i.C_Order_ID=il.C_Order_ID) "
			+ "WHERE C_Order_ID=" + line.getC_Order_ID();
		int no = DB.executeUpdate(sql, line.get_TrxName());
		if (no != 1)
			log.warning("(1) #" + no);

		if (line.isTaxIncluded())
			sql = "UPDATE C_Order i "
				+ " SET GrandTotal=TotalLines "
				+ "WHERE C_Order_ID=" + line.getC_Order_ID();
		else
			sql = "UPDATE C_Order i "
				+ " SET GrandTotal=TotalLines+"
					+ "(SELECT COALESCE(SUM(TaxAmt),0) FROM C_OrderTax it WHERE i.C_Order_ID=it.C_Order_ID) "
					+ "WHERE C_Order_ID=" + line.getC_Order_ID();
		no = DB.executeUpdate(sql, line.get_TrxName());
		if (no != 1)
			log.warning("(2) #" + no);

		line.clearParent();
		return no == 1;
	}



	public boolean calculateRMATaxTotal(I_C_TaxProvider provider, I_M_RMA rma) {
		//	Lines
		BigDecimal totalLines = Env.ZERO;
		ArrayList<Integer> taxList = new ArrayList<Integer>();
		I_M_RMALine[] lines = rma.getLines(false);
		for (int i = 0; i < lines.length; i++)
		{
			I_M_RMALine line = lines[i];
			totalLines = totalLines.add(line.getLineNetAmt());
			Integer taxID = new Integer(line.getC_Tax_ID());
			if (!taxList.contains(taxID))
			{
				MTax tax = new MTax(rma.getCtx(), taxID, rma.get_TrxName());
				if (tax.getC_TaxProvider_ID() != 0)
					continue;
				MRMATax oTax = MRMATax.get (line, rma.getPrecision(), false, rma.get_TrxName());	//	current Tax
				oTax.setIsTaxIncluded(rma.isTaxIncluded());
				if (!oTax.calculateTaxFromLines())
					return false;
				if (!oTax.save(rma.get_TrxName()))
					return false;
				taxList.add(taxID);
			}
		}
		
		//	Taxes
		BigDecimal grandTotal = totalLines;
		I_M_RMATax[] taxes = rma.getTaxes(true);
		for (int i = 0; i < taxes.length; i++)
		{
			I_M_RMATax oTax = taxes[i];
			if (oTax.getC_TaxProvider_ID() != 0) {
				if (!rma.isTaxIncluded())
					grandTotal = grandTotal.add(oTax.getTaxAmt());
				continue;
			}
			I_C_Tax tax = oTax.getTax();
			if (tax.isSummary())
			{
				I_C_Tax[] cTaxes = tax.getChildTaxes(false);
				for (int j = 0; j < cTaxes.length; j++)
				{
					I_C_Tax cTax = cTaxes[j];
					BigDecimal taxAmt = cTax.calculateTax(oTax.getTaxBaseAmt(), false, rma.getPrecision());
					//
					MRMATax newOTax = new MRMATax(rma.getCtx(), 0, rma.get_TrxName());
					newOTax.setClientOrg(rma);
					newOTax.setM_RMA_ID(rma.getM_RMA_ID());
					newOTax.setC_Tax_ID(cTax.getC_Tax_ID());
					newOTax.setPrecision(rma.getPrecision());
					newOTax.setIsTaxIncluded(rma.isTaxIncluded());
					newOTax.setTaxBaseAmt(oTax.getTaxBaseAmt());
					newOTax.setTaxAmt(taxAmt);
					if (!newOTax.save(rma.get_TrxName()))
						return false;
					//
					if (!rma.isTaxIncluded())
						grandTotal = grandTotal.add(taxAmt);
				}
				if (!oTax.delete(true, rma.get_TrxName()))
					return false;
				if (!oTax.save(rma.get_TrxName()))
					return false;
			}
			else
			{
				if (!rma.isTaxIncluded())
					grandTotal = grandTotal.add(oTax.getTaxAmt());
			}
		}		
		//
		rma.setAmt(grandTotal);
		return true;
	}

	public boolean updateRMATax(I_C_TaxProvider provider, I_M_RMALine line) {
		MTax mtax = new MTax(line.getCtx(), line.getC_Tax_ID(), line.get_TrxName());
    	if (mtax.getC_TaxProvider_ID() == 0)
    		return line.updateOrderTax(false);
    	return true;
	}

	public boolean recalculateTax(I_C_TaxProvider provider, I_M_RMALine line, boolean newRecord)
	{
		if (!newRecord && line.is_ValueChanged(MRMALine.COLUMNNAME_C_Tax_ID) && !line.getParent().isProcessed())
		{
			MTax mtax = new MTax(line.getCtx(), line.getC_Tax_ID(), line.get_TrxName());
	    	if (mtax.getC_TaxProvider_ID() == 0)
	    	{
				//	Recalculate Tax for old Tax
				if (!line.updateOrderTax(true))
					return false;
	    	}
		}

        return line.updateHeaderAmt();
	}

	public boolean updateHeaderTax(I_C_TaxProvider provider, I_M_RMALine line)
	{
		//	Update RMA Header
		String sql = "UPDATE M_RMA "
			+ " SET Amt="
				+ "(SELECT COALESCE(SUM(LineNetAmt),0) FROM M_RMALine WHERE M_RMA.M_RMA_ID=M_RMALine.M_RMA_ID) "
			+ "WHERE M_RMA_ID=?";
		int no = DB.executeUpdateEx(sql, new Object[]{line.getM_RMA_ID()}, line.get_TrxName());
		if (no != 1)
			log.warning("(1) #" + no);

		line.clearParent();

		return no == 1;
	}

	public String validateConnection(I_C_TaxProvider provider, IProcessInfo pi) throws Exception {
		throw new IllegalStateException(Msg.getMsg(provider.getCtx(), "ActionNotSupported"));
	}
}
