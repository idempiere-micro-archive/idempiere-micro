/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2010 Heng Sin Low                							  *
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
package org.compiere.validation;

import java.util.List;

import org.compiere.model.IFact;
import org.compiere.model.I_C_AcctSchema;
import org.idempiere.icommon.model.IPO;
import org.idempiere.orm.PO;

/**
 *
 * @author hengsin
 *
 */
public class FactsEventData {
	private I_C_AcctSchema acctSchema;
	private List<IFact> facts;
	private IPO po;

	/**
	 * @param acctSchema
	 * @param facts
	 * @param po
	 */
	public FactsEventData(I_C_AcctSchema acctSchema, List<IFact> facts, IPO po) {
		super();
		this.acctSchema = acctSchema;
		this.facts = facts;
		this.po = po;
	}

	/**
	 * @return the acctSchema
	 */
	public I_C_AcctSchema getAcctSchema() {
		return acctSchema;
	}

	/**
	 * @return the facts
	 */
	public List<IFact> getFacts() {
		return facts;
	}

	/**
	 * @return the po
	 */
	public IPO getPo() {
		return po;
	}
}
