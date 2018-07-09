package org.idempiere.process;

import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Level;

import org.compiere.model.IProcessInfoParameter;
import org.compiere.model.I_M_ProductionPlan;
import org.compiere.impl.MProduction;
import org.compiere.impl.MProductionLine;
import org.compiere.impl.MProductionPlan;
import org.compiere.orm.Query;
import org.compiere.process.ProcessInfo;
import org.compiere.server.ServerProcessCtl;
import org.idempiere.common.util.Env;
import org.compiere.wf.MWorkflow;

import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.idempiere.common.util.AdempiereUserError;

/**
 * 
 * Process to create production lines based on the plans
 * defined for a particular production header
 * @author Paul Bowden
 *
 */
public class ProductionProcess extends SvrProcess {

	private int p_M_Production_ID=0;
	private Timestamp p_MovementDate = null;
	private MProduction m_production = null;
	
	
	protected void prepare() {
		
		IProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
		//	log.fine("prepare - " + para[i]);
			if (para[i].getParameter() == null)
				;
			else if (name.equals("MovementDate"))
				p_MovementDate = (Timestamp)para[i].getParameter();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);		
		}
		
		p_M_Production_ID = getRecord_ID();
		if (p_M_Production_ID > 0)
			m_production = new MProduction(getCtx(), p_M_Production_ID, get_TrxName());

	}	//prepare

	@Override
	protected String doIt() throws Exception {
		if ( m_production == null || m_production.get_ID() == 0 )
			throw new AdempiereUserError("Could not load production header");
		
		try {
			int processed = ProductionProcess.procesProduction(m_production, p_MovementDate, false);
			StringBuilder msgreturn = new StringBuilder("@Processed@ #").append(processed);
			return msgreturn.toString();
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			return e.getMessage();
		}
	}

	public static int procesProduction(MProduction production, Timestamp movementDate, boolean mustBeStocked) {
		ProcessInfo pi = ServerProcessCtl.runDocumentActionWorkflow(production, "CO");
		if (pi.isError()) {
			throw new RuntimeException(pi.getSummary());
		} else {
			if (production.isUseProductionPlan()) {
				Query planQuery = new Query(Env.getCtx(), I_M_ProductionPlan.Table_Name, "M_ProductionPlan.M_Production_ID=?", production.get_TrxName());
				List<MProductionPlan> plans = planQuery.setParameters(production.getM_Production_ID()).list();
				int linesCount = 0;
				for(MProductionPlan plan : plans) {
					MProductionLine[] lines = plan.getLines();
					linesCount += lines.length;
				}
				return linesCount;
			} else {
				return production.getLines().length;
			}

		}
	}
}
