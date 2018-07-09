package org.compiere.impl;

import org.compiere.model.I_C_Invoice;
import org.compiere.model.I_C_Order;
import org.compiere.order.MCharge;
import org.compiere.order.MInOut;
import org.compiere.order.MOrder;
import org.compiere.order.MOrderLine;
import org.compiere.product.IProductPricing;
import org.compiere.tax.Tax;
import org.idempiere.common.util.DB;
import org.idempiere.common.util.Env;

import java.util.Properties;

public class MRMALine extends org.compiere.order.MRMALine {
    /**
     * 	Standard Constructor
     *	@param ctx context
     *	@param M_RMALine_ID id
     *	@param trxName transaction
     */
    public MRMALine (Properties ctx, int M_RMALine_ID, String trxName) {
        super(ctx, M_RMALine_ID, trxName);
    }

    private MRMA		m_parent = null;

    /**
     *  Get Parent
     *  @return parent
     */
    public MRMA getParent()
    {
        if (m_parent == null)
            m_parent = new MRMA(getCtx(), getM_RMA_ID(), get_TrxName());
        return m_parent;
    }   //  getParent

    /**
     * Initialise parameters that are required
     */
    @Override
    protected void init()
    {
        getShipLine();

        if (m_ioLine != null)
        {
            // Get pricing details (Based on invoice if found, on order otherwise)
            //   --> m_ioLine.isInvoiced just work for sales orders - so it doesn't work for purchases
            if (getInvoiceLineId() != 0)
            {
                MInvoiceLine invoiceLine = new MInvoiceLine(getCtx(), getInvoiceLineId(), get_TrxName());
                precision = invoiceLine.getPrecision();
                unitAmount = invoiceLine.getPriceEntered();
                originalQty = invoiceLine.getQtyInvoiced();
                taxId = invoiceLine.getC_Tax_ID();
            }
            else if (m_ioLine.getC_OrderLine_ID() != 0)
            {
                MOrderLine orderLine = new MOrderLine (getCtx(), m_ioLine.getC_OrderLine_ID(), get_TrxName());
                precision = orderLine.getPrecision();
                unitAmount = orderLine.getPriceEntered();
                originalQty = orderLine.getQtyDelivered();
                taxId = orderLine.getC_Tax_ID();
            }
            else
            {
                throw new IllegalStateException("No Invoice/Order line found the Shipment/Receipt line associated");
            }
        }
        else if (getC_Charge_ID() != 0)
        {
            MCharge charge = MCharge.get(this.getCtx(), getC_Charge_ID());
            unitAmount = charge.getChargeAmt();

            I_C_Invoice invoice = getParent().getOriginalInvoice();
            if (invoice != null)
                precision = invoice.getPrecision();
            else
            {
                I_C_Order order = getParent().getOriginalOrder();
                if (order != null)
                    precision = order.getPrecision();
                else
                    throw new IllegalStateException("No Invoice/Order found the Shipment/Receipt associated");
            }

            // Retrieve tax Exempt
            String sql = "SELECT C_Tax_ID FROM C_Tax WHERE AD_Client_ID=? AND IsActive='Y' "
                + "AND IsTaxExempt='Y' AND ValidFrom < SYSDATE ORDER BY IsDefault DESC";

            // Set tax for charge as exempt
            taxId = DB.getSQLValueEx(null, sql, Env.getAD_Client_ID(getCtx()));
            m_ioLine = null;
        }
        else if (getM_Product_ID() != 0)
        {
            IProductPricing pp = MProduct.getProductPricing();
            pp.setRMALine(this, get_TrxName());

            I_C_Invoice invoice = getParent().getOriginalInvoice();
            if (invoice != null)
            {
                pp.setM_PriceList_ID(invoice.getM_PriceList_ID());
                pp.setPriceDate(invoice.getDateInvoiced());

                precision = invoice.getPrecision();
                taxId = Tax.get(getCtx(), getM_Product_ID(), getC_Charge_ID(), invoice.getDateInvoiced(), invoice.getDateInvoiced(),
                    getAD_Org_ID(), getParent().getShipment().getM_Warehouse_ID(),
                    invoice.getC_BPartner_Location_ID(),		//	should be bill to
                    invoice.getC_BPartner_Location_ID(), getParent().isSOTrx(), get_TrxName());
            }
            else
            {
                MOrder order = getParent().getOriginalOrder();
                if (order != null)
                {
                    pp.setM_PriceList_ID(order.getM_PriceList_ID());
                    pp.setPriceDate(order.getDateOrdered());

                    precision = order.getPrecision();
                    taxId = Tax.get(getCtx(), getM_Product_ID(), getC_Charge_ID(), order.getDateOrdered(), order.getDateOrdered(),
                        getAD_Org_ID(), order.getM_Warehouse_ID(),
                        order.getC_BPartner_Location_ID(),		//	should be bill to
                        order.getC_BPartner_Location_ID(), getParent().isSOTrx(), get_TrxName());
                }
                else
                    throw new IllegalStateException("No Invoice/Order found the Shipment/Receipt associated");
            }

            pp.calculatePrice();
            unitAmount = pp.getPriceStd();

            m_ioLine = null;
        }
    }

    /**
     * Get Locator
     * @return locator if based on shipment line and 0 for charge based
     */
    @Override
    public int getM_Locator_ID()
    {
        if (m_ioLine == null && getC_Charge_ID() != 0)
            return 0;
        else if (m_ioLine == null && getM_Product_ID() != 0)
        {
            MInOut shipment = getParent().getShipment();
            MWarehouse warehouse = new MWarehouse (getCtx(), shipment.getM_Warehouse_ID(), get_TrxName());
            MLocator locator = MLocator.getDefault(warehouse);
            return locator.getM_Locator_ID();
        }
        return m_ioLine.getM_Locator_ID();
    }

    public void setClientOrg (MRMA rma)
    {
        super.setClientOrg(rma);
    }	//	setClientOrg
}
