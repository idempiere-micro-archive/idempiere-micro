package org.compiere.impl;

import org.adempiere.exceptions2.NegativeInventoryDisallowedException;
import org.adempiere.exceptions2.PeriodClosedException;
import org.compiere.acct.Doc;
import org.compiere.crm.MBPartner;
import org.compiere.model.*;
import org.compiere.order.X_M_InOut;
import org.compiere.orm.*;
import org.compiere.orm.MClient;
import org.compiere.orm.MOrg;
import org.compiere.orm.PO;
import org.compiere.process.DocAction;
import org.compiere.process2.DocumentEngine;
import org.compiere.util.Msg;
import org.compiere.validation.ModelValidationEngine;
import org.compiere.validation.ModelValidator;
import org.idempiere.common.exceptions.AdempiereException;
import org.idempiere.common.util.CLogger;
import org.idempiere.common.util.Env;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

public class MInOut extends org.compiere.order.MInOut implements DocAction, IPODoc {

    /**
     * 	Invoice Constructor - create header only
     *	@param invoice invoice
     *	@param C_DocTypeShipment_ID document type or 0
     *	@param movementDate optional movement date (default today)
     *	@param M_Warehouse_ID warehouse
     */
    public MInOut (I_C_Invoice invoice, int C_DocTypeShipment_ID, Timestamp movementDate, int M_Warehouse_ID)
    {
        super (invoice, C_DocTypeShipment_ID, movementDate, M_Warehouse_ID);
    }


    /* Doc - To be used on ModelValidator to get the corresponding Doc from the PO */
    private IDoc m_doc;

    /**
     *      Set the accounting document associated to the PO - for use in POST ModelValidator
     *      @param doc Document
     */
    public void setDoc(IDoc doc) {
        m_doc = doc;
    }

    /**
     * 	Copy Constructor - create header only
     *	@param original original
     *	@param movementDate optional movement date (default today)
     *	@param C_DocTypeShipment_ID document type or 0
     */
    public MInOut (MInOut original, int C_DocTypeShipment_ID, Timestamp movementDate) {
        super(original, C_DocTypeShipment_ID, movementDate);
    }


    public MInOut (Properties ctx, int M_InOut_ID, String trxName) {
        super (ctx, M_InOut_ID, trxName);
    }

    /**
     * 	Create Shipment From Order
     *	@param order order
     *	@param movementDate optional movement date
     *	@param forceDelivery ignore order delivery rule
     *	@param allAttributeInstances if true, all attribute set instances
     *	@param minGuaranteeDate optional minimum guarantee date if all attribute instances
     *	@param complete complete document (Process if false, Complete if true)
     *	@param trxName transaction
     *	@return Shipment or null
     */
    public static org.compiere.order.MInOut createFrom (MOrder order, Timestamp movementDate,
                                                        boolean forceDelivery, boolean allAttributeInstances, Timestamp minGuaranteeDate,
                                                        boolean complete, String trxName)
    {
        if (order == null)
            throw new IllegalArgumentException("No Order");
        //
        if (!forceDelivery && X_M_InOut.DELIVERYRULE_CompleteLine.equals(order.getDeliveryRule()))
        {
            return null;
        }

        //	Create Header
        org.compiere.order.MInOut retValue = new org.compiere.order.MInOut(order, 0, movementDate);
        retValue.setDocAction(complete ? X_M_InOut.DOCACTION_Complete : X_M_InOut.DOCACTION_Prepare);

        //	Check if we can create the lines
        MOrderLine[] oLines = order.getLines(true, "M_Product_ID");
        for (int i = 0; i < oLines.length; i++)
        {
            // Calculate how much is left to deliver (ordered - delivered)
            BigDecimal qty = oLines[i].getQtyOrdered().subtract(oLines[i].getQtyDelivered());
            //	Nothing to deliver
            if (qty.signum() == 0)
                continue;
            //	Stock Info
            MStorageOnHand[] storages = null;
            MProduct product = oLines[i].getProduct();
            if (product != null && product.get_ID() != 0 && product.isStocked())
            {
                String MMPolicy = product.getMMPolicy();
                storages = MStorageOnHand.getWarehouse (order.getCtx(), order.getM_Warehouse_ID(),
                    oLines[i].getM_Product_ID(), oLines[i].getM_AttributeSetInstance_ID(),
                    minGuaranteeDate, MClient.MMPOLICY_FiFo.equals(MMPolicy), true, 0, trxName);
            } else {
                continue;
            }

            if (!forceDelivery)
            {
                BigDecimal maxQty = Env.ZERO;
                for (int ll = 0; ll < storages.length; ll++)
                    maxQty = maxQty.add(storages[ll].getQtyOnHand());
                if (X_M_InOut.DELIVERYRULE_Availability.equals(order.getDeliveryRule()))
                {
                    if (maxQty.compareTo(qty) < 0)
                        qty = maxQty;
                }
                else if (X_M_InOut.DELIVERYRULE_CompleteLine.equals(order.getDeliveryRule()))
                {
                    if (maxQty.compareTo(qty) < 0)
                        continue;
                }
            }
            //	Create Line
            if (retValue.get_ID() == 0)	//	not saved yet
                retValue.saveEx(trxName);
            //	Create a line until qty is reached
            for (int ll = 0; ll < storages.length; ll++)
            {
                BigDecimal lineQty = storages[ll].getQtyOnHand();
                if (lineQty.compareTo(qty) > 0)
                    lineQty = qty;
                MInOutLine line = new MInOutLine (retValue);
                line.setOrderLine(oLines[i], storages[ll].getM_Locator_ID(),
                    order.isSOTrx() ? lineQty : Env.ZERO);
                line.setQty(lineQty);	//	Correct UOM for QtyEntered
                if (oLines[i].getQtyEntered().compareTo(oLines[i].getQtyOrdered()) != 0)
                    line.setQtyEntered(lineQty
                        .multiply(oLines[i].getQtyEntered())
                        .divide(oLines[i].getQtyOrdered(), 12, BigDecimal.ROUND_HALF_UP));
                line.setC_Project_ID(oLines[i].getC_Project_ID());
                line.saveEx(trxName);
                //	Delivered everything ?
                qty = qty.subtract(lineQty);
                //	storage[ll].changeQtyOnHand(lineQty, !order.isSOTrx());	// Credit Memo not considered
                //	storage[ll].saveEx(get_TrxName());
                if (qty.signum() == 0)
                    break;
            }
        }	//	for all order lines

        //	No Lines saved
        if (retValue.get_ID() == 0)
            return null;

        return retValue;

    }

    /**
     * 	Create new Shipment by copying
     * 	@param from shipment
     * 	@param dateDoc date of the document date
     * 	@param C_DocType_ID doc type
     * 	@param isSOTrx sales order
     * 	@param counter create counter links
     * 	@param trxName trx
     * 	@param setOrder set the order link
     *	@return Shipment
     */
    public static MInOut copyFrom (org.compiere.order.MInOut from, Timestamp dateDoc, Timestamp dateAcct,
                                                      int C_DocType_ID, boolean isSOTrx, boolean counter, String trxName, boolean setOrder)
    {
        MInOut to = new MInOut(from.getCtx(), 0, null);
        to.set_TrxName(trxName);
        PO.copyValues(from, to, from.getAD_Client_ID(), from.getAD_Org_ID());
        to.set_ValueNoCheck ("M_InOut_ID", org.idempiere.orm.PO.I_ZERO);
        to.set_ValueNoCheck ("DocumentNo", null);
        //
        to.setDocStatus (X_M_InOut.DOCSTATUS_Drafted);		//	Draft
        to.setDocAction(X_M_InOut.DOCACTION_Complete);
        //
        to.setC_DocType_ID (C_DocType_ID);
        to.setIsSOTrx(isSOTrx);
        if (counter)
        {
            MDocType docType = MDocType.get(from.getCtx(), C_DocType_ID);
            if (MDocType.DOCBASETYPE_MaterialDelivery.equals(docType.getDocBaseType()))
            {
                to.setMovementType (isSOTrx ? X_M_InOut.MOVEMENTTYPE_CustomerShipment : X_M_InOut.MOVEMENTTYPE_VendorReturns);
            }
            else if (MDocType.DOCBASETYPE_MaterialReceipt.equals(docType.getDocBaseType()))
            {
                to.setMovementType (isSOTrx ? X_M_InOut.MOVEMENTTYPE_CustomerReturns : X_M_InOut.MOVEMENTTYPE_VendorReceipts);
            }
        }

        //
        to.setDateOrdered (dateDoc);
        to.setDateAcct (dateAcct);
        to.setMovementDate(dateDoc);
        to.setDatePrinted(null);
        to.setIsPrinted (false);
        to.setDateReceived(null);
        to.setNoPackages(0);
        to.setShipDate(null);
        to.setPickDate(null);
        to.setIsInTransit(false);
        //
        to.setIsApproved (false);
        to.setC_Invoice_ID(0);
        to.setTrackingNo(null);
        to.setIsInDispute(false);
        //
        to.setPosted (false);
        to.setProcessed (false);
        //[ 1633721 ] Reverse Documents- Processing=Y
        to.setProcessing(false);
        to.setC_Order_ID(0);	//	Overwritten by setOrder
        to.setM_RMA_ID(0);      //  Overwritten by setOrder
        if (counter)
        {
            to.setC_Order_ID(0);
            to.setRef_InOut_ID(from.getM_InOut_ID());
            //	Try to find Order/Invoice link
            if (from.getC_Order_ID() != 0)
            {
                MOrder peer = new MOrder (from.getCtx(), from.getC_Order_ID(), from.get_TrxName());
                if (peer.getRef_Order_ID() != 0)
                    to.setC_Order_ID(peer.getRef_Order_ID());
            }
            if (from.getC_Invoice_ID() != 0)
            {
                MInvoice peer = new MInvoice (from.getCtx(), from.getC_Invoice_ID(), from.get_TrxName());
                if (peer.getRef_Invoice_ID() != 0)
                    to.setC_Invoice_ID(peer.getRef_Invoice_ID());
            }
            //find RMA link
            if (from.getM_RMA_ID() != 0)
            {
                MRMA peer = new MRMA (from.getCtx(), from.getM_RMA_ID(), from.get_TrxName());
                if (peer.getRef_RMA_ID() > 0)
                    to.setM_RMA_ID(peer.getRef_RMA_ID());
            }
        }
        else
        {
            to.setRef_InOut_ID(0);
            if (setOrder)
            {
                to.setC_Order_ID(from.getC_Order_ID());
                to.setM_RMA_ID(from.getM_RMA_ID()); // Copy also RMA
            }
        }
        //
        if (!to.save(trxName))
            throw new IllegalStateException("Could not create Shipment");
        if (counter)
            from.setRef_InOut_ID(to.getM_InOut_ID());

        if (to.copyLinesFrom(from, counter, setOrder) <= 0)
            throw new IllegalStateException("Could not create Shipment Lines");

        return to;
    }	//	copyFrom


    /**
     *  @deprecated
     * 	Create new Shipment by copying
     * 	@param from shipment
     * 	@param dateDoc date of the document date
     * 	@param C_DocType_ID doc type
     * 	@param isSOTrx sales order
     * 	@param counter create counter links
     * 	@param trxName trx
     * 	@param setOrder set the order link
     *	@return Shipment
     */
    public static org.compiere.order.MInOut copyFrom (org.compiere.order.MInOut from, Timestamp dateDoc,
                                                      int C_DocType_ID, boolean isSOTrx, boolean counter, String trxName, boolean setOrder)
    {
        org.compiere.order.MInOut to = copyFrom ( from, dateDoc, dateDoc,
            C_DocType_ID, isSOTrx, counter,
            trxName, setOrder);
        return to;

    }


    /**
     * 	Set Warehouse and check/set Organization
     *	@param M_Warehouse_ID id
     */
    public void setM_Warehouse_ID (int M_Warehouse_ID)
    {
        if (M_Warehouse_ID == 0)
        {
            log.severe("Ignored - Cannot set AD_Warehouse_ID to 0");
            return;
        }
        super.setM_Warehouse_ID (M_Warehouse_ID);
        //
        MWarehouse wh = MWarehouse.get(getCtx(), getM_Warehouse_ID());
        if (wh.getAD_Org_ID() != getAD_Org_ID())
        {
            log.warning("M_Warehouse_ID=" + M_Warehouse_ID
                + ", Overwritten AD_Org_ID=" + getAD_Org_ID() + "->" + wh.getAD_Org_ID());
            setAD_Org_ID(wh.getAD_Org_ID());
        }
    }	//	setM_Warehouse_ID

    /**
     * 	Before Save
     *	@param newRecord new
     *	@return true or false
     */
    @Override
    protected boolean beforeSave (boolean newRecord)
    {
        MWarehouse wh = MWarehouse.get(getCtx(), getM_Warehouse_ID());
        //	Warehouse Org
        if (newRecord)
        {
            if (wh.getAD_Org_ID() != getAD_Org_ID())
            {
                log.saveError("WarehouseOrgConflict", "");
                return false;
            }
        }

        boolean disallowNegInv = wh.isDisallowNegativeInv();
        String DeliveryRule = getDeliveryRule();
        if((disallowNegInv && X_M_InOut.DELIVERYRULE_Force.equals(DeliveryRule)) ||
            (DeliveryRule == null || DeliveryRule.length()==0))
            setDeliveryRule(X_M_InOut.DELIVERYRULE_Availability);

        // Shipment/Receipt can have either Order/RMA (For Movement type)
        if (getC_Order_ID() != 0 && getM_RMA_ID() != 0)
        {
            log.saveError("OrderOrRMA", "");
            return false;
        }

        //	Shipment - Needs Order/RMA
        if (!getMovementType().contentEquals(X_M_InOut.MOVEMENTTYPE_CustomerReturns) && isSOTrx() && getC_Order_ID() == 0 && getM_RMA_ID() == 0)
        {
            log.saveError("FillMandatory", Msg.translate(getCtx(), "C_Order_ID"));
            return false;
        }

        if (isSOTrx() && getM_RMA_ID() != 0)
        {
            // Set Document and Movement type for this Receipt
            MRMA rma = new MRMA(getCtx(), getM_RMA_ID(), get_TrxName());
            MDocType docType = MDocType.get(getCtx(), rma.getC_DocType_ID());
            setC_DocType_ID(docType.getC_DocTypeShipment_ID());
        }

        return true;
    }	//	beforeSave

    /**************************************************************************
     * 	Process document
     *	@param processAction document action
     *	@return true if performed
     */
    public boolean processIt (String processAction)
    {
        m_processMsg = null;
        DocumentEngine engine = new DocumentEngine (this, getDocStatus());
        return engine.processIt (processAction, getDocAction());
    }	//	process

    /**
     *	Prepare Document
     * 	@return new status (In Progress or Invalid)
     */
    public String prepareIt()
    {
        if (log.isLoggable(Level.INFO)) log.info(toString());
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
        if (m_processMsg != null)
            return DocAction.STATUS_Invalid;

        MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());

        //  Order OR RMA can be processed on a shipment/receipt
        if (getC_Order_ID() != 0 && getM_RMA_ID() != 0)
        {
            m_processMsg = "@OrderOrRMA@";
            return DocAction.STATUS_Invalid;
        }
        //	Std Period open?
        if (!MPeriod.isOpen(getCtx(), getDateAcct(), dt.getDocBaseType(), getAD_Org_ID()))
        {
            m_processMsg = "@PeriodClosed@";
            return DocAction.STATUS_Invalid;
        }

        //	Credit Check
        if (isSOTrx() && !isReversal())
        {
            I_C_Order order = getC_Order();
            if (order != null && MDocType.DOCSUBTYPESO_PrepayOrder.equals(order.getC_DocType().getDocSubTypeSO())
                && !MSysConfig.getBooleanValue(MSysConfig.CHECK_CREDIT_ON_PREPAY_ORDER, true, getAD_Client_ID(), getAD_Org_ID())) {
                // ignore -- don't validate Prepay Orders depending on sysconfig parameter
            } else {
                org.compiere.crm.MBPartner bp = new org.compiere.crm.MBPartner(getCtx(), getC_BPartner_ID(), get_TrxName());
                if (org.compiere.crm.MBPartner.SOCREDITSTATUS_CreditStop.equals(bp.getSOCreditStatus()))
                {
                    m_processMsg = "@BPartnerCreditStop@ - @TotalOpenBalance@="
                        + bp.getTotalOpenBalance()
                        + ", @SO_CreditLimit@=" + bp.getSO_CreditLimit();
                    return DocAction.STATUS_Invalid;
                }
                if (org.compiere.crm.MBPartner.SOCREDITSTATUS_CreditHold.equals(bp.getSOCreditStatus()))
                {
                    m_processMsg = "@BPartnerCreditHold@ - @TotalOpenBalance@="
                        + bp.getTotalOpenBalance()
                        + ", @SO_CreditLimit@=" + bp.getSO_CreditLimit();
                    return DocAction.STATUS_Invalid;
                }
                BigDecimal notInvoicedAmt = org.compiere.crm.MBPartner.getNotInvoicedAmt(getC_BPartner_ID());
                if (MBPartner.SOCREDITSTATUS_CreditHold.equals(bp.getSOCreditStatus(notInvoicedAmt)))
                {
                    m_processMsg = "@BPartnerOverSCreditHold@ - @TotalOpenBalance@="
                        + bp.getTotalOpenBalance() + ", @NotInvoicedAmt@=" + notInvoicedAmt
                        + ", @SO_CreditLimit@=" + bp.getSO_CreditLimit();
                    return DocAction.STATUS_Invalid;
                }
            }
        }

        //	Lines
        MInOutLine[] lines = getLines(true);
        if (lines == null || lines.length == 0)
        {
            m_processMsg = "@NoLines@";
            return DocAction.STATUS_Invalid;
        }
        BigDecimal Volume = Env.ZERO;
        BigDecimal Weight = Env.ZERO;

        //	Mandatory Attributes
        for (int i = 0; i < lines.length; i++)
        {
            MInOutLine line = lines[i];
            MProduct product = line.getProduct();
            if (product != null)
            {
                Volume = Volume.add(product.getVolume().multiply(line.getMovementQty()));
                Weight = Weight.add(product.getWeight().multiply(line.getMovementQty()));
            }
            //
            if (line.getM_AttributeSetInstance_ID() != 0)
                continue;
            if (product != null && product.isASIMandatory(isSOTrx()))
            {
                if(product.getAttributeSet()==null){
                    m_processMsg = "@NoAttributeSet@=" + product.getValue();
                    return DocAction.STATUS_Invalid;

                }
                if (! product.getAttributeSet().excludeTableEntry(MInOutLine.Table_ID, isSOTrx())) {
                    m_processMsg = "@M_AttributeSet_ID@ @IsMandatory@ (@Line@ #" + lines[i].getLine() +
                        ", @M_Product_ID@=" + product.getValue() + ")";
                    return DocAction.STATUS_Invalid;
                }
            }
        }
        setVolume(Volume);
        setWeight(Weight);

        if (!isReversal())	//	don't change reversal
        {
            createConfirmation();
        }

        m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
        if (m_processMsg != null)
            return DocAction.STATUS_Invalid;

        m_justPrepared = true;
        if (!X_M_InOut.DOCACTION_Complete.equals(getDocAction()))
            setDocAction(X_M_InOut.DOCACTION_Complete);
        return DocAction.STATUS_InProgress;
    }	//	prepareIt

    protected MInOutConfirm[]	m_confirms = null;

    /**
     * 	Get Confirmations
     * 	@param requery requery
     *	@return array of Confirmations
     */
    public MInOutConfirm[] getConfirmations(boolean requery)
    {
        if (m_confirms != null && !requery)
        {
            org.idempiere.orm.PO.set_TrxName(m_confirms, get_TrxName());
            return m_confirms;
        }
        List<MInOutConfirm> list = new Query(getCtx(), I_M_InOutConfirm.Table_Name, "M_InOut_ID=?", get_TrxName())
            .setParameters(getM_InOut_ID())
            .list();
        m_confirms = new MInOutConfirm[list.size ()];
        list.toArray (m_confirms);
        return m_confirms;
    }	//	getConfirmations

    /**
     * 	Complete Document
     * 	@return new status (Complete, In Progress, Invalid, Waiting ..)
     */
    public String completeIt()
    {
        //	Re-Check
        if (!m_justPrepared)
        {
            String status = prepareIt();
            m_justPrepared = false;
            if (!DocAction.STATUS_InProgress.equals(status))
                return status;
        }

        // Set the definite document number after completed (if needed)
        setDefiniteDocumentNo();

        m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
        if (m_processMsg != null)
            return DocAction.STATUS_Invalid;

        //	Outstanding (not processed) Incoming Confirmations ?
        MInOutConfirm[] confirmations = getConfirmations(true);
        for (int i = 0; i < confirmations.length; i++)
        {
            MInOutConfirm confirm = confirmations[i];
            if (!confirm.isProcessed())
            {
                if (MInOutConfirm.CONFIRMTYPE_CustomerConfirmation.equals(confirm.getConfirmType()))
                    continue;
                //
                m_processMsg = "Open @M_InOutConfirm_ID@: " +
                    confirm.getConfirmTypeName() + " - " + confirm.getDocumentNo();
                return DocAction.STATUS_InProgress;
            }
        }


        //	Implicit Approval
        if (!isApproved())
            approveIt();
        if (log.isLoggable(Level.INFO)) log.info(toString());
        StringBuilder info = new StringBuilder();

        StringBuilder errors = new StringBuilder();
        //	For all lines
        MInOutLine[] lines = getLines(false);
        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++)
        {
            MInOutLine sLine = lines[lineIndex];
            MProduct product = sLine.getProduct();

            try
            {
                //	Qty & Type
                String MovementType = getMovementType();
                BigDecimal Qty = sLine.getMovementQty();
                if (MovementType.charAt(1) == '-')	//	C- Customer Shipment - V- Vendor Return
                    Qty = Qty.negate();

                //	Update Order Line
                MOrderLine oLine = null;
                if (sLine.getC_OrderLine_ID() != 0)
                {
                    oLine = new MOrderLine (getCtx(), sLine.getC_OrderLine_ID(), get_TrxName());
                    if (log.isLoggable(Level.FINE)) log.fine("OrderLine - Reserved=" + oLine.getQtyReserved()
                        + ", Delivered=" + oLine.getQtyDelivered());
                }


                // Load RMA Line
                MRMALine rmaLine = null;

                if (sLine.getM_RMALine_ID() != 0)
                {
                    rmaLine = new MRMALine(getCtx(), sLine.getM_RMALine_ID(), get_TrxName());
                }

                if (log.isLoggable(Level.INFO)) log.info("Line=" + sLine.getLine() + " - Qty=" + sLine.getMovementQty());

                //	Stock Movement - Counterpart MOrder.reserveStock
                if (product != null
                    && product.isStocked() )
                {
                    //Ignore the Material Policy when is Reverse Correction
                    if(!isReversal())
                    {
                        BigDecimal movementQty = sLine.getMovementQty();
                        BigDecimal qtyOnLineMA = MInOutLineMA.getManualQty(sLine.getM_InOutLine_ID(), get_TrxName());

                        if (   (movementQty.signum() != 0 && qtyOnLineMA.signum() != 0 && movementQty.signum() != qtyOnLineMA.signum()) // must have same sign
                            || (qtyOnLineMA.abs().compareTo(movementQty.abs())>0)) { // compare absolute values
                            // More then line qty on attribute tab for line 10
                            m_processMsg = "@Over_Qty_On_Attribute_Tab@ " + sLine.getLine();
                            return X_M_InOut.DOCSTATUS_Invalid;
                        }

                        checkMaterialPolicy(sLine,movementQty.subtract(qtyOnLineMA));
                    }

                    log.fine("Material Transaction");
                    MTransaction mtrx = null;

                    //
                    BigDecimal overReceipt = BigDecimal.ZERO;
                    if (!isReversal())
                    {
                        if (oLine != null)
                        {
                            BigDecimal toDelivered = oLine.getQtyOrdered()
                                .subtract(oLine.getQtyDelivered());
                            if (toDelivered.signum() < 0) // IDEMPIERE-2889
                                toDelivered = Env.ZERO;
                            if (sLine.getMovementQty().compareTo(toDelivered) > 0)
                                overReceipt = sLine.getMovementQty().subtract(
                                    toDelivered);
                            if (overReceipt.signum() != 0)
                            {
                                sLine.setQtyOverReceipt(overReceipt);
                                sLine.saveEx();
                            }
                        }
                    }
                    else
                    {
                        overReceipt = sLine.getQtyOverReceipt();
                    }
                    BigDecimal orderedQtyToUpdate = sLine.getMovementQty().subtract(overReceipt);
                    //
                    if (sLine.getM_AttributeSetInstance_ID() == 0)
                    {
                        MInOutLineMA mas[] = MInOutLineMA.get(getCtx(),
                            sLine.getM_InOutLine_ID(), get_TrxName());
                        for (int j = 0; j < mas.length; j++)
                        {
                            MInOutLineMA ma = mas[j];
                            BigDecimal QtyMA = ma.getMovementQty();
                            if (MovementType.charAt(1) == '-')	//	C- Customer Shipment - V- Vendor Return
                                QtyMA = QtyMA.negate();

                            //	Update Storage - see also VMatch.createMatchRecord
                            if (!MStorageOnHand.add(getCtx(), getM_Warehouse_ID(),
                                sLine.getM_Locator_ID(),
                                sLine.getM_Product_ID(),
                                ma.getM_AttributeSetInstance_ID(),
                                QtyMA,ma.getDateMaterialPolicy(),
                                get_TrxName()))
                            {
                                String lastError = CLogger.retrieveErrorString("");
                                m_processMsg = "Cannot correct Inventory OnHand (MA) [" + product.getValue() + "] - " + lastError;
                                return DocAction.STATUS_Invalid;
                            }

                            //	Create Transaction
                            mtrx = new MTransaction (getCtx(), sLine.getAD_Org_ID(),
                                MovementType, sLine.getM_Locator_ID(),
                                sLine.getM_Product_ID(), ma.getM_AttributeSetInstance_ID(),
                                QtyMA, getMovementDate(), get_TrxName());
                            mtrx.setM_InOutLine_ID(sLine.getM_InOutLine_ID());
                            if (!mtrx.save())
                            {
                                m_processMsg = "Could not create Material Transaction (MA) [" + product.getValue() + "]";
                                return DocAction.STATUS_Invalid;
                            }
                        }

                        if (oLine!=null && mtrx!=null && oLine.getQtyOrdered().signum() > 0)
                        {
                            if (sLine.getC_OrderLine_ID() != 0)
                            {
                                if (!MStorageReservation.add(getCtx(), oLine.getM_Warehouse_ID(),
                                    sLine.getM_Product_ID(),
                                    oLine.getM_AttributeSetInstance_ID(),
                                    orderedQtyToUpdate.negate(),
                                    isSOTrx(),
                                    get_TrxName()))
                                {
                                    String lastError = CLogger.retrieveErrorString("");
                                    m_processMsg = "Cannot correct Inventory " + (isSOTrx()? "Reserved" : "Ordered") + " (MA) - [" + product.getValue() + "] - " + lastError;
                                    return DocAction.STATUS_Invalid;
                                }
                            }
                        }

                    }
                    //	sLine.getM_AttributeSetInstance_ID() != 0
                    if (mtrx == null)
                    {
                        Timestamp dateMPolicy= null;
                        MStorageOnHand[] storages = MStorageOnHand.getWarehouse(getCtx(), 0,
                            sLine.getM_Product_ID(), sLine.getM_AttributeSetInstance_ID(), null,
                            MClient.MMPOLICY_FiFo.equals(product.getMMPolicy()), false,
                            sLine.getM_Locator_ID(), get_TrxName());
                        for (MStorageOnHand storage : storages) {
                            if (storage.getQtyOnHand().compareTo(sLine.getMovementQty()) >= 0) {
                                dateMPolicy = storage.getDateMaterialPolicy();
                                break;
                            }
                        }

                        if (dateMPolicy == null && storages.length > 0)
                            dateMPolicy = storages[0].getDateMaterialPolicy();

                        if(dateMPolicy==null)
                            dateMPolicy = getMovementDate();

                        //	Fallback: Update Storage - see also VMatch.createMatchRecord
                        if (!MStorageOnHand.add(getCtx(), getM_Warehouse_ID(),
                            sLine.getM_Locator_ID(),
                            sLine.getM_Product_ID(),
                            sLine.getM_AttributeSetInstance_ID(),
                            Qty,dateMPolicy,get_TrxName()))
                        {
                            String lastError = CLogger.retrieveErrorString("");
                            m_processMsg = "Cannot correct Inventory OnHand [" + product.getValue() + "] - " + lastError;
                            return DocAction.STATUS_Invalid;
                        }
                        if (oLine!=null && oLine.getQtyOrdered().signum() > 0)
                        {
                            if (!MStorageReservation.add(getCtx(), oLine.getM_Warehouse_ID(),
                                sLine.getM_Product_ID(),
                                oLine.getM_AttributeSetInstance_ID(),
                                orderedQtyToUpdate.negate(), isSOTrx(), get_TrxName()))
                            {
                                m_processMsg = "Cannot correct Inventory Reserved " + (isSOTrx()? "Reserved [" :"Ordered [") + product.getValue() + "]";
                                return DocAction.STATUS_Invalid;
                            }
                        }

                        //	FallBack: Create Transaction
                        mtrx = new MTransaction (getCtx(), sLine.getAD_Org_ID(),
                            MovementType, sLine.getM_Locator_ID(),
                            sLine.getM_Product_ID(), sLine.getM_AttributeSetInstance_ID(),
                            Qty, getMovementDate(), get_TrxName());
                        mtrx.setM_InOutLine_ID(sLine.getM_InOutLine_ID());
                        if (!mtrx.save())
                        {
                            m_processMsg = CLogger.retrieveErrorString("Could not create Material Transaction [" + product.getValue() + "]");
                            return DocAction.STATUS_Invalid;
                        }
                    }
                }	//	stock movement

                //	Correct Order Line
                if (product != null && oLine != null)		//	other in VMatch.createMatchRecord
                {
                    oLine.setQtyReserved(oLine.getQtyReserved().subtract(sLine.getMovementQty().subtract(sLine.getQtyOverReceipt())));
                }

                //	Update Sales Order Line
                if (oLine != null)
                {
                    if (isSOTrx()							//	PO is done by Matching
                        || sLine.getM_Product_ID() == 0)	//	PO Charges, empty lines
                    {
                        if (isSOTrx())
                            oLine.setQtyDelivered(oLine.getQtyDelivered().subtract(Qty));
                        else
                            oLine.setQtyDelivered(oLine.getQtyDelivered().add(Qty));
                        oLine.setDateDelivered(getMovementDate());	//	overwrite=last
                    }
                    if (!oLine.save())
                    {
                        m_processMsg = "Could not update Order Line";
                        return DocAction.STATUS_Invalid;
                    }
                    else
                    if (log.isLoggable(Level.FINE)) log.fine("OrderLine -> Reserved=" + oLine.getQtyReserved()
                        + ", Delivered=" + oLine.getQtyReserved());
                }
                //  Update RMA Line Qty Delivered
                else if (rmaLine != null)
                {
                    if (isSOTrx())
                    {
                        rmaLine.setQtyDelivered(rmaLine.getQtyDelivered().add(Qty));
                    }
                    else
                    {
                        rmaLine.setQtyDelivered(rmaLine.getQtyDelivered().subtract(Qty));
                    }
                    if (!rmaLine.save())
                    {
                        m_processMsg = "Could not update RMA Line";
                        return DocAction.STATUS_Invalid;
                    }
                }

                //	Create Asset for SO
                if (product != null
                    && isSOTrx()
                    && product.isCreateAsset()
                    && !product.getM_Product_Category().getA_Asset_Group().isFixedAsset()
                    && sLine.getMovementQty().signum() > 0
                    && !isReversal())
                {
                    log.fine("Asset");
                    info.append("@A_Asset_ID@: ");
                    int noAssets = sLine.getMovementQty().intValue();
                    if (!product.isOneAssetPerUOM())
                        noAssets = 1;
                    for (int i = 0; i < noAssets; i++)
                    {
                        if (i > 0)
                            info.append(" - ");
                        int deliveryCount = i+1;
                        if (!product.isOneAssetPerUOM())
                            deliveryCount = 0;
                        MAsset asset = new MAsset (this, sLine, deliveryCount);
                        if (!asset.save(get_TrxName()))
                        {
                            m_processMsg = "Could not create Asset";
                            return DocAction.STATUS_Invalid;
                        }
                        info.append(asset.getValue());
                    }
                }	//	Asset


                //	Matching
                if (!isSOTrx()
                    && sLine.getM_Product_ID() != 0
                    && !isReversal())
                {
                    BigDecimal matchQty = sLine.getMovementQty();
                    //	Invoice - Receipt Match (requires Product)
                    MInvoiceLine iLine = MInvoiceLine.getOfInOutLine (sLine);
                    if (iLine != null && iLine.getM_Product_ID() != 0)
                    {
                        if (matchQty.compareTo(iLine.getQtyInvoiced())>0)
                            matchQty = iLine.getQtyInvoiced();

                        MMatchInv[] matches = MMatchInv.get(getCtx(),
                            sLine.getM_InOutLine_ID(), iLine.getC_InvoiceLine_ID(), get_TrxName());
                        if (matches == null || matches.length == 0)
                        {
                            MMatchInv inv = new MMatchInv (iLine, getMovementDate(), matchQty);
                            if (sLine.getM_AttributeSetInstance_ID() != iLine.getM_AttributeSetInstance_ID())
                            {
                                iLine.setM_AttributeSetInstance_ID(sLine.getM_AttributeSetInstance_ID());
                                iLine.saveEx();	//	update matched invoice with ASI
                                inv.setM_AttributeSetInstance_ID(sLine.getM_AttributeSetInstance_ID());
                            }
                            if (!inv.save(get_TrxName()))
                            {
                                m_processMsg = CLogger.retrieveErrorString("Could not create Inv Matching");
                                return DocAction.STATUS_Invalid;
                            }
                            addDocsPostProcess(inv);
                        }
                    }

                    //	Link to Order
                    if (sLine.getC_OrderLine_ID() != 0)
                    {
                        log.fine("PO Matching");
                        //	Ship - PO
                        MMatchPO po = MMatchPO.create (null, sLine, getMovementDate(), matchQty);
                        if (po != null) {
                            if (!po.save(get_TrxName()))
                            {
                                m_processMsg = "Could not create PO Matching";
                                return DocAction.STATUS_Invalid;
                            }
                            if (!po.isPosted())
                                addDocsPostProcess(po);
                            MMatchInv matchInvCreated = po.getMatchInvCreated();
                            if (matchInvCreated != null) {
                                addDocsPostProcess(matchInvCreated);
                            }
                        }
                        //	Update PO with ASI
                        if (   oLine != null && oLine.getM_AttributeSetInstance_ID() == 0
                            && sLine.getMovementQty().compareTo(oLine.getQtyOrdered()) == 0) //  just if full match [ 1876965 ]
                        {
                            oLine.setM_AttributeSetInstance_ID(sLine.getM_AttributeSetInstance_ID());
                            oLine.saveEx(get_TrxName());
                        }
                    }
                    else	//	No Order - Try finding links via Invoice
                    {
                        //	Invoice has an Order Link
                        if (iLine != null && iLine.getC_OrderLine_ID() != 0)
                        {
                            //	Invoice is created before  Shipment
                            log.fine("PO(Inv) Matching");
                            //	Ship - Invoice
                            MMatchPO po = MMatchPO.create (iLine, sLine,
                                getMovementDate(), matchQty);
                            if (po != null) {
                                if (!po.save(get_TrxName()))
                                {
                                    m_processMsg = "Could not create PO(Inv) Matching";
                                    return DocAction.STATUS_Invalid;
                                }
                                if (!po.isPosted())
                                    addDocsPostProcess(po);
                            }

                            //	Update PO with ASI
                            oLine = new MOrderLine (getCtx(), iLine.getC_OrderLine_ID(), get_TrxName());
                            if (   oLine != null && oLine.getM_AttributeSetInstance_ID() == 0
                                && sLine.getMovementQty().compareTo(oLine.getQtyOrdered()) == 0) //  just if full match [ 1876965 ]
                            {
                                oLine.setM_AttributeSetInstance_ID(sLine.getM_AttributeSetInstance_ID());
                                oLine.saveEx(get_TrxName());
                            }
                        }
                    }	//	No Order
                }	//	PO Matching
            }
            catch (NegativeInventoryDisallowedException e)
            {
                log.severe(e.getMessage());
                errors.append(Msg.getElement(getCtx(), "Line")).append(" ").append(sLine.getLine()).append(": ");
                errors.append(e.getMessage()).append("\n");
            }
        }	//	for all lines

        if (errors.toString().length() > 0)
        {
            m_processMsg = errors.toString();
            return DocAction.STATUS_Invalid;
        }

        //	Counter Documents
        org.compiere.order.MInOut counter = createCounterDoc();
        if (counter != null)
            info.append(" - @CounterDoc@: @M_InOut_ID@=").append(counter.getDocumentNo());

        //  Drop Shipments
        org.compiere.order.MInOut dropShipment = createDropShipment();
        if (dropShipment != null)
            info.append(" - @DropShipment@: @M_InOut_ID@=").append(dropShipment.getDocumentNo());
        //	User Validation
        String valid = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
        if (valid != null)
        {
            m_processMsg = valid;
            return DocAction.STATUS_Invalid;
        }

        m_processMsg = info.toString();
        setProcessed(true);
        setDocAction(X_M_InOut.DOCACTION_Close);
        return DocAction.STATUS_Completed;
    }	//	completeIt

    /**
     * Automatically creates a customer shipment for any
     * drop shipment material receipt
     * Based on createCounterDoc() by JJ
     * @return shipment if created else null
     */
    protected org.compiere.order.MInOut createDropShipment() {

        if ( isSOTrx() || !isDropShip() || getC_Order_ID() == 0 )
            return null;

        int linkedOrderID = new MOrder (getCtx(), getC_Order_ID(), get_TrxName()).getLink_Order_ID();
        if (linkedOrderID <= 0)
            return null;

        //	Document Type
        int C_DocTypeTarget_ID = 0;
        MDocType[] shipmentTypes = MDocType.getOfDocBaseType(getCtx(), MDocType.DOCBASETYPE_MaterialDelivery);

        for (int i = 0; i < shipmentTypes.length; i++ )
        {
            if (shipmentTypes[i].isSOTrx() && ( C_DocTypeTarget_ID == 0 || shipmentTypes[i].isDefault() ) )
                C_DocTypeTarget_ID = shipmentTypes[i].getC_DocType_ID();
        }

        //	Deep Copy
        MInOut dropShipment = copyFrom(this, getMovementDate(), getDateAcct(),
            C_DocTypeTarget_ID, !isSOTrx(), false, get_TrxName(), true);

        dropShipment.setC_Order_ID(linkedOrderID);

        // get invoice id from linked order
        int invID = new MOrder (getCtx(), linkedOrderID, get_TrxName()).getC_Invoice_ID();
        if ( invID != 0 )
            dropShipment.setC_Invoice_ID(invID);

        dropShipment.setC_BPartner_ID(getDropShip_BPartner_ID());
        dropShipment.setC_BPartner_Location_ID(getDropShip_Location_ID());
        dropShipment.setAD_User_ID(getDropShip_User_ID());
        dropShipment.setIsDropShip(false);
        dropShipment.setDropShip_BPartner_ID(0);
        dropShipment.setDropShip_Location_ID(0);
        dropShipment.setDropShip_User_ID(0);
        dropShipment.setMovementType(X_M_InOut.MOVEMENTTYPE_CustomerShipment);

        //	References (Should not be required
        dropShipment.setSalesRep_ID(getSalesRep_ID());
        dropShipment.saveEx(get_TrxName());

        //		Update line order references to linked sales order lines
        org.compiere.order.MInOutLine[] lines = dropShipment.getLines(true);
        for (int i = 0; i < lines.length; i++)
        {
            org.compiere.order.MInOutLine dropLine = lines[i];
            MOrderLine ol = new MOrderLine(getCtx(), dropLine.getC_OrderLine_ID(), null);
            if ( ol.getC_OrderLine_ID() != 0 ) {
                dropLine.setC_OrderLine_ID(ol.getLink_OrderLine_ID());
                dropLine.saveEx();
            }
        }

        if (log.isLoggable(Level.FINE)) log.fine(dropShipment.toString());

        dropShipment.setDocAction(DocAction.ACTION_Complete);
        // added AdempiereException by Zuhri
        if (!dropShipment.processIt(DocAction.ACTION_Complete))
            throw new AdempiereException("Failed when processing document - " + dropShipment.getProcessMsg());
        // end added
        dropShipment.saveEx();

        return dropShipment;
    }

    /**
     * 	Set the definite document number after completed
     */
    protected void setDefiniteDocumentNo() {
        MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
        if (dt.isOverwriteDateOnComplete()) {
            setMovementDate(new Timestamp (System.currentTimeMillis()));
            if (getDateAcct().before(getMovementDate())) {
                setDateAcct(getMovementDate());
                MPeriod.testPeriodOpen(getCtx(), getDateAcct(), getC_DocType_ID(), getAD_Org_ID());
            }
        }
        if (dt.isOverwriteSeqOnComplete()) {
            String value = MSequence.getDocumentNo(getC_DocType_ID(), get_TrxName(), true, this);
            if (value != null)
                setDocumentNo(value);
        }
    }

    /**
     * 	Check Material Policy
     * 	Sets line ASI
     */
    protected void checkMaterialPolicy(MInOutLine line, BigDecimal qty)
    {

        int no = MInOutLineMA.deleteInOutLineMA(line.getM_InOutLine_ID(), get_TrxName());
        if (no > 0)
            if (log.isLoggable(Level.CONFIG)) log.config("Delete old #" + no);

        if(Env.ZERO.compareTo(qty)==0)
            return;

        //	Incoming Trx
        String MovementType = getMovementType();
        boolean inTrx = MovementType.charAt(1) == '+';	//	V+ Vendor Receipt

        boolean needSave = false;

        MProduct product = line.getProduct();

        //	Need to have Location
        if (product != null
            && line.getM_Locator_ID() == 0)
        {
            //MWarehouse w = MWarehouse.get(getCtx(), getM_Warehouse_ID());
            line.setM_Warehouse_ID(getM_Warehouse_ID());
            line.setM_Locator_ID(inTrx ? Env.ZERO : line.getMovementQty());	//	default Locator
            needSave = true;
        }

        //	Attribute Set Instance
        //  Create an  Attribute Set Instance to any receipt FIFO/LIFO
        if (product != null && line.getM_AttributeSetInstance_ID() == 0)
        {
            //Validate Transaction
            if (getMovementType().compareTo(X_M_InOut.MOVEMENTTYPE_VendorReceipts) == 0 )
            {
                //auto balance negative on hand
                BigDecimal qtyToReceive = autoBalanceNegative(line, product,qty);

                //Allocate remaining qty.
                if (qtyToReceive.compareTo(Env.ZERO)>0)
                {
                    MInOutLineMA ma = MInOutLineMA.addOrCreate(line, 0, qtyToReceive, getMovementDate(),true);
                    ma.saveEx();
                }

            } else if (getMovementType().compareTo(X_M_InOut.MOVEMENTTYPE_CustomerReturns) == 0){
                BigDecimal qtyToReturn = autoBalanceNegative(line, product,qty);

                if (line.getM_RMALine_ID()!=0 && qtyToReturn.compareTo(Env.ZERO)>0){
                    //Linking to shipment line
                    MRMALine rmaLine = new MRMALine(getCtx(), line.getM_RMALine_ID(), get_TrxName());
                    if(rmaLine.getM_InOutLine_ID()>0){
                        //retrieving ASI which is not already returned
                        MInOutLineMA shipmentMAS[] = MInOutLineMA.getNonReturned(getCtx(), rmaLine.getM_InOutLine_ID(), get_TrxName());

                        for(MInOutLineMA sMA : shipmentMAS){
                            BigDecimal lineMAQty = sMA.getMovementQty();
                            if(lineMAQty.compareTo(qtyToReturn)>0){
                                lineMAQty = qtyToReturn;
                            }

                            MInOutLineMA ma = MInOutLineMA.addOrCreate(line, sMA.getM_AttributeSetInstance_ID(), lineMAQty, sMA.getDateMaterialPolicy(),true);
                            ma.saveEx();

                            qtyToReturn = qtyToReturn.subtract(lineMAQty);
                            if(qtyToReturn.compareTo(Env.ZERO)==0)
                                break;
                        }
                    }
                }
                if(qtyToReturn.compareTo(Env.ZERO)>0){
                    //Use movement data for  Material policy if no linkage found to Shipment.
                    MInOutLineMA ma = MInOutLineMA.addOrCreate(line, 0, qtyToReturn, getMovementDate(),true);
                    ma.saveEx();
                }
            }
            // Create consume the Attribute Set Instance using policy FIFO/LIFO
            else if(getMovementType().compareTo(X_M_InOut.MOVEMENTTYPE_VendorReturns) == 0 || getMovementType().compareTo(X_M_InOut.MOVEMENTTYPE_CustomerShipment) == 0)
            {
                String MMPolicy = product.getMMPolicy();
                Timestamp minGuaranteeDate = getMovementDate();
                MStorageOnHand[] storages = MStorageOnHand.getWarehouse(getCtx(), getM_Warehouse_ID(), line.getM_Product_ID(), line.getM_AttributeSetInstance_ID(),
                    minGuaranteeDate, MClient.MMPOLICY_FiFo.equals(MMPolicy), true, line.getM_Locator_ID(), get_TrxName(), false);
                BigDecimal qtyToDeliver = qty;
                for (MStorageOnHand storage: storages)
                {
                    if (storage.getQtyOnHand().compareTo(qtyToDeliver) >= 0)
                    {
                        MInOutLineMA ma = new MInOutLineMA (line,
                            storage.getM_AttributeSetInstance_ID(),
                            qtyToDeliver,storage.getDateMaterialPolicy(),true);
                        ma.saveEx();
                        qtyToDeliver = Env.ZERO;
                    }
                    else
                    {
                        MInOutLineMA ma = new MInOutLineMA (line,
                            storage.getM_AttributeSetInstance_ID(),
                            storage.getQtyOnHand(),storage.getDateMaterialPolicy(),true);
                        ma.saveEx();
                        qtyToDeliver = qtyToDeliver.subtract(storage.getQtyOnHand());
                        if (log.isLoggable(Level.FINE)) log.fine( ma + ", QtyToDeliver=" + qtyToDeliver);
                    }

                    if (qtyToDeliver.signum() == 0)
                        break;
                }

                if (qtyToDeliver.signum() != 0)
                {
                    //Over Delivery
                    MInOutLineMA ma = MInOutLineMA.addOrCreate(line, line.getM_AttributeSetInstance_ID(), qtyToDeliver, getMovementDate(),true);
                    ma.saveEx();
                    if (log.isLoggable(Level.FINE)) log.fine("##: " + ma);
                }
            }	//	outgoing Trx
        }	//	attributeSetInstance

        if (needSave)
        {
            line.saveEx();
        }
    }	//	checkMaterialPolicy

    protected BigDecimal autoBalanceNegative(org.compiere.order.MInOutLine line, MProduct product, BigDecimal qtyToReceive) {
        MStorageOnHand[] storages = MStorageOnHand.getWarehouseNegative(getCtx(), getM_Warehouse_ID(), line.getM_Product_ID(), 0,
            null, MClient.MMPOLICY_FiFo.equals(product.getMMPolicy()), line.getM_Locator_ID(), get_TrxName(), false);

        Timestamp dateMPolicy = null;

        for (MStorageOnHand storage : storages)
        {
            if (storage.getQtyOnHand().signum() < 0 && qtyToReceive.compareTo(Env.ZERO)>0)
            {
                dateMPolicy = storage.getDateMaterialPolicy();
                BigDecimal lineMAQty = qtyToReceive;
                if(lineMAQty.compareTo(storage.getQtyOnHand().negate())>0)
                    lineMAQty = storage.getQtyOnHand().negate();

                //Using ASI from storage record
                MInOutLineMA ma = new MInOutLineMA (line, storage.getM_AttributeSetInstance_ID(), lineMAQty,dateMPolicy,true);
                ma.saveEx();
                qtyToReceive = qtyToReceive.subtract(lineMAQty);
            }
        }
        return qtyToReceive;
    }


    /**************************************************************************
     * 	Create Counter Document
     * 	@return InOut
     */
    protected org.compiere.order.MInOut createCounterDoc()
    {
        //	Is this a counter doc ?
        if (getRef_InOut_ID() != 0)
            return null;

        //	Org Must be linked to BPartner
        org.compiere.orm.MOrg org = MOrg.get(getCtx(), getAD_Org_ID());
        int counterC_BPartner_ID = org.getLinkedC_BPartner_ID(get_TrxName());
        if (counterC_BPartner_ID == 0)
            return null;
        //	Business Partner needs to be linked to Org
        MBPartner bp = new MBPartner (getCtx(), getC_BPartner_ID(), get_TrxName());
        int counterAD_Org_ID = bp.getAD_OrgBP_ID_Int();
        if (counterAD_Org_ID == 0)
            return null;

        MBPartner counterBP = new MBPartner (getCtx(), counterC_BPartner_ID, null);
        MOrgInfo counterOrgInfo = MOrgInfo.get(getCtx(), counterAD_Org_ID, get_TrxName());
        if (log.isLoggable(Level.INFO)) log.info("Counter BP=" + counterBP.getName());

        //	Document Type
        int C_DocTypeTarget_ID = 0;
        MDocTypeCounter counterDT = MDocTypeCounter.getCounterDocType(getCtx(), getC_DocType_ID());
        if (counterDT != null)
        {
            if (log.isLoggable(Level.FINE)) log.fine(counterDT.toString());
            if (!counterDT.isCreateCounter() || !counterDT.isValid())
                return null;
            C_DocTypeTarget_ID = counterDT.getCounter_C_DocType_ID();
        }
        else	//	indirect
        {
            C_DocTypeTarget_ID = MDocTypeCounter.getCounterDocType_ID(getCtx(), getC_DocType_ID());
            if (log.isLoggable(Level.FINE)) log.fine("Indirect C_DocTypeTarget_ID=" + C_DocTypeTarget_ID);
            if (C_DocTypeTarget_ID <= 0)
                return null;
        }

        //	Deep Copy
        MInOut counter = copyFrom(this, getMovementDate(), getDateAcct(),
            C_DocTypeTarget_ID, !isSOTrx(), true, get_TrxName(), true);

        //
        counter.setAD_Org_ID(counterAD_Org_ID);
        counter.setM_Warehouse_ID(counterOrgInfo.getM_Warehouse_ID());
        //
        counter.setBPartner(counterBP);

        if ( isDropShip() )
        {
            counter.setIsDropShip(true );
            counter.setDropShip_BPartner_ID(getDropShip_BPartner_ID());
            counter.setDropShip_Location_ID(getDropShip_Location_ID());
            counter.setDropShip_User_ID(getDropShip_User_ID());
        }

        //	Refernces (Should not be required
        counter.setSalesRep_ID(getSalesRep_ID());
        counter.saveEx(get_TrxName());

        String MovementType = counter.getMovementType();
        boolean inTrx = MovementType.charAt(1) == '+';	//	V+ Vendor Receipt

        //	Update copied lines
        MInOutLine[] counterLines = counter.getLines(true);
        for (int i = 0; i < counterLines.length; i++)
        {
            MInOutLine counterLine = counterLines[i];
            counterLine.setClientOrg(counter);
            counterLine.setM_Warehouse_ID(counter.getM_Warehouse_ID());
            counterLine.setM_Locator_ID(0);
            counterLine.setM_Locator_ID(inTrx ? Env.ZERO : counterLine.getMovementQty());
            //
            counterLine.saveEx(get_TrxName());
        }

        if (log.isLoggable(Level.FINE)) log.fine(counter.toString());

        //	Document Action
        if (counterDT != null)
        {
            if (counterDT.getDocAction() != null)
            {
                counter.setDocAction(counterDT.getDocAction());
                // added AdempiereException by zuhri
                if (!counter.processIt(counterDT.getDocAction()))
                    throw new AdempiereException("Failed when processing document - " + counter.getProcessMsg());
                // end added
                counter.saveEx(get_TrxName());
            }
        }
        return counter;
    }	//	createCounterDoc

    /**
     * 	Void Document.
     * 	@return true if success
     */
    public boolean voidIt()
    {
        if (log.isLoggable(Level.INFO)) log.info(toString());

        if (X_M_InOut.DOCSTATUS_Closed.equals(getDocStatus())
            || X_M_InOut.DOCSTATUS_Reversed.equals(getDocStatus())
            || X_M_InOut.DOCSTATUS_Voided.equals(getDocStatus()))
        {
            m_processMsg = "Document Closed: " + getDocStatus();
            return false;
        }

        //	Not Processed
        if (X_M_InOut.DOCSTATUS_Drafted.equals(getDocStatus())
            || X_M_InOut.DOCSTATUS_Invalid.equals(getDocStatus())
            || X_M_InOut.DOCSTATUS_InProgress.equals(getDocStatus())
            || X_M_InOut.DOCSTATUS_Approved.equals(getDocStatus())
            || X_M_InOut.DOCSTATUS_NotApproved.equals(getDocStatus()) )
        {
            // Before Void
            m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_VOID);
            if (m_processMsg != null)
                return false;

            //	Set lines to 0
            org.compiere.order.MInOutLine[] lines = getLines(false);
            for (int i = 0; i < lines.length; i++)
            {
                org.compiere.order.MInOutLine line = lines[i];
                BigDecimal old = line.getMovementQty();
                if (old.signum() != 0)
                {
                    line.setQty(Env.ZERO);
                    StringBuilder msgadd = new StringBuilder("Void (").append(old).append(")");
                    line.addDescription(msgadd.toString());
                    line.saveEx(get_TrxName());
                }
            }
            //
            // Void Confirmations
            setDocStatus(X_M_InOut.DOCSTATUS_Voided); // need to set & save docstatus to be able to check it in MInOutConfirm.voidIt()
            saveEx();
            voidConfirmations();
        }
        else
        {
            boolean accrual = false;
            try
            {
                MPeriod.testPeriodOpen(getCtx(), getDateAcct(), getC_DocType_ID(), getAD_Org_ID());
            }
            catch (PeriodClosedException e)
            {
                accrual = true;
            }

            if (accrual)
                return reverseAccrualIt();
            else
                return reverseCorrectIt();
        }

        // After Void
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_VOID);
        if (m_processMsg != null)
            return false;

        setProcessed(true);
        setDocAction(X_M_InOut.DOCACTION_None);
        return true;
    }	//	voidIt

    /**
     * 	Close Document.
     * 	@return true if success
     */
    public boolean closeIt()
    {
        if (log.isLoggable(Level.INFO)) log.info(toString());
        // Before Close
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_CLOSE);
        if (m_processMsg != null)
            return false;

        setProcessed(true);
        setDocAction(X_M_InOut.DOCACTION_None);

        // After Close
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_CLOSE);
        if (m_processMsg != null)
            return false;
        return true;
    }	//	closeIt

    /**
     * 	Reverse Correction - same date
     * 	@return true if success
     */
    public boolean reverseCorrectIt()
    {
        if (log.isLoggable(Level.INFO)) log.info(toString());
        // Before reverseCorrect
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSECORRECT);
        if (m_processMsg != null)
            return false;

        org.compiere.order.MInOut reversal = reverse(false);
        if (reversal == null)
            return false;

        // After reverseCorrect
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSECORRECT);
        if (m_processMsg != null)
            return false;

        m_processMsg = reversal.getDocumentNo();
        setProcessed(true);
        setDocStatus(X_M_InOut.DOCSTATUS_Reversed);		//	 may come from void
        setDocAction(X_M_InOut.DOCACTION_None);
        return true;
    }	//	reverseCorrectionIt

    protected org.compiere.order.MInOut reverse(boolean accrual) {
        MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
        Timestamp reversalDate = accrual ? Env.getContextAsDate(getCtx(), "#Date") : getDateAcct();
        if (reversalDate == null) {
            reversalDate = new Timestamp(System.currentTimeMillis());
        }
        Timestamp reversalMovementDate = accrual ? reversalDate : getMovementDate();
        if (!MPeriod.isOpen(getCtx(), reversalDate, dt.getDocBaseType(), getAD_Org_ID()))
        {
            m_processMsg = "@PeriodClosed@";
            return null;
        }

        //	Reverse/Delete Matching
        if (!isSOTrx())
        {
            if (!reverseMatching(reversalDate))
                return null;
        }

        //	Deep Copy
        MInOut reversal = copyFrom (this, reversalMovementDate, reversalDate,
            getC_DocType_ID(), isSOTrx(), false, get_TrxName(), true);
        if (reversal == null)
        {
            m_processMsg = "Could not create Ship Reversal";
            return null;
        }
        reversal.setReversal(true);

        //	Reverse Line Qty
        org.compiere.order.MInOutLine[] sLines = getLines(false);
        org.compiere.order.MInOutLine[] rLines = reversal.getLines(false);
        for (int i = 0; i < rLines.length; i++)
        {
            org.compiere.order.MInOutLine rLine = rLines[i];
            rLine.setQtyEntered(rLine.getQtyEntered().negate());
            rLine.setMovementQty(rLine.getMovementQty().negate());
            rLine.setQtyOverReceipt(rLine.getQtyOverReceipt().negate());
            rLine.setM_AttributeSetInstance_ID(sLines[i].getM_AttributeSetInstance_ID());
            // Goodwill: store original (voided/reversed) document line
            rLine.setReversalLine_ID(sLines[i].getM_InOutLine_ID());
            if (!rLine.save(get_TrxName()))
            {
                m_processMsg = "Could not correct Ship Reversal Line";
                return null;
            }
            //	We need to copy MA
            if (rLine.getM_AttributeSetInstance_ID() == 0)
            {
                MInOutLineMA mas[] = MInOutLineMA.get(getCtx(),
                    sLines[i].getM_InOutLine_ID(), get_TrxName());
                for (int j = 0; j < mas.length; j++)
                {
                    MInOutLineMA ma = new MInOutLineMA (rLine,
                        mas[j].getM_AttributeSetInstance_ID(),
                        mas[j].getMovementQty().negate(),mas[j].getDateMaterialPolicy(),true);
                    ma.saveEx();
                }
            }
            //	De-Activate Asset
            MAsset asset = MAsset.getFromShipment(getCtx(), sLines[i].getM_InOutLine_ID(), get_TrxName());
            if (asset != null)
            {
                asset.setIsActive(false);
                asset.setDescription(asset.getDescription() + " (" + reversal.getDocumentNo() + " #" + rLine.getLine() + "<-)");
                asset.saveEx();
            }
        }
        reversal.setC_Order_ID(getC_Order_ID());
        // Set M_RMA_ID
        reversal.setM_RMA_ID(getM_RMA_ID());
        StringBuilder msgadd = new StringBuilder("{->").append(getDocumentNo()).append(")");
        reversal.addDescription(msgadd.toString());
        //FR1948157
        reversal.setReversal_ID(getM_InOut_ID());
        reversal.saveEx(get_TrxName());
        //
        reversal.docsPostProcess = this.docsPostProcess;
        this.docsPostProcess = new ArrayList<PO>();
        //
        if (!reversal.processIt(DocAction.ACTION_Complete)
            || !reversal.getDocStatus().equals(DocAction.STATUS_Completed))
        {
            m_processMsg = "Reversal ERROR: " + reversal.getProcessMsg();
            return null;
        }
        reversal.closeIt();
        reversal.setProcessing (false);
        reversal.setDocStatus(X_M_InOut.DOCSTATUS_Reversed);
        reversal.setDocAction(X_M_InOut.DOCACTION_None);
        reversal.saveEx(get_TrxName());
        //
        msgadd = new StringBuilder("(").append(reversal.getDocumentNo()).append("<-)");
        addDescription(msgadd.toString());

        //
        // Void Confirmations
        setDocStatus(X_M_InOut.DOCSTATUS_Reversed); // need to set & save docstatus to be able to check it in MInOutConfirm.voidIt()
        saveEx();
        //FR1948157
        this.setReversal_ID(reversal.getM_InOut_ID());
        voidConfirmations();
        return reversal;
    }

    protected boolean reverseMatching(Timestamp reversalDate) {
        MMatchInv[] mInv = MMatchInv.getInOut(getCtx(), getM_InOut_ID(), get_TrxName());
        for (MMatchInv mMatchInv : mInv)
        {
            if (mMatchInv.getReversal_ID() > 0)
                continue;

            String description = mMatchInv.getDescription();
            if (description == null || !description.endsWith("<-)"))
            {
                if (!mMatchInv.reverse(reversalDate))
                {
                    log.log(Level.SEVERE, "Failed to create reversal for match invoice " + mMatchInv.getDocumentNo());
                    return false;
                }
                addDocsPostProcess(new MMatchInv(Env.getCtx(), mMatchInv.getReversal_ID(), get_TrxName()));
            }
        }
        MMatchPO[] mMatchPOList = MMatchPO.getInOut(getCtx(), getM_InOut_ID(), get_TrxName());
        for (MMatchPO mMatchPO : mMatchPOList)
        {
            if (mMatchPO.getReversal_ID() > 0)
                continue;

            String description = mMatchPO.getDescription();
            if (description == null || !description.endsWith("<-)"))
            {
                if (!mMatchPO.reverse(reversalDate))
                {
                    log.log(Level.SEVERE, "Failed to create reversal for match purchase order " + mMatchPO.getDocumentNo());
                    return false;
                }
                addDocsPostProcess(new MMatchPO(Env.getCtx(), mMatchPO.getReversal_ID(), get_TrxName()));
            }
        }
        return true;
    }

    /**
     * 	Reverse Accrual - none
     * 	@return false
     */
    public boolean reverseAccrualIt()
    {
        if (log.isLoggable(Level.INFO)) log.info(toString());
        // Before reverseAccrual
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSEACCRUAL);
        if (m_processMsg != null)
            return false;

        org.compiere.order.MInOut reversal = reverse(true);
        if (reversal == null)
            return false;

        // After reverseAccrual
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSEACCRUAL);
        if (m_processMsg != null)
            return false;

        m_processMsg = reversal.getDocumentNo();
        setProcessed(true);
        setDocStatus(X_M_InOut.DOCSTATUS_Reversed);		//	 may come from void
        setDocAction(X_M_InOut.DOCACTION_None);
        return true;
    }	//	reverseAccrualIt

    /**
     * 	Re-activate
     * 	@return false
     */
    public boolean reActivateIt()
    {
        if (log.isLoggable(Level.INFO)) log.info(toString());
        // Before reActivate
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REACTIVATE);
        if (m_processMsg != null)
            return false;

        // After reActivate
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REACTIVATE);
        if (m_processMsg != null)
            return false;

        return false;
    }	//	reActivateIt

    /**
     * 	Create the missing next Confirmation
     */
    public void createConfirmation()
    {
        MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
        boolean pick = dt.isPickQAConfirm();
        boolean ship = dt.isShipConfirm();
        //	Nothing to do
        if (!pick && !ship)
        {
            log.fine("No need");
            return;
        }

        //	Create Both .. after each other
        if (pick && ship)
        {
            boolean havePick = false;
            boolean haveShip = false;
            org.compiere.order.MInOutConfirm[] confirmations = getConfirmations(false);
            for (int i = 0; i < confirmations.length; i++)
            {
                org.compiere.order.MInOutConfirm confirm = confirmations[i];
                if (org.compiere.order.MInOutConfirm.CONFIRMTYPE_PickQAConfirm.equals(confirm.getConfirmType()))
                {
                    if (!confirm.isProcessed())		//	wait intil done
                    {
                        if (log.isLoggable(Level.FINE)) log.fine("Unprocessed: " + confirm);
                        return;
                    }
                    havePick = true;
                }
                else if (org.compiere.order.MInOutConfirm.CONFIRMTYPE_ShipReceiptConfirm.equals(confirm.getConfirmType()))
                    haveShip = true;
            }
            //	Create Pick
            if (!havePick)
            {
                MInOutConfirm.create (this, org.compiere.order.MInOutConfirm.CONFIRMTYPE_PickQAConfirm, false);
                return;
            }
            //	Create Ship
            if (!haveShip)
            {
                MInOutConfirm.create (this, org.compiere.order.MInOutConfirm.CONFIRMTYPE_ShipReceiptConfirm, false);
                return;
            }
            return;
        }
        //	Create just one
        if (pick)
            MInOutConfirm.create (this, org.compiere.order.MInOutConfirm.CONFIRMTYPE_PickQAConfirm, true);
        else if (ship)
            MInOutConfirm.create (this, org.compiere.order.MInOutConfirm.CONFIRMTYPE_ShipReceiptConfirm, true);
    }	//	createConfirmation

    protected void voidConfirmations()
    {
        for(MInOutConfirm confirm : getConfirmations(true))
        {
            if (!confirm.isProcessed())
            {
                if (!confirm.processIt(org.compiere.order.MInOutConfirm.DOCACTION_Void))
                    throw new AdempiereException(confirm.getProcessMsg());
                confirm.saveEx();
            }
        }
    }

    /**	Lines					*/
    protected MInOutLine[]	m_lines = null;

    /**
     * 	Get Lines of Shipment
     * 	@param requery refresh from db
     * 	@return lines
     */
    public MInOutLine[] getLines (boolean requery)
    {
        if (m_lines != null && !requery) {
            org.idempiere.orm.PO.set_TrxName(m_lines, get_TrxName());
            return m_lines;
        }
        List<MInOutLine> list = new Query(getCtx(), I_M_InOutLine.Table_Name, "M_InOut_ID=?", get_TrxName())
            .setParameters(getM_InOut_ID())
            .setOrderBy(MInOutLine.COLUMNNAME_Line)
            .list();
        //
        m_lines = new MInOutLine[list.size()];
        list.toArray(m_lines);
        return m_lines;
    }	//	getMInOutLines

    /**
     * 	Order Constructor - create header only
     *	@param order order
     *	@param movementDate optional movement date (default today)
     *	@param C_DocTypeShipment_ID document type or 0
     */
    public MInOut (MOrder order, int C_DocTypeShipment_ID, Timestamp movementDate) {
        super( order,C_DocTypeShipment_ID,movementDate );
    }

    /**
     * 	Get Lines of Shipment
     * 	@param requery refresh from db
     * 	@return lines
     */
    public MInOutLine[] getLines2 (boolean requery)
    {
        if (m_lines != null && !requery) {
            org.idempiere.orm.PO.set_TrxName(m_lines, get_TrxName());
            return m_lines;
        }
        List<org.compiere.order.MInOutLine> list = new Query(getCtx(), I_M_InOutLine.Table_Name, "M_InOut_ID=?", get_TrxName())
            .setParameters(getM_InOut_ID())
            .setOrderBy(org.compiere.order.MInOutLine.COLUMNNAME_Line)
            .list();
        //
        m_lines = new MInOutLine[list.size()];
        list.toArray(m_lines);
        return m_lines;
    }	//	getMInOutLines

    /**
     * 	Get Lines of Shipment
     * 	@return lines
     */
    public MInOutLine[] getLines()
    {
        return getLines(false);
    }	//	getLines

}
