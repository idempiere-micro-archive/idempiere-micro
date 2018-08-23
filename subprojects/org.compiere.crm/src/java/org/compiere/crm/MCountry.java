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
package org.compiere.crm;

import java.io.Serializable;
import java.sql.ResultSet;
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.compiere.model.I_AD_Language;
import org.compiere.model.I_C_Country;
import org.compiere.orm.MClient;
import org.compiere.orm.Query;
import org.compiere.util.SystemIDs;
import org.idempiere.common.util.CCache;
import org.idempiere.common.util.CLogger;
import org.idempiere.common.util.Env;
import org.idempiere.common.util.Language;

/**
 *	Location Country Model (Value Object)
 *
 *  @author 	Jorg Janke
 *  @version 	$Id: MCountry.java,v 1.3 2006/07/30 00:58:18 jjanke Exp $
 *   
 *   * @author Michael Judd (Akuna Ltd)
 * 				<li>BF [ 2695078 ] Country is not translated on invoice
 */
public class MCountry extends X_C_Country
	implements Comparator<Object>, Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -4966707939803861163L;

	/**
	 * 	Get Country (cached)
	 * 	@param ctx context
	 *	@param C_Country_ID ID
	 *	@return Country
	 */
	public static MCountry get (Properties ctx, int C_Country_ID)
	{
		loadAllCountriesIfNeeded(ctx);
		MCountry c = s_countries.get(C_Country_ID);
		if (c != null)
			return c;
		c = new MCountry (ctx, C_Country_ID, null);
		if (c.getC_Country_ID() == C_Country_ID)
		{
			s_countries.put(C_Country_ID, c);
			return c;
		}
		return null;
	}	//	get

	/**
	 * 	Get Default Country
	 * 	@param ctx context
	 *	@return Country
	 */
	public static MCountry getDefault (Properties ctx)
	{
		int clientID = Env.getAD_Client_ID(ctx);
		MCountry c = s_default.get(clientID);
		if (c != null)
			return c;

		loadDefaultCountry(ctx);
		c = s_default.get(clientID);
		return c;
	}	//	get

	/**
	 *	Return Countries as Array
	 * 	@param ctx context
	 *  @return MCountry Array
	 */
	public static MCountry[] getCountries(Properties ctx)
	{
		loadAllCountriesIfNeeded(ctx);
		MCountry[] retValue = new MCountry[s_countries.size()];
		s_countries.values().toArray(retValue);
		Arrays.sort(retValue, new MCountry(ctx, 0, null));
		return retValue;
	}	//	getCountries

	private static synchronized void loadAllCountriesIfNeeded(Properties ctx) {
		if (s_countries == null || s_countries.isEmpty()) {
			loadAllCountries(ctx);
		}
	}
	
	/**
	 * 	Load Countries.
	 * 	Set Default Language to Client Language
	 *	@param ctx context
	 */
	private static void loadAllCountries (Properties ctx)
	{
		MClient client = MClient.get (ctx);
		I_AD_Language lang = MLanguage.get(ctx, client.getAD_Language());
		//
		s_countries = new CCache<Integer,MCountry>(I_C_Country.Table_Name, 250);
		List<MCountry> countries = new Query(ctx, I_C_Country.Table_Name, "", null)
			.setOnlyActiveRecords(true)
			.list();
		for (MCountry c : countries) {
			s_countries.put(c.getC_Country_ID(), c);
			//	Country code of Client Language
			if (lang != null && lang.getCountryCode().equals(c.getCountryCode()))
				s_default.put(client.getAD_Client_ID(), c);
		}
		if (s_log.isLoggable(Level.FINE)) s_log.fine("#" + s_countries.size() 
			+ " - Default=" + s_default);
	}	//	loadAllCountries

	/**
	 * Load Default Country for actual client on context
	 * @param ctx
	 */
	private static void loadDefaultCountry(Properties ctx) {
		loadAllCountriesIfNeeded(ctx);
		MClient client = MClient.get (ctx);
		MCountry found = s_default.get(client.getAD_Client_ID());
		if (found != null)
			return;

		I_AD_Language lang = MLanguage.get(ctx, client.getAD_Language());
		MCountry usa = null;

		for (Entry<Integer, MCountry> cachedEntry : s_countries.entrySet()) {
			MCountry c = cachedEntry.getValue();
			//	Country code of Client Language
			if (lang != null && lang.getCountryCode().equals(c.getCountryCode())) {
				found = c;
				break;
			}
			if (c.getC_Country_ID() == SystemIDs.COUNTRY_US)		//	USA
				usa = c;
		}
		if (found != null)
			s_default.put(client.getAD_Client_ID(), found);
		else
			s_default.put(client.getAD_Client_ID(), usa);
		if (s_log.isLoggable(Level.FINE)) s_log.fine("#" + s_countries.size() 
			+ " - Default=" + s_default);
	}

	/**
	 * 	Set the Language for Display (toString)
	 *	@param AD_Language language or null
	 *  @deprecated - not used at all, you can delete references to this method
	 */
	public static void setDisplayLanguage (String AD_Language)
	{
		s_AD_Language = AD_Language;
		if (Language.isBaseLanguage(AD_Language))
			s_AD_Language = null;
	}	//	setDisplayLanguage
	
	/**	Display Language				*/
	@SuppressWarnings("unused")
	private static String		s_AD_Language = null;
	
	/**	Country Cache					*/
	private static CCache<Integer,MCountry>	s_countries = null;
	/**	Default Country 				*/
	private static CCache<Integer,MCountry>	s_default = new CCache<Integer,MCountry>(I_C_Country.Table_Name, 3);
	/**	Static Logger					*/
	private static CLogger		s_log = CLogger.getCLogger (MCountry.class);
	//	Default DisplaySequence	*/
	private static String		DISPLAYSEQUENCE = "@C@, @P@";

	
	/*************************************************************************
	 *	Create empty Country
	 * 	@param ctx context
	 * 	@param C_Country_ID ID
	 *	@param trxName transaction
	 */
	public MCountry (Properties ctx, int C_Country_ID, String trxName)
	{
		super (ctx, C_Country_ID, trxName);
		if (C_Country_ID == 0)
		{
		//	setName (null);
		//	setCountryCode (null);
			setDisplaySequence(DISPLAYSEQUENCE);
			setHasRegion(false);
			setHasPostal_Add(false);
			setIsAddressLinesLocalReverse (false);
			setIsAddressLinesReverse (false);
		}
	}   //  MCountry

	/**
	 *	Create Country from current row in ResultSet
	 * 	@param ctx context
	 *  @param rs ResultSet
	 *	@param trxName transaction
	 */
	public MCountry (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MCountry

	/**
	 *	Return Name - translated if DisplayLanguage is set.
	 *  @return Name
	 */
	public String toString()
	{
		return getTrlName();
	}   //  toString

	/**
	 * 	Get Translated Name
	 *	@return name
	 */
	@JsonIgnore
	public String getTrlName()
	{
		return getTrlName(Env.getAD_Language(Env.getCtx()));
	}	//	getTrlName
	
	/**
	 * 	Get Translated Name
	 *  @param language 
	 *	@return name
	 */
	public String getTrlName(String language)
	{
		return get_Translation(I_C_Country.COLUMNNAME_Name, language);
	}	//	getTrlName
	
	
	/**
	 * 	Get Display Sequence
	 *	@return display sequence
	 */
	public String getDisplaySequence ()
	{
		String ds = super.getDisplaySequence ();
		if (ds == null || ds.length() == 0)
			ds = DISPLAYSEQUENCE;
		return ds;
	}	//	getDisplaySequence

	/**
	 * 	Get Local Display Sequence.
	 * 	If not defined get Display Sequence
	 *	@return local display sequence
	 */
	public String getDisplaySequenceLocal ()
	{
		String ds = super.getDisplaySequenceLocal();
		if (ds == null || ds.length() == 0)
			ds = getDisplaySequence();
		return ds;
	}	//	getDisplaySequenceLocal
	
	/**
	 *  Compare based on Name
	 *  @param o1 object 1
	 *  @param o2 object 2
	 *  @return -1,0, 1
	 */
	public int compare(Object o1, Object o2)
	{
		String s1 = o1.toString();
		if (s1 == null)
			s1 = "";
		String s2 = o2.toString();
		if (s2 == null)
			s2 = "";
		Collator collator = Collator.getInstance();
		return collator.compare(s1, s2);
	}	//	compare

	/**
	 * 	Is the region valid in the country
	 *	@param C_Region_ID region
	 *	@return true if valid
	 */
	public boolean isValidRegion(int C_Region_ID)
	{
		if (C_Region_ID == 0 
			|| getC_Country_ID() == 0
			|| !isHasRegion())
			return false;
		MRegion[] regions = MRegion.getRegions(getCtx(), getC_Country_ID());
		for (int i = 0; i < regions.length; i++)
		{
			if (C_Region_ID == regions[i].getC_Region_ID())
				return true;
		}
		return false;
	}	//	isValidRegion

}	//	MCountry
