package org.compiere.tax;

import org.compiere.model.I_C_Invoice;
import org.compiere.model.I_C_InvoiceLine;
import org.compiere.model.I_C_TaxProvider;

public interface IInvoiceTaxProvider extends ITaxProvider {
    public boolean calculateInvoiceTaxTotal(I_C_TaxProvider provider, I_C_Invoice invoice);

    public boolean updateInvoiceTax(I_C_TaxProvider provider, I_C_InvoiceLine line);

    public boolean recalculateTax(I_C_TaxProvider provider, I_C_InvoiceLine line, boolean newRecord);

    public boolean updateHeaderTax(I_C_TaxProvider provider, I_C_InvoiceLine line);
}
