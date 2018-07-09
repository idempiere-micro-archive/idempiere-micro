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
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.crm.MUser;
import org.compiere.model.I_M_InOutLineConfirm;
import org.compiere.orm.MDocType;
import org.compiere.orm.MRefList;
import org.compiere.orm.Query;
import org.compiere.orm.X_C_DocType;
import org.idempiere.common.exceptions.AdempiereException;
import org.idempiere.common.util.CLogger;
import org.idempiere.common.util.Env;
import org.compiere.util.Msg;
import org.idempiere.orm.PO;

/**
 *	Shipment Confirmation Model
 *	
 *  @author Jorg Janke
 *  @version $Id: MInOutConfirm.java,v 1.3 2006/07/30 00:51:03 jjanke Exp $
 * 
 * @author Teo Sarca, www.arhipac.ro
 * 			<li>BF [ 2800460 ] System generate Material Receipt with no lines
 * 				https://sourceforge.net/tracker/?func=detail&atid=879332&aid=2800460&group_id=176962
 * @author Teo Sarca, teo.sarca@gmail.com
 * 			<li>BF [ 2993853 ] Voiding/Reversing Receipt should void confirmations
 * 				https://sourceforge.net/tracker/?func=detail&atid=879332&aid=2993853&group_id=176962
 * 			<li>FR [ 2994115 ] Add C_DocType.IsPrepareSplitDoc flag
 * 				https://sourceforge.net/tracker/?func=detail&aid=2994115&group_id=176962&atid=879335
 */
public class MInOutConfirm extends X_M_InOutConfirm
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 5270365186462536874L;


	
	/**	Static Logger	*/
	protected static CLogger	s_log	= CLogger.getCLogger (MInOutConfirm.class);
	
	/**************************************************************************
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param M_InOutConfirm_ID id
	 *	@param trxName transaction
	 */
	public MInOutConfirm (Properties ctx, int M_InOutConfirm_ID, String trxName)
	{
		super (ctx, M_InOutConfirm_ID, trxName);
		if (M_InOutConfirm_ID == 0)
		{
		//	setConfirmType (null);
			setDocAction (X_M_InOutConfirm.DOCACTION_Complete);	// CO
			setDocStatus (X_M_InOutConfirm.DOCSTATUS_Drafted);	// DR
			setIsApproved (false);
			setIsCancelled (false);
			setIsInDispute(false);
			super.setProcessed (false);
		}
	}	//	MInOutConfirm

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MInOutConfirm (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MInOutConfirm

	/**
	 * 	Parent Constructor
	 *	@param ship shipment
	 *	@param confirmType confirmation type
	 */
	public MInOutConfirm (MInOut ship, String confirmType)
	{
		this (ship.getCtx(), 0, ship.get_TrxName());
		setClientOrg(ship);
		setM_InOut_ID (ship.getM_InOut_ID());
		setConfirmType (confirmType);
	}	//	MInOutConfirm
	

	
	/**
	 * 	Add to Description
	 *	@param description text
	 */
	public void addDescription (String description)
	{
		String desc = getDescription();
		if (desc == null)
			setDescription(description);
		else{
			StringBuilder msgd = new StringBuilder(desc).append(" | ").append(description);
			setDescription(msgd.toString());
		}	
	}	//	addDescription
	
	/**
	 * 	Get Name of ConfirmType
	 *	@return confirm type
	 */
	public String getConfirmTypeName ()
	{
		return MRefList.getListName (getCtx(), X_M_InOutConfirm.CONFIRMTYPE_AD_Reference_ID, getConfirmType());
	}	//	getConfirmTypeName

	
	/**
	 * 	Get Document Info
	 *	@return document info (untranslated)
	 */
	public String getDocumentInfo()
	{
		StringBuilder msgreturn = new StringBuilder().append(Msg.getElement(getCtx(), "M_InOutConfirm_ID")).append(" ").append(getDocumentNo());
		return msgreturn.toString();
	}	//	getDocumentInfo

	/**
	 * 	Create PDF
	 *	@return File or null
	 */
	public File createPDF ()
	{
		try
		{
			StringBuilder msgfile = new StringBuilder().append(get_TableName()).append(get_ID()).append("_");
			File temp = File.createTempFile(msgfile.toString(), ".pdf");
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
	//	ReportEngine re = ReportEngine.get (getCtx(), ReportEngine.INVOICE, getC_Invoice_ID());
	//	if (re == null)
			return null;
	//	return re.getPDF(file);
	}	//	createPDF

	/**
	 * 	Set Approved
	 *	@param IsApproved approval
	 */
	public void setIsApproved (boolean IsApproved)
	{
		if (IsApproved && !isApproved())
		{
			int AD_User_ID = Env.getAD_User_ID(getCtx());
			MUser user = MUser.get(getCtx(), AD_User_ID);
			StringBuilder info = new StringBuilder().append(user.getName()) 
				.append(": ")
				.append(Msg.translate(getCtx(), "IsApproved"))
				.append(" - ").append(new Timestamp(System.currentTimeMillis()));
			addDescription(info.toString());
		}
		super.setIsApproved (IsApproved);
	}	//	setIsApproved
	
	

	
	/**	Just Prepared Flag			*/
    protected boolean		m_justPrepared = false;

	/**
	 * 	Unlock Document.
	 * 	@return true if success 
	 */
	public boolean unlockIt()
	{
		if (log.isLoggable(Level.INFO)) log.info(toString());
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
		setDocAction(X_M_InOutConfirm.DOCACTION_Prepare);
		return true;
	}	//	invalidateIt
	

	
	/**
	 * 	Approve Document
	 * 	@return true if success 
	 */
	public boolean  approveIt()
	{
		if (log.isLoggable(Level.INFO)) log.info(toString());
		setIsApproved(true);
		return true;
	}	//	approveIt
	
	/**
	 * 	Reject Approval
	 * 	@return true if success 
	 */
	public boolean rejectIt()
	{
		if (log.isLoggable(Level.INFO)) log.info(toString());
		setIsApproved(false);
		return true;
	}	//	rejectIt


	/**
	 * 	Get Document Owner (Responsible)
	 *	@return AD_User_ID
	 */
	public int getDoc_User_ID()
	{
		return getUpdatedBy();
	}	//	getDoc_User_ID

	/**
	 * 	Get Document Currency
	 *	@return C_Currency_ID
	 */
	public int getC_Currency_ID()
	{
	//	MPriceList pl = MPriceList.get(getCtx(), getM_PriceList_ID());
	//	return pl.getC_Currency_ID();
		return 0;
	}	//	getC_Currency_ID
	
}	//	MInOutConfirm
