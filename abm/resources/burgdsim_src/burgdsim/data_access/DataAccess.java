package burgdsim.data_access;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;


/**
 * Interface to objects which can be used to read or write data to databases, flat files etc.
 * A DataAccess object can represent one directory of flat files or one database connection,
 * to access more than one of these 'data stores' multiple Dataaccess objects are used. The 
 * subclass is responsible for creating new data stores if they are required (e.g. creating or
 * connecting to the appropriate database).
 */
public interface DataAccess {
	
//	/**
//	 * Get a value from the DataAccess object.
//	 * @param <T> The expected type of value
//	 * @param <U> The type of the row which we're using to find the value
//	 * @param dataFile The name of the file which contains the data e.g. a database table name or flat file filename. 
//	 * @param column The column which holds the value
//	 * @param rowName The name of the row which holds the result we're looking for (really this is another
//	 * column, but it's the column which holds rowValue).
//	 * @param rowValue The value of the row which holds the result we're looking for
//	 * @param clazz The class of expected data to be returned (e.g. a String or a Double).
//	 * @return The value found from the given parameters or null if nothing is found.
//	 */
//	<T, U> T getValue(Class<T> clazz, String dataFile, String column, String rowName, U rowValue);
	
	/**
	 * Get a value ('result') from the DataAccess object
	 * @param <T> The type of the value to return
	 * @param <U> The type of the column of data which will be used to identify the value to be returned
	 * @param dataFile The name of the file which contains the data e.g. a database table name or flat file filename.
	 * @param resultColumn The column where the result can be found
	 * @param resultColumnClass The type of the column which holds the result
	 * @param searchColumn The column used to search for the result (i.e. used to identify the correct row)
	 * @param searchColumnClass The class of the search column.
	 * @param searchValue The value in the search column which will *uniquely* identify the row which contains
	 * the result.
	 * @return The value or null if none was found (will also print an error message).
	 * @throws SQLException 
	 */
	<T, U> T getValue(String dataFile, String resultColumn, Class<T> resultColumnClass, 
			String searchColumn, Class<U> searchColumnClass, U searchValue ) throws SQLException ;
	
	/**
	 * Convenience function to get a number of doubles from a database simultaneously.
	 * @param dataFile The name of the file which contains the data e.g. a database table name or flat file filename.
	 * @param resultColumns The columns where the result can be found
	 * @param searchColumn The column used to search for the result (i.e. used to identify the correct row)
	 * @param searchColumnClass The class of the search column.
	 * @param searchValue The value in the search column which will *uniquely* identify the row which contains
	 * the result.
	 * @return The values or null if none were found (will also print an error message).
	 * @throws SQLException 
	 */
	<T> List<Double> getDoubleValues(String dataFile , List<String> resultColumns, String searchColumn,
			Class<T> searchColumnClass, String searchValue) throws SQLException;
	
	/**
	 * Create a new row in this data store from list of values. Arrays (linked on the index) hold the
	 * information for each piece of data.
	 * @param <T> The type of value to be written, this must match the one that the DataAccess object
	 * can hold.
	 * @param values The values to be written.
	 * @param dataFile The name of the file which contains the data e.g. a database table name or flat file filename. 
	 * @param columns The columns which will hold the values
	 * @throws Exception Subclasses might need to throw an exception. 
	 */
	<T> void writeValues(T[] values, String dataFile, String[] columns) throws Exception;
	
	/**
	 * Write a value to this DataAccess object.
	 * @param <T> The type of value to be written, this must match the one that the DataAccess object
	 * can hold.
	 * @param value The value to be written
	 * @param clazz The class of the value to be written.
	 * @param dataFile The name of the file which contains the data e.g. a database table name or flat file filename. 
	 * @param column The column which holds the value
	 * @param row The row which holds the value.
	 */
	<T> void writeValue(T value, Class<T> clazz, String dataFile, String column, String row);
	
	/**
	 * Create a new file of data, e.g. a new flat file or database table
	 * @param dataFileName The name of the new data file.
	 * @return true if the operation was successful.
	 */
	boolean createFile(String dataFileName);
	
	/**
	 * Create a new column in a table to store data.
	 * @param <T> The type of data which the column will store.
	 * @param dataFileName The name of the file  which the column should be added to.
	 * @param columnName The name of the new column.
	 * @param columnType The Class of the data which the new colum will store (e.g. String.class or Double.class).
	 * @return true if the operation was successful.
	 */
	<T> boolean createColumn(String dataFileName, String columnName, Class<T> columnType);
	
	/**
	 * Convenience function which creates a fully-defined table. The column names are passed as a Map.
	 * @param dataFileName The name of the file which will store all the data.
	 * @param columns A Map of column names to the Classes they will be used to store.
	 * @return true if the operation was successful.
	 */
	boolean createFullTable(String dataFileName, Map<String, Class<?>> columns);
	
	/**
	 * Use the given object to create custom files. E.g the object could be an sql string and create customised
	 * tables/relations or it could be a list of File objects etc, the implementation will depend on the
	 * subclass. (Optional operation).
	 * @param obj
	 * @return boolean if the files were created successfully.
	 * @throws UnsupportedOperationException If the implementing subclass does not support this operation.
	 * @throws IllegalArgumentException If the given object cannot be used to create files.
	 * @throws Exception If any other errors occur (e.g. SQLException). 
	 */
	boolean createCustomFiles(Object obj) throws UnsupportedOperationException, IllegalArgumentException, Exception ;
	
	/**
	 * Get the number of records stored in the file/table given by the filename
	 * @param dataFileName
	 * @return
	 * @throws Exception 
	 */
	int getNumRecords(String dataFileName) throws Exception;
	
	/**
	 * Close this DataAccess object: free all resources, flush buffers, write files etc.
	 * @throws SQLException If there was a problem closing connection
	 */
	void close() throws SQLException;
	
	/**
	 * Clear any cached data (optional operation). If this DataAccess object doesn't cache any data then
	 * this function won't do anything.
	 * @throws Exception If something goes wrong.
	 */
	void flush() throws Exception;
	
	/**
	 * Empty the tables in the data store, deleting all records they hold but not
	 * the tables themselves.
	 * @param tableNames the names of the tables to be cleared
	 * @throws Exception
	 */
	void clear(List<String> tableNames) throws Exception;

}
