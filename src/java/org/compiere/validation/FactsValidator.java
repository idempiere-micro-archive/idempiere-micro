package org.compiere.validation;

import java.util.List;

import org.compiere.model.IFact;
import org.compiere.model.I_C_AcctSchema;
import org.idempiere.icommon.model.IPO;


public interface FactsValidator {
	
	/**
	 * 	Get Client to be monitored
	 *	@return AD_Client_ID
	 */
	public int getAD_Client_ID();
	
	/**
	 * 
	 * @param facts
	 * @param po
	 * @return error message or null - 
     * if not null, the pocument will be marked as Invalid.
	 */
	public String factsValidate(I_C_AcctSchema schema, List<IFact> facts, IPO po);
}
