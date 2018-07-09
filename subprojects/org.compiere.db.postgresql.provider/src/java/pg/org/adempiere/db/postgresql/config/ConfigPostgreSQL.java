/******************************************************************************
 * The contents of this file are subject to the   Compiere License  Version 1.1
 * ("License"); You may not use this file except in compliance with the License
 * You may obtain a copy of the License at http://www.idempiere.org/license.html
 * Software distributed under the License is distributed on an  "AS IS"  basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * The Original Code is                  Compiere  ERP & CRM  Business Solution
 * The Initial Developer of the Original Code is Jorg Janke  and ComPiere, Inc.
 * Portions created by Jorg Janke are Copyright (C) 1999-2005 Jorg Janke, parts
 * created by ComPiere are Copyright (C) ComPiere, Inc.;   All Rights Reserved.
 * Portions created by Victor Perez are Copyright (C) 1999-2005 e-Evolution,S.C
 * Contributor(s): Victor Perez
 *****************************************************************************/
package pg.org.adempiere.db.postgresql.config;

import org.adempiere.install.IDatabaseConfig;
import org.idempiere.icommon.db.AdempiereDatabase;
import org.idempiere.common.db.Database;
import org.idempiere.common.util.CLogger;

import java.sql.Connection;

/**
 *	PostgreSQL Configuration
 *
 *  @author Victor Perez e-Evolution
 *  @version $Id: ConfigPostgreSQL.java,v 1.0 2005/01/31 06:08:15 vpj-cd Exp $
 */
public class ConfigPostgreSQL implements IDatabaseConfig
{

	private final static CLogger log = CLogger.getCLogger(ConfigPostgreSQL.class);

	/**
	 * 	ConfigPostgreSQL
	 */
	public ConfigPostgreSQL ()
	{
	}	//	ConfigPostgreSQL

	/** Discovered TNS			*/
	private String[] 			p_discovered = null;

	private AdempiereDatabase p_db = Database.getDatabase(Database.DB_POSTGRESQL);

	/**
	 * 	Discover Databases.
	 * 	To be overwritten by database configs
	 *	@param selected selected database
	 *	@return array of databases
	 */
	public String[] discoverDatabases(String selected)
	{
		if (p_discovered != null)
			return p_discovered;
		p_discovered = new String[]{};
		return p_discovered;
	}	//	discoveredDatabases


	/**
	 * 	Test JDBC Connection to Server
	 * 	@param url connection string
	 *  @param uid user id
	 *  @param pwd password
	 * 	@return true if OK
	 */
	private boolean testJDBC (String url, String uid, String pwd)
	{
		try
		{
			@SuppressWarnings("unused")
			Connection conn = p_db.getDriverConnection(url, uid, pwd);
		}
		catch (Exception e)
		{
			log.severe(e.toString());
			return false;
		}
		return true;
	}	//	testJDBC

	@Override
	public String getDatabaseName(String nativeConnectioName) {
		return nativeConnectioName;
	}

	@Override
	public String getName() {
		return Database.DB_POSTGRESQL;
	}

}	//	ConfigPostgreSQL
