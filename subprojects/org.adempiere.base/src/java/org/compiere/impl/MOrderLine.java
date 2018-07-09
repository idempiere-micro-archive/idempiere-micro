package org.compiere.impl;

import org.compiere.acct.Doc;
import org.compiere.model.IDoc;
import org.compiere.model.IPODoc;
import org.compiere.orm.MRole;
import org.compiere.product.MAttributeSet;
import org.compiere.product.MUOM;
import org.compiere.product.ProductNotOnPriceListException;
import org.compiere.util.Msg;
import org.idempiere.common.util.DB;
import org.idempiere.common.util.Env;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

public class MOrderLine extends org.compiere.order.MOrderLine implements IPODoc {

    public MOrderLine (MOrder order) {
        super(order);
    }


        /**
         *  Load Constructor
         *  @param ctx context
         *  @param rs result set record
         *  @param trxName transaction
         */
    public MOrderLine (Properties ctx, ResultSet rs, String trxName)
    {
        super(ctx, rs, trxName);
    }

    /**************************************************************************
     *  Default Constructor
     *  @param ctx context
     *  @param  C_OrderLine_ID  order line to load
     *  @param trxName trx name
     */
    public MOrderLine (Properties ctx, int C_OrderLine_ID, String trxName) {
        super(ctx, C_OrderLine_ID, trxName);
    }

    /**
     * 	Get Base value for Cost Distribution
     *	@param CostDistribution cost Distribution
     *	@return base number
     */
    public BigDecimal getBase (String CostDistribution)
    {
        if (MLandedCost.LANDEDCOSTDISTRIBUTION_Costs.equals(CostDistribution))
        {
            return this.getQtyOrdered().multiply(getPriceActual());  // Actual delivery
        }
        else if (MLandedCost.LANDEDCOSTDISTRIBUTION_Line.equals(CostDistribution))
            return Env.ONE;
        else if (MLandedCost.LANDEDCOSTDISTRIBUTION_Quantity.equals(CostDistribution))
            return getQtyOrdered();
        else if (MLandedCost.LANDEDCOSTDISTRIBUTION_Volume.equals(CostDistribution))
        {
            org.compiere.product.MProduct product = getProduct();
            if (product == null)
            {
                log.severe("No Product");
                return Env.ZERO;
            }
            return getQtyOrdered().multiply(product.getVolume());
        }
        else if (MLandedCost.LANDEDCOSTDISTRIBUTION_Weight.equals(CostDistribution))
        {
            MProduct product = getProduct();
            if (product == null)
            {
                log.severe("No Product");
                return Env.ZERO;
            }
            return getQtyOrdered().multiply(product.getWeight());
        }
        //
        log.severe("Invalid Criteria: " + CostDistribution);
        return Env.ZERO;
    }	//	getBase

    /**************************************************************************
     * 	Before Save
     *	@param newRecord
     *	@return true if it can be saved
     */
    @Override
    protected boolean beforeSave (boolean newRecord)
    {
        if (newRecord && getParent().isComplete()) {
            log.saveError("ParentComplete", Msg.translate(getCtx(), "C_OrderLine"));
            return false;
        }
        //	Get Defaults from Parent
        if (getC_BPartner_ID() == 0 || getC_BPartner_Location_ID() == 0
            || getM_Warehouse_ID() == 0
            || getC_Currency_ID() == 0)
            setOrder (getParent());
        if (m_M_PriceList_ID == 0)
            setHeaderInfo(getParent());


        //	R/O Check - Product/Warehouse Change
        if (!newRecord
            && (is_ValueChanged("M_Product_ID") || is_ValueChanged("M_Warehouse_ID")))
        {
            if (!canChangeWarehouse())
                return false;
        }	//	Product Changed

        //	Charge
        if (getC_Charge_ID() != 0 && getM_Product_ID() != 0)
            setM_Product_ID(0);
        //	No Product
        if (getM_Product_ID() == 0)
            setM_AttributeSetInstance_ID(0);
            //	Product
        else	//	Set/check Product Price
        {
            //	Set Price if Actual = 0
            if (m_productPrice == null
                &&  Env.ZERO.compareTo(getPriceActual()) == 0
                &&  Env.ZERO.compareTo(getPriceList()) == 0)
                setPrice();
            //	Check if on Price list
            if (m_productPrice == null)
                getProductPricing(m_M_PriceList_ID);
            // IDEMPIERE-1574 Sales Order Line lets Price under the Price Limit when updating
            //	Check PriceLimit
            boolean enforce = m_IsSOTrx && getParent().getM_PriceList().isEnforcePriceLimit();
            if (enforce && MRole.getDefault().isOverwritePriceLimit())
                enforce = false;
            //	Check Price Limit?
            if (enforce && getPriceLimit() != Env.ZERO
                && getPriceActual().compareTo(getPriceLimit()) < 0)
            {
                log.saveError("UnderLimitPrice", "PriceEntered=" + getPriceEntered() + ", PriceLimit=" + getPriceLimit());
                return false;
            }
            //
            if (!m_productPrice.isCalculated())
            {
                throw new ProductNotOnPriceListException(m_productPrice, getLine());
            }
        }

        //	UOM
        if (getC_UOM_ID() == 0
            && (getM_Product_ID() != 0
            || getPriceEntered().compareTo(Env.ZERO) != 0
            || getC_Charge_ID() != 0))
        {
            int C_UOM_ID = MUOM.getDefault_UOM_ID(getCtx());
            if (C_UOM_ID > 0)
                setC_UOM_ID (C_UOM_ID);
        }
        //	Qty Precision
        if (newRecord || is_ValueChanged("QtyEntered"))
            setQtyEntered(getQtyEntered());
        if (newRecord || is_ValueChanged("QtyOrdered"))
            setQtyOrdered(getQtyOrdered());

        //	Qty on instance ASI for SO
        if (m_IsSOTrx
            && getM_AttributeSetInstance_ID() != 0
            && (newRecord || is_ValueChanged("M_Product_ID")
            || is_ValueChanged("M_AttributeSetInstance_ID")
            || is_ValueChanged("M_Warehouse_ID")))
        {
            MProduct product = getProduct();
            if (product.isStocked())
            {
                int M_AttributeSet_ID = product.getM_AttributeSet_ID();
                boolean isInstance = M_AttributeSet_ID != 0;
                if (isInstance)
                {
                    MAttributeSet mas = MAttributeSet.get(getCtx(), M_AttributeSet_ID);
                    isInstance = mas.isInstanceAttribute();
                }
                //	Max
                if (isInstance)
                {
                    MStorageOnHand[] storages = MStorageOnHand.getWarehouse(getCtx(),
                        getM_Warehouse_ID(), getM_Product_ID(), getM_AttributeSetInstance_ID(),
                        null, true, false, 0, get_TrxName());
                    BigDecimal qty = Env.ZERO;
                    for (int i = 0; i < storages.length; i++)
                    {
                        if (storages[i].getM_AttributeSetInstance_ID() == getM_AttributeSetInstance_ID())
                            qty = qty.add(storages[i].getQtyOnHand());
                    }

                    if (getQtyOrdered().compareTo(qty) > 0)
                    {
                        log.warning("Qty - Stock=" + qty + ", Ordered=" + getQtyOrdered());
                        log.saveError("QtyInsufficient", "=" + qty);
                        return false;
                    }
                }
            }	//	stocked
        }	//	SO instance

        //	FreightAmt Not used
        if (Env.ZERO.compareTo(getFreightAmt()) != 0)
            setFreightAmt(Env.ZERO);

        //	Set Tax
        if (getC_Tax_ID() == 0)
            setTax();

        //	Get Line No
        if (getLine() == 0)
        {
            String sql = "SELECT COALESCE(MAX(Line),0)+10 FROM C_OrderLine WHERE C_Order_ID=?";
            int ii = DB.getSQLValue (get_TrxName(), sql, getC_Order_ID());
            setLine (ii);
        }

        //	Calculations & Rounding
        setLineNetAmt();	//	extended Amount with or without tax
        setDiscount();

        /* Carlos Ruiz - globalqss
         * IDEMPIERE-178 Orders and Invoices must disallow amount lines without product/charge
         */
        if (getParent().getC_DocTypeTarget().isChargeOrProductMandatory()) {
            if (getC_Charge_ID() == 0 && getM_Product_ID() == 0 && (getPriceEntered().signum() != 0 || getQtyEntered().signum() != 0)) {
                log.saveError("FillMandatory", Msg.translate(getCtx(), "ChargeOrProductMandatory"));
                return false;
            }
        }

        return true;
    }	//	beforeSave

    /**
     * 	Before Delete
     *	@return true if it can be deleted
     */
    @Override
    protected boolean beforeDelete ()
    {
        //	R/O Check - Something delivered. etc.
        if (Env.ZERO.compareTo(getQtyDelivered()) != 0)
        {
            log.saveError("DeleteError", Msg.translate(getCtx(), "QtyDelivered") + "=" + getQtyDelivered());
            return false;
        }
        if (Env.ZERO.compareTo(getQtyInvoiced()) != 0)
        {
            log.saveError("DeleteError", Msg.translate(getCtx(), "QtyInvoiced") + "=" + getQtyInvoiced());
            return false;
        }
        if (Env.ZERO.compareTo(getQtyReserved()) != 0)
        {
            //	For PO should be On Order
            log.saveError("DeleteError", Msg.translate(getCtx(), "QtyReserved") + "=" + getQtyReserved());
            return false;
        }

        // UnLink All Requisitions
        MRequisitionLine.unlinkC_OrderLine_ID(getCtx(), get_ID(), get_TrxName());

        return true;
    }	//	beforeDelete


    /**
     * 	After Delete
     *	@param success success
     *	@return deleted
     */
    @Override
    protected boolean afterDelete (boolean success)
    {
        if (!success)
            return success;
        if (getS_ResourceAssignment_ID() != 0)
        {
            MResourceAssignment ra = new MResourceAssignment(getCtx(), getS_ResourceAssignment_ID(), get_TrxName());
            ra.delete(true);
        }

        return updateHeaderTax();
    }	//	afterDelete

    /**	Product					*/
    protected MProduct m_product = null;

    /**
     * 	Get Product
     *	@return product or null
     */
    public MProduct getProduct()
    {
        if (m_product == null && getM_Product_ID() != 0)
            m_product =  MProduct.get (getCtx(), getM_Product_ID());
        return m_product;
    }	//	getProduct


    /* Doc - To be used on ModelValidator to get the corresponding Doc from the PO */
    private IDoc m_doc;

    @Override
    public void setDoc(IDoc doc) {
        m_doc = doc;
    }
}
