package org.compiere.impl;

import org.adempiere.exceptions.FillMandatoryException;
import org.adempiere.exceptions.WarehouseLocatorConflictException;
import org.compiere.acct.Doc;
import org.compiere.model.IDoc;
import org.compiere.model.IPODoc;
import org.compiere.model.I_M_AttributeSet;
import org.compiere.model.I_M_InOutLine;
import org.compiere.order.MInOut;
import org.compiere.orm.Query;
import org.compiere.product.MAttributeSetInstance;
import org.compiere.product.MUOM;
import org.compiere.util.Msg;
import org.idempiere.common.util.DB;
import org.idempiere.common.util.Env;
import org.idempiere.common.util.Util;

import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;

public class MInOutLine extends org.compiere.order.MInOutLine implements IPODoc {

    /**************************************************************************
     * 	Standard Constructor
     *	@param ctx context
     *	@param M_InOutLine_ID id
     *	@param trxName trx name
     */
    public MInOutLine (Properties ctx, int M_InOutLine_ID, String trxName) {
        super(ctx, M_InOutLine_ID, trxName);
    }

    /**
     *  Parent Constructor
     *  @param inout parent
     */
    public MInOutLine (MInOut inout) {
        super(inout);
    }

    /**
     * 	Set Invoice Line.
     * 	Does not set Quantity!
     *	@param iLine invoice line
     *	@param M_Locator_ID locator
     *	@param Qty qty only fo find suitable locator
     */
    public void setInvoiceLine (MInvoiceLine iLine, int M_Locator_ID, BigDecimal Qty)
    {
        setC_OrderLine_ID(iLine.getC_OrderLine_ID());
        setLine(iLine.getLine());
        setC_UOM_ID(iLine.getC_UOM_ID());
        int M_Product_ID = iLine.getM_Product_ID();
        if (M_Product_ID == 0)
        {
            set_ValueNoCheck("M_Product_ID", null);
            set_ValueNoCheck("M_Locator_ID", null);
            set_ValueNoCheck("M_AttributeSetInstance_ID", null);
        }
        else
        {
            setM_Product_ID(M_Product_ID);
            setM_AttributeSetInstance_ID(iLine.getM_AttributeSetInstance_ID());
            if (M_Locator_ID == 0)
                setM_Locator_ID(Qty);	//	requires warehouse, product, asi
            else
                setM_Locator_ID(M_Locator_ID);
        }
        setC_Charge_ID(iLine.getC_Charge_ID());
        setDescription(iLine.getDescription());
        setIsDescription(iLine.isDescription());
        //
        setC_Project_ID(iLine.getC_Project_ID());
        setC_ProjectPhase_ID(iLine.getC_ProjectPhase_ID());
        setC_ProjectTask_ID(iLine.getC_ProjectTask_ID());
        setC_Activity_ID(iLine.getC_Activity_ID());
        setC_Campaign_ID(iLine.getC_Campaign_ID());
        setAD_OrgTrx_ID(iLine.getAD_OrgTrx_ID());
        setUser1_ID(iLine.getUser1_ID());
        setUser2_ID(iLine.getUser2_ID());
    }	//	setInvoiceLine

    /**
     * 	Set (default) Locator based on qty.
     * 	@param Qty quantity
     * 	Assumes Warehouse is set
     */
    @Override
    public void setM_Locator_ID(BigDecimal Qty)
    {
        //	Locator established
        if (getM_Locator_ID() != 0)
            return;
        //	No Product
        if (getM_Product_ID() == 0)
        {
            set_ValueNoCheck(I_M_InOutLine.COLUMNNAME_M_Locator_ID, null);
            return;
        }

        //	Get existing Location
        int M_Locator_ID = MStorageOnHand.getM_Locator_ID (getM_Warehouse_ID(),
            getM_Product_ID(), getM_AttributeSetInstance_ID(),
            Qty, get_TrxName());
        //	Get default Location
        if (M_Locator_ID == 0)
        {
            MWarehouse wh = MWarehouse.get(getCtx(), getM_Warehouse_ID());
            M_Locator_ID = wh.getDefaultLocator().getM_Locator_ID();
        }
        setM_Locator_ID(M_Locator_ID);
    }	//	setM_Locator_ID


    /**************************************************************************
     * 	Before Save
     *	@param newRecord new
     *	@return save
     */
    @Override
    protected boolean beforeSave (boolean newRecord)
    {
        log.fine("");
        if (newRecord && getParent().isComplete()) {
            log.saveError("ParentComplete", Msg.translate(getCtx(), "M_InOutLine"));
            return false;
        }
        // Locator is mandatory if no charge is defined - teo_sarca BF [ 2757978 ]
        if(getProduct() != null && org.compiere.product.MProduct.PRODUCTTYPE_Item.equals(getProduct().getProductType()))
        {
            if (getM_Locator_ID() <= 0 && getC_Charge_ID() <= 0)
            {
                throw new FillMandatoryException(I_M_InOutLine.COLUMNNAME_M_Locator_ID);
            }
        }

        //	Get Line No
        if (getLine() == 0)
        {
            String sql = "SELECT COALESCE(MAX(Line),0)+10 FROM M_InOutLine WHERE M_InOut_ID=?";
            int ii = DB.getSQLValueEx (get_TrxName(), sql, getM_InOut_ID());
            setLine (ii);
        }
        //	UOM
        if (getC_UOM_ID() == 0)
            setC_UOM_ID (Env.getContextAsInt(getCtx(), "#C_UOM_ID"));
        if (getC_UOM_ID() == 0)
        {
            int C_UOM_ID = MUOM.getDefault_UOM_ID(getCtx());
            if (C_UOM_ID > 0)
                setC_UOM_ID (C_UOM_ID);
        }
        //	Qty Precision
        if (newRecord || is_ValueChanged("QtyEntered"))
            setQtyEntered(getQtyEntered());
        if (newRecord || is_ValueChanged("MovementQty"))
            setMovementQty(getMovementQty());

        //	Order/RMA Line
        if (getC_OrderLine_ID() == 0 && getM_RMALine_ID() == 0)
        {
            if (getParent().isSOTrx())
            {
                log.saveError("FillMandatory", Msg.translate(getCtx(), "C_Order_ID"));
                return false;
            }
        }

        // Validate Locator/Warehouse - teo_sarca, BF [ 2784194 ]
        if (getM_Locator_ID() > 0)
        {
            MLocator locator = MLocator.get(getCtx(), getM_Locator_ID());
            if (getM_Warehouse_ID() != locator.getM_Warehouse_ID())
            {
                throw new WarehouseLocatorConflictException(
                    MWarehouse.get(getCtx(), getM_Warehouse_ID()),
                    locator,
                    getLine());
            }

            // IDEMPIERE-2668
            if (org.compiere.order.MInOut.MOVEMENTTYPE_CustomerShipment.equals(getParent().getMovementType())) {
                if (locator.getM_LocatorType_ID() > 0) {
                    MLocatorType lt = MLocatorType.get(getCtx(), locator.getM_LocatorType_ID());
                    if (! lt.isAvailableForShipping()) {
                        log.saveError("Error", Msg.translate(getCtx(), "LocatorNotAvailableForShipping"));
                        return false;
                    }
                }
            }

        }
        I_M_AttributeSet attributeset = getM_Product().getM_AttributeSet();
        boolean isAutoGenerateLot = false;
        if (attributeset != null)
            isAutoGenerateLot = attributeset.isAutoGenerateLot();
        if (getReversalLine_ID() == 0 && !getParent().isSOTrx() && !getParent().getMovementType().equals(MInOut.MOVEMENTTYPE_VendorReturns) && isAutoGenerateLot
            && getM_AttributeSetInstance_ID() == 0)
        {
            MAttributeSetInstance asi = MAttributeSetInstance.generateLot(getCtx(), (MProduct)getM_Product(), get_TrxName());
            setM_AttributeSetInstance_ID(asi.getM_AttributeSetInstance_ID());
        }
        //	if (getC_Charge_ID() == 0 && getM_Product_ID() == 0)
        //		;

        /**	 Qty on instance ASI
         if (getM_AttributeSetInstance_ID() != 0)
         {
         MProduct product = getProduct();
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
         MStorage storage = MStorage.get(getCtx(), getM_Locator_ID(),
         getM_Product_ID(), getM_AttributeSetInstance_ID(), get_TrxName());
         if (storage != null)
         {
         BigDecimal qty = storage.getQtyOnHand();
         if (getMovementQty().compareTo(qty) > 0)
         {
         log.warning("Qty - Stock=" + qty + ", Movement=" + getMovementQty());
         log.saveError("QtyInsufficient", "=" + qty);
         return false;
         }
         }
         }
         }	/**/

        /* Carlos Ruiz - globalqss
         * IDEMPIERE-178 Orders and Invoices must disallow amount lines without product/charge
         */
        if (getParent().getC_DocType().isChargeOrProductMandatory()) {
            if (getC_Charge_ID() == 0 && getM_Product_ID() == 0) {
                log.saveError("FillMandatory", Msg.translate(getCtx(), "ChargeOrProductMandatory"));
                return false;
            }
        }

        return true;
    }	//	beforeSave

    /**
     * 	Get Base value for Cost Distribution
     *	@param CostDistribution cost Distribution
     *	@return base number
     */
    public BigDecimal getBase (String CostDistribution)
    {
        if (MLandedCost.LANDEDCOSTDISTRIBUTION_Costs.equals(CostDistribution))
        {
            MInvoiceLine m_il = MInvoiceLine.getOfInOutLine(this);
            if (m_il == null)
            {
                m_il = MInvoiceLine.getOfInOutLineFromMatchInv(this);
                if (m_il == null)
                {
                    log.severe("No Invoice Line for: " + this.toString());
                    return Env.ZERO;
                }
            }
            return this.getMovementQty().multiply(m_il.getPriceActual());  // Actual delivery
        }
        else if (MLandedCost.LANDEDCOSTDISTRIBUTION_Line.equals(CostDistribution))
            return Env.ONE;
        else if (MLandedCost.LANDEDCOSTDISTRIBUTION_Quantity.equals(CostDistribution))
            return getMovementQty();
        else if (MLandedCost.LANDEDCOSTDISTRIBUTION_Volume.equals(CostDistribution))
        {
            MProduct product = getProduct();
            if (product == null)
            {
                log.severe("No Product");
                return Env.ZERO;
            }
            return getMovementQty().multiply(product.getVolume());
        }
        else if (MLandedCost.LANDEDCOSTDISTRIBUTION_Weight.equals(CostDistribution))
        {
            MProduct product = getProduct();
            if (product == null)
            {
                log.severe("No Product");
                return Env.ZERO;
            }
            return getMovementQty().multiply(product.getWeight());
        }
        //
        log.severe("Invalid Criteria: " + CostDistribution);
        return Env.ZERO;
    }	//	getBase

    /**	Product					*/
    private MProduct 		m_product = null;

    /**
     * 	Get Product
     *	@return product or null
     */
    public MProduct getProduct()
    {
        if (m_product == null && getM_Product_ID() != 0)
            m_product = MProduct.get (getCtx(), getM_Product_ID());
        return m_product;
    }	//	getProduct

    public void setClientOrg (MInOut inout)
    {
        super.setClientOrg(inout);
    }	//	setClientOrg

    /* Doc - To be used on ModelValidator to get the corresponding Doc from the PO */
    private IDoc m_doc;

    @Override
    public void setDoc(IDoc doc) {
        m_doc = doc;
    }

    /**
     * 	Get Ship lines Of Order Line
     *	@param ctx context
     *	@param C_OrderLine_ID line
     *	@param where optional addition where clause
     *  @param trxName transaction
     *	@return array of receipt lines
     */
    public static MInOutLine[] getOfOrderLine (Properties ctx,
                                                                  int C_OrderLine_ID, String where, String trxName)
    {
        String whereClause = "C_OrderLine_ID=?" + (!Util.isEmpty(where, true) ? " AND "+where : "");
        List<org.compiere.order.MInOutLine> list = new Query(ctx, I_M_InOutLine.Table_Name, whereClause, trxName)
            .setParameters(C_OrderLine_ID)
            .list();
        return list.toArray (new MInOutLine[list.size()]);
    }	//	getOfOrderLine
}
