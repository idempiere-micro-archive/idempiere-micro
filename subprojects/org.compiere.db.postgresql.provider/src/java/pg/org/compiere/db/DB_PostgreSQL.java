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
 * Portions created by Victor Perez are Copyright (C) 1999-2005 e-Evolution,S.C
 * Contributor(s): Victor Perez                                               *
 *****************************************************************************/
package pg.org.compiere.db;

import org.idempiere.common.db.CConnection;
import org.idempiere.common.db.Database;
import org.idempiere.common.dbPort.Convert;
import org.idempiere.common.exceptions.DBException;
import org.idempiere.common.util.*;
import org.idempiere.icommon.db.AdempiereDatabase;
import org.idempiere.icommon.model.IPO;
import org.jfree.io.IOUtils;
import pg.org.compiere.dbPort.Convert_PostgreSQL;
import software.hsharp.api.icommon.ICConnection;
import software.hsharp.db.postgresql.provider.PgDatabaseSetup;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.RowSet;
import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;

/**
 *  PostgreSQL Database Port
 *
 *  @author      @author     Jorg Janke, Victor Perez
 *  @version    $Id: DB_PostgreSQL.java,v 1.23 2005/03/11 20:29:01 jjanke Exp $
 *  ---
 *  Modifications: removed static references to database connection and instead always
 *  get a new connection from database pool manager which manages all connections
 *                 set rw/ro properties for the connection accordingly.
 *  @author Ashley Ramdass (Posterita)
 */
public class DB_PostgreSQL extends PooledPgDB
{

    private static final String POOL_PROPERTIES = "pool.properties";

	public Convert getConvert() {
		return m_convert;
	}

	/**
	 *  PostgreSQL Database
	 */
	public DB_PostgreSQL()
	{
		super();
	}   //  DB_PostgreSQL


	/** Default Port            */
	public static final int         DEFAULT_PORT = 5432;

	/** Data Source				*/
	private DataSource m_ds = null;

	/** Statement Converter     */
	private Convert_PostgreSQL         m_convert = new Convert_PostgreSQL();
	/** Connection String       */
	private String          m_connection;
	/** Cached Database Name	*/
	private String			m_dbName = null;

    @SuppressWarnings("unused")
	private String				m_userName = null;

    /** Connection String       	*/
	private String          		m_connectionURL;

	/**	Logger			*/
	private static CLogger			log	= CLogger.getCLogger (DB_PostgreSQL.class);

    private static int              m_maxbusyconnections = 0;

    public static final String NATIVE_MARKER = "NATIVE_"+Database.DB_POSTGRESQL+"_KEYWORK";

    private CCache<String, String> convertCache = new CCache<String, String>(null, "DB_PostgreSQL_Convert_Cache", 1000, 0, false);

    private Random rand = new Random();

	/**
	 *  Get Database Name
	 *  @return database short name
	 */
	public String getName()
	{
		return Database.DB_POSTGRESQL;
	}   //  getName

	/**
	 *  Get Database Description
	 *  @return database long name and version
	 */
	public String getDescription()
	{       //begin vpj-cd e-evolution 30.09.2005
		//return s_driver.toString();
		java.sql.Driver s_driver = null;
        try
		{
			s_driver = getDriver();
		}
		catch (Exception e)
		{
		}
		if (s_driver != null)
			return s_driver.toString();
		return "No Driver";
                //end vpj-cd e-evolution 30.09.2005
	}   //  getDescription

	/**
	 *  Get Standard JDBC Port
	 *  @return standard port
	 */
	public int getStandardPort()
	{
		return DEFAULT_PORT;
	}   //  getStandardPort

	/**
	 *  Get Database Connection String.
	 *  Requirements:
	 *      - createdb -E UNICODE compiere
	 *  @param connection Connection Descriptor
	 *  @return connection String
	 */
	public String getConnectionURL (ICConnection connection)
	{
		//  jdbc:postgresql://hostname:portnumber/databasename?encoding=UNICODE
		StringBuilder sb = new StringBuilder("jdbc:postgresql://")
			.append(connection.getDbHost())
			.append(":").append(connection.getDbPort())
			.append("/").append(connection.getDbName())
			.append("?encoding=UNICODE");

		String urlParameters = System.getProperty("org.idempiere.postgresql.URLParameters");
	    if (!Util.isEmpty(urlParameters)) {
   			sb.append("&").append(urlParameters);  
	    }

		m_connection = sb.toString();
		return m_connection;
	}   //  getConnectionString

	/**
	 * 	Get Connection URL
	 *	@param dbHost db Host
	 *	@param dbPort db Port
	 *	@param dbName sb Name
	 *	@param userName user name
	 *	@return connection url
	 */
	public String getConnectionURL (String dbHost, int dbPort, String dbName,
		String userName)
	{
		StringBuilder sb = new StringBuilder("jdbc:postgresql://")
			.append(dbHost)
			.append(":").append(dbPort)
			.append("/").append(dbName);

		String urlParameters = System.getProperty("org.idempiere.postgresql.URLParameters") ;
	    if (!Util.isEmpty(urlParameters)) {
			sb.append("?").append(urlParameters);  
		}

		return sb.toString();
	}	//	getConnectionURL

        	/**
	 *  Get Database Connection String
	 *  @param connectionURL Connection URL
	 *  @param userName user name
	 *  @return connection String
	 */
	public String getConnectionURL (String connectionURL, String userName)
	{
		m_userName = userName;
		m_connectionURL = connectionURL;
		return m_connectionURL;
	}	//	getConnectionURL

	/**
	 * 	Get JDBC Catalog
	 *	@return catalog (database name)
	 */
	public String getCatalog()
	{
		if (m_dbName != null)
			return m_dbName;
	//	log.severe("Database Name not set (yet) - call getConnectionURL first");
		return null;
	}	//	getCatalog

	/**
	 * 	Get JDBC Schema
	 *	@return schema (dbo)
	 */
	public String getSchema()
	{
		//begin vpj-cd e-evolution 03/04/2005
		return "adempiere";
		//end vpj-cd e-evolution 03/04/2005
	}	//	getSchema

	/**
	 *  Supports BLOB
	 *  @return true if BLOB is supported
	 */
	public boolean supportsBLOB()
	{
		return true;
	}   //  supportsBLOB



	/*************************************************************************
	 *  Convert an individual Oracle Style statements to target database statement syntax
	 *
	 *  @param oraStatement
	 *  @return converted Statement
	 *  @throws Exception
	 */
	public String convertStatement (String oraStatement)
	{
		String cache = convertCache.get(oraStatement);
		if (cache != null) {
			Convert.logMigrationScript(oraStatement, cache);
			if ("true".equals(System.getProperty("org.idempiere.common.db.postgresql.debug"))) {
				// log.warning("Oracle -> " + oraStatement);
				log.warning("Pgsql  -> " + cache);
			}
			return cache;
		}

		String retValue[] = m_convert.convert(oraStatement);

        //begin vpj-cd e-evolution 03/14/2005
		if (retValue == null || retValue.length == 0 )
			return  oraStatement;
        //end vpj-cd e-evolution 03/14/2005

		if (retValue.length != 1)
			//begin vpj-cd 24/06/2005 e-evolution
			{
			log.log(Level.SEVERE, ("DB_PostgreSQL.convertStatement - Convert Command Number=" + retValue.length
				+ " (" + oraStatement + ") - " + m_convert.getConversionError()));
			throw new IllegalArgumentException
				("DB_PostgreSQL.convertStatement - Convert Command Number=" + retValue.length
					+ " (" + oraStatement + ") - " + m_convert.getConversionError());
			}
			//end vpj-cd 24/06/2005 e-evolution

		convertCache.put(oraStatement, retValue[0]);

		//  Diagnostics (show changed, but not if AD_Error
		if (log.isLoggable(Level.FINE))
		{
			if (!oraStatement.equals(retValue[0]) && retValue[0].indexOf("AD_Error") == -1)
			{
				//begin vpj-cd 24/06/2005 e-evolution
				if (log.isLoggable(Level.FINE))log.log(Level.FINE, "PostgreSQL =>" + retValue[0] + "<= <" + oraStatement + ">");
			}
		}
		    //end vpj-cd 24/06/2005 e-evolution
		//
    	Convert.logMigrationScript(oraStatement, retValue[0]);
		return retValue[0];
	}   //  convertStatement


	/**
	 *  Get Name of System User
	 *  @return e.g. sa, system
	 */
	public String getSystemUser()
	{
		return "postgres";
	}	//	getSystemUser

	/**
	 *  Get Name of System Database
	 *  @param databaseName database Name
	 *  @return e.g. master or database Name
	 */
	public String getSystemDatabase(String databaseName)
	{
		return "template1";
	}	//	getSystemDatabase


	/**
	 *  Create SQL TO Date String from Timestamp
	 *
	 *  @param  time Date to be converted
	 *  @param  dayOnly true if time set to 00:00:00
	 *
	 *  @return TO_DATE('2001-01-30 18:10:20',''YYYY-MM-DD HH24:MI:SS')
	 *      or  TO_DATE('2001-01-30',''YYYY-MM-DD')
	 */
	public String TO_DATE (Timestamp time, boolean dayOnly)
	{
		if (time == null)
		{
			if (dayOnly)
				return "current_date()";
			return "current_date()";
		}

		StringBuilder dateString = new StringBuilder("TO_DATE('");
		//  YYYY-MM-DD HH24:MI:SS.mmmm  JDBC Timestamp format
		String myDate = time.toString();
		if (dayOnly)
		{
			dateString.append(myDate.substring(0,10));
			dateString.append("','YYYY-MM-DD')");
		}
		else
		{
			dateString.append(myDate.substring(0, myDate.indexOf('.')));	//	cut off miliseconds
			dateString.append("','YYYY-MM-DD HH24:MI:SS')");
		}
		return dateString.toString();
	}   //  TO_DATE

	// TODO DAP
	public final static int REFERENCE_DATATYPE_DATE = 15;
	public final static int REFERENCE_DATATYPE_DATETIME = 16;
	public final static int REFERENCE_DATATYPE_TIME = 24;
	public static final int Date       = REFERENCE_DATATYPE_DATE;
	/** Display Type 16	DateTime	*/
	public static final int DateTime   = REFERENCE_DATATYPE_DATETIME;
	/** Display Type 24	Time	*/
	public static final int Time       = REFERENCE_DATATYPE_TIME;


	/**
	 *	Returns true if DisplayType is a Date.
	 *  (stored as Timestamp)
	 *  @param displayType Display Type
	 *  @return true if date
	 */
	public static boolean DisplayType_isDate (int displayType)
	{
		if (displayType == Date || displayType == DateTime || displayType == Time)
			return true;
				
		return false;
	}	//	isDate

	/**
	 *  Create SQL for formatted Date, Number
	 *
	 *  @param  columnName  the column name in the SQL
	 *  @param  displayType Display Type
	 *  @param  AD_Language 6 character language setting (from Env.LANG_*)
	 *
	 *  @return TRIM(TO_CHAR(columnName,'999G999G999G990D00','NLS_NUMERIC_CHARACTERS='',.'''))
	 *      or TRIM(TO_CHAR(columnName,'TM9')) depending on DisplayType and Language
	 *
	 **/
	public String TO_CHAR (String columnName, int displayType, String AD_Language)
	{
		StringBuilder retValue = null;
		if (DisplayType_isDate(displayType)) {
			retValue = new StringBuilder("TO_CHAR(");
	        retValue.append(columnName)
	        	.append(",'")
            	.append(Language.getLanguage(AD_Language).getDBdatePattern())
            	.append("')");	        
		} else {
			retValue = new StringBuilder("CAST (");
			retValue.append(columnName);
			retValue.append(" AS Text)");
		}
		return retValue.toString();
		//  Numbers
		/*
		if (DisplayType.isNumeric(displayType))
		{
			if (displayType == DisplayType.Amount)
				retValue.append(" AS TEXT");
			else
				retValue.append(" AS TEXT");
			//if (!Language.isDecimalPoint(AD_Language))      //  reversed
			//retValue.append(",'NLS_NUMERIC_CHARACTERS='',.'''");
		}
		else if (DisplayType.isDate(displayType))
		{
			retValue.append(",'")
				.append(Language.getLanguage(AD_Language).getDBdatePattern())
				.append("'");
		}
		retValue.append(")");
		//*/		
	}   //  TO_CHAR

	// TODO DAP
	public final static int REFERENCE_DATATYPE_AMOUNT = 12;
	public final static int REFERENCE_DATATYPE_NUMBER = 22;
	public final static int REFERENCE_DATATYPE_COSTPRICE = 37;
	public final static int REFERENCE_DATATYPE_QUANTITY = 29;
	/** Display Type 12	Amount	*/
	public static final int Amount     = REFERENCE_DATATYPE_AMOUNT;
	/** Display Type 22	Number	*/
	public static final int Number     = REFERENCE_DATATYPE_NUMBER;
	/** Display Type 37	CostPrice	*/
	public static final int CostPrice  = REFERENCE_DATATYPE_COSTPRICE;
	/** Display Type 29	Quantity	*/
	public static final int Quantity   = REFERENCE_DATATYPE_QUANTITY;
	/**
	 * 	Get Default Precision.
	 * 	Used for databases who cannot handle dynamic number precision.
	 *	@param displayType display type
	 *	@return scale (decimal precision)
	 */
	public static int DisplayType_getDefaultPrecision(int displayType)
	{
		if (displayType == Amount)
			return 2;
		if (displayType == Number)
			return 6;
		if (displayType == CostPrice
			|| displayType == Quantity)
			return 4;
				
		return 0;
	}	//	getDefaultPrecision
	

	/**
	 * 	Return number as string for INSERT statements with correct precision
	 *	@param number number
	 *	@param displayType display Type
	 *	@return number as string
	 */
	public String TO_NUMBER (BigDecimal number, int displayType)
	{
		if (number == null)
			return "NULL";
		BigDecimal result = number;
		int scale = DisplayType_getDefaultPrecision(displayType);
		if (scale > number.scale())
		{
			try
			{
				result = number.setScale(scale, BigDecimal.ROUND_HALF_UP);
			}
			catch (Exception e)
			{
			//	log.severe("Number=" + number + ", Scale=" + " - " + e.getMessage());
			}
		}
		return result.toString();
	}	//	TO_NUMBER


	/**
	 * 	Get SQL Commands
	 *	@param cmdType CMD_*
	 *	@return array of commands to be executed
	 */
	public String[] getCommands (int cmdType)
	{
		if (AdempiereDatabase.CMD_CREATE_USER == cmdType)
			return new String[]
			{
			"CREATE USER adempiere;",
			};
		//
		if (AdempiereDatabase.CMD_CREATE_DATABASE == cmdType)
			return new String[]
			{
		    "CREATE DATABASE adempiere OWNER adempiere;",
			"GRANT ALL PRIVILEGES ON adempiere TO adempiere;"	,
			"CREATE SCHEMA adempiere;",
			"SET search_path TO adempiere;"
			};
		//
		if (AdempiereDatabase.CMD_DROP_DATABASE == cmdType)
			return new String[]
			{
			"DROP DATABASE adempiere;"
			};
		//
		return null;
	}	//	getCommands


	/**************************************************************************
	 *  Get RowSet
	 * 	@param rs ResultSet
	 *  @return RowSet
	 *  @throws SQLException
	 */
	public RowSet getRowSet (java.sql.ResultSet rs) throws SQLException
	{
		throw new UnsupportedOperationException("PostgreSQL does not support RowSets");
	}	//	getRowSet


	/**
	 * 	Get Cached Connection
	 *	@param connection connection
	 *	@param autoCommit auto commit
	 *	@param transactionIsolation trx isolation
	 *	@return Connection
	 *	@throws Exception
	 */
    public Connection getCachedConnection (ICConnection connection,
                                           boolean autoCommit, int transactionIsolation)
            throws Exception
    {
        Connection conn = null;
        Exception exception = null;
        try
        {
            if (m_ds == null)
                getDataSource(connection);

            //
            try
            {
                int numConnections = getNumBusyConnections();
                if(numConnections >= m_maxbusyconnections && m_maxbusyconnections > 0)
                {
                    //system is under heavy load, wait between 20 to 40 seconds
                    int randomNum = rand.nextInt(40 - 20 + 1) + 20;
                    Thread.sleep(randomNum * 1000);
                }

                conn = m_ds.getConnection();
                if (conn == null) {
                    //try again after 10 to 30 seconds
                    int randomNum = rand.nextInt(30 - 10 + 1) + 10;
                    Thread.sleep(randomNum * 1000);
                    conn = m_ds.getConnection();
                }

                if (conn != null)
                {
                    if (conn.getTransactionIsolation() != transactionIsolation)
                        conn.setTransactionIsolation(transactionIsolation);
                    if (conn.getAutoCommit() != autoCommit)
                        conn.setAutoCommit(autoCommit);
                }
            }
            catch (Exception e)
            {
                exception = e;
                conn = null;
            }

            if (conn == null && exception != null)
            {
                //log might cause infinite loop since it will try to acquire database connection again
            	/*
                log.log(Level.SEVERE, exception.toString());
                log.fine(toString()); */
                System.err.println(exception.toString());
            }
        }
        catch (Exception e)
        {
            exception = e;
        }

        try
        {
            if (conn != null) {
                boolean trace = "true".equalsIgnoreCase(System.getProperty("org.adempiere.db.traceStatus"));
                int numConnections = getNumBusyConnections();
                if (numConnections > 1)
                {
                    if (trace)
                    {
                        log.warning(getStatus());
                    }
                    if(numConnections >= m_maxbusyconnections && m_maxbusyconnections > 0)
                    {
                        if (!trace)
                            log.warning(getStatus());
                        //hengsin: make a best effort to reclaim leak connection
                        Runtime.getRuntime().runFinalization();
                    }
                }
            } else {
                //don't use log.severe here as it will try to access db again
                System.err.println("Failed to acquire new connection. Status=" + getStatus());
            }
        }
        catch (Exception ex)
        {
        }
        if (exception != null)
            throw exception;
        return conn;
    }	//	getCachedConnection

	private String getFileName ()
	{
		//
		String base = null;
		if (Ini.getIni().isClient())
			base = System.getProperty("user.home");
		else
			base = Ini.getIni().getAdempiereHome();
		
		if (base != null && !base.endsWith(File.separator))
			base += File.separator;
		
		//
		return base + getName() + File.separator + POOL_PROPERTIES;
	}	//	getFileName

	protected URL getServerPoolDefaultPropertiesUrl() { return null; }

	/**
	 * 	Create DataSource (Client)
	 *	@param connection connection
	 *	@return data dource
	 */
	public DataSource getDataSource(ICConnection connection)
	{
		if (m_ds != null)
			return m_ds;

		InputStream inputStream = null;
		
		//check property file from home
		String propertyFilename = getFileName();
		File propertyFile = null;
		if (!Util.isEmpty(propertyFilename))
		{
			propertyFile = new File(propertyFilename);
			if (propertyFile.exists() && propertyFile.canRead())
			{
				try {
					inputStream = new FileInputStream(propertyFile);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
		
		//fall back to default config
		URL url = null;
		if (inputStream == null)
		{
			propertyFile = null;

	    	try {
				url = getServerPoolDefaultPropertiesUrl();
				if (url != null) { inputStream = url.openStream(); }
			} catch (Exception e) {
				e.printStackTrace();
			}						
		}
		
		Properties poolProperties = new Properties();
		try {
			if (inputStream != null) {
				poolProperties.load(inputStream);
				inputStream.close();
				inputStream = null;
			}
		} catch (IOException e) {
			throw new DBException(e);
		}

		//auto create property file at home folder from default config
		if (propertyFile == null)			
		{
			String directoryName = propertyFilename.substring(0,  propertyFilename.length() - (POOL_PROPERTIES.length()+1));
			File dir = new File(directoryName);
			if (!dir.exists())
				dir.mkdir();
			propertyFile = new File(propertyFilename);
			try {
				FileOutputStream fos = new FileOutputStream(propertyFile);
				if (url != null) {
					inputStream = url.openStream();
					IOUtils.getInstance().copyStreams(inputStream, fos);
					fos.close();
					inputStream.close();
					inputStream = null;
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		if (inputStream != null)
		{
			try {
				inputStream.close();
			} catch (IOException e) {}
		}

		int connectionTimeout = getIntProperty(poolProperties, "ConnectionTimeout", 5);
        int validationTimeout = getIntProperty(poolProperties, "ValidationTimeout", 1);
        int idleTimeout = getIntProperty(poolProperties, "IdleTimeout", 1*60);
        int leakDetectionThreshold = getIntProperty(poolProperties, "LeakDetectionThreshold", 60);
        int maxLifetime = getIntProperty(poolProperties, "MaxLifetime", 9*60);


		int idleConnectionTestPeriod = getIntProperty(poolProperties, "IdleConnectionTestPeriod", 20*60);
		int acquireRetryAttempts = getIntProperty(poolProperties, "AcquireRetryAttempts", 2);
		int maxIdleTimeExcessConnections = getIntProperty(poolProperties, "MaxIdleTimeExcessConnections", 20*60);
		int maxIdleTime = getIntProperty(poolProperties, "MaxIdleTime", 20*60);
		int unreturnedConnectionTimeout = getIntProperty(poolProperties, "UnreturnedConnectionTimeout", 0);
		boolean testConnectionOnCheckin = getBooleanProperty(poolProperties, "TestConnectionOnCheckin", false);
		boolean testConnectionOnCheckout = getBooleanProperty(poolProperties, "TestConnectionOnCheckout", false);
		String mlogClass = getStringProperty(poolProperties, "com.mchange.v2.log.MLog", "com.mchange.v2.log.FallbackMLog");
		
		int checkoutTimeout = getIntProperty(poolProperties, "CheckoutTimeout", 0);

        try
        {
            System.setProperty("com.mchange.v2.log.MLog", mlogClass);
            //System.setProperty("com.mchange.v2.log.FallbackMLog.DEFAULT_CUTOFF_LEVEL", "ALL");

			int maxPoolSize = getIntProperty(poolProperties, "MaxPoolSize", 400);
			int initialPoolSize = getIntProperty(poolProperties, "InitialPoolSize", 10);
			int minPoolSize = getIntProperty(poolProperties, "MinPoolSize", 50);
			m_maxbusyconnections = (int) (maxPoolSize * 0.9);
			
			//statement pooling
			int maxStatementsPerConnection = getIntProperty(poolProperties, "MaxStatementsPerConnection", 0);

            PgDatabaseSetup defaults = new PgDatabaseSetup();
            PgDatabaseSetup params = new PgDatabaseSetup(
                    connectionTimeout * 1000, //val connectionTimeout: Long
                    validationTimeout * 1000, //val validationTimeout: Long
                    idleTimeout * 1000, //val idleTimeout: Long
                    leakDetectionThreshold * 1000, //val leakDetectionThreshold: Long
                    maxLifetime * 1000, //val maxLifetime: Long
                    maxPoolSize, //val maximumPoolSize: Int
                    minPoolSize, //val minimumIdle: Int
                    defaults.getCachePrepStmts(), //val cachePrepStmts: Boolean
                    defaults.getPrepStmtCacheSize(), //val prepStmtCacheSize: Int
                    defaults.getPrepStmtCacheSqlLimit() //val prepStmtCacheSqlLimit: Int

            );
            setup( params );

            m_ds = connect( connection );
            m_connectionURL = getJdbcUrl();
        }
        catch (Exception ex)
        {
            m_ds = null;
            log.log(Level.SEVERE, "Could not initialise Hikari Datasource", ex);
        }

		return m_ds;
	}

	/**
	 * 	Create Pooled DataSource (Server)
	 *	@param connection connection
	 *	@return data source
	 */
	public ConnectionPoolDataSource createPoolDataSource(CConnection connection)
	{
		throw new UnsupportedOperationException("Not supported/implemented");
	}

	/**
	 * 	Get Connection from Driver
	 *	@param connection info
	 *	@return connection or null
	 */
	public Connection getDriverConnection (ICConnection connection) throws SQLException
	{
		getDriver();
		return DriverManager.getConnection (getConnectionURL (connection),
			connection.getDbUid(), connection.getDbPwd());
	}	//	getDriverConnection

	/**
	 * 	Get Driver Connection
	 *	@param dbUrl URL
	 *	@param dbUid user
	 *	@param dbPwd password
	 *	@return connection
	 *	@throws SQLException
	 */
	public Connection getDriverConnection (String dbUrl, String dbUid, String dbPwd)
		throws SQLException
	{
		getDriver();
		return DriverManager.getConnection (dbUrl, dbUid, dbPwd);
	}	//	getDriverConnection


	/**
	 *  Check and generate an alternative SQL
	 *  @reExNo number of re-execution
	 *  @msg previous execution error message
	 *  @sql previous executed SQL
	 *  @return String, the alternative SQL, null if no alternative
	 */
	public String getAlternativeSQL(int reExNo, String msg, String sql)
	{
		return null; //do not do re-execution of alternative SQL
	}

        	/**
	 *  Get constraint type associated with the index
	 *  @tableName table name
	 *  @IXName Index name
	 *  @return String[0] = 0: do not know, 1: Primary Key  2: Foreign Key
	 *  		String[1] - String[n] = Constraint Name
	 */
	public String getConstraintType(Connection conn, String tableName, String IXName)
	{
		if (IXName == null || IXName.length()==0)
			return "0";
		if (IXName.toUpperCase().endsWith("_KEY"))
			return "1"+IXName;
		else
			return "0";
		//jz temp, modify later from user.constraints
	}

    /**
	 *  Check if DBMS support the sql statement
	 *  @sql SQL statement
	 *  @return true: yes
	 */
	public boolean isSupported(String sql)
	{
		return true;
		//jz temp, modify later
	}

    /**
     * 	Close
     */
    public void close()
    {

        super.close();
        m_ds = null;
    }	//	close

	/**
	 * Dump table lock info to console for current transaction
	 * @param conn
	 */
	public static void dumpLocks(Connection conn)
	{
		Statement stmt = null;
		ResultSet rs = null;
		try {
			String sql = "select pg_class.relname,pg_locks.* from pg_class,pg_locks where pg_class.relfilenode=pg_locks.relation order by 1";
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			int cnt = rs.getMetaData().getColumnCount();
			System.out.println();
			while (rs.next())
			{
				for(int i = 0; i < cnt; i++)
				{
					Object value = rs.getObject(i+1);
					if (i > 0)
						System.out.print(", ");
					System.out.print(value != null ? value.toString() : "");
				}
				System.out.println();
			}
			System.out.println();
		} catch (Exception e) {

		} finally {
			DB.close(rs,stmt);
			rs = null;stmt = null;
		}
	}

	public int getNextID(String name) {

		int m_sequence_id = DB.getSQLValueEx(null, "SELECT nextval('"+name.toLowerCase()+"')");
		return m_sequence_id;
	}

	public boolean createSequence(String name , int increment , int minvalue , int maxvalue ,int  start, String trxName)
	{
		// Check if Sequence exists
		final int cnt = DB.getSQLValueEx(trxName, "SELECT COUNT(*) FROM pg_class WHERE UPPER(relname)=? AND relkind='S'", name.toUpperCase());
		final int no;
		if (start < minvalue)
			start = minvalue;
		//
		// New Sequence
		if (cnt == 0)
		{
			no = DB.executeUpdate("CREATE SEQUENCE "+name.toUpperCase()
								+ " INCREMENT BY " + increment
								+ " MINVALUE " + minvalue
								+ " MAXVALUE " + maxvalue
								+ " START WITH " + start, trxName);
		}
		//
		// Already existing sequence => ALTER
		else
		{
			no = DB.executeUpdate("ALTER SEQUENCE "+name.toUpperCase()
					+ " INCREMENT BY " + increment
					+ " MINVALUE " + minvalue
					+ " MAXVALUE " + maxvalue
					+ " RESTART WITH " + start, trxName);
		}
		if(no == -1 )
			return false;
		else
			return true;
	}

	/**
	 * Postgresql jdbc driver doesn't support setQueryTimeout, query/statement timeout is
	 * implemented using the "SET LOCAL statement_timeout TO" statement.
	 * @return true
	 */
	public boolean isQueryTimeoutSupported() {
		return true;
	}

	/**
	 * Implemented using the limit and offset feature. use 1 base index for start and end parameter
	 * @param sql
	 * @param start
	 * @param end
	 */
	public String addPagingSQL(String sql, int start, int end) {
		String newSql = sql + " " + NATIVE_MARKER + "LIMIT " + ( end - start + 1 )
			+ "  " + NATIVE_MARKER + "OFFSET " + (start - 1);
		return newSql;
	}

	public boolean isPagingSupported() {
		return true;
	}

	private int getIntProperty(Properties properties, String key, int defaultValue)
	{
		int i = defaultValue;
		try
		{
			String s = properties.getProperty(key);
			if (s != null && s.trim().length() > 0)
				i = Integer.parseInt(s);
		}
		catch (Exception e) {}
		return i;
	}

	private boolean getBooleanProperty(Properties properties, String key, boolean defaultValue)
	{
		boolean b = defaultValue;
		try
		{
			String s = properties.getProperty(key);
			if (s != null && s.trim().length() > 0)
				b = Boolean.valueOf(s);
		}
		catch (Exception e) {}
		return b;
	}
	
	private String getStringProperty(Properties properties, String key, String defaultValue)
	{
		String b = defaultValue;
		try
		{
			String s = properties.getProperty(key);
			if (s != null && s.trim().length() > 0)
				b = s.trim();
		}
		catch (Exception e) {}
		return b;
	}

	public boolean forUpdate(IPO po, int timeout) {
    	//only can lock for update if using trx
    	if (po.get_TrxName() == null)
    		return false;
    	
    	String[] keyColumns = po.get_KeyColumns();
		if (keyColumns != null && keyColumns.length > 0 && !po.is_new()) {
			StringBuilder sqlBuffer = new StringBuilder(" SELECT ");
			sqlBuffer.append(keyColumns[0])
				.append(" FROM ")
				.append(po.get_TableName())
				.append(" WHERE ");
			for(int i = 0; i < keyColumns.length; i++) {
				if (i > 0)
					sqlBuffer.append(" AND ");
				sqlBuffer.append(keyColumns[i]).append("=?");
			}
			sqlBuffer.append(" FOR UPDATE ");

			Object[] parameters = new Object[keyColumns.length];
			for(int i = 0; i < keyColumns.length; i++) {
				Object parameter = po.get_Value(keyColumns[i]);
				if (parameter != null && parameter instanceof Boolean) {
					if ((Boolean) parameter)
						parameter = "Y";
					else
						parameter = "N";
				}
				parameters[i] = parameter;
			}

			PreparedStatement stmt = null;
			ResultSet rs = null;
			try {
				stmt = DB.prepareStatement(sqlBuffer.toString(),
					ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, po.get_TrxName());
				for(int i = 0; i < keyColumns.length; i++) {
					stmt.setObject(i+1, parameters[i]);
				}
				stmt.setQueryTimeout(timeout > 0 ? timeout : AdempiereDatabase.LOCK_TIME_OUT);
				
				rs = stmt.executeQuery();
				if (rs.next()) {
					return true;
				} else {
					return false;
				}
			} catch (Exception e) {
				if (log.isLoggable(Level.INFO))log.log(Level.INFO, e.getLocalizedMessage(), e);
				throw new DBException("Could not lock record for " + po.toString() + " caused by " + e.getLocalizedMessage());
			} finally {
				DB.close(rs, stmt);
				rs = null;stmt = null;
			}			
		}
		return false;
	}
	
	public String getNameOfUniqueConstraintError(Exception e) {
		String info = e.getMessage();
		int fromIndex = info.indexOf("\"");
		if (fromIndex == -1)
			fromIndex = info.indexOf("\u00ab"); // quote for spanish postgresql message
		if (fromIndex == -1)
			return info;
		int toIndex = info.indexOf("\"", fromIndex + 1);
		if (toIndex == -1)
			toIndex = info.indexOf("\u00bb", fromIndex + 1);
		if (toIndex == -1)
			return info;
		return info.substring(fromIndex + 1, toIndex);
	}
}   //  DB_PostgreSQL
