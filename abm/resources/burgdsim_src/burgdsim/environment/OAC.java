package burgdsim.environment;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;


import burgdsim.data_access.DataAccess;
import burgdsim.data_access.DataAccessFactory;
import burgdsim.main.ContextCreator;
import burgdsim.main.Functions;
import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;

/**
 * Implementation of the Output Area Classification (Vickers) for each output area.
 * <p>
 * Classification consists of 41 individual variables and supergroup, group and subgroup
 * membership. There are two ways of calculating the derived values (e.g. 'attractiveness')
 * for each OAC: individual variable values for the OAC (these will be unique for the particular
 * output area) or average values for the OAC group (these will be the same for every OAC with 
 * the same group).   
 * <p>
 * To create OAC objects a shapefile is read which will contain each variable value for every
 * output area. The variables are called v1-v41 so here there are get/set methods
 * for each variable. Average values for every subgroup, however, are stored separately (e.g. in a 
 * database or flat file) and can be read in from a DataAccess object.
 * <p>
 * A map called 'variableValues' maps the variable numbers to their values. This is built up when
 * the Communities are read in from the shapefile in EnvironmentFactory.createCommunities()  
 * If using the average values for each subgroup then init() should be called, this will re-populate
 * the variable map with average subgroup values.
 * 
 * @author Nick Malleson
 *
 */
public class OAC implements Sociotype, Cacheable  {
	
	private int id;
	private static Map<Integer,String> variableNames; // Dictionary of variable names and numbers
	// Map of variable values (e.g. v31 maps to key 31). This is either initialised in init() (when using
	// average subgroup values) or in the get/set methods if reading in data from a shapefile (using
	// individual variables for each OAC rather than averages).
	private volatile Map<Integer, Double> variableValues;
	
	// A cache of variable values for each subgroup (only relevant with average derived values)
	private static Map<String, List<Double>> subgroupVariableCache;
	private String supergroup;
	private String group;
	private String subgroup;
	// These two should actually be part of the Community, but they're here because when using a Grid environment
	// all community data is read in from a separate text file which contains Sociotype information
	private double CE; // collective efficacy 
//	private double TV; // traffic voluem
	
	// Required to store TOD weights for the four components which go into calculating occupancy
	private volatile static Map<Integer, double[]> occupancyWeights; // double[] stores all four weights
	
	private double attractCache = -1; // Cache the attractiveness so don't need to re-calculate each time;
	
	// These are only used in sensitivity tests: return a specific attractiveness or occupancy value rather
	// than calculating it properly.
	private double const_attract = 0.5;
	private double const_occupancy = 0.5;
	
	
	// TODO EFFICIENCY create static cache of all the values returned from compare() (will be a matrix)
	
	public OAC() {
		this.buildVariableNames();		// Map stores variable names and values, don't think I actually use this
		this.variableValues = new Hashtable<Integer, Double>();
	}

	/**
	 * Compare sociotypes by calculating the Euclidean distance between all their variables. The return value
	 * is normalised and "reversed" so that dissimilar areas return 0, identical ones return 1.
	 * Also, each variable is normalised so that those with larger magnitude don't dominate the calculation.
	 * Checks to see whether to use average subgroup values or individual values for each community
	 */
	public <T extends Sociotype> double compare(T sociotype) {
		OAC t = (OAC) sociotype; // Cast to an OAC
		double sum = 0; // The sum of squares
		for (int i=1; i<42; i++) { // Iterate over the 41 variables
			if (GlobalVars.OAC_PARAMS.AVERAGE_DERIVED_VARS) {
				sum += (Math.pow( this.getVariableValue(i)-t.getVariableValue(i), 2 ));
			}
			else {
			
			}
		}
		
		double val = Math.sqrt(sum);
		// Now normalise and 'reverse' so that range is 0 (dissimilar) to 1 (identical)
		double maxSize = Math.sqrt(41); // i.e. difference between every variable is 1 (maximum)
		return 1 - Functions.normalise(val, 0, maxSize);
	}

	/**
	 * Attractiveness caluclated from following: (see thesis/model_dev/classes.tex, section 'Socioeconomic Types')
	 * <br>full time students, rooms per household, > 2 car household, HE qualifications
	 */
	public double getAttractiveness() {
		// If doing a sensitivity test just return the constant value for attractiveness, not changing with TOD
		if (ContextCreator.isTrue(GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.CONST_ATT)) {
//			Outputter.describeln("OAC.getAttractiveness() returning constant attractiveness: "+this.const_attract);
			return this.const_attract;
		}
		
		if (this.attractCache==-1) { // Value isn't cached, calculate it
			double v31 = getVariableValue(31);
			double v22 = getVariableValue(22);
			double v26 = getVariableValue(26);
			double v24 = getVariableValue(24);
			if (GlobalVars.OAC_PARAMS.AVERAGE_DERIVED_VARS) {
				// Calculate attractiveness from the average variable values for this OAC's classification group.
				// These have already been normalised so can be used directly
//				System.out.println("OAC.java: OAC "+this.toString()+"got double values: "+v31+", "+v22+", "+v26+", "+v24);
				this.attractCache = (v31+v22+v26+v24 ) / 4; // Doesn't need normalising, average data has already normalised.
			}
			else {
				// Attractiveness calculated from this OAs individual variable values, these must
				// be normalised first.
				this.attractCache = ( Functions.normalise(v31, minv31, maxv31) + Functions.normalise(v22, minv22, maxv22) + 
						Functions.normalise(v26, minv26, maxv26) + Functions.normalise(v24, minv24, maxv24)) / 4;
//				System.out.println(v31+", "+minv31+", "+maxv31+", "+normalise(v31, minv31, maxv31));
//				System.out.println(v22+", "+minv22+", "+maxv22+", "+normalise(v22, minv22, maxv22));
//				System.out.println(v26+", "+minv26+", "+maxv26+", "+normalise(v26, minv26, maxv26));
//				System.out.println(v24+", "+minv24+", "+maxv24+", "+normalise(v24, minv24, maxv24));
			}
		}
		return this.attractCache;
	}

	public String getDescription() {
		return "OAC Sociotype: "+this.subgroup;
	}
	
	
	

	/**
	 * Occupancy is calculated from students (v31), unemployed (v32), part-time (v33) and economically
	 * inactive (v34). See Environment/RiskProfile thesis chapter for description of why these variables
	 * are used.
	 * <p>
	 * Similar to Community.getTrafficVolume, will use look-up tables to calculate the weights for each of the
	 * variables depending on the time of day by interpolating between two hourly values.
	 * @throws Exception 
	 */
	public double getOccupancy(double time) throws Exception {
		// If doing a sensitivity test just return the constant value for occupancy, not changing with TOD
		if (ContextCreator.isTrue(GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.CONST_OCC)) {
//			Outputter.describeln("OAC.getOccupancy() returning constant occupancy: "+this.const_occupancy);
			return this.const_occupancy;
		}
		double v31 = getVariableValue(31);
		double v32 = getVariableValue(32);
		double v33 = getVariableValue(33);
		double v34 = getVariableValue(34);
		double[] weights = OAC.getOccupancyWeights(time); // The weights which depend on the TOD
		if (GlobalVars.OAC_PARAMS.AVERAGE_DERIVED_VARS) {
			 
			return ( weights[0]*v31 + weights[1]*v32 + weights[2]*v33 + weights[3]*v34) / 4;    
			// TODO XXXX - note divide by four. See paragraph in DevelopmentLog about numbers getting smaller and smaller each time they're multiplied
		}
		else {
			// Attractiveness calculated from this OAs individual variable values, these must
			// be normalised first.
			return (weights[0] * Functions.normalise(v31, minv31, maxv31) + 
					weights[1] * Functions.normalise(v32, minv32, maxv32) + 
					weights[2] * Functions.normalise(v33, minv33, maxv33) + 
					weights[3] * Functions.normalise(v34, minv34, maxv34)) / 4;
		}
	}


	public String toString() {
		return "OAC "+this.id+": "+this.subgroup;
	}
	
	/* Getters and setters */
	
	/**
	 * Convenience function to get values from the store
	 * @param i The variable number
	 * @return The value of the variable
	 */
	private double getVariableValue(int i) {
		checkValueStore();
		return this.variableValues.get(i);
	}
	
	/**
	 * Convenience function to add values to the store
	 * @param i The variable number
	 * @param v The variable value
	 */
	private void setVariableValue(int i, double v) {
		checkValueStore();
		this.variableValues.put(i, v);
	}
	
	private synchronized void checkValueStore() {
		if (this.variableValues==null) {
			Outputter.errorln("OAC error, the variable store hasn't been initialised. Was init() called after" +
					"this object was created? (OAC '"+this.toString()+"')");
		}
	}
	
	public double getCE() {
		return this.CE;
	}
	
	public void setCE(double CE) {
		this.CE = CE;
	}
	

	public String getSupergroup() {
		return supergroup;
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setSupergroup(String supergroup) {
		this.supergroup = supergroup;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getSubgroup() {
		return subgroup;
	}

	public void setSubgroup(String subgroup) {
		this.subgroup = subgroup;
	}
		
	/**
	 * Read in the average subgroup variable values for this OAC. The variable store will already have been
	 * initialised and populated when Communities were read in by EnvironmentFactory.createCommunity() so
	 * this just replaces the values with average values for this OAC's subgroup.  
	 * <p>
	 * This must be called *after* this OAC has been created because it is necessary to know this OAC's
	 * subgroup. The subgroup values are read in from a shapefile and set by the Shapefile loader while the
	 * new object is created, so the subgroup isn't known until the object has been created..
	 * @throws Exception 
	 */
	public void init() throws Exception {
		// Using average values for each value depending on the group of this OAC so populate map now.
		// If not using average values the map is populated as the shapefile containing OAC data is read
		// by using the set() methods.
		if (GlobalVars.OAC_PARAMS.AVERAGE_DERIVED_VARS) {
			if (this.subgroup==null) {
				Outputter.errorln("OAC.init() warning, this OAC has no subgroup, "+
						"this should have been set (unless using a NULL environment). Setting to arbitrary value.");
				this.subgroup="1a1";
			}
			// See if the variables for this subgroup are in the cache
			if (OAC.subgroupVariableCache == null) {
				OAC.subgroupVariableCache = new Hashtable<String, List<Double>>();
				GlobalVars.CACHES.add(this);
			}
			
			if (OAC.subgroupVariableCache.containsKey(this.subgroup)) {
				// Variables are in cache for this OAC's subgroup, set them for this OAC
				List<Double> variables = OAC.subgroupVariableCache.get(this.subgroup);
//				System.out.println("Found variables for subgroup: "+this.subgroup);
				for (int i=1; i<42; i++) {
					this.setVariableValue(i, variables.get(i-1));
				}
			}
			else { // Variables not in cache, get them from the datastore
//				System.out.println("Didn't find variables for subgroup: "+this.subgroup+" adding them.");
				DataAccess da = DataAccessFactory.getDataAccessObject(GlobalVars.OAC_PARAMS.OAC_DATA_LOC);
				// Get every variable value from a single data store call using convenience method
				List<String> columnNames = new ArrayList<String>(); // The names of columns holding variable values
				for (int i=1; i<42; i++) 
					columnNames.add("v"+i);
				List<Double> columnValues = da.getDoubleValues(
						GlobalVars.OAC_PARAMS.SUBGROUP_PROFILES, // Table holding data 
						columnNames,					// List of columns to return
						"subgroup", 					// The column which will uniquely identify OAC groups
						String.class, 					// The column type
						this.subgroup.toString()); 		// The unique identifier for this OAC's sub-group.
				OAC.subgroupVariableCache.put(this.subgroup, columnValues);
				// Now set each value for this OAC
				for (int i=1; i<42; i++) {
					this.setVariableValue(i, columnValues.get(i-1));
				} 
			}
			
			// Old code which gets every variable separately
//			for (int i=1; i<42; i++) { // Loop over every variable for this OAC (41 variables in total)
//				// Find the value for the variable from the DataAccess object
//				double value = da.getValue(
//						GlobalVars.OAC_PARAMS.SUBGROUP_PROFILES,	// The name of file or database table
//						("v"+i), 						// The variable name (as a string), e.g. "v31"
//						Double.class, 					// The expected return type
//						"subgroup", 					// The column which will uniquely identify OAC groups
//						String.class, 					// The column type
//						this.subgroup.toString()); 		// The unique identifier for this OAC's sub-group.
//				setVariableValue(i, value); // Store the variable value
//			}
////			System.out.println("XXXX  - GET RID OF THIS LINE IN OAC!! NTO READING VALUES FOR EFFICIENCY");
////			for (int i=1; i<42; i++) { // XXXX TEMP: not getting actual data to speed up. 
////				//TODO remove this
////				setVariableValue(i, 0.5); 
////			}
			
		} // if AVERAGE_DERIVED_VALUES
	} // init()

	/**
	 * Give all parameters a value. Actual value isn't important, this is used by NULL environment
	 * to ensure that null values wont lead to errors later.
	 */
	public void initDefaultSociotype() {
		this.supergroup ="1";
		this.group = "1a";
		this.subgroup="1a1";
		for (int i=0; i<42; i++)
			this.setVariableValue(i, 1.0);
		
	}

	/* Each variable has a real name */
	private void buildVariableNames() {
		if (OAC.variableNames==null) {
			OAC.variableNames = new Hashtable<Integer, String>();
			OAC.variableNames.put(1, "Age 0 - 4");
			OAC.variableNames.put(2, "Age 5 -14");
			OAC.variableNames.put(2, "Age 25 - 44");
			OAC.variableNames.put(4, "Age 45 - 64");
			OAC.variableNames.put(5, "Age 65+");
			OAC.variableNames.put(6, "Indian/Pakistani/Bangladeshi");
			OAC.variableNames.put(7, "Black African, Black Caribbean or Black Other");
			OAC.variableNames.put(8, "Born outside UK");
			OAC.variableNames.put(9,"Population density");
			OAC.variableNames.put(10, "Divorced");
			OAC.variableNames.put(11, "Single person household (not pensioner)");
			OAC.variableNames.put(12, "Single pensioner household (pensioner)");
			OAC.variableNames.put(13, "Lone parent household");
			OAC.variableNames.put(14, "Two adult no children");
			OAC.variableNames.put(15, "Households with non-dependent children");
			OAC.variableNames.put(16, "Rent (public)");
			OAC.variableNames.put(17, "Rent (private)");
			OAC.variableNames.put(18, "Terraced Housing");
			OAC.variableNames.put(19, "Detached Housing");
			OAC.variableNames.put(20, "All Flats");
			OAC.variableNames.put(21, "No central heating");
			OAC.variableNames.put(22, "Rooms per household");
			OAC.variableNames.put(23, "People per room");
			OAC.variableNames.put(24, "HE qualifications");
			OAC.variableNames.put(25, "Routine/Semi-Routine occupation");
			OAC.variableNames.put(26, "2+ Car household");
			OAC.variableNames.put(27, "Public transport to work");
			OAC.variableNames.put(28, "Work from home");
			OAC.variableNames.put(29, "Llti (SIR)");
			OAC.variableNames.put(30, "Provide unpaid care");
			OAC.variableNames.put(31, "Students (full time)");
			OAC.variableNames.put(32, "Unemployed");
			OAC.variableNames.put(33, "Working part-time");
			OAC.variableNames.put(34, "Economically inactive looking after family");
			OAC.variableNames.put(35, "Agriculture/fishing employment");
			OAC.variableNames.put(36, "Mining/quarrying/construction employment");
			OAC.variableNames.put(37, "Manufacturing employment");
			OAC.variableNames.put(38, "Hotel & catering employment");
			OAC.variableNames.put(39, "Health/social work employment");
			OAC.variableNames.put(40, "Financial intermediation employment");
			OAC.variableNames.put(41, "Wholesale/retail employment");
		}		
	}
	
	/**
	 * Returns the weights used to calculate occupancy rates. Function is public for convenience, this
	 * way other Sociotypes can use it assuming they know the order that the weights are returned.. 
	 * @throws Exception 
	 */ 
	public synchronized static double[] getOccupancyWeights(double time) throws Exception {
		if (OAC.occupancyWeights==null) {
			// Need to fill the hashtable which caches the weights which are applied to each variable (these
			// depend on the time of day). Read in the weights from an external data store.
			Outputter.debugln("OAC.getOccupancyWeights populating hashtable from external data",
					Outputter.DEBUG_TYPES.DATA_ACCESS);
			OAC.occupancyWeights = new Hashtable<Integer, double[]>(25);
			DataAccess da = DataAccessFactory.getDataAccessObject(GlobalVars.OAC_PARAMS.OAC_DATA_LOC);
			for (int i=0; i<25; i++) { // Loop over every possible time value (0 -> 24) (24 is included to interpolate if time > 23, 11pm)
				double[] weights = new double[4];
				for (int j=31; j<35; j++) { // Loop over the four variables which go into occupancy (lucky that they are incremental, otherwise couldn't use the loop
					double value= da.getValue(
							GlobalVars.OAC_PARAMS.OCCUPANCY_WEIGHTS,	// The name of file or database table
							("v"+j), 						// The variable name (as a string), e.g. "v31"
							Double.class, 					// The expected return type
							"theTime", 						// The column which will uniquely identify the time
							Integer.class, 					// The column type
							i); 					// The unique identifier for the time
					weights[j-31] = value;
				}
				Outputter.debugln("\tOAC.getOccupancyWeights found these weights for time "+i+
						": "+makeString(weights), Outputter.DEBUG_TYPES.DATA_ACCESS);
				OAC.occupancyWeights.put(i, weights);
			}
		} // if occupancyWeights==null
		
		// Because weights are stored for each hour need to interpolate between upper and lower hours
		double[] lowerWeights = OAC.occupancyWeights.get((int)time);
		double[] upperWeights = OAC.occupancyWeights.get((int)time+1);
		double interpolateAmount = time - (int) time; // The amount to interpolate between
		
		double[] theWeights = new double[4];
		for (int i=0; i<theWeights.length; i++) {
			theWeights[i] = 
				lowerWeights[i] + 
				interpolateAmount*(
						upperWeights[i]-
						lowerWeights[i]); 
		}
//		Outputter.debugln("OAC.getOccupancyWeights found these weights for time "+time+
//				"\n\t interpolated from (lower): "+makeString(lowerWeights)+
//				"\n\t interpolated from (upper): "+makeString(upperWeights),
//				Outputter.DEBUG_TYPES.DATA_ACCESS);
		return theWeights;
	}
	
	/**
	 * Convenience function to convert a double array into a list of strings (for printing)
	 * @param array
	 * @return
	 */
	private static String makeString(double[] array) {
		String list = "[";
		for (int i=0; i<array.length; i++) {
			list+=String.valueOf(array[i])+",";
		}
		return list+="]";
	}

	/** Used in sensitivity tests to set a constant attractiveness value that this community will always return,
	 * rather than calculating based on time of day etc. */
	public void setConstAttract(double const_attract) {
		if (!ContextCreator.isTrue(GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.CONST_ATT)) {
			Outputter.errorln("OAC.setConstAttract() has been called but the global parameter " +
					"GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.CONST_ATT is 0 so this shouldn't have" +
					"been called.");
		}
		this.const_attract = const_attract;
	}

	/** Used in sensitivity tests to set a constant occupancy value that this community will always return,
	 * rather than calculating based on time of day etc. */
	public void setConstOccupancy(double const_occupancy) {
		if (!ContextCreator.isTrue(GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.CONST_OCC)) {
			Outputter.errorln("OAC.setConstOccupancy() has been called but the global parameter " +
					"GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.CONST_OCC is 0 so this shouldn't have" +
					"been called.");
		}
		this.const_occupancy = const_occupancy;
	}

	/*
	 * Get/Set methods for the 41 individual variable which make up the classification. These are
	 * needed when the individual OAC values are read in from a shapefile along with the Communities
	 * they represent. Also need min/max values to normalise. 
	 * (These were auto-generated from csv files in extras/ directory (load them into a spreadsheet,
	 * copy and paste into vim, do some formatting then copy and past into here)) */
	private static double maxv1 = Double.MIN_VALUE ; private static double minv1 = Double.MAX_VALUE ;
	private static double maxv2 = Double.MIN_VALUE ; private static double minv2 = Double.MAX_VALUE ;
	private static double maxv3 = Double.MIN_VALUE ; private static double minv3 = Double.MAX_VALUE ;
	private static double maxv4 = Double.MIN_VALUE ; private static double minv4 = Double.MAX_VALUE ;
	private static double maxv5 = Double.MIN_VALUE ; private static double minv5 = Double.MAX_VALUE ;
	private static double maxv6 = Double.MIN_VALUE ; private static double minv6 = Double.MAX_VALUE ;
	private static double maxv7 = Double.MIN_VALUE ; private static double minv7 = Double.MAX_VALUE ;
	private static double maxv8 = Double.MIN_VALUE ; private static double minv8 = Double.MAX_VALUE ;
	private static double maxv9 = Double.MIN_VALUE ; private static double minv9 = Double.MAX_VALUE ;
	private static double maxv10 = Double.MIN_VALUE ; private static double minv10 = Double.MAX_VALUE ;
	private static double maxv11 = Double.MIN_VALUE ; private static double minv11 = Double.MAX_VALUE ;
	private static double maxv12 = Double.MIN_VALUE ; private static double minv12 = Double.MAX_VALUE ;
	private static double maxv13 = Double.MIN_VALUE ; private static double minv13 = Double.MAX_VALUE ;
	private static double maxv14 = Double.MIN_VALUE ; private static double minv14 = Double.MAX_VALUE ;
	private static double maxv15 = Double.MIN_VALUE ; private static double minv15 = Double.MAX_VALUE ;
	private static double maxv16 = Double.MIN_VALUE ; private static double minv16 = Double.MAX_VALUE ;
	private static double maxv17 = Double.MIN_VALUE ; private static double minv17 = Double.MAX_VALUE ;
	private static double maxv18 = Double.MIN_VALUE ; private static double minv18 = Double.MAX_VALUE ;
	private static double maxv19 = Double.MIN_VALUE ; private static double minv19 = Double.MAX_VALUE ;
	private static double maxv20 = Double.MIN_VALUE ; private static double minv20 = Double.MAX_VALUE ;
	private static double maxv21 = Double.MIN_VALUE ; private static double minv21 = Double.MAX_VALUE ;
	private static double maxv22 = Double.MIN_VALUE ; private static double minv22 = Double.MAX_VALUE ;
	private static double maxv23 = Double.MIN_VALUE ; private static double minv23 = Double.MAX_VALUE ;
	private static double maxv24 = Double.MIN_VALUE ; private static double minv24 = Double.MAX_VALUE ;
	private static double maxv25 = Double.MIN_VALUE ; private static double minv25 = Double.MAX_VALUE ;
	private static double maxv26 = Double.MIN_VALUE ; private static double minv26 = Double.MAX_VALUE ;
	private static double maxv27 = Double.MIN_VALUE ; private static double minv27 = Double.MAX_VALUE ;
	private static double maxv28 = Double.MIN_VALUE ; private static double minv28 = Double.MAX_VALUE ;
	private static double maxv29 = Double.MIN_VALUE ; private static double minv29 = Double.MAX_VALUE ;
	private static double maxv30 = Double.MIN_VALUE ; private static double minv30 = Double.MAX_VALUE ;
	private static double maxv31 = Double.MIN_VALUE ; private static double minv31 = Double.MAX_VALUE ;
	private static double maxv32 = Double.MIN_VALUE ; private static double minv32 = Double.MAX_VALUE ;
	private static double maxv33 = Double.MIN_VALUE ; private static double minv33 = Double.MAX_VALUE ;
	private static double maxv34 = Double.MIN_VALUE ; private static double minv34 = Double.MAX_VALUE ;
	private static double maxv35 = Double.MIN_VALUE ; private static double minv35 = Double.MAX_VALUE ;
	private static double maxv36 = Double.MIN_VALUE ; private static double minv36 = Double.MAX_VALUE ;
	private static double maxv37 = Double.MIN_VALUE ; private static double minv37 = Double.MAX_VALUE ;
	private static double maxv38 = Double.MIN_VALUE ; private static double minv38 = Double.MAX_VALUE ;
	private static double maxv39 = Double.MIN_VALUE ; private static double minv39 = Double.MAX_VALUE ;
	private static double maxv40 = Double.MIN_VALUE ; private static double minv40 = Double.MAX_VALUE ;
	private static double maxv41 = Double.MIN_VALUE ; private static double minv41 = Double.MAX_VALUE ;
	

	public void setV1 (double v1 ) { if ( v1 >maxv1 ) maxv1 = v1 ; if ( v1 <minv1 ) minv1 = v1 ; this.setVariableValue( 1 , v1 );}
	public void setV2 (double v2 ) { if ( v2 >maxv2 ) maxv2 = v2 ; if ( v2 <minv2 ) minv2 = v2 ; this.setVariableValue( 2 , v2 );}
	public void setV3 (double v3 ) { if ( v3 >maxv3 ) maxv3 = v3 ; if ( v3 <minv3 ) minv3 = v3 ; this.setVariableValue( 3 , v3 );}
	public void setV4 (double v4 ) { if ( v4 >maxv4 ) maxv4 = v4 ; if ( v4 <minv4 ) minv4 = v4 ; this.setVariableValue( 4 , v4 );}
	public void setV5 (double v5 ) { if ( v5 >maxv5 ) maxv5 = v5 ; if ( v5 <minv5 ) minv5 = v5 ; this.setVariableValue( 5 , v5 );}
	public void setV6 (double v6 ) { if ( v6 >maxv6 ) maxv6 = v6 ; if ( v6 <minv6 ) minv6 = v6 ; this.setVariableValue( 6 , v6 );}
	public void setV7 (double v7 ) { if ( v7 >maxv7 ) maxv7 = v7 ; if ( v7 <minv7 ) minv7 = v7 ; this.setVariableValue( 7 , v7 );}
	public void setV8 (double v8 ) { if ( v8 >maxv8 ) maxv8 = v8 ; if ( v8 <minv8 ) minv8 = v8 ; this.setVariableValue( 8 , v8 );}
	public void setV9 (double v9 ) { if ( v9 >maxv9 ) maxv9 = v9 ; if ( v9 <minv9 ) minv9 = v9 ; this.setVariableValue( 9 , v9 );}
	public void setV10 (double v10 ) { if ( v10 >maxv10 ) maxv10 = v10 ; if ( v10 <minv10 ) minv10 = v10 ; this.setVariableValue( 10 , v10 );}
	public void setV11 (double v11 ) { if ( v11 >maxv11 ) maxv11 = v11 ; if ( v11 <minv11 ) minv11 = v11 ; this.setVariableValue( 11 , v11 );}
	public void setV12 (double v12 ) { if ( v12 >maxv12 ) maxv12 = v12 ; if ( v12 <minv12 ) minv12 = v12 ; this.setVariableValue( 12 , v12 );}
	public void setV13 (double v13 ) { if ( v13 >maxv13 ) maxv13 = v13 ; if ( v13 <minv13 ) minv13 = v13 ; this.setVariableValue( 13 , v13 );}
	public void setV14 (double v14 ) { if ( v14 >maxv14 ) maxv14 = v14 ; if ( v14 <minv14 ) minv14 = v14 ; this.setVariableValue( 14 , v14 );}
	public void setV15 (double v15 ) { if ( v15 >maxv15 ) maxv15 = v15 ; if ( v15 <minv15 ) minv15 = v15 ; this.setVariableValue( 15 , v15 );}
	public void setV16 (double v16 ) { if ( v16 >maxv16 ) maxv16 = v16 ; if ( v16 <minv16 ) minv16 = v16 ; this.setVariableValue( 16 , v16 );}
	public void setV17 (double v17 ) { if ( v17 >maxv17 ) maxv17 = v17 ; if ( v17 <minv17 ) minv17 = v17 ; this.setVariableValue( 17 , v17 );}
	public void setV18 (double v18 ) { if ( v18 >maxv18 ) maxv18 = v18 ; if ( v18 <minv18 ) minv18 = v18 ; this.setVariableValue( 18 , v18 );}
	public void setV19 (double v19 ) { if ( v19 >maxv19 ) maxv19 = v19 ; if ( v19 <minv19 ) minv19 = v19 ; this.setVariableValue( 19 , v19 );}
	public void setV20 (double v20 ) { if ( v20 >maxv20 ) maxv20 = v20 ; if ( v20 <minv20 ) minv20 = v20 ; this.setVariableValue( 20 , v20 );}
	public void setV21 (double v21 ) { if ( v21 >maxv21 ) maxv21 = v21 ; if ( v21 <minv21 ) minv21 = v21 ; this.setVariableValue( 21 , v21 );}
	public void setV22 (double v22 ) { if ( v22 >maxv22 ) maxv22 = v22 ; if ( v22 <minv22 ) minv22 = v22 ; this.setVariableValue( 22 , v22 );}
	public void setV23 (double v23 ) { if ( v23 >maxv23 ) maxv23 = v23 ; if ( v23 <minv23 ) minv23 = v23 ; this.setVariableValue( 23 , v23 );}
	public void setV24 (double v24 ) { if ( v24 >maxv24 ) maxv24 = v24 ; if ( v24 <minv24 ) minv24 = v24 ; this.setVariableValue( 24 , v24 );}
	public void setV25 (double v25 ) { if ( v25 >maxv25 ) maxv25 = v25 ; if ( v25 <minv25 ) minv25 = v25 ; this.setVariableValue( 25 , v25 );}
	public void setV26 (double v26 ) { if ( v26 >maxv26 ) maxv26 = v26 ; if ( v26 <minv26 ) minv26 = v26 ; this.setVariableValue( 26 , v26 );}
	public void setV27 (double v27 ) { if ( v27 >maxv27 ) maxv27 = v27 ; if ( v27 <minv27 ) minv27 = v27 ; this.setVariableValue( 27 , v27 );}
	public void setV28 (double v28 ) { if ( v28 >maxv28 ) maxv28 = v28 ; if ( v28 <minv28 ) minv28 = v28 ; this.setVariableValue( 28 , v28 );}
	public void setV29 (double v29 ) { if ( v29 >maxv29 ) maxv29 = v29 ; if ( v29 <minv29 ) minv29 = v29 ; this.setVariableValue( 29 , v29 );}
	public void setV30 (double v30 ) { if ( v30 >maxv30 ) maxv30 = v30 ; if ( v30 <minv30 ) minv30 = v30 ; this.setVariableValue( 30 , v30 );}
	public void setV31 (double v31 ) { if ( v31 >maxv31 ) maxv31 = v31 ; if ( v31 <minv31 ) minv31 = v31 ; this.setVariableValue( 31 , v31 );}
	public void setV32 (double v32 ) { if ( v32 >maxv32 ) maxv32 = v32 ; if ( v32 <minv32 ) minv32 = v32 ; this.setVariableValue( 32 , v32 );}
	public void setV33 (double v33 ) { if ( v33 >maxv33 ) maxv33 = v33 ; if ( v33 <minv33 ) minv33 = v33 ; this.setVariableValue( 33 , v33 );}
	public void setV34 (double v34 ) { if ( v34 >maxv34 ) maxv34 = v34 ; if ( v34 <minv34 ) minv34 = v34 ; this.setVariableValue( 34 , v34 );}
	public void setV35 (double v35 ) { if ( v35 >maxv35 ) maxv35 = v35 ; if ( v35 <minv35 ) minv35 = v35 ; this.setVariableValue( 35 , v35 );}
	public void setV36 (double v36 ) { if ( v36 >maxv36 ) maxv36 = v36 ; if ( v36 <minv36 ) minv36 = v36 ; this.setVariableValue( 36 , v36 );}
	public void setV37 (double v37 ) { if ( v37 >maxv37 ) maxv37 = v37 ; if ( v37 <minv37 ) minv37 = v37 ; this.setVariableValue( 37 , v37 );}
	public void setV38 (double v38 ) { if ( v38 >maxv38 ) maxv38 = v38 ; if ( v38 <minv38 ) minv38 = v38 ; this.setVariableValue( 38 , v38 );}
	public void setV39 (double v39 ) { if ( v39 >maxv39 ) maxv39 = v39 ; if ( v39 <minv39 ) minv39 = v39 ; this.setVariableValue( 39 , v39 );}
	public void setV40 (double v40 ) { if ( v40 >maxv40 ) maxv40 = v40 ; if ( v40 <minv40 ) minv40 = v40 ; this.setVariableValue( 40 , v40 );}
	public void setV41 (double v41 ) { if ( v41 >maxv41 ) maxv41 = v41 ; if ( v41 <minv41 ) minv41 = v41 ; this.setVariableValue( 41 , v41 );}
	
	/**
	 * Prints out the difference between every OAC (using average values stored in the database). Not used in the
	 * model but required to put into thesis.
	 * @throws Exception 
	 */	
	public static void printOACDifferences() throws Exception {
		System.out.println("\nPrinting matrix of all OAC subgroup differences: ");
		
		List<String> subGroupNames = new ArrayList<String>();
		// Naively get all the subgroups from the database, very messy, build up all possible supergroup
		// strings and check to see which ones have an entry in the database
		DataAccess da = DataAccessFactory.getDataAccessObject(GlobalVars.OAC_PARAMS.OAC_DATA_LOC);
		for (int i=1; i<8; i++) { // supergroup
			for (int j=0; j<4; j++) { // group
				for (int k=1; k<6; k++) { // subgroup
					Double val = null;
					String group = null; // Groups are letters, not numbers
					if (j==0) group="a";
					else if (j==1) group="b";
					else if (j==2) group="c";
					else if (j==3) group="d";
					val = da.getValue(
							GlobalVars.OAC_PARAMS.SUBGROUP_PROFILES,	// The name of file or database table
							("v1"), 						// The variable name (as a string), e.g. "v31"
							Double.class, 					// The expected return type
							"subgroup", 					// The column which will uniquely identify OAC groups
							String.class, 					// The column type
							(i+group+k) 					// The unique identifier for this OAC's sub-group.
					);
					if (val!=null) {
						subGroupNames.add(i+group+k);
						System.out.println("Found "+(i+group+k));
					}
				}
			}
		}
		// Headers
		System.out.print("OACSubgroup");
		for (String subgroup:subGroupNames) System.out.print(", "+subgroup);
		System.out.println();
		// Values
		for (String a:subGroupNames) {
			System.out.print(a);
			for (String b:subGroupNames) {
				System.out.print(", "+compare(a,b,da));
			}
			System.out.println();
		}
		
	}
	// Another compare function which takes subgroup names, used for debugging etc
	private static double compare(String group1, String group2, DataAccess da) throws SQLException {
		double sum = 0; // The sum of squares
		for (int i=1; i<42; i++) { // Iterate over the 41 variables
			double val1 = da.getValue( GlobalVars.OAC_PARAMS.SUBGROUP_PROFILES, 
					("v"+i), Double.class, "subgroup", String.class, group1);
			double val2 = da.getValue( GlobalVars.OAC_PARAMS.SUBGROUP_PROFILES, 
					("v"+i), Double.class, "subgroup", String.class, group2);
			sum += (Math.pow( val1-val2, 2 ));
		}
		
		double val = Math.sqrt(sum);
		// Now normalise and 'reverse' so that range is 0 (dissimilar) to 1 (identical)
		double maxSize = Math.sqrt(41); // i.e. difference between every variable is 1 (maximum)
		return 1 - Functions.normalise(val, 0, maxSize);
	}

	public void clearCaches() {
		OAC.subgroupVariableCache = null;
	}

	public String getParameterValuesString() {
		try {
			return "type: "+this.subgroup+" att: "+this.getAttractiveness()+
			" occ: "+this.getOccupancy(GlobalVars.time);
		} catch (Exception e) {
			Outputter.errorln("OAC.getParamerValuesString error. Message: "+e.getMessage());
			Outputter.errorln(e.getStackTrace());
		}
		return "";
	}
}
