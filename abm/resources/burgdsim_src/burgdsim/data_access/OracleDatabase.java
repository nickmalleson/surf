package burgdsim.data_access;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Map;

import burgdsim.environment.Cacheable;
import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;

/**
 * Will read and write model data stored on an Oracle database, specifically the one on the NGS
 * I have been allocated.
 * @author Nick Malleson
 */
public class OracleDatabase extends Database implements Cacheable {

	private static boolean firstRun = true;
	private volatile Map<String, PreparedStatement> prepStatMap = null; // A cache of preciously created prepared statements

	/**
	 * Create a new Oracle. Note that all oracle data is stored in the same database (unlike with
	 * Derby) so the database name here isn't actually used.
	 * 
	 * @param databaseName
	 * @throws SQLException  
	 * @throws ClassNotFoundException If the driver class can't be found
	 * @throws InstantiationException If the driver class cannot be instantiated
	 * @throws IllegalAccessException If the driver class cannot be instantiated
	 */
	public OracleDatabase(String dbName) throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		super(dbName);
		this.prepStatMap = new Hashtable<String, PreparedStatement>();
		this.createConnection();
	}

	public OracleDatabase(String dbName, Connection connection) throws SQLException {
		super(dbName);
		this.connection = connection;
		this.statement = connection.createStatement();
		this.prepStatMap = new Hashtable<String, PreparedStatement>();
	}

	/** Create a connection to the database passed to the constructor (This will only
	 * create one connection because, when using Oracle on NGS the same database is used
	 * to store everyrthing, unlike with Derby where different databases are used.
	 * @throws SQLException 
	 * @throws ClassNotFoundException If the driver class can't be found
	 * @throws InstantiationException If the driver class cannot be instantiated
	 * @throws IllegalAccessException If the driver class cannot be instantiated
	 */
	private void createConnection() throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		GlobalVars.CACHES.add(this);	//  Need to reset static variables if simulation is restarted
		try {
			if (firstRun) {
				// Dynamically load the oracle driver
				this.loadDriver();
				OracleDatabase.firstRun = false;
			}
			// Create a connection
			Outputter.debugln("OracleDatabase attempting to create connection...", Outputter.DEBUG_TYPES.DATA_ACCESS);
			for (int i=0; i<5; i++) { // try five times to create a connection, oracle a bit dodgy
				try {
					this.connection = DriverManager.getConnection(
							"jdbc:oracle:thin:@(DESCRIPTION=(LOAD_BALANCE=yes)(FAILOVER=ON) (ADDRESS=(PROTOCOL=tcp)(HOST=db1-vip.ngs.rl.ac.uk)(PORT=1521))(ADDRESS=(PROTOCOL=tcp)(HOST=db2-vip.ngs.rl.ac.uk)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=ngs11.esc.rl.ac.uk)(FAILOVER_MODE=(TYPE=SESSION)(METHOD=BASIC))))",
							"ngsdb0050", 		// username
					"n1cksp4ssw0rd"); 	// password
					break; // if we get here then connection was successful (no exception thrown).
				}
				catch (Exception e) {
					Outputter.errorln("Oracle connection failed, trying again (attempt: "+i+")");
					if (i==4) {
						Outputter.errorln("Oracle connection failed 5 times, can't continue.");
						throw new SQLException("OracleDatabase.createConnection() couldn't create a connection.");
					}
				}
			}
			Outputter.debugln("...created database connection", Outputter.DEBUG_TYPES.DATA_ACCESS);

			this.statement = this.connection.createStatement();

			if (GlobalVars.ON_NGS==0) {
			}
			else if (GlobalVars.ON_NGS==1) {
			}
			else {
				Outputter.errorln("OracleDatabase.createConnection() error: unrecognised ON_NGS value, expecting " +
						"1 or 0 but got: "+GlobalVars.ON_NGS);
			}



		} catch (SQLException e) {
			Outputter.errorln("OracleDatabase() error trying to connect to a database: "+e.getMessage());
			Outputter.errorln(e.getStackTrace());
			throw e;
		}
	}

	/**
	 * Overridden version of write values because Oracle doesn't allow multiple inserts in same command, e.g. 
	 * INSERT INTO xx VALUES (yy), (xx), (aa) so can't cache writes. Performes the write using a cache of 
	 * prepared statements instead.
	 * @throws Exception 
	 */
	@Override
	public synchronized <T> void writeValues(T[] values, String dataFile, String[] columns) throws Exception {
		int length = values.length;
		if (length<1 || columns.length!=length) {
			Outputter.errorln("Database.writeValues() error: arrays length is less than 1 or lengths " +
			"aren't equal.");
		}
		for (int a=0; a<2; a++) { // Might try twice to execute query (possible problem first time)
			PreparedStatement ps = null;
			try {
				// Build up sql string to add data to database		
				String insertStatement = ""; // The "INSERT INTO ..." bit of the query 
				insertStatement += "INSERT INTO "+dataFile+" (";
				for (int i=0; i<values.length; i++) {
					if (i==values.length-1)
						insertStatement +=columns[i]; // Don't add comma to end of last value
					else
						insertStatement +=columns[i]+", ";
				}
				insertStatement += ") VALUES (";
				for (int i=0; i<values.length; i++) {
					if (i==values.length-1)
						insertStatement +="?"; // Don't add comma to end of last value
					else
						insertStatement +="?, ";
				}
				insertStatement +=")";

				// See if this insert query has already been created and cached
				ps = this.prepStatMap.get(insertStatement);
				if (ps==null) { // Add the prepared statement to the cache
					ps = this.connection.prepareStatement(insertStatement);
					this.prepStatMap.put(insertStatement, ps);
					Outputter.debugln("OracleDatabase.writeValues() Added new insert statement to cache: "+insertStatement,
							Outputter.DEBUG_TYPES.DATA_ACCESS);
				}
				// Loop over values to be inserted and add them to the prepared statement
				for (int i=0; i<values.length; i++) {
					if (values[i] instanceof Integer) 
						ps.setInt(i+1, (Integer)values[i]); // Need to add 1 because first column is index 1 (not 0)
					else if (values[i] instanceof Double)
						ps.setDouble(i+1, (Double)values[i]);
					else if (values[i] instanceof String)
						ps.setString(i+1, (String)values[i]);
					else {
						String message = "OracleDatabase.writeValues() error, unrecognised type of input value: "+
						values[i]+" (class "+values[i].getClass()+")." ;
						Outputter.errorln(message);
						throw new Exception(message);
					}
				}
				ps.execute();
				break; // leave the for loop if the statement executed successfully
			} // try
			catch (SQLException e) {
				if (a==0) { // If this is the first time round the loop
					Outputter.errorln("OracleDatabase.writeValues() caused an SQL exception when trying to execute " +
							"the prepared statement: "+ps.toString()+". Message: "+e.getMessage()+
					"\nWill try re-opening connection and re-building the statement then executing again, if the " +
					"query runs successfully there won't be any more messages.");
					this.connection.close(); // Close the database connection
					this.prepStatMap.clear(); // Empty all prepared statements
					this.createConnection(); // Reopen the connection
					// Now will go back to top of for loop and try to write values again
				} // if a == 0
				else { // This is the second time round the loop and query still didn't work
					Outputter.errorln("\tQuery still failed, cannot continue.");
					throw new SQLException(e.getMessage());
				}
			} // catch SQLException
		} // for
	} // writeValues

	/**
	 * Loads the appropriate JDBC driver for this environment/framework (copied directly from Oracle
	 * demo code).
	 * @throws ClassNotFoundException If the driver class can't be found
	 * @throws InstantiationException If the driver class cannot be instantiated
	 * @throws IllegalAccessException If the driver class cannot be instantiated
	 */
	private void loadDriver() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			Outputter.debugln("OracleDatabase.loadDriver(): Loaded the appropriate driver", 
					Outputter.DEBUG_TYPES.DATA_ACCESS);
		} catch (ClassNotFoundException e) {
			Outputter.errorln("OracleDatabase.loadDriver: Unable to load the JDBC driver, ClassNotFoundException. " +
					"Message: '"+e.getMessage()+"'");
			Outputter.errorln(e.getStackTrace());
			throw e;
		}
	}

	/* (non-Javadoc)
	 * @see data_access.Database#close()
	 */
	@Override
	public void close() throws SQLException {
		Outputter.debugln("OracleDatabase.close()", Outputter.DEBUG_TYPES.DATA_ACCESS);
		super.close();
	}

	/**
	 * This is required by Cacheable interface. When the simulation is restarded this function will
	 * be called to reset any static variables which need to be.
	 */
	public void clearCaches() {
		firstRun = true;
	}

	/**
	 * Oracle database objects can share the same connection (because always connecting to same database).
	 * This is used by DataAccessFactory to create OracleDatabase objects once the first connection has 
	 * been established.
	 * @return This database's connection
	 */
	public Connection getConnection() {
		return this.connection;
	}

	/**
	 * Return the schema designated for me on the NGS: 'NGSDB0050'
	 */
	protected String getSchema() {
		return "NGSDB0050";
	}
}
