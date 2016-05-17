package burgdsim.data_access;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;

/**
 * Convenience class for talking to databases.
 * @author Nick Malleson
 *
 */
public abstract class Database implements DataAccess {

	protected String databaseName;

	protected volatile Connection connection = null;	// The connection to the database
	protected volatile Statement statement = null;	// The statement used to execute queries

	public static int uniqueIDs = 0;
	public int id;

	// Combine multiple "INSERT INTO ... " lines into a single statement where possible 
	private volatile Map<String, List<String>> writeCache; // (don't need to add to GlobaLVars.CACHES, not static)
	private int cachedQueries = 0;

	public Database(String dbName) {
		this.databaseName = dbName;
		this.id = uniqueIDs++;
		this.writeCache = new Hashtable<String, List<String>>();
		//		this.username = "burgdsimuser";
		//		this.password = "password";
	}
	
	public synchronized <T> void writeValues(T[] values, String dataFile, String[] columns) throws SQLException, Exception {
		int length = values.length;
		if (length<1 || columns.length!=length) {
			Outputter.errorln("Database.writeValues() error: arrays length is less than 1 or lengths " +
			"aren't equal.");
		}
		// Build up sql string to add data to database		
		String insertStatement = ""; // The "INSERT INTO ..." bit of the quesy 
		insertStatement += "INSERT INTO "+dataFile+" (";
		for (int i=0; i<values.length; i++) {
			if (i==values.length-1)
				insertStatement +=columns[i]; // Don't add comma to end of last value
			else
				insertStatement +=columns[i]+", ";
		}
		insertStatement += ") VALUES ";
		// Cache this insert

		String valuesStatement = ""; // The "( ... )" bit of the statement (which contains the values to be inserted)
		valuesStatement+="(";
		for (int i=0; i<values.length; i++) {
			if (i==values.length-1) { // Don't add comma to end of last value
				if (values[i] instanceof String) { // Need to wrap strings in quotes
					String val = (String) values[i];
					valuesStatement += "'"+val+"'"; 
				}
				else {
					valuesStatement+=values[i];
				}
			}
			else { // Do add comma to end of value
				if (values[i] instanceof String) {
					String val = (String) values[i];
					valuesStatement += "'"+val+"', "; 
				}
				else {
					valuesStatement += values[i]+", ";
				}
			}
		}
		valuesStatement+=")";
		// Don't cache writes if the simulation hasn't been initialised, there are some dependancies which
		// will fail if the objects aren't stored in the correct order
		if (GlobalVars.getIteration()<1) {
			String sql =insertStatement+valuesStatement; 
			Outputter.debugln("Database.cacheWrites() writing statement immediately becuase sim hasn't started: '"+
					sql+"'.", Outputter.DEBUG_TYPES.DATA_ACCESS);
			try {
				this.statement.execute(sql);
			}
			catch (SQLException e) {
				Outputter.errorln("Database.writeValues() caused an SQL exception when trying to execute sql: "+sql);
				throw new SQLException(e.getMessage()+" SQL: "+sql);
			}
		} 
		else {
			// Don't execute this statement now, cache it so that insert statements executing on the same table can be combined
			this.cacheWrites(insertStatement, valuesStatement);	
		}

	}

	private void cacheWrites(String insertStatement, String valuesStatement) throws SQLException {		
		if (!this.writeCache.containsKey(insertStatement)) { 
			// Haven't got any matching insert queries, create new entry
			List<String> storedValues = new ArrayList<String>();
			storedValues.add(valuesStatement);
			this.writeCache.put(insertStatement, storedValues);
			//			Outputter.debugln("Database.cacheWrites() Adding new queries to write cache: '"+insertStatement+"', '"+valuesStatement+"'.", 
			//					Outputter.DEBUG_TYPES.DATA_ACCESS);
		}
		else { // Already have an INSERT INTO statement that matches this one
			this.writeCache.get(insertStatement).add(valuesStatement); // existing values queries
			//			Outputter.debugln("Database.cacheWrites() Appending queries to write cache: '"+insertStatement+"', '"+valuesStatement+"'.", 
			//					Outputter.DEBUG_TYPES.DATA_ACCESS);
		}
		if (this.cachedQueries++ > 10000) { // If the cache gets large flush it.
			this.executeCachedWrites();
		}
	}

	private void executeCachedWrites() throws SQLException {
		Outputter.debugln("Database.executeCachedWrites() called...", Outputter.DEBUG_TYPES.DATA_ACCESS);
		for (String key:this.writeCache.keySet()) {
			//			String insertStatement = key;
			String valuesStatement = "";
			for (String v:this.writeCache.get(key)) {
				valuesStatement+=v+",";
			}
			valuesStatement = valuesStatement.substring(0, valuesStatement.length()-1); // Remove last character, will be unwanter ','
			Outputter.debugln("Database.executeCachedWrites() Executing sql: '"+(key+valuesStatement)+"'.", 
					Outputter.DEBUG_TYPES.DATA_ACCESS);
			try {
				this.statement.execute(key+valuesStatement);
			} catch (SQLException e){
				Outputter.errorln("Database.executeCachesWrites() threw an SQLException trying to execute sql:\n"+
						key+valuesStatement);
				Outputter.errorln(e.getStackTrace());
				throw e;
			}
		}
		this.cachedQueries = 0;
		this.writeCache.clear();

	}

	public <T> void writeValue(T value, Class<T> clazz, String dataFile, String column, String row) {
		// TODO implement method
		Outputter.errorln("DerbyDatabase: not implemented method: writeValue");
	}

	public void close() throws SQLException {
		Outputter.debugln("Database.close(), closing connections", Outputter.DEBUG_TYPES.DATA_ACCESS);
//		System.out.println("STACK TRACE:");
//		try {
//			throw new Exception();
//		}
//		catch (Exception e) {
//			e.printStackTrace();
//		}
		try {
			this.executeCachedWrites(); // Clear anything stored in the cache
//			this.connection.commit();	// Commit any pending database changes
			this.connection.close(); 	// Not sure this is necessary, should be closed automatically anyway 


		} catch (SQLException e) {
			Outputter.errorln("Database.close() error, caught SQL Exception: "+e.toString());
			Outputter.errorln(e.getStackTrace());
			throw e;
		} 
	}

	public boolean createCustomFiles(Object obj) throws UnsupportedOperationException, IllegalArgumentException, SQLException {
		String sqlString;
		if (obj instanceof String) {
			sqlString = (String) obj;
		}
		else {
			String error = "Database.createCustomFiles() is expecting a string but got a "+obj.getClass().toString();
			Outputter.errorln(error);
			throw new IllegalArgumentException(error);
		}
		Statement statement = this.connection.createStatement();
		statement.execute(sqlString);
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public synchronized <T, U> T getValue(String dataFile, String resultColumn, Class<T> resultColumnClass, 
			String searchColumn, Class<U> searchColumnClass, U searchValue ) throws SQLException {
		ResultSet rs = null ;
		try {
			SQLObject.DESCRIPTIONS searchValueType = SQLObject.getDescriptionFromClass(searchColumnClass);
			String query = null;
			if (searchValueType.equals(SQLObject.DESCRIPTIONS.NUMBER)) {
				query = "SELECT "+resultColumn+", "+searchColumn+" FROM "+dataFile+" WHERE "+searchColumn+"="+searchValue.toString();
			}
			else if (searchValueType.equals(SQLObject.DESCRIPTIONS.TEXT)) {
				query = "SELECT "+resultColumn+", "+searchColumn+" FROM "+dataFile+" WHERE "+searchColumn+"='"+searchValue.toString()+"'";

			}
			Outputter.debugln("Database.getValue() running query: "+query,	Outputter.DEBUG_TYPES.DATA_ACCESS);
			rs = this.statement.executeQuery(query);

			// Find out what the *types* of the search and result results columns are, check these against expected types
			ResultSetMetaData md = rs.getMetaData();
			// Create SQL objects, these will map check the integer Types match an appropriate java class
			int resultColNum = rs.findColumn(resultColumn); // This is needed later
			SQLObject resultObject = new SQLObject(md.getColumnType(resultColNum));
//			SQLObject searchObj = new SQLObject(md.getColumnType(rs.findColumn(searchColumn)));
//			System.out.println("TEST: "+resultObject.getTheClass()+": "+md.getColumnType(resultColNum));
			// TODO Checking types doesn't work, something to do with Postgres hacks (see below)
//			System.out.println("HERE: "+resultObject.getTheClass().toString()+", "+resultColumnClass.getClass().toString());
//			if (!resultObject.getTheClass().equals(resultColumnClass))
//				Outputter.errorln("Database.getValue(): Expecte Results object type ("+resultColumnClass.toString()+
//						") doesn't match database type: "+resultObject.getTheClass()+" ("+resultObject.getSQLTypeAsString()+")" );
//			if (!searchObj.getTheClass().equals(searchColumnClass))
//				Outputter.errorln("Database.getValue(): Found following value: ");
			
			// Types are ok, now get the value from the results set
			if (!rs.next()) {
				Outputter.errorln("Database.getValue(): error, no results returned from query: "+query);
				return null;
			}
			// Pointer is now on first row of results set (there should only be one row).
			T returnValue = (T) rs.getObject(resultColNum);
			// TODO Here look for BigDecimal objects and convert them to Doubles or Integers
			// (hack for a problem with Oracle database which return values as BigDecimals)
			if (returnValue instanceof BigDecimal) {
				BigDecimal retVal = (BigDecimal)returnValue;
				Outputter.debug("Database.getValue() found a BigDecimal "+retVal.toString()+
						" (probably using an Oracle database)", Outputter.DEBUG_TYPES.DATA_ACCESS);
				if (resultColumnClass.equals(Double.class)) {
					Outputter.debugln("Returning double: "+retVal.doubleValue(), Outputter.DEBUG_TYPES.DATA_ACCESS);
					return (T)((Double)(retVal).doubleValue());
				}
				else if (resultColumnClass.equals(Integer.class)) {
					Integer i = Integer.parseInt(retVal.toString());
					Outputter.debugln("Returning integer: "+i, Outputter.DEBUG_TYPES.DATA_ACCESS);
					return (T) i;
				}
				else {
					Outputter.debugln("Haven't hacked what to do if I get a BigDecimal from the database " +
							"but looking for a: "+resultObject.getTheClass(), Outputter.DEBUG_TYPES.DATA_ACCESS);
				}
			}
			if (rs.next()) {
				Outputter.errorln("Database.getValue(), error, more than one result returned, should "+
						"have only got 1. The unique identifier: "+searchColumn+" doesn't uniquely identify " +
				"the row.");
			}
			Outputter.debugln("Database.getValue(): Found following value: "+returnValue.toString()+
					" of type "+returnValue.getClass(), Outputter.DEBUG_TYPES.DATA_ACCESS);
			return returnValue;	

		} catch (SQLException e) {
			Outputter.errorln("Database. getValue() throw an SQL Exception: "+e.getMessage());
			Outputter.errorln(e.getStackTrace());
			throw e;
		}
	}
	
	
	public <T> List<Double> getDoubleValues(String dataFile, List<String> resultColumns, 
			String searchColumn, Class<T> searchColumnClass, String searchValue) throws SQLException {
		ResultSet rs = null ;
		String query = null;
		try {
			SQLObject.DESCRIPTIONS searchValueType = SQLObject.getDescriptionFromClass(searchColumnClass);
			// Construct an sql query to get all values from the result columns
			query = "SELECT ";
			for (String resultColumn:resultColumns) {
				query += resultColumn+", ";
			}
			query = query.substring(0, query.length()-2); // remove last comma
			query += " FROM " +dataFile + " WHERE ";
			// Slightly different WHERE clause if using a number or text to search
			if (searchValueType.equals(SQLObject.DESCRIPTIONS.NUMBER))
				query += searchColumn+"="+searchValue.toString();
			else if (searchValueType.equals(SQLObject.DESCRIPTIONS.TEXT))
				query += searchColumn+"='"+searchValue.toString()+"'";
			Outputter.debugln("Database.getDoubleValues() running query: "+query, Outputter.DEBUG_TYPES.DATA_ACCESS);
			rs = this.statement.executeQuery(query);
			
			if (!rs.next()) {
				Outputter.errorln("Database.getDoubleValues(): error, no results returned from query: "+query);
				return null;
			}
			// Pointer is now on first row of results set (there should only be one row). Go through all results
			// adding them to a list which will be returned
			List<Double> results = new ArrayList<Double>();
			for (int i=1; i<=rs.getMetaData().getColumnCount(); i++) {
				results.add(rs.getDouble(i));
			}
			return results;


		} catch (SQLException e) {
			Outputter.errorln("Database. getDoubleValues() throw an SQL Exception: "+e.getMessage()+" trying " +
					"to execute sql: "+query);
			Outputter.errorln(e.getStackTrace());
			throw e;
		}
	}

	public void clear(List<String> tableNames) throws Exception {
		// Iterate over all tables, deleting all values
		String tableName = null;
		// Convert table names to lower case, these will be compared to database names (also in lower case)
		for (int i=0; i<tableNames.size(); i++) tableNames.set(i, tableNames.get(i).toLowerCase()); 
		try {
			DatabaseMetaData dbm = connection.getMetaData();
			String types[] = { "TABLE" };
			ResultSet rs = dbm.getTables(null, this.getSchema(), null, types);
			Outputter.debugln("Database.clear(): deleting data from tables:", Outputter.DEBUG_TYPES.DATA_ACCESS);
			System.out.println("Looking for tables: "+tableNames.toString());
			while (rs.next())
			{
				tableName = rs.getString("TABLE_NAME").toLowerCase();
//				System.out.print("Found table: "+tableName+", type: "+rs.getString("TABLE_TYPE")+", schem: "+
//						rs.getString("TABLE_SCHEM")+", cat: "+rs.getString("TABLE_CAT")+" ");
				if (tableNames.contains(tableName)) {
					Outputter.debugln("\tExecuting: DELETE FROM "+tableName, Outputter.DEBUG_TYPES.DATA_ACCESS);
					this.statement.execute("DELETE FROM "+tableName);
					this.statement.execute("commit");
				}
			}
		}
		catch (SQLException e) {
			Outputter.errorln("Database.clear() caught an SQL exception while trying to delete" +
					" all values from table: '"+tableName+"'. Message: "+e.getMessage());
			Outputter.errorln(e.getStackTrace());
			throw new Exception(e);
		}
	}
	
	/** Get the number of records stored in the file/table given by the filename */
	public int getNumRecords(String dataFileName) throws Exception {
		String sql = "SELECT COUNT(*) FROM "+dataFileName;
		Outputter.debugln("Database.getNumRecords() executing sql: "+sql+
				" using statement: "+this.statement.toString(), Outputter.DEBUG_TYPES.DATA_ACCESS);
		ResultSet rs = this.statement.executeQuery(sql);
		rs.next();
		return rs.getInt(1); // COUNT(*) query return single row
	}
	
	public <T> boolean createColumn(String dataFileName, String columnName, Class<T> columnType) {
		Outputter.errorln("Database: not implemented method: createColumn");
		// TODO Auto-generated method stub
		return false;
	}

	public boolean createFile(String dataFileName) {
		Outputter.errorln("Database: not implemented method: createFile");
		// TODO Auto-generated method stub
		return false;
	}

	public boolean createFullTable(String dataFileName, Map<String, Class<?>> columns) {
		Outputter.errorln("Database: not implemented method: createFullTable");
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Checks to see if passed object is a Road and if the unique id's are equal
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Database))
			return false;
		Database d = (Database) obj;
		return this.id==d.id;
	}

	@Override
	public int hashCode() {
		return this.id;
	}

	public void flush() throws SQLException {
		this.executeCachedWrites();
	}
	
	/**
	 * Get the schema for tables stored in the database (can return null if not necessary).
	 * @return
	 */
	protected abstract String getSchema();

}
