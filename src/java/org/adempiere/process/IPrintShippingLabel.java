package org.adempiere.process;

import org.compiere.order.MShipperLabels;
import org.compiere.orm.MAttachment;

public interface IPrintShippingLabel {

	public String printToLabelPrinter(MAttachment attachment, MShipperLabels labelType) throws Exception;
	
	public String printImageLabel(MAttachment attachment, MShipperLabels labelType, String title) throws Exception;
	
}