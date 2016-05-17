package burgdsim.data_access;

import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Map;

import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;

/**
 * Factory class will return the a relevant DataAccess object, could be used to read flat files or get
 * values from a database.
 * 
 * @author Nick Malleson
 */
public abstract class DataAccessFactory {
	
	// Store all the used data access objects which are used (all the different databases or flat
	// file directories, there might be more than one).
	private static Map<String, DataAccess> dataAccessObjecs;
	
	/**
	 * Get a DataAccess object, will create a new one if one with the given name doesn't already exist.
	 * @param dataStoreName The name of the data store.
	 * @return a (new) DataAccess object
	 * @throws Exception If there is a problem creating (or connecting to) the data store.
	 */
	public static DataAccess getDataAccessObject(String dataStoreName) throws Exception {
//		System.out.println("LOOKING FOR DATA STORE WITH NAME: "+dataStoreName);
		if (dataAccessObjecs == null) {
			dataAccessObjecs = new Hashtable<String, DataAccess>();
		}
		else if (dataAccessObjecs.containsKey(dataStoreName)) {
//			System.out.println("FOUND EXISTING, RETURNING. "+" : "+dataAccessObjecs.keySet().toString());
			return dataAccessObjecs.get(dataStoreName);
		}
//		System.out.println("NONE FOUND, CREATING NEW ONE. "+" : "+dataAccessObjecs.keySet().toString());
		// Create and initialise the appropriate data access object from variable set in GlobalVars
		if (GlobalVars.DATA_ACCESS_TYPE.equals("DERBYDB")) {
			DataAccess dataAccessObject = new DerbyDatabase(dataStoreName);
			dataAccessObjecs.put(dataStoreName, dataAccessObject);
			return dataAccessObject;
		}
		else if (GlobalVars.DATA_ACCESS_TYPE.equals("ORACLEDB")) {
			DataAccess dataAccessObject = null;
			if (dataAccessObjecs.size()>0) { // Can use existing oracle connection in the new object
				for (String name:dataAccessObjecs.keySet()) {
					dataAccessObject = new OracleDatabase(dataStoreName, 
							((OracleDatabase)dataAccessObjecs.get(name)).getConnection());
					Outputter.debugln("DataAccessFactory.getDataAccessObject() creating new OracleDatabase " +
							"using existing connection from: "+name, Outputter.DEBUG_TYPES.DATA_ACCESS);
					break;
				}
			}
			else {
				dataAccessObject = new OracleDatabase(dataStoreName);
			}
			dataAccessObjecs.put(dataStoreName, dataAccessObject);
			return dataAccessObject;			
		}
		
		
		Outputter.errorln("DataAccessFactory.getDataAccessObject() unrecognised type of data access: "+
				dataStoreName.toString());

		return null;
	}
	
	/**
	 * Close all the data access objects which are open at the moment.
	 * @throws SQLException 
	 */
	public static void closeAllObjects() throws SQLException {
		if (dataAccessObjecs != null) {
			for (String name:dataAccessObjecs.keySet()) {
				dataAccessObjecs.get(name).close();
			}
		}
	}
	
	/** Have to 'reset' static variables so they don't persist over multiple simulation runs (this will
	 * cause problems with batch runs or if the repast GUI is used to restart a model).
	 * <p>
	 * This should be called in ContextCreator before. The process could be
	 * done by using the Cacheable interface (like GISRoute does) but it isn't possible to register the
	 * EnvironmentFactory as a Cacheable objects in GlobalVars.caches() because it is static (i.e.
	 * GlobalVars.caches.add(this) wont work)
	 */
	public static void init() {
		dataAccessObjecs = null;
	}
	
	

}
