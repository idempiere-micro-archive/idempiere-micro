package org.compiere.impl;

import org.compiere.model.*;
import org.compiere.tax.IInvoiceTaxProvider;
import org.compiere.tax.MTax;
import org.compiere.tax.MTaxProvider;
import org.idempiere.common.util.DB;
import org.idempiere.common.util.Env;

import java.math.BigDecimal;
import java.util.ArrayList;

public class StandardTaxProvider extends org.compiere.order.StandardTaxProvider implements IInvoiceTaxProvider {
    @Override
    public boolean calculateInvoiceTaxTotal(I_C_TaxProvider provider, I_C_Invoice invoice) {
        //	Lines
        BigDecimal totalLines = Env.ZERO;
        ArrayList<Integer> taxList = new ArrayList<Integer>();
        I_C_InvoiceLine[] lines = invoice.getLines(false);
        for (int i = 0; i < lines.length; i++)
        {
            I_C_InvoiceLine line = lines[i];
            totalLines = totalLines.add(line.getLineNetAmt());
            if (!taxList.contains(line.getC_Tax_ID()))
            {
                MTax tax = new MTax(invoice.getCtx(), line.getC_Tax_ID(), invoice.get_TrxName());
                if (tax.getC_TaxProvider_ID() != 0)
                    continue;
                MInvoiceTax iTax = MInvoiceTax.get (line, invoice.getPrecision(), false, invoice.get_TrxName()); //	current Tax
                if (iTax != null)
                {
                    iTax.setIsTaxIncluded(invoice.isTaxIncluded());
                    if (!iTax.calculateTaxFromLines())
                        return false;
                    iTax.saveEx();
                    taxList.add(line.getC_Tax_ID());
                }
            }
        }

        //	Taxes
        BigDecimal grandTotal = totalLines;
        I_C_InvoiceTax[] taxes = invoice.getTaxes(true);
        for (int i = 0; i < taxes.length; i++)
        {
            I_C_InvoiceTax iTax = taxes[i];
            if (iTax.getC_TaxProvider_ID() != 0) {
                if (!invoice.isTaxIncluded())
                    grandTotal = grandTotal.add(iTax.getTaxAmt());
                continue;
            }
            I_C_Tax tax = iTax.getTax();
            if (tax.isSummary())
            {
                I_C_Tax[] cTaxes = tax.getChildTaxes(false);	//	Multiple taxes
                for (int j = 0; j < cTaxes.length; j++)
                {
                    I_C_Tax cTax = cTaxes[j];
                    BigDecimal taxAmt = cTax.calculateTax(iTax.getTaxBaseAmt(), false, invoice.getPrecision());
                    //
                    MInvoiceTax newITax = new MInvoiceTax(invoice.getCtx(), 0, invoice.get_TrxName());
                    newITax.setClientOrg(invoice);
                    newITax.setAD_Org_ID(invoice.getAD_Org_ID());
                    newITax.setC_Invoice_ID(invoice.getC_Invoice_ID());
                    newITax.setC_Tax_ID(cTax.getC_Tax_ID());
                    newITax.setPrecision(invoice.getPrecision());
                    newITax.setIsTaxIncluded(invoice.isTaxIncluded());
                    newITax.setTaxBaseAmt(iTax.getTaxBaseAmt());
                    newITax.setTaxAmt(taxAmt);
                    newITax.saveEx(invoice.get_TrxName());
                    //
                    if (!invoice.isTaxIncluded())
                        grandTotal = grandTotal.add(taxAmt);
                }
                iTax.deleteEx(true, invoice.get_TrxName());
            }
            else
            {
                if (!invoice.isTaxIncluded())
                    grandTotal = grandTotal.add(iTax.getTaxAmt());
            }
        }
        //
        invoice.setTotalLines(totalLines);
        invoice.setGrandTotal(grandTotal);
        return true;
    }

    @Override
    public boolean updateInvoiceTax(I_C_TaxProvider provider, I_C_InvoiceLine line) {
        MTax mtax = new MTax(line.getCtx(), line.getC_Tax_ID(), line.get_TrxName());
        if (mtax.getC_TaxProvider_ID() == 0)
            return line.updateInvoiceTax(false);
        return true;
    }

    @Override
    public boolean recalculateTax(I_C_TaxProvider provider, I_C_InvoiceLine line, boolean newRecord) {
        if (!newRecord && (line instanceof org.idempiere.orm.PO) &&
            ((org.idempiere.orm.PO)line).is_ValueChanged(MInvoiceLine.COLUMNNAME_C_Tax_ID))
        {
            MTax mtax = new MTax(line.getCtx(), line.getC_Tax_ID(), line.get_TrxName());
            if (mtax.getC_TaxProvider_ID() == 0)
            {
                //	Recalculate Tax for old Tax
                if (!line.updateInvoiceTax(true))
                    return false;
            }
        }
        return line.updateHeaderTax();
    }

    @Override
    public boolean updateHeaderTax(I_C_TaxProvider provider, I_C_InvoiceLine line)
    {
        //		Update Invoice Header
        String sql = "UPDATE C_Invoice i"
            + " SET TotalLines="
            + "(SELECT COALESCE(SUM(LineNetAmt),0) FROM C_InvoiceLine il WHERE i.C_Invoice_ID=il.C_Invoice_ID) "
            + "WHERE C_Invoice_ID=?";
        int no = DB.executeUpdateEx(sql, new Object[]{line.getC_Invoice_ID()}, line.get_TrxName());
        if (no != 1)
            log.warning("(1) #" + no);

        if (line.isTaxIncluded())
            sql = "UPDATE C_Invoice i "
                + " SET GrandTotal=TotalLines "
                + "WHERE C_Invoice_ID=?";
        else
            sql = "UPDATE C_Invoice i "
                + " SET GrandTotal=TotalLines+"
                + "(SELECT COALESCE(SUM(TaxAmt),0) FROM C_InvoiceTax it WHERE i.C_Invoice_ID=it.C_Invoice_ID) "
                + "WHERE C_Invoice_ID=?";
        no = DB.executeUpdateEx(sql, new Object[]{line.getC_Invoice_ID()}, line.get_TrxName());
        if (no != 1)
            log.warning("(2) #" + no);
        line.clearParent();

        return no == 1;
    }
}
