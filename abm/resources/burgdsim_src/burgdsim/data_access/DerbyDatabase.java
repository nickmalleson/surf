package burgdsim.data_access;

import java.io.File;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import burgdsim.environment.Cacheable;
import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;

/**
 * Class to utilise the Apache Derby database. This seems ideal for this model because
 * the entire database can be packaged seamlessly with the model although it's apparantly pretty slow.
 * Used the example SimpleApp.java file from the Derby demos.
 * <p>
 * There are two types of driver to use: embedded and client. The client allows multiple simultaneous
 * database connections but the server application must be started. The embedded driver doesn't allow
 * multiple connections but doesn't require a server.
 * @author Nick Malleson
 */
public class DerbyDatabase extends Database implements Cacheable {

	private static boolean usingEmbedded = false;	// Use the embedded driver, if false then use network/client 
	
//	private String framework = "embedded";
	private String driver ;
	private String protocol = "jdbc:derby:";
	private String connectionURL ;
	private Properties properties = null;

	private static boolean firstRun = true; // Some things need to be done if this is the first Derby db object

	/**
	 * Create a new Apache Derby database with the given name or connect to an existing one if it exists.
	 * 
	 * @param databaseName
	 * @throws SQLException  
	 * @throws ClassNotFoundException If the driver class can't be found
	 * @throws InstantiationException If the driver class cannot be instantiated
	 * @throws IllegalAccessException If the driver class cannot be instantiated
	 */
	public DerbyDatabase(String dbName) throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		super(dbName);
		this.createConnection();
	}
	
	/** Create a connection to the database passed to the constructor 
	 * @throws SQLException 
	 * @throws ClassNotFoundException If the driver class can't be found
	 * @throws InstantiationException If the driver class cannot be instantiated
	 * @throws IllegalAccessException If the driver class cannot be instantiated
	 */
	private void createConnection() throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		// Some things might need to be done before the Derby system is run.
		if (DerbyDatabase.firstRun==true) {
			// Need to start the network server (doesn't matter if it is already running, this wont break it).
			// (not starting it here, it must be started manually, see the script in lib directory)
//			if (!DerbyDatabase.usingEmbedded) {
//				startNetworkServer();	
//			}			
		}
		GlobalVars.CACHES.add(this);	//  Need to reset static variables if simulation is restarted
		if (usingEmbedded) {
			this.driver = "org.apache.derby.jdbc.EmbeddedDriver";
			// Set some database properties, see (http://db.apache.org/derby/docs/10.4/tuning/)
			// The system.home property is static, must be set before system loaded, not passed in getConnection()
			System.getProperties().setProperty("derby.system.home", "./data/derby_databases");
			// When using embedded the connection is left null, all databases created under home location
			this.connectionURL = ""; 
		}
		else { // Using the client driver.
			this.driver = "org.apache.derby.jdbc.ClientDriver";
			// Using client need to provide a full path to the database server
			if (GlobalVars.ON_NGS==0) {
				String absPath = (new File("")).getAbsolutePath();			// 
				this.connectionURL = "//localhost:1527/"+absPath+"/data/derby_databases/"; // NOT ON NGS
//				System.out.println("NOT ON NGS");
			}
			else if (GlobalVars.ON_NGS==1) {
				this.connectionURL = "./data/derby_databases/"; // ON NGS
//				System.out.println("ON NGS");
			}
			else {
				Outputter.errorln("DerbyDatabase.createConnection() error: unrecognised ON_NGS value, expecting " +
						"1 or 0 but got: "+GlobalVars.ON_NGS);
			}
		}
		this.properties = new Properties();
		// Username and password don't really matter, but making username 'app' means that new tables etc
		// are created under the same schema whether using embedded or client drives which makes it slightly
		// easier to manage using SquirrelSQL (a GUI).
		this.properties.put("username", "app");
		this.properties.put("password", "app");
		this.loadDriver(); // load the driver (Might not be necessary with Java 6.)
		DerbyDatabase.firstRun=false;

		// Connect to the database, creating it if it does not exist
		String connectionString = this.protocol + this.connectionURL + this.databaseName+ ";create=true";
		try {
			Outputter.debugln("DerbyDatabase: attempting to connect to db using: "+connectionString, Outputter.DEBUG_TYPES.DATA_ACCESS);
			this.connection = DriverManager.getConnection(connectionString, this.properties);
			Outputter.debugln("DerbyDatabase: Created database connection: "+this.connection.toString(), Outputter.DEBUG_TYPES.DATA_ACCESS);
//			// We want to control transactions manually. Autocommit is on by default in JDBC.
//			connection.setAutoCommit(false);
			this.statement = this.connection.createStatement(); // Used for executing SQL statements

		} catch (SQLException e) {
			Outputter.errorln("DerbyDatabase() error trying to connect to a database using connection string: "+connectionString);
			Outputter.errorln(e.getStackTrace());
			throw e;
		}
	}


	public <T> boolean createColumn(String dataFileName, String columnName, Class<T> columnType) {
		Outputter.errorln("DerbyDatabase: not implemented method: createColumn");
		// TODO Auto-generated method stub
		return false;
	}

	public boolean createFile(String dataFileName) {
		Outputter.errorln("DerbyDatabase: not implemented method: createFile");
		// TODO Auto-generated method stub
		return false;
	}

	public boolean createFullTable(String dataFileName, Map<String, Class<?>> columns) {
		Outputter.errorln("DerbyDatabase: not implemented method: createFullTable");
		// TODO Auto-generated method stub
		return false;
	}
	

	/**
	 * Loads the appropriate JDBC driver for this environment/framework (copied directly from Derby
	 * demo code).
	 * @throws ClassNotFoundException If the driver class can't be found
	 * @throws InstantiationException If the driver class cannot be instantiated
	 * @throws IllegalAccessException If the driver class cannot be instantiated
	 */
	private void loadDriver() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		/*
		 *  The JDBC driver is loaded by loading its class.
		 *  If you are using JDBC 4.0 (Java SE 6) or newer, JDBC drivers may
		 *  be automatically loaded, making this code optional.
		 *
		 *  In an embedded environment, this will also start up the Derby
		 *  engine (though not any databases), since it is not already
		 *  running. In a client environment, the Derby engine is being run
		 *  by the network server framework.
		 *
		 *  In an embedded environment, any static Derby system properties
		 *  must be set before loading the driver to take effect.
		 */
		try {
			Class.forName(driver).newInstance();
			Outputter.debugln("DerbyDatabase.loadDriver(): Loaded the appropriate driver", 
					Outputter.DEBUG_TYPES.DATA_ACCESS);
		} catch (ClassNotFoundException e) {
			Outputter.errorln("DerbyDatabase.loadDriver: Unable to load the JDBC driver " + driver);
			Outputter.errorln(e.getStackTrace());
			throw e;
		} catch (InstantiationException e) {
			Outputter.errorln("DerbyDatabase.loadDriver: Unable to instantiate the JDBC driver " + driver);
			Outputter.errorln(e.getStackTrace());
			throw e;
		} catch (IllegalAccessException e) {
			Outputter.errorln("DerbyDatabase.loadDriver: Not allowed to access the JDBC driver " + driver);
			Outputter.errorln(e.getStackTrace());
			throw e;
		}
	}
	
	
	
	/* (non-Javadoc)
	 * @see data_access.Database#close()
	 */
	@Override
	public void close() throws SQLException {
		super.close();
		Outputter.debugln("DerbyDatabase.close(), might want to shut down network server here. Might matter " +
				"on BNG environment if the server doesn't persist after model has stopped running.", 
				Outputter.DEBUG_TYPES.DATA_ACCESS);
//		try {
//			throw new Exception();
//		}
//		catch (Exception e) {
//			e.printStackTrace();
//		}
//		System.exit(0);
	}

	/**
	 * Starts the derby network server, required for client connections to the database.
	 * <p>
	 * This webpage <url>http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html?page=3</url>
	 * ("When Runtime.exec() won't") has useful info about running a command using exec.
	 */
	@SuppressWarnings("unused")
	private void startNetworkServer() {
		try {
			Outputter.debugln("DerbyDatabase starting network server.", Outputter.DEBUG_TYPES.DATA_ACCESS);
			Runtime rt = Runtime.getRuntime();
			String command = "java -cp $CLASSPATH:" +
			"./lib/derby.jar:" +
			"./lib/derbytools.jar:" +
			"./lib/derbynet.jar " +
			"org.apache.derby.drda.NetworkServerControl start";
			//				command = "javac";
			Outputter.debugln("DerbyDatabase starting network server, running command: "+command,
					Outputter.DEBUG_TYPES.DATA_ACCESS);
			//				Process proc = rt.exec(command);
			rt.exec(command);
			// TODO BUG HERE: If the network server is started the program must wait a few seconds otherwise
			// it will try to access the database before it is ready. BUT: the process (proc) above wont return
			// (it doesn't exit) so how to tell when it's ready? NOTE: once model exits the network server
			// continues to run.

			// Direct any output from the command otherwise it can hang.
			//				InputStream stderr = proc.getErrorStream();
			//				InputStream stdout = proc.getInputStream();
			//				InputStreamReader isrErr = new InputStreamReader(stderr);
			//				InputStreamReader isrOut = new InputStreamReader(stdout);
			//				BufferedReader brErr = new BufferedReader(isrErr);
			//				BufferedReader brOut = new BufferedReader(isrOut);
			//				String line = null;
			//				System.out.println("NetworkServer error output: **");
			//				while ( (line = brErr.readLine()) != null)
			//					System.out.println(line);
			//				System.out.println("**");
			//				String outputString = "NetworkServer output: **\n";
			//				while ( (line = brOut.readLine()) != null)
			//					outputString += "\t" + line +"\n";
			//				outputString += "**\n";
			//				int exitVal = proc.waitFor();
			//				outputString+="Network server exitValue: " + exitVal+"\n";
			//				Outputter.debugln(outputString, Outputter.DEBUG_TYPES.DATA_ACCESS);
		} 
		catch (Throwable t)
		{
			t.printStackTrace();
		}
	}




	/**
	 * This is required by Cacheable interface. When the simulation is restarded this function will
	 * be called to reset any static variables which need to be.
	 */
	public void clearCaches() {
		firstRun = true;
		
	}

	/**
	 * Return null, schema isn't necessary with Derby connection.
	 */
	protected String getSchema() {
		return null;
	}
}
