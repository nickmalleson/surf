package burgdsim.data_access;

import java.sql.Types;
import java.util.Hashtable;
import java.util.Map;

import burgdsim.main.Outputter;

/**
 * Class which can be used to map SQL Types to equivalent Java classes
 * @author Nick Malleson
 *
 * @param <T>
 */
public class SQLObject  {
	
	private Class<?> theClass;		// The Java class the object represents
	private int sqlType;			// The sql type (as an intege, see java.sql.Types).
	
	public enum DESCRIPTIONS {TEXT, NUMBER};
//	private DESCRIPTIONS description;
	
	// Maps Classes to either 'number' or 'text', sql queries are slightly different for each
	private static Map<Class<?>, DESCRIPTIONS> classDescriptionMap;
	
	// Maps primitive type classes to sql Types (an integer), used so the results from a database can be
	// compared to the class type which is expected to be returned by a query.
	private static Map<Integer, Class<?>> classTypeMap = null;
	
	// Maps SQL integer types to an understandable String
	private static Map<Integer, String> sqlTypeString = null;

	
	public SQLObject(int sqlType) {
		this.sqlType = sqlType;
		// Find an associated java class to match this type
//		if (sqlType==Types.NUMERIC && GlobalVars.DATA_ACCESS_TYPE.equals(GlobalVars.DATA_ACCESS_TYPES.ORACLEDB)) {
//			// TODO Get rid of hack here for postgres: NUMERIC data type can represent Integers, but I map it to
//			// doubles (which works with Derby).
//			this.theClass = Integer.class;
//			
//		}
//		else {
			this.theClass = findClassFromSQLType(sqlType);	
//		}
	}
		
	public Class<?> getTheClass() {
		return theClass;
	}

//	public DESCRIPTIONS getDescription() {
//		return description;
//	}
	
	/**
	 * Find an equivalent Class from an SQL type (defined in java.sql.Types).
	 * @param type
	 * @return
	 */
	private static Class<?> findClassFromSQLType(int type) {
		if (classTypeMap==null) { // Need to populate the map first
			classTypeMap = new Hashtable<Integer, Class<?>>();
			classTypeMap.put(Types.INTEGER, Integer.class);
			classTypeMap.put(Types.DOUBLE, Double.class);
			classTypeMap.put(Types.FLOAT, Float.class);
			classTypeMap.put(Types.VARCHAR, String.class);
			classTypeMap.put(Types.CHAR, String.class);
			classTypeMap.put(Types.NUMERIC, Double.class); // NOTE: in postgres this also represents integers!

		}
		if (!classTypeMap.containsKey(type)) {
			Outputter.errorln("SQLObject.findSQLType: unrecognised sql type: "+type+" there isn't an " +
					"equivalent Class type that I know about.");
			return null;
		}
		return classTypeMap.get(type); 
	}


	public static DESCRIPTIONS getDescriptionFromClass(Class<?> clazz) {
		if (classDescriptionMap==null) {
			classDescriptionMap = new Hashtable<Class<?>, DESCRIPTIONS>();
			classDescriptionMap.put(Short.class, DESCRIPTIONS.NUMBER);
			classDescriptionMap.put(Integer.class, DESCRIPTIONS.NUMBER);
			classDescriptionMap.put(Long.class, DESCRIPTIONS.NUMBER);
			classDescriptionMap.put(Double.class, DESCRIPTIONS.NUMBER);
			classDescriptionMap.put(Float.class, DESCRIPTIONS.NUMBER);
			classDescriptionMap.put(String.class, DESCRIPTIONS.TEXT);
			classDescriptionMap.put(Character.class, DESCRIPTIONS.TEXT);
		}
		if (!classDescriptionMap.containsKey(clazz)) {
			Outputter.errorln("SQLObject.getDescriptionFromClass: unrecognised class: "+clazz.toString());
			return null;
		}
		return classDescriptionMap.get(clazz);
	}
	
	/**
	 * Maps SQL types (an integer) to an understandable string, useful for debugging
	 * @return 
	 */
	public String getSQLTypeAsString() {
		if (sqlTypeString==null){
			sqlTypeString = new Hashtable<Integer, String>();
			sqlTypeString.put(Types.DOUBLE, "double");
			sqlTypeString.put(Types.INTEGER, "integer");
			sqlTypeString.put(Types.CHAR, "char");
			sqlTypeString.put(Types.VARCHAR, "varchar");
			sqlTypeString.put(Types.FLOAT, "float");
			sqlTypeString.put(Types.SMALLINT, "smallint");
			sqlTypeString.put(Types.NUMERIC, "numeric");
			
		}
		if (!sqlTypeString.containsKey(this.sqlType)) {
			Outputter.errorln("SQLObject.getSQLTypeAsString() error: don't have a String for this SQL Type: "+this.sqlType);
		}
		return sqlTypeString.get(this.sqlType);		
	}


}
