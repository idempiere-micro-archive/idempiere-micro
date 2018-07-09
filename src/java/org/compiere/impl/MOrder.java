package org.compiere.impl;

import org.adempiere.exceptions.FillMandatoryException;
import org.compiere.acct.Doc;
import org.compiere.crm.MBPartner;
import org.compiere.model.*;
import org.compiere.order.*;
import org.compiere.orm.*;
import org.compiere.orm.MOrg;
import org.compiere.process.DocAction;
import org.compiere.process2.DocumentEngine;
import org.compiere.product.MPriceList;
import org.compiere.product.MPriceListVersion;
import org.compiere.util.Msg;
import org.compiere.validation.ModelValidationEngine;
import org.compiere.validation.ModelValidator;
import org.idempiere.common.exceptions.AdempiereException;
import org.idempiere.common.util.CLogger;
import org.idempiere.common.util.DB;
import org.idempiere.common.util.Env;
import org.idempiere.common.util.Util;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class MOrder extends org.compiere.order.MOrder implements DocAction, IPODoc {

    /**************************************************************************
     *  Default Constructor
     *  @param ctx context
     *  @param  C_Order_ID    order to load, (0 create new order)
     *  @param trxName trx name
     */
    public MOrder(Properties ctx, int C_Order_ID, String trxName) {
        super(ctx, C_Order_ID, trxName);
    }


    /**
     *  Load Constructor
     *  @param ctx context
     *  @param rs result set record
     *  @param trxName transaction
     */
    public MOrder (Properties ctx, ResultSet rs, String trxName)
    {
        super(ctx, rs, trxName);
    }	//	MOrder


    /**************************************************************************
     *  Project Constructor
     *  @param  project Project to create Order from
     *  @param IsSOTrx sales order
     * 	@param	DocSubTypeSO if SO DocType Target (default DocSubTypeSO_OnCredit)
     */
    public MOrder (MProject project, boolean IsSOTrx, String DocSubTypeSO)
    {
        this (project.getCtx(), 0, project.get_TrxName());
        setAD_Client_ID(project.getAD_Client_ID());
        setAD_Org_ID(project.getAD_Org_ID());
        setC_Campaign_ID(project.getC_Campaign_ID());
        setSalesRep_ID(project.getSalesRep_ID());
        //
        setC_Project_ID(project.getC_Project_ID());
        setDescription(project.getName());
        Timestamp ts = project.getDateContract();
        if (ts != null)
            setDateOrdered (ts);
        ts = project.getDateFinish();
        if (ts != null)
            setDatePromised (ts);
        //
        setC_BPartner_ID(project.getC_BPartner_ID());
        setC_BPartner_Location_ID(project.getC_BPartner_Location_ID());
        setAD_User_ID(project.getAD_User_ID());
        //
        setM_Warehouse_ID(project.getM_Warehouse_ID());
        setM_PriceList_ID(project.getM_PriceList_ID());
        setC_PaymentTerm_ID(project.getC_PaymentTerm_ID());
        //
        setIsSOTrx(IsSOTrx);
        if (IsSOTrx)
        {
            if (DocSubTypeSO == null || DocSubTypeSO.length() == 0)
                setC_DocTypeTarget_ID(DocSubTypeSO_OnCredit);
            else
                setC_DocTypeTarget_ID(DocSubTypeSO);
        }
        else
            setC_DocTypeTarget_ID();
    }	//	MOrder

    /**
     * 	Create Invoice
     *	@param dt order document type
     *	@param shipment optional shipment
     *	@param invoiceDate invoice date
     *	@return invoice or null
     */
    protected I_C_Invoice createInvoice (MDocType dt, MInOut shipment, Timestamp invoiceDate)
    {
        if (log.isLoggable(Level.INFO)) log.info(dt.toString());
        MInvoice invoice = new MInvoice (this, dt.getC_DocTypeInvoice_ID(), invoiceDate);
        if (!invoice.save(get_TrxName()))
        {
            m_processMsg = "Could not create Invoice";
            return null;
        }

        //	If we have a Shipment - use that as a base
        if (shipment != null)
        {
            if (!INVOICERULE_AfterDelivery.equals(getInvoiceRule()))
                setInvoiceRule(INVOICERULE_AfterDelivery);
            //
            MInOutLine[] sLines = shipment.getLines(false);
            for (int i = 0; i < sLines.length; i++)
            {
                MInOutLine sLine = sLines[i];
                //
                MInvoiceLine iLine = new MInvoiceLine(invoice);
                iLine.setShipLine(sLine);
                //	Qty = Delivered
                if (sLine.sameOrderLineUOM())
                    iLine.setQtyEntered(sLine.getQtyEntered());
                else
                    iLine.setQtyEntered(sLine.getMovementQty());
                iLine.setQtyInvoiced(sLine.getMovementQty());
                if (!iLine.save(get_TrxName()))
                {
                    m_processMsg = "Could not create Invoice Line from Shipment Line";
                    return null;
                }
                //
                sLine.setIsInvoiced(true);
                if (!sLine.save(get_TrxName()))
                {
                    log.warning("Could not update Shipment line: " + sLine);
                }
            }
        }
        else	//	Create Invoice from Order
        {
            if (!INVOICERULE_Immediate.equals(getInvoiceRule()))
                setInvoiceRule(INVOICERULE_Immediate);
            //
            org.compiere.order.MOrderLine[] oLines = getLines();
            for (int i = 0; i < oLines.length; i++)
            {
                org.compiere.order.MOrderLine oLine = oLines[i];
                //
                MInvoiceLine iLine = new MInvoiceLine(invoice);
                iLine.setOrderLine(oLine);
                //	Qty = Ordered - Invoiced
                iLine.setQtyInvoiced(oLine.getQtyOrdered().subtract(oLine.getQtyInvoiced()));
                if (oLine.getQtyOrdered().compareTo(oLine.getQtyEntered()) == 0)
                    iLine.setQtyEntered(iLine.getQtyInvoiced());
                else
                    iLine.setQtyEntered(iLine.getQtyInvoiced().multiply(oLine.getQtyEntered())
                        .divide(oLine.getQtyOrdered(), 12, BigDecimal.ROUND_HALF_UP));
                if (!iLine.save(get_TrxName()))
                {
                    m_processMsg = "Could not create Invoice Line from Order Line";
                    return null;
                }
            }
        }

        // Copy payment schedule from order to invoice if any
        for (MOrderPaySchedule ops : MOrderPaySchedule.getOrderPaySchedule(getCtx(), getC_Order_ID(), 0, get_TrxName())) {
            MInvoicePaySchedule ips = new MInvoicePaySchedule(getCtx(), 0, get_TrxName());
            copyValues(ops, ips);
            ips.setC_Invoice_ID(invoice.getC_Invoice_ID());
            ips.setAD_Org_ID(ops.getAD_Org_ID());
            ips.setProcessing(ops.isProcessing());
            ips.setIsActive(ops.isActive());
            if (!ips.save()) {
                m_processMsg = "ERROR: creating pay schedule for invoice from : "+ ops.toString();
                return null;
            }
        }

        // added AdempiereException by zuhri
        if (!invoice.processIt(DocAction.ACTION_Complete))
            throw new AdempiereException("Failed when processing document - " + invoice.getProcessMsg());
        // end added
        invoice.saveEx(get_TrxName());
        setC_CashLine_ID(invoice.getC_CashLine_ID());
        if (!DOCSTATUS_Completed.equals(invoice.getDocStatus()))
        {
            m_processMsg = "@C_Invoice_ID@: " + invoice.getProcessMsg();
            return null;
        }
        return invoice;
    }	//	createInvoice

    /**************************************************************************
     * 	Before Save
     *	@param newRecord new
     *	@return save
     */
    @Override
    protected boolean beforeSave (boolean newRecord)
    {
        //	Client/Org Check
        if (getAD_Org_ID() == 0)
        {
            int context_AD_Org_ID = Env.getAD_Org_ID(getCtx());
            if (context_AD_Org_ID != 0)
            {
                setAD_Org_ID(context_AD_Org_ID);
                log.warning("Changed Org to Context=" + context_AD_Org_ID);
            }
        }
        if (getAD_Client_ID() == 0)
        {
            m_processMsg = "AD_Client_ID = 0";
            return false;
        }

        //	New Record Doc Type - make sure DocType set to 0
        if (newRecord && getC_DocType_ID() == 0)
            setC_DocType_ID (0);

        //	Default Warehouse
        if (getM_Warehouse_ID() == 0)
        {
            int ii = Env.getContextAsInt(getCtx(), "#M_Warehouse_ID");
            if (ii != 0)
                setM_Warehouse_ID(ii);
            else
            {
                throw new FillMandatoryException(COLUMNNAME_M_Warehouse_ID);
            }
        }
        MWarehouse wh = MWarehouse.get(getCtx(), getM_Warehouse_ID());
        //	Warehouse Org
        if (newRecord
            || is_ValueChanged("AD_Org_ID") || is_ValueChanged("M_Warehouse_ID"))
        {
            if (wh.getAD_Org_ID() != getAD_Org_ID())
                log.saveWarning("WarehouseOrgConflict", "");
        }

        boolean disallowNegInv = wh.isDisallowNegativeInv();
        String DeliveryRule = getDeliveryRule();
        if((disallowNegInv && DELIVERYRULE_Force.equals(DeliveryRule)) ||
            (DeliveryRule == null || DeliveryRule.length()==0))
            setDeliveryRule(DELIVERYRULE_Availability);

        //	Reservations in Warehouse
        if (!newRecord && is_ValueChanged("M_Warehouse_ID"))
        {
            org.compiere.order.MOrderLine[] lines = getLines(false,null);
            for (int i = 0; i < lines.length; i++)
            {
                if (!lines[i].canChangeWarehouse())
                    return false;
            }
        }

        //	No Partner Info - set Template
        if (getC_BPartner_ID() == 0)
            setBPartner(org.compiere.crm.MBPartner.getTemplate(getCtx(), getAD_Client_ID()));
        if (getC_BPartner_Location_ID() == 0)
            setBPartner(new MBPartner(getCtx(), getC_BPartner_ID(), null));
        //	No Bill - get from Ship
        if (getBill_BPartner_ID() == 0)
        {
            setBill_BPartner_ID(getC_BPartner_ID());
            setBill_Location_ID(getC_BPartner_Location_ID());
        }
        if (getBill_Location_ID() == 0)
            setBill_Location_ID(getC_BPartner_Location_ID());

        //	Default Price List
        if (getM_PriceList_ID() == 0)
        {
            int ii = DB.getSQLValueEx(null,
                "SELECT M_PriceList_ID FROM M_PriceList "
                    + "WHERE AD_Client_ID=? AND IsSOPriceList=? AND IsActive=?"
                    + "ORDER BY IsDefault DESC", getAD_Client_ID(), isSOTrx(), true);
            if (ii != 0)
                setM_PriceList_ID (ii);
        }
        //	Default Currency
        if (getC_Currency_ID() == 0)
        {
            String sql = "SELECT C_Currency_ID FROM M_PriceList WHERE M_PriceList_ID=?";
            int ii = DB.getSQLValue (null, sql, getM_PriceList_ID());
            if (ii != 0)
                setC_Currency_ID (ii);
            else
                setC_Currency_ID(Env.getContextAsInt(getCtx(), "#C_Currency_ID"));
        }

        //	Default Sales Rep
        if (getSalesRep_ID() == 0)
        {
            int ii = Env.getContextAsInt(getCtx(), "#SalesRep_ID");
            if (ii != 0)
                setSalesRep_ID (ii);
        }

        //	Default Document Type
        if (getC_DocTypeTarget_ID() == 0)
            setC_DocTypeTarget_ID(DocSubTypeSO_Standard);

        //	Default Payment Term
        if (getC_PaymentTerm_ID() == 0)
        {
            int ii = Env.getContextAsInt(getCtx(), "#C_PaymentTerm_ID");
            if (ii != 0)
                setC_PaymentTerm_ID(ii);
            else
            {
                String sql = "SELECT C_PaymentTerm_ID FROM C_PaymentTerm WHERE AD_Client_ID=? AND IsDefault='Y' AND IsActive='Y'";
                ii = DB.getSQLValue(null, sql, getAD_Client_ID());
                if (ii != 0)
                    setC_PaymentTerm_ID (ii);
            }
        }

        // IDEMPIERE-63
        // for documents that can be reactivated we cannot allow changing
        // C_DocTypeTarget_ID or C_DocType_ID if they were already processed and isOverwriteSeqOnComplete
        // neither change the Date if isOverwriteDateOnComplete
        BigDecimal previousProcessedOn = (BigDecimal) get_ValueOld(COLUMNNAME_ProcessedOn);
        if (! newRecord && previousProcessedOn != null && previousProcessedOn.signum() > 0) {
            int previousDocTypeID = (Integer) get_ValueOld(COLUMNNAME_C_DocTypeTarget_ID);
            MDocType previousdt = MDocType.get(getCtx(), previousDocTypeID);
            if (is_ValueChanged(COLUMNNAME_C_DocType_ID) || is_ValueChanged(COLUMNNAME_C_DocTypeTarget_ID)) {
                if (previousdt.isOverwriteSeqOnComplete()) {
                    log.saveError("Error", Msg.getMsg(getCtx(), "CannotChangeProcessedDocType"));
                    return false;
                }
            }
            if (is_ValueChanged(COLUMNNAME_DateOrdered)) {
                if (previousdt.isOverwriteDateOnComplete()) {
                    log.saveError("Error", Msg.getMsg(getCtx(), "CannotChangeProcessedDate"));
                    return false;
                }
            }
        }

        // IDEMPIERE-1597 Price List and Date must be not-updateable
        if (!newRecord && (is_ValueChanged(COLUMNNAME_M_PriceList_ID) || is_ValueChanged(COLUMNNAME_DateOrdered))) {
            int cnt = DB.getSQLValueEx(get_TrxName(), "SELECT COUNT(*) FROM C_OrderLine WHERE C_Order_ID=? AND M_Product_ID>0", getC_Order_ID());
            if (cnt > 0) {
                if (is_ValueChanged(COLUMNNAME_M_PriceList_ID)) {
                    log.saveError("Error", Msg.getMsg(getCtx(), "CannotChangePl"));
                    return false;
                }
                if (is_ValueChanged(COLUMNNAME_DateOrdered)) {
                    MPriceList pList =  MPriceList.get(getCtx(), getM_PriceList_ID(), null);
                    MPriceListVersion plOld = pList.getPriceListVersion((Timestamp)get_ValueOld(COLUMNNAME_DateOrdered));
                    MPriceListVersion plNew = pList.getPriceListVersion((Timestamp)get_Value(COLUMNNAME_DateOrdered));
                    if (plNew == null || !plNew.equals(plOld)) {
                        log.saveError("Error", Msg.getMsg(getCtx(), "CannotChangeDateOrdered"));
                        return false;
                    }
                }
            }
        }

        if (! recursiveCall && (!newRecord && is_ValueChanged(COLUMNNAME_C_PaymentTerm_ID))) {
            recursiveCall = true;
            try {
                MPaymentTerm pt = new MPaymentTerm (getCtx(), getC_PaymentTerm_ID(), get_TrxName());
                boolean valid = pt.applyOrder(this);
                setIsPayScheduleValid(valid);
            } catch (Exception e) {
                throw e;
            } finally {
                recursiveCall = false;
            }
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
    }	//	processIt

    /**
     * 	Get Lines of Order.
     * 	(used by web store)
     * 	@return lines
     */
    public MOrderLine[] getLines()
    {
        return getLines(false, null);
    }	//	getLines

    /**	Order Lines					*/
    protected MOrderLine[] 	m_lines = null;

    /**
     * 	Get Lines of Order
     * 	@param requery requery
     * 	@param orderBy optional order by column
     * 	@return lines
     */
    public MOrderLine[] getLines (boolean requery, String orderBy)
    {
        if (m_lines != null && !requery) {
            set_TrxName(m_lines, get_TrxName());
            return m_lines;
        }
        //
        String orderClause = "";
        if (orderBy != null && orderBy.length() > 0)
            orderClause += orderBy;
        else
            orderClause += "Line";
        m_lines = getLines(null, orderClause);
        return m_lines;
    }	//	getLines

    /**************************************************************************
     * 	Get Lines of Order
     * 	@param whereClause where clause or null (starting with AND)
     * 	@param orderClause order clause
     * 	@return lines
     */
    public MOrderLine[] getLines (String whereClause, String orderClause)
    {
        //red1 - using new Query class from Teo / Victor's MDDOrder.java implementation
        StringBuilder whereClauseFinal = new StringBuilder(MOrderLine.COLUMNNAME_C_Order_ID+"=? ");
        if (!Util.isEmpty(whereClause, true))
            whereClauseFinal.append(whereClause);
        if (orderClause.length() == 0)
            orderClause = MOrderLine.COLUMNNAME_Line;
        //
        List<MOrderLine> list = new Query(getCtx(), I_C_OrderLine.Table_Name, whereClauseFinal.toString(), get_TrxName())
            .setParameters(get_ID())
            .setOrderBy(orderClause)
            .list();
        for (MOrderLine ol : list) {
            ol.setHeaderInfo(this);
        }
        //
        return list.toArray(new MOrderLine[list.size()]);
    }	//	getLines

    /**************************************************************************
     *	Prepare Document
     * 	@return new status (In Progress or Invalid)
     */
    public String prepareIt()
    {
        if (log.isLoggable(Level.INFO)) log.info(toString());
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
        if (m_processMsg != null)
            return DocAction.STATUS_Invalid;
        MDocType dt = MDocType.get(getCtx(), getC_DocTypeTarget_ID());

        //	Std Period open?
        if (!MPeriod.isOpen(getCtx(), getDateAcct(), dt.getDocBaseType(), getAD_Org_ID()))
        {
            m_processMsg = "@PeriodClosed@";
            return DocAction.STATUS_Invalid;
        }

        if (isSOTrx() && getDeliveryViaRule().equals(DELIVERYVIARULE_Shipper))
        {
            if (getM_Shipper_ID() == 0)
            {
                m_processMsg = "@FillMandatory@" + Msg.getElement(getCtx(), COLUMNNAME_M_Shipper_ID);
                return DocAction.STATUS_Invalid;
            }

            if (!calculateFreightCharge())
                return DocAction.STATUS_Invalid;
        }

        //	Lines
        MOrderLine[] lines = getLines(true, MOrderLine.COLUMNNAME_M_Product_ID);
        if (lines.length == 0)
        {
            m_processMsg = "@NoLines@";
            return DocAction.STATUS_Invalid;
        }

        // Bug 1564431
        if (getDeliveryRule() != null && getDeliveryRule().equals(DELIVERYRULE_CompleteOrder))
        {
            for (int i = 0; i < lines.length; i++)
            {
                org.compiere.order.MOrderLine line = lines[i];
                org.compiere.product.MProduct product = line.getProduct();
                if (product != null && product.isExcludeAutoDelivery())
                {
                    m_processMsg = "@M_Product_ID@ "+product.getValue()+" @IsExcludeAutoDelivery@";
                    return DocAction.STATUS_Invalid;
                }
            }
        }

        //	Convert DocType to Target
        if (getC_DocType_ID() != getC_DocTypeTarget_ID() )
        {
            //	Cannot change Std to anything else if different warehouses
            if (getC_DocType_ID() != 0)
            {
                MDocType dtOld = MDocType.get(getCtx(), getC_DocType_ID());
                if (MDocType.DOCSUBTYPESO_StandardOrder.equals(dtOld.getDocSubTypeSO())		//	From SO
                    && !MDocType.DOCSUBTYPESO_StandardOrder.equals(dt.getDocSubTypeSO()))	//	To !SO
                {
                    for (int i = 0; i < lines.length; i++)
                    {
                        if (lines[i].getM_Warehouse_ID() != getM_Warehouse_ID())
                        {
                            log.warning("different Warehouse " + lines[i]);
                            m_processMsg = "@CannotChangeDocType@";
                            return DocAction.STATUS_Invalid;
                        }
                    }
                }
            }

            //	New or in Progress/Invalid
            if (DOCSTATUS_Drafted.equals(getDocStatus())
                || DOCSTATUS_InProgress.equals(getDocStatus())
                || DOCSTATUS_Invalid.equals(getDocStatus())
                || getC_DocType_ID() == 0)
            {
                setC_DocType_ID(getC_DocTypeTarget_ID());
            }
            else	//	convert only if offer
            {
                if (dt.isOffer())
                    setC_DocType_ID(getC_DocTypeTarget_ID());
                else
                {
                    m_processMsg = "@CannotChangeDocType@";
                    return DocAction.STATUS_Invalid;
                }
            }
        }	//	convert DocType

        //	Mandatory Product Attribute Set Instance
        for (MOrderLine line : getLines()) {
            if (line.getM_Product_ID() > 0 && line.getM_AttributeSetInstance_ID() == 0) {
                MProduct product = line.getProduct();
                if (product.isASIMandatory(isSOTrx())) {
                    if(product.getAttributeSet()==null){
                        m_processMsg = "@NoAttributeSet@=" + product.getValue();
                        return DocAction.STATUS_Invalid;

                    }
                    if (! product.getAttributeSet().excludeTableEntry(MOrderLine.Table_ID, isSOTrx())) {
                        StringBuilder msg = new StringBuilder("@M_AttributeSet_ID@ @IsMandatory@ (@Line@ #")
                            .append(line.getLine())
                            .append(", @M_Product_ID@=")
                            .append(product.getValue())
                            .append(")");
                        m_processMsg = msg.toString();
                        return DocAction.STATUS_Invalid;
                    }
                }
            }
        }

        //	Lines
        if (explodeBOM())
            lines = getLines(true, MOrderLine.COLUMNNAME_M_Product_ID);
        if (!reserveStock(dt, lines))
        {
            String innerMsg = CLogger.retrieveErrorString("");
            m_processMsg = "Cannot reserve Stock";
            if (! Util.isEmpty(innerMsg))
                m_processMsg = m_processMsg + " -> " + innerMsg;
            return DocAction.STATUS_Invalid;
        }
        if (!calculateTaxTotal())
        {
            m_processMsg = "Error calculating tax";
            return DocAction.STATUS_Invalid;
        }

        if (   getGrandTotal().signum() != 0
            && (PAYMENTRULE_OnCredit.equals(getPaymentRule()) || PAYMENTRULE_DirectDebit.equals(getPaymentRule())))
        {
            if (!createPaySchedule())
            {
                m_processMsg = "@ErrorPaymentSchedule@";
                return DocAction.STATUS_Invalid;
            }
        } else {
            if (MOrderPaySchedule.getOrderPaySchedule(getCtx(), getC_Order_ID(), 0, get_TrxName()).length > 0)
            {
                m_processMsg = "@ErrorPaymentSchedule@";
                return DocAction.STATUS_Invalid;
            }
        }

        //	Credit Check
        if (isSOTrx())
        {
            if (   MDocType.DOCSUBTYPESO_POSOrder.equals(dt.getDocSubTypeSO())
                && PAYMENTRULE_Cash.equals(getPaymentRule())
                && !MSysConfig.getBooleanValue(MSysConfig.CHECK_CREDIT_ON_CASH_POS_ORDER, true, getAD_Client_ID(), getAD_Org_ID())) {
                // ignore -- don't validate for Cash POS Orders depending on sysconfig parameter
            } else if (MDocType.DOCSUBTYPESO_PrepayOrder.equals(dt.getDocSubTypeSO())
                && !MSysConfig.getBooleanValue(MSysConfig.CHECK_CREDIT_ON_PREPAY_ORDER, true, getAD_Client_ID(), getAD_Org_ID())) {
                // ignore -- don't validate Prepay Orders depending on sysconfig parameter
            } else {
                MBPartner bp = new MBPartner (getCtx(), getBill_BPartner_ID(), get_TrxName()); // bill bp is guaranteed on beforeSave

                if (getGrandTotal().signum() > 0)  // IDEMPIERE-365 - just check credit if is going to increase the debt
                {

                    if (MBPartner.SOCREDITSTATUS_CreditStop.equals(bp.getSOCreditStatus()))
                    {
                        m_processMsg = "@BPartnerCreditStop@ - @TotalOpenBalance@="
                            + bp.getTotalOpenBalance()
                            + ", @SO_CreditLimit@=" + bp.getSO_CreditLimit();
                        return DocAction.STATUS_Invalid;
                    }
                    if (MBPartner.SOCREDITSTATUS_CreditHold.equals(bp.getSOCreditStatus()))
                    {
                        m_processMsg = "@BPartnerCreditHold@ - @TotalOpenBalance@="
                            + bp.getTotalOpenBalance()
                            + ", @SO_CreditLimit@=" + bp.getSO_CreditLimit();
                        return DocAction.STATUS_Invalid;
                    }
                    BigDecimal grandTotal = MConversionRate.convertBase(getCtx(),
                        getGrandTotal(), getC_Currency_ID(), getDateOrdered(),
                        getC_ConversionType_ID(), getAD_Client_ID(), getAD_Org_ID());
                    if (MBPartner.SOCREDITSTATUS_CreditHold.equals(bp.getSOCreditStatus(grandTotal)))
                    {
                        m_processMsg = "@BPartnerOverOCreditHold@ - @TotalOpenBalance@="
                            + bp.getTotalOpenBalance() + ", @GrandTotal@=" + grandTotal
                            + ", @SO_CreditLimit@=" + bp.getSO_CreditLimit();
                        return DocAction.STATUS_Invalid;
                    }
                }
            }
        }

        m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
        if (m_processMsg != null)
            return DocAction.STATUS_Invalid;

        m_justPrepared = true;
        //	if (!DOCACTION_Complete.equals(getDocAction()))		don't set for just prepare
        //		setDocAction(DOCACTION_Complete);
        return DocAction.STATUS_InProgress;
    }	//	prepareIt

    /**
     * 	Reserve Inventory.
     * 	Counterpart: MInOut.completeIt()
     * 	@param dt document type or null
     * 	@param lines order lines (ordered by M_Product_ID for deadlock prevention)
     * 	@return true if (un) reserved
     */
    protected boolean reserveStock (MDocType dt, MOrderLine[] lines)
    {
        if (dt == null)
            dt = MDocType.get(getCtx(), getC_DocType_ID());

        //	Binding
        boolean binding = !dt.isProposal();
        //	Not binding - i.e. Target=0
        if (DOCACTION_Void.equals(getDocAction())
            //	Closing Binding Quotation
            || (MDocType.DOCSUBTYPESO_Quotation.equals(dt.getDocSubTypeSO())
            && DOCACTION_Close.equals(getDocAction()))
            ) // || isDropShip() )
            binding = false;
        boolean isSOTrx = isSOTrx();
        if (log.isLoggable(Level.FINE)) log.fine("Binding=" + binding + " - IsSOTrx=" + isSOTrx);
        //	Force same WH for all but SO/PO
        int header_M_Warehouse_ID = getM_Warehouse_ID();
        if (MDocType.DOCSUBTYPESO_StandardOrder.equals(dt.getDocSubTypeSO())
            || MDocType.DOCBASETYPE_PurchaseOrder.equals(dt.getDocBaseType()))
            header_M_Warehouse_ID = 0;		//	don't enforce

        BigDecimal Volume = Env.ZERO;
        BigDecimal Weight = Env.ZERO;

        //	Always check and (un) Reserve Inventory
        for (int i = 0; i < lines.length; i++)
        {
            MOrderLine line = lines[i];
            //	Check/set WH/Org
            if (header_M_Warehouse_ID != 0)	//	enforce WH
            {
                if (header_M_Warehouse_ID != line.getM_Warehouse_ID())
                    line.setM_Warehouse_ID(header_M_Warehouse_ID);
                if (getAD_Org_ID() != line.getAD_Org_ID())
                    line.setAD_Org_ID(getAD_Org_ID());
            }
            //	Binding
            BigDecimal target = binding ? line.getQtyOrdered() : Env.ZERO;
            BigDecimal difference = target
                .subtract(line.getQtyReserved())
                .subtract(line.getQtyDelivered());

            if (difference.signum() == 0 || line.getQtyOrdered().signum() < 0)
            {
                if (difference.signum() == 0 || line.getQtyReserved().signum() == 0)
                {
                    org.compiere.product.MProduct product = line.getProduct();
                    if (product != null)
                    {
                        Volume = Volume.add(product.getVolume().multiply(line.getQtyOrdered()));
                        Weight = Weight.add(product.getWeight().multiply(line.getQtyOrdered()));
                    }
                    continue;
                }
                else if (line.getQtyOrdered().signum() < 0 && line.getQtyReserved().signum() > 0)
                {
                    difference = line.getQtyReserved().negate();
                }
            }

            if (log.isLoggable(Level.FINE)) log.fine("Line=" + line.getLine()
                + " - Target=" + target + ",Difference=" + difference
                + " - Ordered=" + line.getQtyOrdered()
                + ",Reserved=" + line.getQtyReserved() + ",Delivered=" + line.getQtyDelivered());

            //	Check Product - Stocked and Item
            org.compiere.product.MProduct product = line.getProduct();
            if (product != null)
            {
                if (product.isStocked())
                {
                    //	Update Reservation Storage
                    if (!MStorageReservation.add(getCtx(), line.getM_Warehouse_ID(),
                        line.getM_Product_ID(),
                        line.getM_AttributeSetInstance_ID(),
                        difference, isSOTrx, get_TrxName()))
                        return false;
                }	//	stocked
                //	update line
                line.setQtyReserved(line.getQtyReserved().add(difference));
                if (!line.save(get_TrxName()))
                    return false;
                //
                Volume = Volume.add(product.getVolume().multiply(line.getQtyOrdered()));
                Weight = Weight.add(product.getWeight().multiply(line.getQtyOrdered()));
            }	//	product
        }	//	reverse inventory

        setVolume(Volume);
        setWeight(Weight);
        return true;
    }	//	reserveStock

    /**************************************************************************
     * 	Complete Document
     * 	@return new status (Complete, In Progress, Invalid, Waiting ..)
     */
    public String completeIt()
    {
        MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
        String DocSubTypeSO = dt.getDocSubTypeSO();

        //	Just prepare
        if (DOCACTION_Prepare.equals(getDocAction()))
        {
            setProcessed(false);
            return DocAction.STATUS_InProgress;
        }

        // Set the definite document number after completed (if needed)
        setDefiniteDocumentNo();

        //	Offers
        if (MDocType.DOCSUBTYPESO_Proposal.equals(DocSubTypeSO)
            || MDocType.DOCSUBTYPESO_Quotation.equals(DocSubTypeSO))
        {
            //	Binding
            if (MDocType.DOCSUBTYPESO_Quotation.equals(DocSubTypeSO))
                reserveStock(dt, getLines(true, MOrderLine.COLUMNNAME_M_Product_ID));
            m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
            if (m_processMsg != null)
                return DocAction.STATUS_Invalid;
            m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
            if (m_processMsg != null)
                return DocAction.STATUS_Invalid;
            setProcessed(true);
            return DocAction.STATUS_Completed;
        }
        //	Waiting Payment - until we have a payment
        if (!m_forceCreation
            && MDocType.DOCSUBTYPESO_PrepayOrder.equals(DocSubTypeSO)
            && getC_Payment_ID() == 0 && getC_CashLine_ID() == 0)
        {
            setProcessed(true);
            return DocAction.STATUS_WaitingPayment;
        }

        //	Re-Check
        if (!m_justPrepared)
        {
            String status = prepareIt();
            m_justPrepared = false;
            if (!DocAction.STATUS_InProgress.equals(status))
                return status;
        }

        m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
        if (m_processMsg != null)
            return DocAction.STATUS_Invalid;

        //	Implicit Approval
        if (!isApproved())
            approveIt();
        getLines(true,null);
        if (log.isLoggable(Level.INFO)) log.info(toString());
        StringBuilder info = new StringBuilder();

        boolean realTimePOS = MSysConfig.getBooleanValue(MSysConfig.REAL_TIME_POS, false , getAD_Client_ID());

        //	Create SO Shipment - Force Shipment
        MInOut shipment = null;
        if (MDocType.DOCSUBTYPESO_OnCreditOrder.equals(DocSubTypeSO)		//	(W)illCall(I)nvoice
            || MDocType.DOCSUBTYPESO_WarehouseOrder.equals(DocSubTypeSO)	//	(W)illCall(P)ickup
            || MDocType.DOCSUBTYPESO_POSOrder.equals(DocSubTypeSO)			//	(W)alkIn(R)eceipt
            || MDocType.DOCSUBTYPESO_PrepayOrder.equals(DocSubTypeSO))
        {
            if (!DELIVERYRULE_Force.equals(getDeliveryRule()))
            {
                MWarehouse wh = new MWarehouse (getCtx(), getM_Warehouse_ID(), get_TrxName());
                if (!wh.isDisallowNegativeInv())
                    setDeliveryRule(DELIVERYRULE_Force);
            }
            //
            shipment = createShipment (dt, realTimePOS ? null : getDateOrdered());
            if (shipment == null)
                return DocAction.STATUS_Invalid;
            info.append("@M_InOut_ID@: ").append(shipment.getDocumentNo());
            String msg = shipment.getProcessMsg();
            if (msg != null && msg.length() > 0)
                info.append(" (").append(msg).append(")");
        }	//	Shipment


        //	Create SO Invoice - Always invoice complete Order
        if ( MDocType.DOCSUBTYPESO_POSOrder.equals(DocSubTypeSO)
            || MDocType.DOCSUBTYPESO_OnCreditOrder.equals(DocSubTypeSO)
            || MDocType.DOCSUBTYPESO_PrepayOrder.equals(DocSubTypeSO))
        {
            I_C_Invoice invoice = createInvoice (dt, shipment, realTimePOS ? null : getDateOrdered());
            if (invoice == null)
                return DocAction.STATUS_Invalid;
            info.append(" - @C_Invoice_ID@: ").append(invoice.getDocumentNo());
            String msg = invoice.getProcessMsg();
            if (msg != null && msg.length() > 0)
                info.append(" (").append(msg).append(")");
        }	//	Invoice

        String msg = createPOSPayments();
        if (msg != null) {
            m_processMsg = msg;
            return DocAction.STATUS_Invalid;
        }

        //	Counter Documents
        org.compiere.order.MOrder counter = createCounterDoc();
        if (counter != null)
            info.append(" - @CounterDoc@: @Order@=").append(counter.getDocumentNo());
        //	User Validation
        String valid = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
        if (valid != null)
        {
            if (info.length() > 0)
                info.append(" - ");
            info.append(valid);
            m_processMsg = info.toString();
            return DocAction.STATUS_Invalid;
        }

        //landed cost
        if (!isSOTrx())
        {
            String error = landedCostAllocation();
            if (!Util.isEmpty(error))
            {
                m_processMsg = error;
                return DocAction.STATUS_Invalid;
            }
        }

        setProcessed(true);
        m_processMsg = info.toString();
        //
        setDocAction(DOCACTION_Close);
        return DocAction.STATUS_Completed;
    }	//	completeIt



    protected String landedCostAllocation() {
        MOrderLandedCost[] landedCosts = MOrderLandedCost.getOfOrder(getC_Order_ID(), get_TrxName());
        for(MOrderLandedCost landedCost : landedCosts) {
            String error = landedCost.distributeLandedCost();
            if (!Util.isEmpty(error))
                return error;
        }
        return "";
    }



    protected String createPOSPayments() {

        // Just for POS order with payment rule mixed
        if (! this.isSOTrx())
            return null;
        if (! org.compiere.order.MOrder.DocSubTypeSO_POS.equals(this.getC_DocType().getDocSubTypeSO()))
            return null;
        if (! PAYMENTRULE_MixedPOSPayment.equals(this.getPaymentRule()))
            return null;

        // Verify sum of all payments pos must be equal to the grandtotal of POS invoice (minus withholdings)
        I_C_Invoice[] invoices = this.getInvoices();
        if (invoices == null || invoices.length == 0)
            return "@NoPOSInvoices@";
        I_C_Invoice lastInvoice = invoices[0];
        BigDecimal grandTotal = lastInvoice.getGrandTotal();

        List<X_C_POSPayment> pps = new Query(this.getCtx(), X_C_POSPayment.Table_Name, "C_Order_ID=?", this.get_TrxName())
            .setParameters(this.getC_Order_ID())
            .setOnlyActiveRecords(true)
            .list();
        BigDecimal totalPOSPayments = Env.ZERO;
        for (X_C_POSPayment pp : pps) {
            totalPOSPayments = totalPOSPayments.add(pp.getPayAmt());
        }
        if (totalPOSPayments.compareTo(grandTotal) != 0)
            return "@POSPaymentDiffers@ - @C_POSPayment_ID@=" + totalPOSPayments + ", @GrandTotal@=" + grandTotal;

        String whereClause = "AD_Org_ID=? AND C_Currency_ID=?";
        MBankAccount ba = new Query(this.getCtx(),MBankAccount.Table_Name,whereClause,this.get_TrxName())
            .setParameters(this.getAD_Org_ID(), this.getC_Currency_ID())
            .setOrderBy("IsDefault DESC")
            .first();
        if (ba == null)
            return "@NoAccountOrgCurrency@";

        MDocType[] doctypes = MDocType.getOfDocBaseType(this.getCtx(), MDocType.DOCBASETYPE_ARReceipt);
        if (doctypes == null || doctypes.length == 0)
            return "No document type for AR Receipt";
        MDocType doctype = null;
        for (MDocType doc : doctypes) {
            if (doc.getAD_Org_ID() == this.getAD_Org_ID()) {
                doctype = doc;
                break;
            }
        }
        if (doctype == null)
            doctype = doctypes[0];

        // Create a payment for each non-guarantee record
        // associate the payment id and mark the record as processed
        for (X_C_POSPayment pp : pps) {
            X_C_POSTenderType  tt = new X_C_POSTenderType (getCtx(),pp.getC_POSTenderType_ID(), get_TrxName());
            if (tt.isGuarantee())
                continue;
            if (pp.isPostDated())
                continue;

            MPayment payment = new MPayment(this.getCtx(), 0, this.get_TrxName());
            payment.setAD_Org_ID(this.getAD_Org_ID());

            payment.setTenderType(pp.getTenderType());
            if (MPayment.TENDERTYPE_CreditCard.equals(pp.getTenderType())) {
                payment.setTrxType(MPayment.TRXTYPE_Sales);
                payment.setCreditCardType(pp.getCreditCardType());
                payment.setCreditCardNumber(pp.getCreditCardNumber());
                payment.setVoiceAuthCode(pp.getVoiceAuthCode());
            }

            payment.setC_BankAccount_ID(ba.getC_BankAccount_ID());
            payment.setRoutingNo(pp.getRoutingNo());
            payment.setAccountNo(pp.getAccountNo());
            payment.setSwiftCode(pp.getSwiftCode());
            payment.setIBAN(pp.getIBAN());
            payment.setCheckNo(pp.getCheckNo());
            payment.setMicr(pp.getMicr());
            payment.setIsPrepayment(false);

            payment.setDateAcct(this.getDateAcct());
            payment.setDateTrx(this.getDateOrdered());
            //
            payment.setC_BPartner_ID(this.getC_BPartner_ID());
            payment.setC_Invoice_ID(lastInvoice.getC_Invoice_ID());
            // payment.setC_Order_ID(this.getC_Order_ID()); / do not set order to avoid the prepayment flag
            payment.setC_DocType_ID(doctype.getC_DocType_ID());
            payment.setC_Currency_ID(this.getC_Currency_ID());

            payment.setPayAmt(pp.getPayAmt());

            //	Copy statement line reference data
            payment.setA_Name(pp.getA_Name());

            payment.setC_POSTenderType_ID(pp.getC_POSTenderType_ID());

            //	Save payment
            payment.saveEx();

            pp.setC_Payment_ID(payment.getC_Payment_ID());
            pp.setProcessed(true);
            pp.saveEx();

            payment.setDocAction(MPayment.DOCACTION_Complete);
            if (!payment.processIt (MPayment.DOCACTION_Complete))
                return "Cannot Complete the Payment :" + payment;

            payment.saveEx();
        }

        return null;
    }

    /**
     * 	Set the definite document number after completed
     */
    protected void setDefiniteDocumentNo() {
        MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
        if (dt.isOverwriteDateOnComplete()) {
            /* a42niem - BF IDEMPIERE-63 - check if document has been completed before */
            if (this.getProcessedOn().signum() == 0) {
                setDateOrdered(new Timestamp (System.currentTimeMillis()));
                if (getDateAcct().before(getDateOrdered())) {
                    setDateAcct(getDateOrdered());
                    MPeriod.testPeriodOpen(getCtx(), getDateAcct(), getC_DocType_ID(), getAD_Org_ID());
                }
            }
        }
        if (dt.isOverwriteSeqOnComplete()) {
            /* a42niem - BF IDEMPIERE-63 - check if document has been completed before */
            if (this.getProcessedOn().signum() == 0) {
                String value = MSequence.getDocumentNo(getC_DocType_ID(), get_TrxName(), true, this);
                if (value != null)
                    setDocumentNo(value);
            }
        }
    }

    /**
     * 	Create Shipment
     *	@param dt order document type
     *	@param movementDate optional movement date (default today)
     *	@return shipment or null
     */
    protected MInOut createShipment(MDocType dt, Timestamp movementDate)
    {
        if (log.isLoggable(Level.INFO)) log.info("For " + dt);
        MInOut shipment = new MInOut (this, dt.getC_DocTypeShipment_ID(), movementDate);
        //	shipment.setDateAcct(getDateAcct());
        if (!shipment.save(get_TrxName()))
        {
            m_processMsg = "Could not create Shipment";
            return null;
        }
        //
        MOrderLine[] oLines = getLines(true, null);
        for (int i = 0; i < oLines.length; i++)
        {
            MOrderLine oLine = oLines[i];
            //
            MInOutLine ioLine = new MInOutLine(shipment);
            //	Qty = Ordered - Delivered
            BigDecimal MovementQty = oLine.getQtyOrdered().subtract(oLine.getQtyDelivered());
            //	Location
            int M_Locator_ID = MStorageOnHand.getM_Locator_ID (oLine.getM_Warehouse_ID(),
                oLine.getM_Product_ID(), oLine.getM_AttributeSetInstance_ID(),
                MovementQty, get_TrxName());
            if (M_Locator_ID == 0)		//	Get default Location
            {
                MWarehouse wh = MWarehouse.get(getCtx(), oLine.getM_Warehouse_ID());
                M_Locator_ID = wh.getDefaultLocator().getM_Locator_ID();
            }
            //
            ioLine.setOrderLine(oLine, M_Locator_ID, MovementQty);
            ioLine.setQty(MovementQty);
            if (oLine.getQtyEntered().compareTo(oLine.getQtyOrdered()) != 0)
                ioLine.setQtyEntered(MovementQty
                    .multiply(oLine.getQtyEntered())
                    .divide(oLine.getQtyOrdered(), 6, BigDecimal.ROUND_HALF_UP));
            if (!ioLine.save(get_TrxName()))
            {
                m_processMsg = "Could not create Shipment Line";
                return null;
            }
        }
        // added AdempiereException by zuhri
        if (!shipment.processIt(DocAction.ACTION_Complete))
            throw new AdempiereException("Failed when processing document - " + shipment.getProcessMsg());
        // end added
        shipment.saveEx(get_TrxName());
        if (!DOCSTATUS_Completed.equals(shipment.getDocStatus()))
        {
            m_processMsg = "@M_InOut_ID@: " + shipment.getProcessMsg();
            return null;
        }
        return shipment;
    }	//	createShipment

    /**
     * 	Create new Order by copying
     * 	@param from order
     * 	@param dateDoc date of the document date
     * 	@param C_DocTypeTarget_ID target document type
     * 	@param isSOTrx sales order
     * 	@param counter create counter links
     *	@param copyASI copy line attributes Attribute Set Instance, Resaouce Assignment
     * 	@param trxName trx
     *	@return Order
     */
    public static MOrder copyFrom (MOrder from, Timestamp dateDoc,
                                   int C_DocTypeTarget_ID, boolean isSOTrx, boolean counter, boolean copyASI,
                                   String trxName) {
        MOrder to = new MOrder(from.getCtx(), 0, trxName);
        return (MOrder) doCopyFrom (from, dateDoc,
            C_DocTypeTarget_ID, isSOTrx, counter, copyASI,
            trxName, to);
    }


    /**
     * 	Create Counter Document
     * 	@return counter order
     */
    protected org.compiere.order.MOrder createCounterDoc()
    {
        //	Is this itself a counter doc ?
        if (getRef_Order_ID() != 0)
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
        MOrder counter = copyFrom (this, getDateOrdered(),
            C_DocTypeTarget_ID, !isSOTrx(), true, false, get_TrxName());
        //
        counter.setAD_Org_ID(counterAD_Org_ID);
        counter.setM_Warehouse_ID(counterOrgInfo.getM_Warehouse_ID());
        //
//		counter.setBPartner(counterBP); // was set on copyFrom
        counter.setDatePromised(getDatePromised());		// default is date ordered
        //	References (Should not be required)
        counter.setSalesRep_ID(getSalesRep_ID());
        counter.saveEx(get_TrxName());

        //	Update copied lines
        MOrderLine[] counterLines = counter.getLines(true, null);
        for (int i = 0; i < counterLines.length; i++)
        {
            MOrderLine counterLine = counterLines[i];
            counterLine.setOrder(counter);	//	copies header values (BP, etc.)
            counterLine.setPrice();
            counterLine.setTax();
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
     * 	Set Qtys to 0 - Sales: reverse all documents
     * 	@return true if success
     */
    public boolean voidIt()
    {
        if (log.isLoggable(Level.INFO)) log.info(toString());
        // Before Void
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_VOID);
        if (m_processMsg != null)
            return false;

        if (getLink_Order_ID() > 0) {
            org.compiere.order.MOrder so = new org.compiere.order.MOrder(getCtx(), getLink_Order_ID(), get_TrxName());
            so.setLink_Order_ID(0);
            so.saveEx();
        }

        if (!createReversals())
            return false;

        MOrderLine[] lines = getLines(true, MOrderLine.COLUMNNAME_M_Product_ID);
        for (int i = 0; i < lines.length; i++)
        {
            MOrderLine line = lines[i];
            BigDecimal old = line.getQtyOrdered();
            if (old.signum() != 0)
            {
                line.addDescription(Msg.getMsg(getCtx(), "Voided") + " (" + old + ")");
                line.setQty(Env.ZERO);
                line.setLineNetAmt(Env.ZERO);
                line.saveEx(get_TrxName());
            }
            //AZ Goodwill
            if (!isSOTrx())
            {
                deleteMatchPOCostDetail(line);
            }
            if (line.getLink_OrderLine_ID() > 0) {
                MOrderLine soline = new MOrderLine(getCtx(), line.getLink_OrderLine_ID(), get_TrxName());
                soline.setLink_OrderLine_ID(0);
                soline.saveEx();
            }
        }

        // update taxes
        MOrderTax[] taxes = getTaxes(true);
        for (MOrderTax tax : taxes )
        {
            if ( !(tax.calculateTaxFromLines() && tax.save()) )
                return false;
        }

        addDescription(Msg.getMsg(getCtx(), "Voided"));
        //	Clear Reservations
        if (!reserveStock(null, lines))
        {
            m_processMsg = "Cannot unreserve Stock (void)";
            return false;
        }

        // UnLink All Requisitions
        MRequisitionLine.unlinkC_Order_ID(getCtx(), get_ID(), get_TrxName());

        /* globalqss - 2317928 - Reactivating/Voiding order must reset posted */
        MFactAcct.deleteEx(Table_ID, getC_Order_ID(), get_TrxName());
        setPosted(false);

        // After Void
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_VOID);
        if (m_processMsg != null)
            return false;

        setTotalLines(Env.ZERO);
        setGrandTotal(Env.ZERO);
        setProcessed(true);
        setDocAction(DOCACTION_None);
        return true;
    }	//	voidIt

    /**
     * 	Get Shipments of Order
     * 	@return shipments
     */
    public MInOut[] getShipments()
    {
        final String whereClause = "EXISTS (SELECT 1 FROM M_InOutLine iol, C_OrderLine ol"
            +" WHERE iol.M_InOut_ID=M_InOut.M_InOut_ID"
            +" AND iol.C_OrderLine_ID=ol.C_OrderLine_ID"
            +" AND ol.C_Order_ID=?)";
        List<PO> list = new Query(getCtx(), I_M_InOut.Table_Name, whereClause, get_TrxName())
            .setParameters(get_ID())
            .setOrderBy("M_InOut_ID DESC")
            .list();
        return list.toArray(new MInOut[list.size()]);
    }	//	getShipments

    /**
     * 	Create Shipment/Invoice Reversals
     * 	@return true if success
     */
    protected boolean createReversals()
    {
        //	Cancel only Sales
        if (!isSOTrx())
            return true;

        log.info("createReversals");
        StringBuilder info = new StringBuilder();

        //	Reverse All *Shipments*
        info.append("@M_InOut_ID@:");
        MInOut[] shipments = getShipments();
        for (int i = 0; i < shipments.length; i++)
        {
            MInOut ship = shipments[i];
            //	if closed - ignore
            if (MInOut.DOCSTATUS_Closed.equals(ship.getDocStatus())
                || MInOut.DOCSTATUS_Reversed.equals(ship.getDocStatus())
                || MInOut.DOCSTATUS_Voided.equals(ship.getDocStatus()) )
                continue;
            ship.set_TrxName(get_TrxName());

            //	If not completed - void - otherwise reverse it
            if (!MInOut.DOCSTATUS_Completed.equals(ship.getDocStatus()))
            {
                if (ship.voidIt())
                    ship.setDocStatus(MInOut.DOCSTATUS_Voided);
            }
            else if (ship.reverseCorrectIt())	//	completed shipment
            {
                ship.setDocStatus(MInOut.DOCSTATUS_Reversed);
                info.append(" ").append(ship.getDocumentNo());
            }
            else
            {
                m_processMsg = "Could not reverse Shipment " + ship;
                return false;
            }
            ship.setDocAction(MInOut.DOCACTION_None);
            ship.saveEx(get_TrxName());
        }	//	for all shipments

        //	Reverse All *Invoices*
        info.append(" - @C_Invoice_ID@:");
        I_C_Invoice[] invoices = getInvoices();
        for (int i = 0; i < invoices.length; i++)
        {
            I_C_Invoice invoice = invoices[i];
            //	if closed - ignore
            if (MInvoice.DOCSTATUS_Closed.equals(invoice.getDocStatus())
                || MInvoice.DOCSTATUS_Reversed.equals(invoice.getDocStatus())
                || MInvoice.DOCSTATUS_Voided.equals(invoice.getDocStatus()) )
                continue;
            invoice.set_TrxName(get_TrxName());

            //	If not completed - void - otherwise reverse it
            if (!MInvoice.DOCSTATUS_Completed.equals(invoice.getDocStatus()))
            {
                if (invoice.voidIt())
                    invoice.setDocStatus(MInvoice.DOCSTATUS_Voided);
            }
            else if (invoice.reverseCorrectIt())	//	completed invoice
            {
                invoice.setDocStatus(MInvoice.DOCSTATUS_Reversed);
                info.append(" ").append(invoice.getDocumentNo());
            }
            else
            {
                m_processMsg = "Could not reverse Invoice " + invoice;
                return false;
            }
            invoice.setDocAction(MInvoice.DOCACTION_None);
            invoice.saveEx(get_TrxName());
        }	//	for all shipments

        m_processMsg = info.toString();
        return true;
    }	//	createReversals


    /**
     * 	Close Document.
     * 	Cancel not delivered Quantities
     * 	@return true if success
     */
    public boolean closeIt()
    {
        if (log.isLoggable(Level.INFO)) log.info(toString());
        // Before Close
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_CLOSE);
        if (m_processMsg != null)
            return false;

        //	Close Not delivered Qty - SO/PO
        MOrderLine[] lines = getLines(true, MOrderLine.COLUMNNAME_M_Product_ID);
        for (int i = 0; i < lines.length; i++)
        {
            MOrderLine line = lines[i];
            BigDecimal old = line.getQtyOrdered();
            if (old.compareTo(line.getQtyDelivered()) != 0)
            {
                line.setQtyLostSales(line.getQtyOrdered().subtract(line.getQtyDelivered()));
                line.setQtyOrdered(line.getQtyDelivered());
                //	QtyEntered unchanged
                line.addDescription("Close (" + old + ")");
                line.saveEx(get_TrxName());
            }
        }
        //	Clear Reservations
        if (!reserveStock(null, lines))
        {
            m_processMsg = "Cannot unreserve Stock (close)";
            return false;
        }

        setProcessed(true);
        setDocAction(DOCACTION_None);
        // After Close
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_CLOSE);
        if (m_processMsg != null)
            return false;
        return true;
    }	//	closeIt

    /**
     * @author: phib
     * re-open a closed order
     * (reverse steps of close())
     */
    public String reopenIt() {
        if (log.isLoggable(Level.INFO)) log.info(toString());
        if (!DOCSTATUS_Closed.equals(getDocStatus()))
        {
            return "Not closed - can't reopen";
        }

        //
        MOrderLine[] lines = getLines(true, MOrderLine.COLUMNNAME_M_Product_ID);
        for (int i = 0; i < lines.length; i++)
        {
            MOrderLine line = lines[i];
            if (Env.ZERO.compareTo(line.getQtyLostSales()) != 0)
            {
                line.setQtyOrdered(line.getQtyLostSales().add(line.getQtyDelivered()));
                line.setQtyLostSales(Env.ZERO);
                //	QtyEntered unchanged

                // Strip Close() tags from description
                String desc = line.getDescription();
                if (desc == null)
                    desc = "";
                Pattern pattern = Pattern.compile("( \\| )?Close \\(.*\\)");
                String[] parts = pattern.split(desc);
                desc = "";
                for (String s : parts) {
                    desc = desc.concat(s);
                }
                line.setDescription(desc);
                if (!line.save(get_TrxName()))
                    return "Couldn't save orderline";
            }
        }
        //	Clear Reservations
        if (!reserveStock(null, lines))
        {
            m_processMsg = "Cannot unreserve Stock (close)";
            return "Failed to update reservations";
        }

        setDocStatus(DOCSTATUS_Completed);
        setDocAction(DOCACTION_Close);
        if (!this.save(get_TrxName()))
            return "Couldn't save reopened order";
        else
            return "";
    }	//	reopenIt
    /**
     * 	Reverse Correction - same void
     * 	@return true if success
     */
    public boolean reverseCorrectIt()
    {
        if (log.isLoggable(Level.INFO)) log.info(toString());
        // Before reverseCorrect
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSECORRECT);
        if (m_processMsg != null)
            return false;

        // After reverseCorrect
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSECORRECT);
        if (m_processMsg != null)
            return false;

        return voidIt();
    }	//	reverseCorrectionIt

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

        // After reverseAccrual
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSEACCRUAL);
        if (m_processMsg != null)
            return false;

        return false;
    }	//	reverseAccrualIt

    /**
     * 	Re-activate.
     * 	@return true if success
     */
    public boolean reActivateIt()
    {
        if (log.isLoggable(Level.INFO)) log.info(toString());
        // Before reActivate
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REACTIVATE);
        if (m_processMsg != null)
            return false;



        MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
        String DocSubTypeSO = dt.getDocSubTypeSO();

        //	Replace Prepay with POS to revert all doc
        if (MDocType.DOCSUBTYPESO_PrepayOrder.equals (DocSubTypeSO))
        {
            MDocType newDT = null;
            MDocType[] dts = MDocType.getOfClient (getCtx());
            for (int i = 0; i < dts.length; i++)
            {
                MDocType type = dts[i];
                if (MDocType.DOCSUBTYPESO_PrepayOrder.equals(type.getDocSubTypeSO()))
                {
                    if (type.isDefault() || newDT == null)
                        newDT = type;
                }
            }
            if (newDT == null)
                return false;
            else
                setC_DocType_ID (newDT.getC_DocType_ID());
        }

        //	PO - just re-open
        if (!isSOTrx()) {
            if (log.isLoggable(Level.INFO)) log.info("Existing documents not modified - " + dt);
            //	Reverse Direct Documents
        } else if (MDocType.DOCSUBTYPESO_OnCreditOrder.equals(DocSubTypeSO)	//	(W)illCall(I)nvoice
            || MDocType.DOCSUBTYPESO_WarehouseOrder.equals(DocSubTypeSO)	//	(W)illCall(P)ickup
            || MDocType.DOCSUBTYPESO_POSOrder.equals(DocSubTypeSO))			//	(W)alkIn(R)eceipt
        {
            if (!createReversals())
                return false;
        }
        else
        {
            if (log.isLoggable(Level.INFO)) log.info("Existing documents not modified - SubType=" + DocSubTypeSO);
        }

        /* globalqss - 2317928 - Reactivating/Voiding order must reset posted */
        MFactAcct.deleteEx(Table_ID, getC_Order_ID(), get_TrxName());
        setPosted(false);

        // After reActivate
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REACTIVATE);
        if (m_processMsg != null)
            return false;

        setDocAction(DOCACTION_Complete);
        setProcessed(false);
        return true;
    }	//	reActivateIt

    //AZ Goodwill
    protected String deleteMatchPOCostDetail(MOrderLine line)
    {
        // Get Account Schemas to delete MCostDetail
        MAcctSchema[] acctschemas = MAcctSchema.getClientAcctSchema(getCtx(), getAD_Client_ID());
        for(int asn = 0; asn < acctschemas.length; asn++)
        {
            MAcctSchema as = acctschemas[asn];

            if (as.isSkipOrg(getAD_Org_ID()))
            {
                continue;
            }

            // update/delete Cost Detail and recalculate Current Cost
            MMatchPO[] mPO = MMatchPO.getOrderLine(getCtx(), line.getC_OrderLine_ID(), get_TrxName());
            // delete Cost Detail if the Matched PO has been deleted
            if (mPO.length == 0)
            {
                MCostDetail cd = MCostDetail.get(getCtx(), "C_OrderLine_ID=?",
                    line.getC_OrderLine_ID(), line.getM_AttributeSetInstance_ID(),
                    as.getC_AcctSchema_ID(), get_TrxName());
                if (cd !=  null)
                {
                    cd.setProcessed(false);
                    cd.delete(true);
                }
            }
        }

        return "";
    }

    /* Doc - To be used on ModelValidator to get the corresponding Doc from the PO */
    private IDoc m_doc;

    @Override
    public void setDoc(IDoc doc) {
        m_doc = doc;
    }
}
