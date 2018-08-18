/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.idempiere.org/license.html           *
 *****************************************************************************/
package org.compiere.order;

import java.io.File;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.crm.MBPartner;
import org.compiere.model.*;
import org.compiere.orm.*;
import org.compiere.product.*;
import org.compiere.tax.MTax;
import org.compiere.tax.MTaxProvider;
import org.idempiere.common.exceptions.AdempiereException;
import org.adempiere.exceptions.BPartnerNoBillToAddressException;
import org.adempiere.exceptions.BPartnerNoShipToAddressException;
import org.adempiere.exceptions.FillMandatoryException;
import org.compiere.tax.ITaxProvider;
import org.idempiere.common.util.DB;
import org.idempiere.common.util.Env;
import org.compiere.util.Msg;
import org.idempiere.common.util.Util;
 


/**
 *  Order Model.
 * 	Please do not set DocStatus and C_DocType_ID directly. 
 * 	They are set in the process() method. 
 * 	Use DocAction and C_DocTypeTarget_ID instead.
 *
 *  @author Jorg Janke
 *
 *  @author victor.perez@e-evolution.com, e-Evolution http://www.e-evolution.com
 * 			<li> FR [ 2520591 ] Support multiples calendar for Org 
 *			@see http://sourceforge.net/tracker2/?func=detail&atid=879335&aid=2520591&group_id=176962
 *  @version $Id: MOrder.java,v 1.5 2006/10/06 00:42:24 jjanke Exp $
 * 
 * @author Teo Sarca, www.arhipac.ro
 * 			<li>BF [ 2419978 ] Voiding PO, requisition don't set on NULL
 * 			<li>BF [ 2892578 ] Order should autoset only active price lists
 * 				https://sourceforge.net/tracker/?func=detail&aid=2892578&group_id=176962&atid=879335
 * @author Michael Judd, www.akunagroup.com
 *          <li>BF [ 2804888 ] Incorrect reservation of products with attributes
 */
public class MOrder extends X_C_Order implements I_C_Order
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -7784588474522162502L;

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
		return doCopyFrom (from, dateDoc,
		C_DocTypeTarget_ID, isSOTrx, counter, copyASI,
		trxName, to);
	}


	protected static MOrder doCopyFrom (MOrder from, Timestamp dateDoc,
		int C_DocTypeTarget_ID, boolean isSOTrx, boolean counter, boolean copyASI, 
		String trxName, MOrder to)
	{
		to.set_TrxName(trxName);
		copyValues(from, to, from.getAD_Client_ID(), from.getAD_Org_ID());
		to.set_ValueNoCheck ("C_Order_ID", I_ZERO);
		to.set_ValueNoCheck ("DocumentNo", null);
		//
		to.setDocStatus (DOCSTATUS_Drafted);		//	Draft
		to.setDocAction(DOCACTION_Complete);
		//
		to.setC_DocType_ID(0);
		to.setC_DocTypeTarget_ID (C_DocTypeTarget_ID);
		to.setIsSOTrx(isSOTrx);
		//
		to.setIsSelected (false);
		to.setDateOrdered (dateDoc);
		to.setDateAcct (dateDoc);
		to.setDatePromised (dateDoc);	//	assumption
		to.setDatePrinted(null);
		to.setIsPrinted (false);
		//
		to.setIsApproved (false);
		to.setIsCreditApproved(false);
		to.setC_Payment_ID(0);
		to.setC_CashLine_ID(0);
		//	Amounts are updated  when adding lines
		to.setGrandTotal(Env.ZERO);
		to.setTotalLines(Env.ZERO);
		//
		to.setIsDelivered(false);
		to.setIsInvoiced(false);
		to.setIsSelfService(false);
		to.setIsTransferred (false);
		to.setPosted (false);
		to.setProcessed (false);
		if (counter) {
			to.setRef_Order_ID(from.getC_Order_ID());
			MOrg org = MOrg.get(from.getCtx(), from.getAD_Org_ID());
			int counterC_BPartner_ID = org.getLinkedC_BPartner_ID(trxName);
			if (counterC_BPartner_ID == 0)
				return null;
			to.setBPartner(MBPartner.get(from.getCtx(), counterC_BPartner_ID));
		} else
			to.setRef_Order_ID(0);
		//
		if (!to.save(trxName))
			throw new IllegalStateException("Could not create Order");
		if (counter)
			from.setRef_Order_ID(to.getC_Order_ID());

		if (to.copyLinesFrom(from, counter, copyASI) == 0)
			throw new IllegalStateException("Could not create Order Lines");
		
		// don't copy linked PO/SO
		to.setLink_Order_ID(0);
		
		return to;
	}	//	copyFrom
	
	
	/**************************************************************************
	 *  Default Constructor
	 *  @param ctx context
	 *  @param  C_Order_ID    order to load, (0 create new order)
	 *  @param trxName trx name
	 */
	public MOrder(Properties ctx, int C_Order_ID, String trxName)
	{
		super (ctx, C_Order_ID, trxName);
		//  New
		if (C_Order_ID == 0)
		{
			setDocStatus(DOCSTATUS_Drafted);
			setDocAction (DOCACTION_Prepare);
			//
			setDeliveryRule (DELIVERYRULE_Availability);
			setFreightCostRule (FREIGHTCOSTRULE_FreightIncluded);
			setInvoiceRule (INVOICERULE_Immediate);
			setPaymentRule(PAYMENTRULE_OnCredit);
			setPriorityRule (PRIORITYRULE_Medium);
			setDeliveryViaRule (DELIVERYVIARULE_Pickup);
			//
			setIsDiscountPrinted (false);
			setIsSelected (false);
			setIsTaxIncluded (false);
			setIsSOTrx (true);
			setIsDropShip(false);
			setSendEMail (false);
			//
			setIsApproved(false);
			setIsPrinted(false);
			setIsCreditApproved(false);
			setIsDelivered(false);
			setIsInvoiced(false);
			setIsTransferred(false);
			setIsSelfService(false);
			//
			super.setProcessed(false);
			setProcessing(false);
			setPosted(false);

			setDateAcct (new Timestamp(System.currentTimeMillis()));
			setDatePromised (new Timestamp(System.currentTimeMillis()));
			setDateOrdered (new Timestamp(System.currentTimeMillis()));

			setFreightAmt (Env.ZERO);
			setChargeAmt (Env.ZERO);
			setTotalLines (Env.ZERO);
			setGrandTotal (Env.ZERO);
		}
	}	//	MOrder



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

	/**	Order Lines					*/
	protected MOrderLine[] 	m_lines = null;
	/**	Tax Lines					*/
	protected MOrderTax[] 	m_taxes = null;
	/** Force Creation of order		*/
	protected boolean			m_forceCreation = false;
	
	/**
	 * 	Overwrite Client/Org if required
	 * 	@param AD_Client_ID client
	 * 	@param AD_Org_ID org
	 */
	public void setClientOrg (int AD_Client_ID, int AD_Org_ID)
	{
		super.setClientOrg(AD_Client_ID, AD_Org_ID);
	}	//	setClientOrg


	/**
	 * 	Add to Description
	 *	@param description text
	 */
	public void addDescription (String description)
	{
		String desc = getDescription();
		if (desc == null)
			setDescription(description);
		else
			setDescription(desc + " | " + description);
	}	//	addDescription
	
	/**
	 * 	Set Business Partner (Ship+Bill)
	 *	@param C_BPartner_ID bpartner
	 */
	public void setC_BPartner_ID (int C_BPartner_ID)
	{
		super.setC_BPartner_ID (C_BPartner_ID);
		super.setBill_BPartner_ID (C_BPartner_ID);
	}	//	setC_BPartner_ID
	
	/**
	 * 	Set Business Partner Location (Ship+Bill)
	 *	@param C_BPartner_Location_ID bp location
	 */
	public void setC_BPartner_Location_ID (int C_BPartner_Location_ID)
	{
		super.setC_BPartner_Location_ID (C_BPartner_Location_ID);
		super.setBill_Location_ID(C_BPartner_Location_ID);
	}	//	setC_BPartner_Location_ID

	/**
	 * 	Set Business Partner Contact (Ship+Bill)
	 *	@param AD_User_ID user
	 */
	public void setAD_User_ID (int AD_User_ID)
	{
		super.setAD_User_ID (AD_User_ID);
		super.setBill_User_ID (AD_User_ID);
	}	//	setAD_User_ID

	/**
	 * 	Set Ship Business Partner
	 *	@param C_BPartner_ID bpartner
	 */
	public void setShip_BPartner_ID (int C_BPartner_ID)
	{
		super.setC_BPartner_ID (C_BPartner_ID);
	}	//	setShip_BPartner_ID
	
	/**
	 * 	Set Ship Business Partner Location
	 *	@param C_BPartner_Location_ID bp location
	 */
	public void setShip_Location_ID (int C_BPartner_Location_ID)
	{
		super.setC_BPartner_Location_ID (C_BPartner_Location_ID);
	}	//	setShip_Location_ID

	/**
	 * 	Set Ship Business Partner Contact
	 *	@param AD_User_ID user
	 */
	public void setShip_User_ID (int AD_User_ID)
	{
		super.setAD_User_ID (AD_User_ID);
	}	//	setShip_User_ID
	
	
	/**
	 * 	Set Warehouse
	 *	@param M_Warehouse_ID warehouse
	 */
	public void setM_Warehouse_ID (int M_Warehouse_ID)
	{
		super.setM_Warehouse_ID (M_Warehouse_ID);
	}	//	setM_Warehouse_ID
	
	/**
	 * 	Set Drop Ship
	 *	@param IsDropShip drop ship
	 */
	public void setIsDropShip (boolean IsDropShip)
	{
		super.setIsDropShip (IsDropShip);
	}	//	setIsDropShip
	
	/*************************************************************************/

	/** Sales Order Sub Type - SO	*/
	public static final String		DocSubTypeSO_Standard = "SO";
	/** Sales Order Sub Type - OB	*/
	public static final String		DocSubTypeSO_Quotation = "OB";
	/** Sales Order Sub Type - ON	*/
	public static final String		DocSubTypeSO_Proposal = "ON";
	/** Sales Order Sub Type - PR	*/
	public static final String		DocSubTypeSO_Prepay = "PR";
	/** Sales Order Sub Type - WR	*/
	public static final String		DocSubTypeSO_POS = "WR";
	/** Sales Order Sub Type - WP	*/
	public static final String		DocSubTypeSO_Warehouse = "WP";
	/** Sales Order Sub Type - WI	*/
	public static final String		DocSubTypeSO_OnCredit = "WI";
	/** Sales Order Sub Type - RM	*/
	public static final String		DocSubTypeSO_RMA = "RM";

	/**
	 * 	Set Target Sales Document Type
	 * 	@param DocSubTypeSO_x SO sub type - see DocSubTypeSO_*
	 */
	public void setC_DocTypeTarget_ID (String DocSubTypeSO_x)
	{
		String sql = "SELECT C_DocType_ID FROM C_DocType "
			+ "WHERE AD_Client_ID=? AND AD_Org_ID IN (0," + getAD_Org_ID()
			+ ") AND DocSubTypeSO=? "
			+ " AND IsActive='Y' "
			+ "ORDER BY AD_Org_ID DESC, IsDefault DESC";
		int C_DocType_ID = DB.getSQLValue(null, sql, getAD_Client_ID(), DocSubTypeSO_x);
		if (C_DocType_ID <= 0)
			log.severe ("Not found for AD_Client_ID=" + getAD_Client_ID () + ", SubType=" + DocSubTypeSO_x);
		else
		{
			if (log.isLoggable(Level.FINE)) log.fine("(SO) - " + DocSubTypeSO_x);
			setC_DocTypeTarget_ID (C_DocType_ID);
			setIsSOTrx(true);
		}
	}	//	setC_DocTypeTarget_ID

	/**
	 * 	Set Target Document Type.
	 * 	Standard Order or PO
	 */
	public void setC_DocTypeTarget_ID ()
	{
		if (isSOTrx())		//	SO = Std Order
		{
			setC_DocTypeTarget_ID(DocSubTypeSO_Standard);
			return;
		}
		//	PO
		String sql = "SELECT C_DocType_ID FROM C_DocType "
			+ "WHERE AD_Client_ID=? AND AD_Org_ID IN (0," + getAD_Org_ID()
			+ ") AND DocBaseType='POO' "
			+ "ORDER BY AD_Org_ID DESC, IsDefault DESC";
		int C_DocType_ID = DB.getSQLValue(null, sql, getAD_Client_ID());
		if (C_DocType_ID <= 0)
			log.severe ("No POO found for AD_Client_ID=" + getAD_Client_ID ());
		else
		{
			if (log.isLoggable(Level.FINE)) log.fine("(PO) - " + C_DocType_ID);
			setC_DocTypeTarget_ID (C_DocType_ID);
		}
	}	//	setC_DocTypeTarget_ID


	/**
	 * 	Set Business Partner Defaults & Details.
	 * 	SOTrx should be set.
	 * 	@param bp business partner
	 */
	public void setBPartner (I_C_BPartner bp)
	{
		if (bp == null)
			return;

		setC_BPartner_ID(bp.getC_BPartner_ID());
		//	Defaults Payment Term
		int ii = 0;
		if (isSOTrx())
			ii = bp.getC_PaymentTerm_ID();
		else
			ii = bp.getPO_PaymentTerm_ID();
		if (ii != 0)
			setC_PaymentTerm_ID(ii);
		//	Default Price List
		if (isSOTrx())
			ii = bp.getM_PriceList_ID();
		else
			ii = bp.getPO_PriceList_ID();
		if (ii != 0)
			setM_PriceList_ID(ii);
		//	Default Delivery/Via Rule
		String ss = bp.getDeliveryRule();
		if (ss != null)
			setDeliveryRule(ss);
		ss = bp.getDeliveryViaRule();
		if (ss != null)
			setDeliveryViaRule(ss);
		//	Default Invoice/Payment Rule
		ss = bp.getInvoiceRule();
		if (ss != null)
			setInvoiceRule(ss);
		ss = bp.getPaymentRule();
		if (ss != null)
			setPaymentRule(ss);
		//	Sales Rep
		ii = bp.getSalesRep_ID();
		if (ii != 0)
			setSalesRep_ID(ii);


		//	Set Locations
		I_C_BPartner_Location[] locs = bp.getLocations();
		if (locs != null)
		{
			for (int i = 0; i < locs.length; i++)
			{
				if (locs[i].isShipTo())
					super.setC_BPartner_Location_ID(locs[i].getC_BPartner_Location_ID());
				if (locs[i].isBillTo())
					setBill_Location_ID(locs[i].getC_BPartner_Location_ID());
			}
			//	set to first
			if (getC_BPartner_Location_ID() == 0 && locs.length > 0)
				super.setC_BPartner_Location_ID(locs[0].getC_BPartner_Location_ID());
			if (getBill_Location_ID() == 0 && locs.length > 0)
				setBill_Location_ID(locs[0].getC_BPartner_Location_ID());
		}
		if (getC_BPartner_Location_ID() == 0)
		{	
			throw new BPartnerNoShipToAddressException(bp);
		}	
			
		if (getBill_Location_ID() == 0)
		{
			throw new BPartnerNoBillToAddressException(bp);
		}	

		//	Set Contact
		I_AD_User[] contacts = bp.getContacts();
		if (contacts != null && contacts.length == 1)
			setAD_User_ID(contacts[0].getAD_User_ID());
	}	//	setBPartner


	/**
	 * 	Copy Lines From other Order
	 *	@param otherOrder order
	 *	@param counter set counter info
	 *	@param copyASI copy line attributes Attribute Set Instance, Resaouce Assignment
	 *	@return number of lines copied
	 */
	public int copyLinesFrom (MOrder otherOrder, boolean counter, boolean copyASI)
	{
		if (isProcessed() || isPosted() || otherOrder == null)
			return 0;
		MOrderLine[] fromLines = otherOrder.getLines(false, null);
		int count = 0;
		for (int i = 0; i < fromLines.length; i++)
		{
			MOrderLine line = new MOrderLine (this);
			copyValues(fromLines[i], line, getAD_Client_ID(), getAD_Org_ID());
			line.setC_Order_ID(getC_Order_ID());
			//
			line.setQtyDelivered(Env.ZERO);
			line.setQtyInvoiced(Env.ZERO);
			line.setQtyReserved(Env.ZERO);
			line.setDateDelivered(null);
			line.setDateInvoiced(null);
			//
			line.setOrder(this);
			line.set_ValueNoCheck ("C_OrderLine_ID", I_ZERO);	//	new
			//	References
			if (!copyASI)
			{
				line.setM_AttributeSetInstance_ID(0);
				line.setS_ResourceAssignment_ID(0);
			}
			if (counter)
				line.setRef_OrderLine_ID(fromLines[i].getC_OrderLine_ID());
			else
				line.setRef_OrderLine_ID(0);

			// don't copy linked lines
			line.setLink_OrderLine_ID(0);
			//	Tax
			if (getC_BPartner_ID() != otherOrder.getC_BPartner_ID())
				line.setTax();		//	recalculate
			//
			//
			line.setProcessed(false);
			if (line.save(get_TrxName()))
				count++;
			//	Cross Link
			if (counter)
			{
				fromLines[i].setRef_OrderLine_ID(line.getC_OrderLine_ID());
				fromLines[i].saveEx(get_TrxName());
			}
		}
		if (fromLines.length != count)
			log.log(Level.SEVERE, "Line difference - From=" + fromLines.length + " <> Saved=" + count);
		return count;
	}	//	copyLinesFrom

	
	/**************************************************************************
	 * 	String Representation
	 *	@return info
	 */
	public String toString ()
	{
		StringBuffer sb = new StringBuffer ("MOrder[")
			.append(get_ID()).append("-").append(getDocumentNo())
			.append(",IsSOTrx=").append(isSOTrx())
			.append(",C_DocType_ID=").append(getC_DocType_ID())
			.append(", GrandTotal=").append(getGrandTotal())
			.append ("]");
		return sb.toString ();
	}	//	toString

	/**
	 * 	Get Document Info
	 *	@return document info (untranslated)
	 */
	public String getDocumentInfo()
	{
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID() > 0 ? getC_DocType_ID() : getC_DocTypeTarget_ID());
		return dt.getNameTrl() + " " + getDocumentNo();
	}	//	getDocumentInfo

	/**
	 * 	Create PDF
	 *	@return File or null
	 */
	public File createPDF ()
	{
		try
		{
			File temp = File.createTempFile(get_TableName()+get_ID()+"_", ".pdf");
			return createPDF (temp);
		}
		catch (Exception e)
		{
			log.severe("Could not create PDF - " + e.getMessage());
		}
		return null;
	}	//	getPDF

	/**
	 * 	Create PDF file
	 *	@param file output file
	 *	@return file if success
	 */
	public File createPDF (File file)
	{
		return null;
	}	//	createPDF
	
	/**
	 * 	Set Price List (and Currency, TaxIncluded) when valid
	 * 	@param M_PriceList_ID price list
	 */
	public void setM_PriceList_ID (int M_PriceList_ID)
	{
		MPriceList pl = MPriceList.get(getCtx(), M_PriceList_ID, null);
		if (pl.get_ID() == M_PriceList_ID)
		{
			super.setM_PriceList_ID(M_PriceList_ID);
			setC_Currency_ID(pl.getC_Currency_ID());
			setIsTaxIncluded(pl.isTaxIncluded());
		}
	}	//	setM_PriceList_ID

	
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

	/**
	 * 	Get Lines of Order.
	 * 	(used by web store)
	 * 	@return lines
	 */
	public MOrderLine[] getLines()
	{
		return getLines(false, null);
	}	//	getLines
	
	/**
	 * 	Renumber Lines
	 *	@param step start and step
	 */
	public void renumberLines (int step)
	{
		int number = step;
		MOrderLine[] lines = getLines(true, null);	//	Line is default
		for (int i = 0; i < lines.length; i++)
		{
			MOrderLine line = lines[i];
			line.setLine(number);
			line.saveEx(get_TrxName());
			number += step;
		}
		m_lines = null;
	}	//	renumberLines
	
	/**
	 * 	Does the Order Line belong to this Order
	 *	@param C_OrderLine_ID line
	 *	@return true if part of the order
	 */
	public boolean isOrderLine(int C_OrderLine_ID)
	{
		if (m_lines == null)
			getLines();
		for (int i = 0; i < m_lines.length; i++)
			if (m_lines[i].getC_OrderLine_ID() == C_OrderLine_ID)
				return true;
		return false;
	}	//	isOrderLine

	/**
	 * 	Get Taxes of Order
	 *	@param requery requery
	 *	@return array of taxes
	 */
	public MOrderTax[] getTaxes(boolean requery)
	{
		if (m_taxes != null && !requery)
			return m_taxes;
		//
		List<MOrderTax> list = new Query(getCtx(), I_C_OrderTax.Table_Name, "C_Order_ID=?", get_TrxName())
									.setParameters(get_ID())
									.list();
		m_taxes = list.toArray(new MOrderTax[list.size()]);
		return m_taxes;
	}	//	getTaxes
	
	
	/**
	 * 	Get Invoices of Order
	 * 	@return invoices
	 */
	public I_C_Invoice[] getInvoices()
	{
		final String whereClause = "EXISTS (SELECT 1 FROM C_InvoiceLine il, C_OrderLine ol"
							        +" WHERE il.C_Invoice_ID=C_Invoice.C_Invoice_ID"
							        		+" AND il.C_OrderLine_ID=ol.C_OrderLine_ID"
							        		+" AND ol.C_Order_ID=?)";
		List<PO> list = new Query(getCtx(), I_C_Invoice.Table_Name, whereClause, get_TrxName())
									.setParameters(get_ID())
									.setOrderBy("C_Invoice_ID DESC")
									.list();
		return list.toArray(new I_C_Invoice[list.size()]);
	}	//	getInvoices

	/**
	 * 	Get latest Invoice of Order
	 * 	@return invoice id or 0
	 */
	public int getC_Invoice_ID()
	{
 		String sql = "SELECT C_Invoice_ID FROM C_Invoice "
			+ "WHERE C_Order_ID=? AND DocStatus IN ('CO','CL') "
			+ "ORDER BY C_Invoice_ID DESC";
		int C_Invoice_ID = DB.getSQLValue(get_TrxName(), sql, get_ID());
		return C_Invoice_ID;
	}	//	getC_Invoice_ID


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
	 *	Get ISO Code of Currency
	 *	@return Currency ISO
	 */
	public String getCurrencyISO()
	{
		return MCurrency.getISO_Code (getCtx(), getC_Currency_ID());
	}	//	getCurrencyISO
	
	/**
	 * 	Get Currency Precision
	 *	@return precision
	 */
	public int getPrecision()
	{
		return MCurrency.getStdPrecision(getCtx(), getC_Currency_ID());
	}	//	getPrecision

	/**
	 * 	Get Document Status
	 *	@return Document Status Clear Text
	 */
	public String getDocStatusName()
	{
		return MRefList.getListName(getCtx(), 131, getDocStatus());
	}	//	getDocStatusName

	/**
	 * 	Set DocAction
	 *	@param DocAction doc action
	 */
	public void setDocAction (String DocAction)
	{
		setDocAction (DocAction, false);
	}	//	setDocAction

	/**
	 * 	Set DocAction
	 *	@param DocAction doc action
	 *	@param forceCreation force creation
	 */
	public void setDocAction (String DocAction, boolean forceCreation)
	{
		super.setDocAction (DocAction);
		m_forceCreation = forceCreation;
	}	//	setDocAction
	
	/**
	 * 	Set Processed.
	 * 	Propagate to Lines/Taxes
	 *	@param processed processed
	 */
	public void setProcessed (boolean processed)
	{
		super.setProcessed (processed);
		if (get_ID() == 0)
			return;
		String set = "SET Processed='"
			+ (processed ? "Y" : "N")
			+ "' WHERE C_Order_ID=" + getC_Order_ID();
		int noLine = DB.executeUpdateEx("UPDATE C_OrderLine " + set, get_TrxName());
		int noTax = DB.executeUpdateEx("UPDATE C_OrderTax " + set, get_TrxName());
		m_lines = null;
		m_taxes = null;
		if (log.isLoggable(Level.FINE)) log.fine("setProcessed - " + processed + " - Lines=" + noLine + ", Tax=" + noTax);
	}	//	setProcessed
	
	
	
	/**
	 * 	Validate Order Pay Schedule
	 *	@return pay schedule is valid
	 */
	public boolean validatePaySchedule()
	{
		MOrderPaySchedule[] schedule = MOrderPaySchedule.getOrderPaySchedule
			(getCtx(), getC_Order_ID(), 0, get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("#" + schedule.length);
		if (schedule.length == 0)
		{
			setIsPayScheduleValid(false);
			return false;
		}
		//	Add up due amounts
		BigDecimal total = Env.ZERO;
		for (int i = 0; i < schedule.length; i++)
		{
			schedule[i].setParent(this);
			BigDecimal due = schedule[i].getDueAmt();
			if (due != null)
				total = total.add(due);
		}
		boolean valid = getGrandTotal().compareTo(total) == 0;
		setIsPayScheduleValid(valid);
		
		//	Update Schedule Lines
		for (int i = 0; i < schedule.length; i++)
		{
			if (schedule[i].isValid() != valid)
			{
				schedule[i].setIsValid(valid);
				schedule[i].saveEx(get_TrxName());				
			}
		}
		return valid;
	}	//	validatePaySchedule

	
	protected volatile static boolean recursiveCall = false;
	/**************************************************************************
	 * 	Before Save
	 *	@param newRecord new
	 *	@return save
	 */
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

		boolean disallowNegInv = true;
		String DeliveryRule = getDeliveryRule();
		if((disallowNegInv && DELIVERYRULE_Force.equals(DeliveryRule)) ||
				(DeliveryRule == null || DeliveryRule.length()==0))
			setDeliveryRule(DELIVERYRULE_Availability);
		
		//	Reservations in Warehouse
		if (!newRecord && is_ValueChanged("M_Warehouse_ID"))
		{
			MOrderLine[] lines = getLines(false,null);
			for (int i = 0; i < lines.length; i++)
			{
				if (!lines[i].canChangeWarehouse())
					return false;
			}
		}
		
		//	No Partner Info - set Template
		if (getC_BPartner_ID() == 0)
			setBPartner(MBPartner.getTemplate(getCtx(), getAD_Client_ID()));
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
	
	
	/**
	 * 	After Save
	 *	@param newRecord new
	 *	@param success success
	 *	@return true if can be saved
	 */
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if (!success || newRecord)
			return success;
		
		// TODO: The changes here with UPDATE are not being saved on change log - audit problem  
		
		//	Propagate Description changes
		if (is_ValueChanged("Description") || is_ValueChanged("POReference"))
		{
			String sql = "UPDATE C_Invoice i"
				+ " SET (Description,POReference)="
					+ "(SELECT Description,POReference "
					+ "FROM C_Order o WHERE i.C_Order_ID=o.C_Order_ID) "
				+ "WHERE DocStatus NOT IN ('RE','CL') AND C_Order_ID=" + getC_Order_ID();
			int no = DB.executeUpdateEx(sql, get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Description -> #" + no);
		}

		//	Propagate Changes of Payment Info to existing (not reversed/closed) invoices
		if (is_ValueChanged("PaymentRule") || is_ValueChanged("C_PaymentTerm_ID")
			|| is_ValueChanged("C_Payment_ID")
			|| is_ValueChanged("C_CashLine_ID"))
		{
			String sql = "UPDATE C_Invoice i "
				+ "SET (PaymentRule,C_PaymentTerm_ID,C_Payment_ID,C_CashLine_ID)="
					+ "(SELECT PaymentRule,C_PaymentTerm_ID,C_Payment_ID,C_CashLine_ID "
					+ "FROM C_Order o WHERE i.C_Order_ID=o.C_Order_ID)"
				+ "WHERE DocStatus NOT IN ('RE','CL') AND C_Order_ID=" + getC_Order_ID();
			//	Don't touch Closed/Reversed entries
			int no = DB.executeUpdate(sql, get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Payment -> #" + no);
		}
	      
		//	Propagate Changes of Date Account to existing (not completed/reversed/closed) invoices
		if (is_ValueChanged("DateAcct"))
		{
			String sql = "UPDATE C_Invoice i "
				+ "SET (DateAcct)="
					+ "(SELECT DateAcct "
					+ "FROM C_Order o WHERE i.C_Order_ID=o.C_Order_ID)"
				+ "WHERE DocStatus NOT IN ('CO','RE','CL') AND Processed='N' AND C_Order_ID=" + getC_Order_ID();
			//	Don't touch Completed/Closed/Reversed entries
			int no = DB.executeUpdate(sql, get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("DateAcct -> #" + no);
		}
	      
		//	Sync Lines
		if (   is_ValueChanged("AD_Org_ID")
		    || is_ValueChanged(COLUMNNAME_C_BPartner_ID)
		    || is_ValueChanged(COLUMNNAME_C_BPartner_Location_ID)
		    || is_ValueChanged(COLUMNNAME_DateOrdered)
		    || is_ValueChanged(COLUMNNAME_DatePromised)
		    || is_ValueChanged(COLUMNNAME_M_Warehouse_ID)
		    || is_ValueChanged(COLUMNNAME_M_Shipper_ID)
		    || is_ValueChanged(COLUMNNAME_C_Currency_ID)) {
			MOrderLine[] lines = getLines();
			for (MOrderLine line : lines) {
				if (is_ValueChanged("AD_Org_ID"))
					line.setAD_Org_ID(getAD_Org_ID());
				if (is_ValueChanged(COLUMNNAME_C_BPartner_ID))
					line.setC_BPartner_ID(getC_BPartner_ID());
				if (is_ValueChanged(COLUMNNAME_C_BPartner_Location_ID))
					line.setC_BPartner_Location_ID(getC_BPartner_Location_ID());
				if (is_ValueChanged(COLUMNNAME_DateOrdered))
					line.setDateOrdered(getDateOrdered());
				if (is_ValueChanged(COLUMNNAME_DatePromised))
					line.setDatePromised(getDatePromised());
				if (is_ValueChanged(COLUMNNAME_M_Warehouse_ID))
					line.setM_Warehouse_ID(getM_Warehouse_ID());
				if (is_ValueChanged(COLUMNNAME_M_Shipper_ID))
					line.setM_Shipper_ID(getM_Shipper_ID());
				if (is_ValueChanged(COLUMNNAME_C_Currency_ID))
					line.setC_Currency_ID(getC_Currency_ID());
				line.saveEx();
			}
		}
		//
		return true;
	}	//	afterSave
	
	/**
	 * 	Before Delete
	 *	@return true of it can be deleted
	 */
	protected boolean beforeDelete ()
	{
		if (isProcessed())
			return false;
		// automatic deletion of lines is driven by model cascade definition in dictionary - see IDEMPIERE-2060
		return true;
	}	//	beforeDelete
	

	
	/**	Process Message 			*/
	protected String		m_processMsg = null;
	/**	Just Prepared Flag			*/
	protected boolean		m_justPrepared = false;

	/**
	 * 	Unlock Document.
	 * 	@return true if success 
	 */
	public boolean unlockIt()
	{
		if (log.isLoggable(Level.INFO)) log.info("unlockIt - " + toString());
		setProcessing(false);
		return true;
	}	//	unlockIt
	
	/**
	 * 	Invalidate Document
	 * 	@return true if success 
	 */
	public boolean invalidateIt()
	{
		if (log.isLoggable(Level.INFO)) log.info(toString());
		setDocAction(DOCACTION_Prepare);
		return true;
	}	//	invalidateIt
	
	

	
	protected boolean calculateFreightCharge()
	{
		MClientInfo ci = MClientInfo.get(getCtx(), getAD_Client_ID(), get_TrxName());
		if (ci.getC_ChargeFreight_ID() == 0 && ci.getM_ProductFreight_ID() == 0)
		{
			m_processMsg = "Product or Charge for Freight is not defined at Client window > Client Info tab";
			return false;
		}
		
		MOrderLine[] ols = getLines(false, MOrderLine.COLUMNNAME_Line);
		if (ols.length == 0)
		{
			m_processMsg = "@NoLines@";
			return false;
		}
		
		MOrderLine freightLine = null;
		for (MOrderLine ol : ols)
		{
			if ((ol.getM_Product_ID() > 0 && ol.getM_Product_ID() == ci.getM_ProductFreight_ID()) ||
					(ol.getC_Charge_ID() > 0 && ol.getC_Charge_ID() == ci.getC_ChargeFreight_ID()))
			{
				freightLine = ol;
				break;
			}
		}
		
		if (getFreightCostRule().equals(FREIGHTCOSTRULE_FreightIncluded))
		{
			if (freightLine != null)
			{
				boolean deleted = freightLine.delete(false);
				if (!deleted)
				{
					freightLine.setC_BPartner_Location_ID(getC_BPartner_Location_ID());
					freightLine.setM_Shipper_ID(getM_Shipper_ID());
					freightLine.setQty(BigDecimal.ONE);
					freightLine.setPrice(BigDecimal.ZERO);
					freightLine.saveEx();
				}
			}
		}
		else if (getFreightCostRule().equals(FREIGHTCOSTRULE_FixPrice))
		{
			if (freightLine == null)
			{
				freightLine = new MOrderLine(this);
			
				if (ci.getC_ChargeFreight_ID() > 0)
					freightLine.setC_Charge_ID(ci.getC_ChargeFreight_ID());
				else if (ci.getM_ProductFreight_ID() > 0)
					freightLine.setM_Product_ID(ci.getM_ProductFreight_ID());
				else
					throw new AdempiereException("Product or Charge for Freight is not defined at Client window > Client Info tab");
			}
			
			freightLine.setC_BPartner_Location_ID(getC_BPartner_Location_ID());
			freightLine.setM_Shipper_ID(getM_Shipper_ID());
			freightLine.setQty(BigDecimal.ONE);
			freightLine.setPrice(getFreightAmt());
			freightLine.saveEx();
		}
		else if (getFreightCostRule().equals(FREIGHTCOSTRULE_Calculated))
		{
			if (ci.getC_UOM_Weight_ID() == 0)
			{
				m_processMsg = "UOM for Weight is not defined at Client window > Client Info tab";
				return false;
			}
			if (ci.getC_UOM_Length_ID() == 0)
			{
				m_processMsg = "UOM for Length is not defined at Client window > Client Info ta";
				return false;
			}
			
			for (MOrderLine ol : ols)
			{
				if ((ol.getM_Product_ID() > 0 && ol.getM_Product_ID() == ci.getM_ProductFreight_ID()) ||
						(ol.getC_Charge_ID() > 0 && ol.getC_Charge_ID() == ci.getC_ChargeFreight_ID()))
					continue;
				else if (ol.getM_Product_ID() > 0)
				{
					MProduct product = new MProduct(getCtx(), ol.getM_Product_ID(), get_TrxName());
					
					BigDecimal weight = product.getWeight();
					if (weight == null || weight.compareTo(BigDecimal.ZERO) == 0)
					{
						m_processMsg = "No weight defined for product " + product.toString();
						return false;
					}
				}
			}
			
			boolean ok = false;
			MShippingTransaction st = null;
			try
			{			
				st = SalesOrderRateInquiryProcess.createShippingTransaction(getCtx(), this, MShippingTransaction.ACTION_RateInquiry, isPriviledgedRate(), get_TrxName());
				ok = st.processOnline();
				st.saveEx();
			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, "processOnline", e);
			}
			
			if (ok)
			{
				if (freightLine == null)
				{
					freightLine = new MOrderLine(this);
				
					if (ci.getC_ChargeFreight_ID() > 0)
						freightLine.setC_Charge_ID(ci.getC_ChargeFreight_ID());
					else if (ci.getM_ProductFreight_ID() > 0)
						freightLine.setM_Product_ID(ci.getM_ProductFreight_ID());
					else
						throw new AdempiereException("Product or Charge for Freight is not defined at Client window > Client Info tab");
				}
				
				freightLine.setC_BPartner_Location_ID(getC_BPartner_Location_ID());
				freightLine.setM_Shipper_ID(getM_Shipper_ID());
				freightLine.setQty(BigDecimal.ONE);
				freightLine.setPrice(st.getPrice());
				freightLine.saveEx();
			}
			else
			{
				m_processMsg = st.getErrorMessage();
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * 	Explode non stocked BOM.
	 * 	@return true if bom exploded
	 */
	protected boolean explodeBOM()
	{
		boolean retValue = false;
		String where = "AND IsActive='Y' AND EXISTS "
			+ "(SELECT * FROM M_Product p WHERE C_OrderLine.M_Product_ID=p.M_Product_ID"
			+ " AND	p.IsBOM='Y' AND p.IsVerified='Y' AND p.IsStocked='N')";
		//
		String sql = "SELECT COUNT(*) FROM C_OrderLine "
			+ "WHERE C_Order_ID=? " + where; 
		int count = DB.getSQLValue(get_TrxName(), sql, getC_Order_ID());
		while (count != 0)
		{
			retValue = true;
			renumberLines (1000);		//	max 999 bom items	

			//	Order Lines with non-stocked BOMs
			MOrderLine[] lines = getLines (where, MOrderLine.COLUMNNAME_Line);
			for (int i = 0; i < lines.length; i++)
			{
				MOrderLine line = lines[i];
				MProduct product = MProduct.get (getCtx(), line.getM_Product_ID());
				if (log.isLoggable(Level.FINE)) log.fine(product.getName());
				//	New Lines
				int lineNo = line.getLine ();
				//find default BOM with valid dates and to this product
				/*/MPPProductBOM bom = MPPProductBOM.get(product, getAD_Org_ID(),getDatePromised(), get_TrxName());
				if(bom != null)
				{	
					MPPProductBOMLine[] bomlines = bom.getLines(getDatePromised());
					for (int j = 0; j < bomlines.length; j++)
					{
						MPPProductBOMLine bomline = bomlines[j];
						MOrderLine newLine = new MOrderLine (this);
						newLine.setLine (++lineNo);
						newLine.setM_Product_ID (bomline.getM_Product_ID ());
						newLine.setC_UOM_ID (bomline.getC_UOM_ID ());
						newLine.setQty (line.getQtyOrdered ().multiply (
							bomline.getQtyBOM()));
						if (bomline.getDescription () != null)
							newLine.setDescription (bomline.getDescription ());
						//
						newLine.setPrice ();
						newLine.saveEx(get_TrxName());
					}
				}	*/

				for (MProductBOM bom : MProductBOM.getBOMLines(product))
				{
					MOrderLine newLine = new MOrderLine(this);
					newLine.setLine(++lineNo);
					newLine.setM_Product_ID(bom.getM_ProductBOM_ID(), true);
					newLine.setQty(line.getQtyOrdered().multiply(bom.getBOMQty()));
					if (bom.getDescription() != null)
						newLine.setDescription(bom.getDescription());
					newLine.setPrice();
					newLine.saveEx(get_TrxName());
				}
				
				//	Convert into Comment Line
				line.setM_Product_ID (0);
				line.setM_AttributeSetInstance_ID (0);
				line.setPrice (Env.ZERO);
				line.setPriceLimit (Env.ZERO);
				line.setPriceList (Env.ZERO);
				line.setLineNetAmt (Env.ZERO);
				line.setFreightAmt (Env.ZERO);
				//
				String description = product.getName ();
				if (product.getDescription () != null)
					description += " " + product.getDescription ();
				if (line.getDescription () != null)
					description += " " + line.getDescription ();
				line.setDescription (description);
				line.saveEx(get_TrxName());
			}	//	for all lines with BOM

			m_lines = null;		//	force requery
			count = DB.getSQLValue (get_TrxName(), sql, getC_Invoice_ID ());
			renumberLines (10);
		}	//	while count != 0
		return retValue;
	}	//	explodeBOM




	/**
	 * 	Calculate Tax and Total
	 * 	@return true if tax total calculated
	 */
	public boolean calculateTaxTotal()
	{
		log.fine("");
		//	Delete Taxes
		DB.executeUpdateEx("DELETE C_OrderTax WHERE C_Order_ID=" + getC_Order_ID(), get_TrxName());
		m_taxes = null;
		
		MTaxProvider[] providers = getTaxProviders();
		for (MTaxProvider provider : providers)
		{
			ITaxProvider calculator = MTaxProvider.getTaxProvider(provider, new StandardTaxProvider());
			if (calculator == null)
				throw new AdempiereException(Msg.getMsg(getCtx(), "TaxNoProvider"));
			
			if (!calculator.calculateOrderTaxTotal(provider, this))
				return false;
		}
		return true;
	}	//	calculateTaxTotal
	
	
	/**
	 * 	(Re) Create Pay Schedule
	 *	@return true if valid schedule
	 */
	protected boolean createPaySchedule()
	{
		if (getC_PaymentTerm_ID() == 0)
			return false;
		MPaymentTerm pt = new MPaymentTerm(getCtx(), getC_PaymentTerm_ID(), null);
		if (log.isLoggable(Level.FINE)) log.fine(pt.toString());

		int numSchema = pt.getSchedule(false).length;
		
		MOrderPaySchedule[] schedule = MOrderPaySchedule.getOrderPaySchedule
			(getCtx(), getC_Order_ID(), 0, get_TrxName());
		
		if (schedule.length > 0) {
			if (numSchema == 0)
				return false; // created a schedule for a payment term that doesn't manage schedule
			return validatePaySchedule();
		} else {
			boolean isValid = pt.applyOrder(this);		//	calls validate pay schedule
			if (numSchema == 0)
				return true; // no schedule, no schema, OK
			else
				return isValid;
		}
	}	//	createPaySchedule
	
	
	/**
	 * 	Approve Document
	 * 	@return true if success 
	 */
	public boolean  approveIt()
	{
		if (log.isLoggable(Level.INFO)) log.info("approveIt - " + toString());
		setIsApproved(true);
		return true;
	}	//	approveIt
	
	/**
	 * 	Reject Approval
	 * 	@return true if success 
	 */
	public boolean rejectIt()
	{
		if (log.isLoggable(Level.INFO)) log.info("rejectIt - " + toString());
		setIsApproved(false);
		return true;
	}	//	rejectIt


	/*************************************************************************
	 * 	Get Summary
	 *	@return Summary of Document
	 */
	public String getSummary()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(getDocumentNo());
		//	: Grand Total = 123.00 (#1)
		sb.append(": ").
			append(Msg.translate(getCtx(),"GrandTotal")).append("=").append(getGrandTotal());
		if (m_lines != null)
			sb.append(" (#").append(m_lines.length).append(")");
		//	 - Description
		if (getDescription() != null && getDescription().length() > 0)
			sb.append(" - ").append(getDescription());
		return sb.toString();
	}	//	getSummary
	
	/**
	 * 	Get Process Message
	 *	@return clear text error message
	 */
	public String getProcessMsg()
	{
		return m_processMsg;
	}	//	getProcessMsg
	
	/**
	 * 	Get Document Owner (Responsible)
	 *	@return AD_User_ID
	 */
	public int getDoc_User_ID()
	{
		return getSalesRep_ID();
	}	//	getDoc_User_ID

	/**
	 * 	Get Document Approval Amount
	 *	@return amount
	 */
	public BigDecimal getApprovalAmt()
	{
		return getGrandTotal();
	}	//	getApprovalAmt
	


	/**
	 * 	Document Status is Complete or Closed
	 *	@return true if CO, CL or RE
	 */
	public boolean isComplete()
	{
		String ds = getDocStatus();
		return DOCSTATUS_Completed.equals(ds) 
			|| DOCSTATUS_Closed.equals(ds)
			|| DOCSTATUS_Reversed.equals(ds);
	}	//	isComplete

	/**
	 * Finds all order lines that contains not yet delivered physical items of a specific product.
	 * 
	 * @param conn			An open connection.
	 * @param productId		The product id being allocated
	 * @return  Order lines to allocate products to.
	 * @throws SQLException
	 */
	/*  commenting out wrong unused function - column qtyallocated does not exist
	public static List<MOrderLine> getOrderLinesToAllocate(Connection conn, int productId, String trxName) throws SQLException {
		final String OrderLinesToAllocate = "select C_OrderLine.* from C_OrderLine " + 
				   "JOIN C_Order ON C_OrderLine.C_Order_ID=C_Order.C_Order_ID " + 
				   "JOIN M_Product ON C_OrderLine.M_Product_ID=M_Product.M_Product_ID " + 
				   "where C_Order.IsSOTrx='Y' AND C_Order.DocStatus='CO' AND QtyAllocated<(QtyOrdered-QtyDelivered) " + 
				   "AND M_Product.M_Product_ID=? " + 
				   "order by PriorityRule, C_OrderLine.Created ";
		List<MOrderLine> result = new Vector<MOrderLine>();
		Properties ctx = Env.getCtx();
		MOrderLine line;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement(OrderLinesToAllocate);
			ps.setInt(1, productId);
			rs = ps.executeQuery();
			while(rs.next()) {
				line = new MOrderLine(ctx, rs, trxName);
				result.add(line);
			}
		} catch (SQLException e) {
			throw e;
		} finally {
			DB.close(rs, ps);
			rs = null; ps = null;
		}
		return(result);
	}
	*/
	
	/**
	 * Finds all products that can be allocated. A product can be allocated if there are more items 
	 * on hand than what is already allocated. To be allocated the item must also be in demand
	 * (reserved < allocated)
	 * 
	 * @param 	conn
	 * @return
	 * @throws 	SQLException
	 */
	/*  commenting out wrong unused function - column qtyallocated does not exist
	public static List<StockInfo> getProductsToAllocate(Connection conn, int WarehouseID) throws SQLException {
		
		List<StockInfo> result = new Vector<StockInfo>();
		StockInfo si;
		String query1 = "select M_Product_ID, sum(qtyonhand), sum(qtyreserved), sum(m_Product_Stock_v.qtyallocated) " +
						"from M_Product_Stock_v " + 
						"WHERE M_Warehouse_ID=? AND M_Product_ID in " +
						"(select DISTINCT C_OrderLine.M_Product_ID FROM C_OrderLine " +
					   "JOIN C_Order ON C_OrderLine.C_Order_ID=C_Order.C_Order_ID " + 
					   "JOIN M_Product ON C_OrderLine.M_Product_ID=M_Product.M_Product_ID " +
					   "JOIN M_Product_Stock_v ON C_OrderLine.M_Product_ID=M_Product_Stock_v.M_Product_ID " +
					   "WHERE " +
					   "C_Order.IsSOTrx='Y' AND C_Order.DocStatus='CO' AND C_OrderLine.M_Warehouse_ID=? AND " + 
					   "(QtyOrdered-QtyDelivered)>0 AND (QtyOrdered-QtyDelivered)>C_OrderLine.QtyAllocated)" + 
					   "group by M_Product_ID " + 
					   "order by M_Product_ID";
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement(query1);
			ps.setInt(1, WarehouseID);
			ps.setInt(2, WarehouseID);
			rs = ps.executeQuery();
			while(rs.next()) {
				si = new StockInfo();
				si.productId = rs.getInt(1);
				si.qtyOnHand = rs.getBigDecimal(2);
				si.qtyReserved = rs.getBigDecimal(3);
				si.qtyAvailable = si.qtyOnHand.subtract(si.qtyReserved);
				si.qtyAllocated = rs.getBigDecimal(4);
				result.add(si);
			}
		} catch (SQLException e) {
			throw e;
		} finally {
			DB.close(rs, ps);
			rs = null; ps = null;
		}
		return(result);
	}
	
	public static class StockInfo {
		
		public int			productId;
		public BigDecimal	qtyOnHand;
		public BigDecimal	qtyAvailable;
		public BigDecimal	qtyReserved;
		public BigDecimal	qtyAllocated;
		
		public StockInfo() {}
		
	}
	*/
	
	/**
	 * Set process message
	 * @param processMsg
	 */
	public void setProcessMessage(String processMsg)
	{
		m_processMsg = processMsg;
	}
	
	/**
	 * Get tax providers
	 * @return array of tax provider
	 */
	public MTaxProvider[] getTaxProviders()
	{
		Hashtable<Integer, MTaxProvider> providers = new Hashtable<Integer, MTaxProvider>();
		MOrderLine[] lines = getLines();
		for (MOrderLine line : lines)
		{
            MTax tax = new MTax(line.getCtx(), line.getC_Tax_ID(), line.get_TrxName());
            MTaxProvider provider = providers.get(tax.getC_TaxProvider_ID());
            if (provider == null)
            	providers.put(tax.getC_TaxProvider_ID(), new MTaxProvider(tax.getCtx(), tax.getC_TaxProvider_ID(), tax.get_TrxName()));
		}
		
		MTaxProvider[] retValue = new MTaxProvider[providers.size()];
		providers.values().toArray(retValue);
		return retValue;
	}

	/** Returns C_DocType_ID (or C_DocTypeTarget_ID if C_DocType_ID is not set) */
	public int getDocTypeID()
	{
		return getC_DocType_ID() > 0 ? getC_DocType_ID() : getC_DocTypeTarget_ID();
	}

}	//	MOrder
