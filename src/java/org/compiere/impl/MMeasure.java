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
package org.compiere.impl;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;

import javax.script.ScriptEngine;

import org.adempiere.util.MeasureInterface;
import org.compiere.crm.MUser;
import org.compiere.orm.MRole;
import org.compiere.orm.MTable;
import org.compiere.rule.MRule;
import org.idempiere.common.util.CCache;
import org.idempiere.common.util.DB;
import org.idempiere.common.util.Env;
import org.compiere.util.Msg;
import org.compiere.webutil.TimeUtil;
import org.idempiere.common.util.Util;
 


/**
 * 	Performance Measure
 *	
 *  @author Jorg Janke
 *  @version $Id: MMeasure.java,v 1.2 2006/07/30 00:51:05 jjanke Exp $
 * 
 * @author Teo Sarca, SC ARHIPAC SERVICE SRL
 * 			<li>BF [ 1887674 ] Deadlock when try to modify PA Goal's Measure Target
 * @author victor.perez@e-evolution.com, www.e-evolution.com
 * 			<li>FR [ 2905227 ] Calculate Measure based on the script to PA
 * 			<li>https://sourceforge.net/tracker/?func=detail&aid=2905227&group_id=176962&atid=879335
 */
public class MMeasure extends X_PA_Measure
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 6274990637485210675L;

	/**
	 * 	Get MMeasure from Cache
	 *	@param ctx context
	 *	@param PA_Measure_ID id
	 *	@return MMeasure
	 */
	public static MMeasure get (Properties ctx, int PA_Measure_ID)
	{
		Integer key = new Integer (PA_Measure_ID);
		MMeasure retValue = (MMeasure)s_cache.get (key);
		if (retValue != null)
			return retValue;
		retValue = new MMeasure (ctx, PA_Measure_ID, null);
		if (retValue.get_ID() != 0)
			s_cache.put (key, retValue);
		return retValue;
	} //	get

	/**	Cache						*/
	private static CCache<Integer, MMeasure> s_cache 
		= new CCache<Integer, MMeasure> (Table_Name, 10);
	
	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param PA_Measure_ID id
	 *	@param trxName trx
	 */
	public MMeasure (Properties ctx, int PA_Measure_ID, String trxName)
	{
		super (ctx, PA_Measure_ID, trxName);
	}	//	MMeasure

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName trx
	 */
	public MMeasure (Properties ctx, ResultSet rs, String trxName)
	{
		super (ctx, rs, trxName);
	}	//	MMeasure

	/**
	 * 	String Representation
	 *	@return info
	 */
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ("MMeasure[");
		sb.append (get_ID()).append ("-").append (getName()).append ("]");
		return sb.toString ();
	}	//	toString
	
	/**
	 * 	Before Save
	 *	@param newRecord new
	 *	@return true
	 */
	protected boolean beforeSave (boolean newRecord)
	{
		if (MEASURETYPE_Calculated.equals(getMeasureType())
			&& getPA_MeasureCalc_ID() == 0)
		{
			log.saveError("FillMandatory", Msg.getElement(getCtx(), "PA_MeasureCalc_ID"));
			return false;
		}
		else if (MEASURETYPE_Ratio.equals(getMeasureType())
			&& getPA_Ratio_ID() == 0)
		{
			log.saveError("FillMandatory", Msg.getElement(getCtx(), "PA_Ratio_ID"));
			return false;
		}
		else if (MEASURETYPE_UserDefined.equals(getMeasureType())
			&& (getCalculationClass() == null || getCalculationClass().length()==0))
		{
			log.saveError("FillMandatory", Msg.getElement(getCtx(), "CalculationClass"));
			return false;
		}
		else if (MEASURETYPE_Request.equals(getMeasureType())
			&& getR_RequestType_ID() == 0)
		{
			log.saveError("FillMandatory", Msg.getElement(getCtx(), "R_RequestType_ID"));
			return false;
		}
		else if (MEASURETYPE_Project.equals(getMeasureType())
			&& getC_ProjectType_ID() == 0)
		{
			log.saveError("FillMandatory", Msg.getElement(getCtx(), "C_ProjectType_ID"));
			return false;
		}
		return true;
	}	//	beforeSave
	
	/**
	 * 	After Save
	 *	@param newRecord new
	 *	@param success success
	 *	@return succes
	 */
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		//	Update Goals with Manual Measure
		if (success && MEASURETYPE_Manual.equals(getMeasureType()))
			updateManualGoals();
		
		return success;
	}	//	afterSave
	
	/**
	 * 	Update/save Goals
	 * 	@return true if updated
	 */
	public boolean updateGoals()
	{
		String mt = getMeasureType();
		try
		{
			if (MEASURETYPE_Manual.equals(mt))
				return updateManualGoals();
			else if (MEASURETYPE_Achievements.equals(mt))
				return updateAchievementGoals();
			else if (MEASURETYPE_Calculated.equals(mt))
				return updateCalculatedGoals();
			else if (MEASURETYPE_Ratio.equals(mt))
				return updateRatios();
			else if (MEASURETYPE_Request.equals(mt))
				return updateRequests();
			else if (MEASURETYPE_Project.equals(mt))
				return updateProjects();
			else if(MEASURETYPE_UserDefined.equals(mt))
				return updateUserDefined();
			//	Projects
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "MeasureType=" + mt, e);
		}
		return false;
	}	//	updateGoals

	/**
	 * 	Update/save Manual Goals
	 * 	@return true if updated
	 */
	private boolean updateManualGoals()
	{
		if (!MEASURETYPE_Manual.equals(getMeasureType()))
			return false;
		MGoal[] goals = MGoal.getMeasureGoals (getCtx(), getPA_Measure_ID());
		for (int i = 0; i < goals.length; i++)
		{
			MGoal goal = goals[i];
			goal.setMeasureActual(getManualActual());
			goal.saveEx(get_TrxName());
		}
		return true;
	}	//	updateManualGoals
	
	/**
	 * 	Update/save Goals with Achievement
	 * 	@return true if updated
	 */
	private boolean updateAchievementGoals()
	{
		if (!MEASURETYPE_Achievements.equals(getMeasureType()))
			return false;
		Timestamp today = new Timestamp(System.currentTimeMillis());
		MGoal[] goals = MGoal.getMeasureGoals (getCtx(), getPA_Measure_ID());
		for (int i = 0; i < goals.length; i++)
		{
			MGoal goal = goals[i];
			String MeasureScope = goal.getMeasureScope();
			String trunc = TimeUtil.TRUNC_DAY;
			if (MGoal.MEASUREDISPLAY_Year.equals(MeasureScope))
				trunc = TimeUtil.TRUNC_YEAR;
			else if (MGoal.MEASUREDISPLAY_Quarter.equals(MeasureScope))
				trunc = TimeUtil.TRUNC_QUARTER;
			else if (MGoal.MEASUREDISPLAY_Month.equals(MeasureScope))
				trunc = TimeUtil.TRUNC_MONTH;
			else if (MGoal.MEASUREDISPLAY_Week.equals(MeasureScope))
				trunc = TimeUtil.TRUNC_WEEK;
			Timestamp compare = TimeUtil.trunc(today, trunc); 
			//
			MAchievement[] achievements = MAchievement.getOfMeasure(getCtx(), getPA_Measure_ID());
			BigDecimal ManualActual = Env.ZERO;
			for (int j = 0; j < achievements.length; j++)
			{
				MAchievement achievement = achievements[j];
				if (achievement.isAchieved() && achievement.getDateDoc() != null)
				{
					Timestamp ach = TimeUtil.trunc(achievement.getDateDoc(), trunc);
					if (compare.equals(ach))
						ManualActual = ManualActual.add(achievement.getManualActual());
				}
			}
			goal.setMeasureActual(ManualActual);
			goal.saveEx(get_TrxName());
		}
		return true;
	}	//	updateAchievementGoals

	/**
	 * 	Update/save Goals with Calculation
	 * 	@return true if updated
	 */
	private boolean updateCalculatedGoals()
	{
		if (!MEASURETYPE_Calculated.equals(getMeasureType()))
			return false;
		MGoal[] goals = MGoal.getMeasureGoals (getCtx(), getPA_Measure_ID());
		for (int i = 0; i < goals.length; i++)
		{
			MGoal goal = goals[i];
			//	Find Role
			MRole role = null;
			if (goal.getAD_Role_ID() != 0)
				role = MRole.get(getCtx(), goal.getAD_Role_ID());
			else if (goal.getAD_User_ID() != 0)
			{
				MUser user = MUser.get(getCtx(), goal.getAD_User_ID());
				MRole[] roles = user.getRoles(goal.getAD_Org_ID());
				if (roles.length > 0)
					role = roles[0];
			}
			if (role == null)
				role = MRole.getDefault(getCtx(), false);	//	could result in wrong data
			//
			MMeasureCalc mc = MMeasureCalc.get(getCtx(), getPA_MeasureCalc_ID());
			if (mc == null || mc.get_ID() == 0 || mc.get_ID() != getPA_MeasureCalc_ID())
			{
				log.log(Level.SEVERE, "Not found PA_MeasureCalc_ID=" + getPA_MeasureCalc_ID());
				return false;
			}
			String sql = mc.getSqlPI(goal.getRestrictions(false), 
				goal.getMeasureScope(), getMeasureDataType(), null, role);
			BigDecimal ManualActual = DB.getSQLValueBD(null, sql, new Object[]{});
			//	SQL may return no rows or null
			if (ManualActual == null)
			{
				ManualActual = Env.ZERO;
				if (log.isLoggable(Level.FINE)) log.fine("No Value = " + sql);
			}
			goal.setMeasureActual(ManualActual);
			goal.saveEx(get_TrxName());
		}
		return true;
	}	//	updateCalculatedGoals
	
	/**
	 * 	Update/save Goals with Ratios
	 * 	@return true if updated
	 */
	private boolean updateRatios()
	{
		if (!MEASURETYPE_Ratio.equals(getMeasureType()))
			return false;
		return false;
	}		//	updateRatios
	
	/**
	 * 	Update/save Goals with Requests
	 * 	@return true if updated
	 */
	private boolean updateRequests()
	{
		if (!MEASURETYPE_Request.equals(getMeasureType())
			|| getR_RequestType_ID() == 0)
			return false;
		MGoal[] goals = MGoal.getMeasureGoals (getCtx(), getPA_Measure_ID());
		for (int i = 0; i < goals.length; i++)
		{
			MGoal goal = goals[i];
			//	Find Role
			MRole role = null;
			if (goal.getAD_Role_ID() != 0)
				role = MRole.get(getCtx(), goal.getAD_Role_ID());
			else if (goal.getAD_User_ID() != 0)
			{
				MUser user = MUser.get(getCtx(), goal.getAD_User_ID());
				MRole[] roles = user.getRoles(goal.getAD_Org_ID());
				if (roles.length > 0)
					role = roles[0];
			}
			if (role == null)
				role = MRole.getDefault(getCtx(), false);	//	could result in wrong data
			//
			MRequestType rt = MRequestType.get(getCtx(), getR_RequestType_ID());
			String sql = rt.getSqlPI(goal.getRestrictions(false), 
				goal.getMeasureScope(), getMeasureDataType(), null, role);
			BigDecimal ManualActual = DB.getSQLValueBD(null, sql, new Object[]{});
			//	SQL may return no rows or null
			if (ManualActual == null)
			{
				ManualActual = Env.ZERO;
				if (log.isLoggable(Level.FINE)) log.fine("No Value = " + sql);
			}
			goal.setMeasureActual(ManualActual);
			goal.saveEx(get_TrxName());
		}
		return true;
	}		//	updateRequests

	/**
	 * 	Update/save Goals with Projects
	 * 	@return true if updated
	 */
	private boolean updateProjects()
	{
		if (!MEASURETYPE_Project.equals(getMeasureType())
			|| getC_ProjectType_ID() == 0)
			return false;
		MGoal[] goals = MGoal.getMeasureGoals (getCtx(), getPA_Measure_ID());
		for (int i = 0; i < goals.length; i++)
		{
			MGoal goal = goals[i];
			//	Find Role
			MRole role = null;
			if (goal.getAD_Role_ID() != 0)
				role = MRole.get(getCtx(), goal.getAD_Role_ID());
			else if (goal.getAD_User_ID() != 0)
			{
				MUser user = MUser.get(getCtx(), goal.getAD_User_ID());
				MRole[] roles = user.getRoles(goal.getAD_Org_ID());
				if (roles.length > 0)
					role = roles[0];
			}
			if (role == null)
				role = MRole.getDefault(getCtx(), false);	//	could result in wrong data
			//
			MProjectType pt = MProjectType.get(getCtx(), getC_ProjectType_ID());
			String sql = pt.getSqlPI(goal.getRestrictions(false), 
				goal.getMeasureScope(), getMeasureDataType(), null, role);		
			BigDecimal ManualActual = DB.getSQLValueBD(null, sql, new Object[]{});
			//	SQL may return no rows or null
			if (ManualActual == null)
			{
				ManualActual = Env.ZERO;
				if (log.isLoggable(Level.FINE)) log.fine("No Value = " + sql);
			}
			goal.setMeasureActual(ManualActual);
			goal.saveEx(get_TrxName());
		}
		return true;
	}	//	updateProjects
	/**
	 * 	Update/save update User Defined
	 * 	@return true if updated
	 */
	private boolean updateUserDefined()
	{
		MGoal[] goals = MGoal.getMeasureGoals (getCtx(), getPA_Measure_ID());
		for (MGoal goal:goals)
		{
			BigDecimal amt = Env.ZERO;
			PO po = (PO)new MTable(getCtx(),get_Table_ID(),get_TrxName()).getPO(get_ID(), get_TrxName());
			StringTokenizer st = new StringTokenizer(getCalculationClass(), ";,", false);
			while (st.hasMoreTokens())      //  for each class
			{
				String cmd = st.nextToken().trim();	
				StringBuilder retValue = new StringBuilder();
				if (cmd.toLowerCase().startsWith(MRule.SCRIPT_PREFIX)) {
					
					MRule rule = MRule.get(getCtx(), cmd.substring(MRule.SCRIPT_PREFIX.length()));
					if (rule == null) {
						retValue = new StringBuilder("Script ").append(cmd).append(" not found"); 
						log.log(Level.SEVERE, retValue.toString());
						break;
					}
					if ( !  (rule.getEventType().equals(MRule.EVENTTYPE_MeasureForPerformanceAnalysis) 
						  && rule.getRuleType().equals(MRule.RULETYPE_JSR223ScriptingAPIs))) {
						retValue = new StringBuilder("Script ").append(cmd)
							.append(" must be of type JSR 223 and event measure"); 
						log.log(Level.SEVERE, retValue.toString());
						break;
					}
					ScriptEngine engine = rule.getScriptEngine();
					MRule.setContext(engine, po.getCtx(), 0);
					engine.put(MRule.ARGUMENTS_PREFIX + "Ctx", po.getCtx());
					engine.put(MRule.ARGUMENTS_PREFIX + "PO", po);
					try 
					{
						Object value =  engine.eval(rule.getScript());
						amt = (BigDecimal)value;
					}
					catch (Exception e)
					{
						log.log(Level.SEVERE, "", e);
						retValue = 	new StringBuilder("Script Invalid: ").append(e.toString());
						return false;
					}	
				} 
				else 
				{
					MeasureInterface custom = null;
					try
					{
						Class<?> clazz = Class.forName(cmd);
						custom = (MeasureInterface)clazz.newInstance();
					}
					catch (Exception e)
					{
						log.log(Level.SEVERE, "No custom measure class "
								+ cmd + " - " + e.toString(), e);
						return false;
					}
					
					try
					{
						amt = custom.getValue();
					}
					catch (Exception e)
					{
						log.log(Level.SEVERE, custom.toString(), e);
						return false;
					}					
				}			
				
				if (!Util.isEmpty(retValue.toString()))		//	interrupt on first error
				{
					log.severe (retValue.toString());
					return false;
				}
			}			
			goal.setMeasureActual(amt);
			goal.saveEx(get_TrxName());
		}
		return true;
	}	//	updateUserDefinedGoals
}	//	MMeasure
