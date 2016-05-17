package burgdsim.main;

import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import repast.simphony.engine.environment.RunEnvironment;
import burgdsim.burglars.Burglar;
import burgdsim.environment.Cacheable;
import burgdsim.environment.Community;
import burgdsim.environment.Environment;
import burgdsim.environment.Junction;
import burgdsim.environment.OAC;
import burgdsim.environment.Road;
import burgdsim.environment.Sociotype;
import burgdsim.environment.buildings.Building;

/**
 * A place to store all model parameters. All parameters are stored as static variables which can be updated
 * by Scenario to represent a particular model configuration using reflection. This is really bad way of holding
 * model parameters because each parameter is given a value when GlobalVars is first initialised. Therefore
 * parameters which are derived from other parameters (e.g. MINS_PER_ITER) need to be re-calculated after
 * Scenario has set all the parameter values (see recaclDeriverFields()). This should really be completely
 * re-designed and all parameters handled properly by Scenario.
 * @author Nick Malleson
 */

public class GlobalVars {

	public static int getIteration() { return (int)RunEnvironment.getInstance().getCurrentSchedule().getTickCount();}
	/** Simulation time (hours) in 24 hr decimal format, e.g. 3:15 am = 3.25 */
	public static double time = 0.0; 
	public static int days = 0;
	/**Total number of ellapsed minutes, useful for some calculations.*/
	public static double mins = 0.0;
	// Numer of iterations per day
	//	public static double ITER_PER_DAY = 5760; // For GIS: using 15s iterations (240perhour*24)

	/** The number of iterations in a day. This field has to be final, it cannot be changed in a scenarios
	 * file. This is because many other variables depend on it (e.g. PECS variables). It is theoretically possible
	 * to use the recalculateDeriverFields() function to change this in a scenarios file and then recalculate the
	 * variables, but if the variables were set the in scenarios file these changes would be lost. (Rubbish!)*/
	public static final int ITER_PER_DAY = 1440; // For GRID: assume 1 iteration = 1 min for simplicity 

	public static double MINS_PER_ITER = (60*24)/ITER_PER_DAY; // Useful measurement (= mins per day / iter per day)
	public static long START_TIME; // USed to measure the execution time of the model
	/** The length of time that the simulation should run for. */
	public static int RUN_TIME = 5000;

	/** A description of the model, can be useful to distinguish scenarios with similar parameters*/
	public static String DESCRIPTION = "";

	/** The number of burglars in the model */
	public static int NUM_BURGLARS = 0;

	/** The number of burglaries which have occurred */
	public static int NUM_BURGLARIES = 0;

	/** Whether or not the scenario has been set up already. It can be set up either in BurgdSimMain (if doing batch
	 * runs) or in ContextCreator to view the sim as it runs but not in both! ContextCreator.end() will reset
	 * the value to false in preparation for another run.*/
	public static boolean SCENARIO_INITIALISED = false;

	/** When running the model programatically (or using MPJ) this needs to be set so that the simulation
	 * can be ended if necessary (the runner takes control of the scheduling). */
	public static BurgdSimRunner RUNNER = null;

	/** When using MPJ this Logger can be used to log simulation output to different files depending on
	 * which node this model is running on. If null then Outputter ignores it. */
	public static Logger logger = null;
	//	/** Whether to exit after the environment has been tested or just delete any bad objects found and continue */
	//	public static final boolean EXIT_AFTER_TESTING = false;

	// Parameters to control where data is read/written to/from
	//	public static enum DATA_ACCESS_TYPES {DERBYDB, FLATFILE, MYSQLDB, ORACLEDB};
	//	public static final DATA_ACCESS_TYPES DATA_ACCESS_TYPE = DATA_ACCESS_TYPES.ORACLEDB;
	public static String DATA_ACCESS_TYPE; // Set in scenario parameters file, either ORACLEDB or DEBRYDB

	public static double DETERIORATE_AMOUNT = 1.0/(double)GlobalVars.ITER_PER_DAY; // Amount that drug/sleep level deteriorate per day
	/** The amount greater than the old motive intensity that a new motive intensity must have in order to
	 * take control of the agent away from the old motive. This prevents the agents rapidly switching between
	 * two motives which have similar intensities.*/
	public static  double INTENSITY_DIFFERENCE = 0.1;
	//	public static final double DETERIORATE_AMOUNT = 0.5/GlobalVars.ITER_PER_DAY; // (deteriorate more slowly)
	private static double SLEEP_HOURS = 8.0; // number of hours per day an agent should spend sleeping
	private static double WORK_HOURS = 8.0; // number of hours per day an agent should spend working
	public static double SLEEP_GAIN = 24.0 / (SLEEP_HOURS*ITER_PER_DAY); // amount of  sleep gained by one iteration of sleeping (8 hours per day for 1 unit increase)
	public static double WORK_GAIN = 24.0 / (WORK_HOURS*ITER_PER_DAY); // The amount of wealth gained by one iteration of working (8 hours per day for 1 unit increase)
	public static double BURGLE_GAIN = 1; // Amount of wealth from burgling.

	// XXXX Need to vary costs depending on how many expensive activities the agent must do, global values will not work if some agents must do more activities than others becasue they all make the same amount of money

	public static double COST_DRUGS = 0.5; // cost = half day work
	public static double DRUGS_GAIN = 1;	// increase in level of drugs by taking them

	private static double SOCIAL_HOURS = 2; // The number of hours per day a person should spend socialising
	public static double INITIAL_COST_SOCIALISE = WORK_GAIN * SOCIAL_HOURS*(ITER_PER_DAY/24); // init cost = enough wealth to socialise for x hours (assuming x hours socialising gives 1 unit)
	public static double SOCIAL_GAIN = 24 / (SOCIAL_HOURS*ITER_PER_DAY); // Amount of socialLevel gained by socialising per iteration (2 hours per day for 1 unit increase)
	public static double COST_SOCIALISE = INITIAL_COST_SOCIALISE / (ITER_PER_DAY * (SOCIAL_HOURS/24)); // cost = initial cost / iterations in a two hour period
	/** Threshold at which the DoNothing motive will take over if all other motives have lower intensities */ 
	public static double DO_NOTHING_THRESHOLD = 0.5; // If all motives < T.HOLD then do perform DoNothing action.

	/** Time spend on search before choosing new target (in hours) */
	public static final double BULLS_EYE_SEARCH_TIME = 0.5;

	//	/** A weight used to reduce the probability that an agent commits a burglary. Used to reduce changce of
	//	 * committing a burglary because so many houses can be viewed each turn */
	//	public static double BURGLARY_

	/** Whether or not to test the environment (will exit before running model). */
	public static final boolean TEST_ENVIRONMENT = false;
	
	/** Used to print similarity of some communities and exit. */
	public static int PRINT_COMMUNITY_SIMILARITY = 0;

	/** The possible types of environment */
	public static enum ENVIRONMENT_TYPES { NULL, GIS, GRID };
	/** THe type of environment being used. This can be set by scenarios using the TYPE_OF_ENVIRONMENT variable */
	public static ENVIRONMENT_TYPES ENVIRONMENT_TYPE=ENVIRONMENT_TYPES.GIS;
	/** Can be used by scenarios to set the type of environment. -1 -> null, 0 -> grid 1 -> GIS (default) */
	public static int TYPE_OF_ENVIRONMENT = 1;

	public static Environment<Building> BUILDING_ENVIRONMENT;
	public static Environment<Road> ROAD_ENVIRONMENT;
	public static Environment<Junction> JUNCTION_ENVIRONMENT;
	public static Environment<Burglar> BURGLAR_ENVIRONMENT;
	public static Environment<Community> COMMUNITY_ENVIRONMENT;

	// Need to explicitly say which Sociotype subclass we're using to read in from shapefiles or flat files.
	// (note: this can also be set using a master parameter in the scenarios file). 
	public static Class<? extends Sociotype> SOCIOTYPE_CLASS = OAC.class;
	
	/** Used in Grid sensitivity test: if 1, ContextCreator will use SensitivityTestSociotype for
	 * the SOCIOTYPE_CLASS, otherwise will use whatever is compiled (OAC.class by default). */
	public static int USE_SENSITIVITY_TEST_SOCIOTYPE = 0;
	/** Make all SensitivityTestSociotypes the same (i.e. compare() will always return 1.0. */
	public static int MAKE_SENSITIVITY_TEST_SOCIOTYPES_SAME = 0;

	public static int RANDOM_SEED = -1;

	/** Can optionally redirect errors to standard out and turn off all other error reporting (so errors that
	 * aren't caught by the model aren't displayed). Hack for a problem with geotools on NGS where geotools
	 * throws loads of errors that I can't catch but don't affect model, this stops them being reported.<br>
	 * 0 -> do not redirect, 1-> redirect*/
	public static int REDIRECT_ERRORS = 1;

	/** A unique name given to each model (and the data store used to store its history). Set in Scenario. */
	public static String MODEL_NAME;
	/** A unique model id which, this is auto-generated from the database and links to the model name. The ID
	 * is used to distinguish between different model results in the database and is set in ContextCreator.populateDataStore()*/
	public static int MODEL_ID;

	/** Whether or not the model is running on the NGS. This is read in from the scenarios file (as a
	 * master parameter). It will change how Derby looks for databases in client mode. (0=false, 1=yes)*/
	public static int ON_NGS = 0 ;

	public static double CONST_TRAVEL_TIME = 5;	// Constant travel time, will take 5 iterations to reach destination. Only used by NullEnvironment

	/** Define 1 unit of distance. In the GRID environment this is 1 (as in 1 cell). In the GIS environment it is
	 * 20m (roughly size of a house) which equates to x units in the model (Worked this out by calculating the
	 * distance between two objects using DistanceOp and GISRoute.distance() functions and comparing outputs.
	 * WARNING: buffer used in getObjectsWithin() different to distance returned by getDistance(Coord, COord) in SimphonyGISEnvironment*/
	public static double getDistanceUnit() {
		if (ENVIRONMENT_TYPE.equals(ENVIRONMENT_TYPES.GRID) || ENVIRONMENT_TYPE.equals(ENVIRONMENT_TYPES.NULL)) {
			return 1.0;
		}
		else {
			//			return 0.00018;
			return 20;
		}
	}

	public static final class GRID_PARAMS { // Parameters for a Simphony grid
		public static int XDIM = 10;
		public static int YDIM = 10;

		public static String GRID_DATA_LOC = "grid_data"; // Database storing some grid environment data
		public static String BUILDING_DATA = "building_data"; // Acc, sec, vis for buildings
		// Locations of image files. NOTE: if using GIMP, make sure you do Image -> Flatten Image (or another function)
		// because it is possible to save a png with four integers for each pixel (e.g. r,g,b,transparacy) but
		// EnvironmentFactory assumes only 3 integers, it wont work otherwise.
		public static  String BUILDINGS_FILENAME = "data/grid/default/city_buildings.png"; // Image storing buildings and roads
		public static String COMMUNITIES_FILENAME = "data/grid/default/city_communities.png";
		// TODO move this so it's stored in the database
		public static String COMMUNITIES_DATA_LOCATION = "data/grid/default/communities_values.csv";

		public static final Color ROAD_COLOR = Color.BLACK;
		public static final Color HOUSE_COLOR = Color.RED;
		public static final Color DRUG_DEALER_COLOR = Color.WHITE;
		public static final Color SOCIAL_COLOR = Color.BLUE;
		public static final Color WORK_COLOR = Color.GREEN;
	}

	public static class GIS_PARAMS {
		/** The root directory for all GIS data, if too large it might not be stored with project */ 
		//		public static String GIS_DATA_ROOT = "data/gis_data/shape-broken/";
		//		public static String GIS_DATA_ROOT = "data/gis_data/shape-correct/";
		public static String GIS_DATA_ROOT = "/Volumes/DISK2/Documents2/research_not_syncd/phd-old/model/eclipse_workspace/BurgdSim/data/gis_data/easel/";
		//		public static String GIS_DATA_ROOT = "/Users/nick/Documents/phd-data/burgdsim_data/leeds/centre/";
		//		public static String GIS_DATA_ROOT = "/Volumes/NICKDISK/phd-data/burgdsim_data/leeds/centre/";
		//		public static String GIS_DATA_ROOT = "/Users/nick/Documents/phd-data/burgdsim_data/leeds/centre/";

		// public static String GIS_DATA_ROOT = "/Users/nick/Documents/phd-data/burgdsim_data/leeds/small_centre/";
		// These allow filenames to be changed by Scenario:
		public static String STATIONS_FILE = "stations.shp";
		public static String COMMUNITIES_FILE = "community-oac.shp";
		public static String BUILDINGS_FILE = "buildings.shp";
		public static String ROADS_FILE = "roads.shp";

		public static String BUILDING_FILENAME = GIS_DATA_ROOT+BUILDINGS_FILE;
		public static String ROAD_FILENAME = GIS_DATA_ROOT+ROADS_FILE;
		public static String PEOPLE_FILENAME = GIS_DATA_ROOT+"one_person.shp";
		public static String STATIONS_FILENAME = GIS_DATA_ROOT+STATIONS_FILE;
		public static String COMMUNITIES_FILENAME = GIS_DATA_ROOT+COMMUNITIES_FILE;
		//		public static final String COMMUNITIES_FILENAME = GIS_DATA_ROOT+"community_oac.shp";
		public static final int HOUSE_TYPE = 1;
		public static final int WORKPLACE_TYPE = 2;
		public static final int SOCIAL_TYPE = 3;
		public static final int DRUG_DEALER_TYPE = 4;

		/** The (possible) location of a serialised BuildingsOnRoads cache object */
		public static String BUILDINGS_ROADS_CACHE_LOCATION = GIS_DATA_ROOT+"buildingsRoadsCache.ser";
		public static String BUILDINGS_ROADS_COORDS_CACHE_LOCATION = GIS_DATA_ROOT+"buildingsRoadsCoordsCache.ser";


		// TODO if buffer is changed to 0.001 the model breaks (something to do with getCoordsAlongRoad) WHY?!?
		public static final double XXXX_BUFFER = 0.002; // used in EnvironmentFactory.findRoadAtCoords() and a couple of times in GISRoute when searching for surrounding objects
		//		public static final double XXXX_little_buffer = 0.000000001; // used in GISRoute.getCoordsAlongRoad()
		public static final double XXXX_little_buffer = 0.0000001; // used in GISRoute.getCoordsAlongRoad()

		//		public static final double XXXX_little_buffer = 0.00001; // used in GISRoute.getCoordsAlongRoad()
		//		public static final double TRAVEL_PER_TURN = ISRoute.convertFromMeters(100*GlobalVars.MINS_PER_ITER); // assume 4 mph -> 100 meters per min

		//		public static double TRAVEL_PER_TURN = 100*GlobalVars.MINS_PER_ITER; // 4 mph -> 100 meters per min
		// Increase by 20% to fix problem with agents not being able to satisfy needs.
		public static double TRAVEL_PER_TURN = 120*GlobalVars.MINS_PER_ITER; 

	}

	// Parameters used by transport networks
	public static final class TRANSPORT_PARAMS {

		// This variable is used by NetworkEdge.getWeight() function so that it knows what travel options
		// are available to the agent (e.g. has a car). Can't be passed as a parameter because NetworkEdge.getWeight()
		// must override function in RepastEdge because this is the one called by ShortestPath.
		public static Burglar currentBurglar = null;
		public static Object currentBurglarLock = new Object();

		public static final String WALK = "walk";
		public static final String BUS = "bus";
		public static final String TRAIN = "train";
		public static final String CAR = "car";
		// List of all transport methods in order of quickest first
		public static final List<String> ALL_PARAMS = Arrays.asList(new String[]{TRAIN, CAR, BUS, WALK});

		// Used in 'access' field by Roads to indicate that they are a 'majorRoad' (i.e. motorway or a-road).
		public static final String MAJOR_ROAD = "majorRoad";		
		// Speed advantage for car drivers if the road is a major road'
		public static final double MAJOR_ROAD_ADVANTAGE = 3;

		// The speed associated with different types of road (a multiplier, i.e. x times faster than walking)
		public static double getSpeed(String type) {
			if (type.equals(WALK))
				return 1;
			else if (type.equals(BUS))
				return 2;
			else if (type.equals(TRAIN))
				return 10;
			else if (type.equals(CAR))
				return 5;
			else {
				Outputter.errorln("GlobalVars.TRANSPORT_PARAMS.getSpeed() error: unrecognised type: "+type);
				return 1;
			}
		}
	}

	/** Parameters for the output area classification */
	public static final class OAC_PARAMS {
		/** Calculate derived values from the average values of the the area's group */
		public static boolean AVERAGE_DERIVED_VARS = false;
		// Database/directory where oac data are stored
		public static String OAC_DATA_LOC = "oac_data";

		// Table/file where subgroup profiles are stored
		public static String SUBGROUP_PROFILES = "subgroup_profiles";
		// Where to store the weights which are used when calculating community occupancy levels
		public static String OCCUPANCY_WEIGHTS = "occupancy_weights";

	}

	// Parameters which control the implementation of the Burglars' memory. Either MAP or LIST
	public static final class MEMORY_IMPLEMENTATIONS {
		public static final String MAP = "MAP";
		public static final String LIST = "LIST";
		//		private static String type = null;
		/**
		 * Get the memory type used in the simulation. Can be either LIST or MAP. If on a batch run then
		 * get the memory type from the parameters file (can be altered), if not on batch run then always use
		 * map
		 */
		public static final String getImplementation() {
			return MAP; // Just return a map
			
			//			if (!GlobalVars.BATCH_RUN)
			//				return MAP;
			//			else {
			//				if (type==null) {
			//					type = (String)RunEnvironment.getInstance().getParameters().getValue("memory_type");
			//					if ( ! (type.equals(LIST) || type.equals(MAP) )) {
			//						Outputter.errorln("GlobalVars.MEMORY_IMPLEMENTATIONS error: unrecognised burglar memory type" +
			//								" read in from batch parameters file: "+type);
			//					}
			//				}
			//				return type;
			//			}
		}
	}

	// A class which contains all the weights used in the burglary process. These can be different for each
	// agent (they are set using the VictimChooser and TargetChooser constructors). See BurglarAgents.doc
	// for an explanation of what these weights do.
	public static final class BURGLARY_WEIGHTS {
		public static enum TARGET_CHOOSER { // used in TargetChooser, looking for a Community to search 
			DIST_W, 		// distance between current position and potential target
			ATTRACT_W, 		// attractiveness of potential target
			SOCIALDIFF_W, 	// difference between target sociotype and home sociotype
			PREVSUCC_W 		// number of previous successes 
		} ;
		public static enum VICTIM_CHOOSER { // used in VictimChooser, looking for an individual House to burgle
			CE_W,			// Collective efficacy of the community the house is in
			TV_W,			// Traffic volume of the community
			OCC_W,			// Occupancy levels in the community
			ACC_W,			// Accessibility of the house
			VIS_W,			// Visibility of the house
			SEC_W			// Security of the house
		};
	} // BURGLARY_WEIGHTS class

	/** Some parameters specific to burglary */
	public static class BURGLARY_PARAMS {
		/** This is used by the DislikeSecurityBurglar in scenario Base 8. It is the weight that the burglar will
		 * apply to building security when looking for a victim (default is 1). */
		public static double DISLIKE_SECURITY_BURGLARY_WEIGHT = 1.0;
	}

	/** Parameters which determine the effect of a burglary on the houses surrounding the burgled property.*/
	public static final class BURGLARY_BUILDING_PARAMS {
		/** Amount that security will deteriorate per day (0.5 unit per week at the moment)*/
		public static final double SECURITY_DETERIORATE = 0.5/7;
		/** Amount that security will increase after a burglary for properties 1 unit away from the burglary
		 * (this deteriorates so that further properties are also affected but not by as much*/
		public static double SECURITY_DISTANCE_W = 2.0;
		/** The amount that security will increase in the house that was burgled */
		public static double SECURITY_INCREASE = 5.0;
		/** The cut-off point for increasing security of buildings: if security increase is less than this
		 * value (i.e. house a long way from burglary) then don't bother increasing security because it is
		 * deemed negligible. */
		public static final double NEGLIDGIBLE_SECURITY_INCREASE = 0.1;

		/** Whether or not to increase security after a burglary at all (used in Base6 experiment to try to
		 * make burglaries more clustered. 1 -> increase (default), 0 -> don't increase */
		public static int INCREASE_SECURITY_AFTER_BURGLARY = 1;
		
		/** Whether or not to increase the security of surrounding properties after a burglary.
		 * 1 -> increase (default), 0 -> don't increase */
		public static int INCREASE_SECURITY_SURROUNDING_BURGLARY = 1;

		/** Instead of using linear security deterioration (specified by SECURITY_DETERIORATE variable), if this
		 * is true (1) then the security will halve each week instead. This is used in the Base 7 scenario to
		 * try to stop security increasing indefinitely throughout the course of a simulation. Default is 0 
		 * (use normal method)*/
		public static int HALVE_SECURITY_OVER_WEEK = 0;
	}

	/** Filenames / database table names where model history should be stored and booleans indicating what
	 * should be stored. NOTE: need to be careful about setting output information to true/false, e.g. if
	 * not outputting burglar information then trying to write burglary information will probably throw an
	 * error (if database with keys is used). */
	public static final class HISTORY_PARAMS {
		public static final String RESULTS_DATABASE = "model_results";
		public static final String MODEL_NAMES = "ModelNames"; // table to store model names and a unique id

		/** Can be used by Scenario to turn all output on or off. 0 means off, 1 means on and the -1 here
		 * means don't do anything (i.e. unless the parameter has been specified in the scenarios file leave
		 * the values as they are. */
		public static int ALL_OUTOUT_ONOFF = -1;

		/* All the parameters. 1 is true, 0 false. */
		public static int COMMUNITIES_OUT = 1;
		public static int SCENARIO_PARAMETERS_OUT = 1;
		public static int BUILDINGS_OUT = 1;
		public static int BURGLARS_OUT = 1;
		public static int BURGLAR_INFO_OUT = 1;
		public static int BURGLAR_MEMORY_OUT = 1;

		public static int SECURITY_OUT = 0;		
		public static int STATE_VARIABLES_OUT = 0;
		public static int MOTIVES_OUT = 0;
		public static int ACTIONS_OUT = 0;

		public static int BURGLARY_OUT = 1;

		//		public static boolean COMMUNITIES_OUT = true;
		//		public static boolean SCENARIO_PARAMETERS_OUT = true;
		//		public static boolean BUILDINGS_OUT = true;
		//		public static boolean BURGLARS_OUT = true;
		//		public static boolean BURGLAR_INFO_OUT = true;
		//		public static boolean BURGLAR_MEMORY_OUT = true;
		//		
		//		public static boolean SECURITY_OUT = false;		
		//		public static boolean STATE_VARIABLES_OUT = false;
		//		public static boolean MOTIVES_OUT = false;
		//		public static boolean ACTIONS_OUT = false;
		//		
		//		public static boolean BURGLARY_OUT = true;


		public static final String COMMUNITIES = "Communities";
		public static final String SCENARIO_PARAMETERS = "ScenarioParameters";
		public static final String BUILDINGS = "Buildings";
		public static final String SECURITY = "Secure"; // "Security" is a reserved database word, use "Secure" instead
		public static final String BURGLARS = "Burglars";
		public static final String BURGLAR_INFO = "BurglarInfo";
		public static final String BURGLAR_MEMORY = "BurglarMemory";
		public static final String STATE_VARIABLES = "StateVariables";
		public static final String MOTIVES = "Motives";
		public static final String ACTIONS = "Actions";
		public static final String BURGLARY = "Burglary";
	}
	/** The number of burglars to store information about at each iteration (if -1 then store all burglars'
	 * information). Might still store information once per day (see HISTORY_PARAMS.BURGLARS_OUT) */
	public static int STORE_NUM_BURGLARS = 2;

	/** Variables used in NULL environment sensitivity testing not related to burglary (i.e. PECS parameters) */
	public static class NON_BURGLARY_SENSITIVITY_TEST_PARAMETERS {

		/** Whether or not to use the History class to print and graph agent information (default 0, no). */
		public static int USE_HISTORY = 0;
		public static History HISTORY;

		/** Wether or not the History should print a history table (all agent needs and actions at every iteration).
		 * Default 0, no */
		public static int PRINT_HISTORY_TABLE = 0;

		/** Whether or not BurgdSimMain should summarise all agents' daily state variable / motive values. 
		 * Not implemented in BurgdSimMainMPJ so doesn't apply to grid runs. (Default 0, no).*/
		public static int PRINT_AGENT_SUMMARY_TABLE = 0;
		/** The test variable which is currently being used in sensitivity tests. This is used so that 
		 * BurgdSimMain knows which variable to add to the table it generates summarising agent motive / state
		 * variable values. Default is empty string. */
		public static String TEST_VARIABLE = "";		
	}

	/** Variables used in sensitivity testing with NULL and Grid environments which effect burglary */
	public static class BURGLARY_SENSITIVITY_TEST_PARAMETERS {

		/** Variable is used when when performing sensitivity tests using the NULL environment. See 
		 * EnvironmentFactory.createBuildingsAndRoads() to see what effects different strings will have. By default
		 * (empty string) this will just create a single building of each type and a single community).
		 */
		public static String NULL_ENVIRIONMENT_BUILDING_CONFIGURATION = "";

		/** A string representation of the parameter being tested. This will be set by EnvironmentFactory or
		 * BurglarFactory when the simulation is being set up. 
		 * (Can't specify the field directly in the scenarios file because it is internal to an 
		 * agent/house/community so isn't normally set that way.) */
		public static String ENVIRONMENT_SENSITIVITY_TEST_PARAMETER = "";
		
		/** The burglar weight being tested. If this is the empty string (default) then it is ignored and it
		 * is assumed that the parameter being tested is a building or community variable defined in 
		 * ENVIRONMENT_SENSITIVITY_TEST_PARAMETER. Otherwise this string represents a burglar behaviour weight 
		 * being tested and the ENVIRONMENT_SENSITIVITY_TEST_PARAMETER must be the corresponding environment 
		 * parameter so that EnvironmentFactory knows which parameter to vary when creating buildings or communities. 
		 * For example, if this is "ACC_W" then the ENVIRONMENT_SENSITIVITY_TEST_PARAMETER should be "Accessibility" 
		 * (house accessibility): eleven houses will be created with increasing accessibility values and 11 burglars 
		 * will be created with ascending ass_w values.  */
		public static String BURGLAR_SENSITIVITY_TEST_PARAMETER = "";
		
		/** USed in grid tests when instead of increasing a parameter value between 0 and 1 a burglar is created
		 *  with this value. */
		public static double BURGLAR_PARMETER_VALUE = -1.0;

		/** Stop community occupancy changing depending on the time of day, make it return a particular, constant, 
		 * value. Default 0 means ignore this and calculate occupancy properly*/
		public static int CONST_OCC = 0;
		/** Stop community attractiveness changing depending on the time of day, make it return a particular, constant, 
		 * value. Default 0 means ignore this and calculate attractiveness properly*/
		public static int CONST_ATT = 0;
		/** Stop building traffic volume changing depending on the time of day, make it return a particular, constant, 
		 * value. Default 0 means ignore this and calculate traffic volume properly*/
		public static int CONST_TV = 0;
		
		/** The x coordinate of the house that the agent should start in. Default -1 (ignore) */
		public static int BURGLAR_HOME_X = -1;
		/** The y coordinate of the house that the agent should start in. Default -1 (ignore) */
		public static int BURGLAR_HOME_Y = -1;

		/**
		 * This is used in the GIS sensitivity tests, it determines whether or not the burglars created
		 * have access to public transport.
		 * 
		 * <ul><li>0 (default) -> ignore the parameter</li>
		 * <li> 1 -> Run test but burglars don't have access to transport </li>
		 * <li> 2 -> Run test and burglars do have access to transport</li></ul> 
		 */
		public static int GIS_SENSITIVITY_TEST_TYPE = 0;
		
		/**
		 * Used in GIS sensitivity tests to make change the value of the DISTW burglary parameter.
		 * -1.0 (default) means ignore, otherwise value must be in range 0-1.
		 */
		public static double GIS_SENSITIVITY_TEST_DISTW = -1.0;
		
		
		/**
		 * Set the parameter being tested on the given object to the given value. This method will map 
		 * SENSITIVITY_TEST_PARAMETER strings to fields in a House, Community, VictimChooser or TargetChoooser
		 * as apropriate
		 * @param o The object to have its parameter set
		 * @param value The value to set the parameter
		 * @param setValue Whether or not to actually set the value. If false then don't set the value, just return
		 * boolean indicating whether or not the operation would have been successful.
		 * @return True if the operation was successful, false otherwise.
		 */
		public static <T> boolean setObjectValue(T inObject, Double value, boolean setValue) {
			try {
				T o = inObject;
				//				T o = null; // Use generic object so don't have to have different methods for House, Community etc.
				//				// Try to cast the object
				//				if (inObject instanceof Building) {
				//					o = (T) ((Building) inObject);
				//				}
				//				else if (inObject instanceof Community) {
				//					o = (T) ((Community) inObject);
				//				}
				//				else if (inObject instanceof OAC) {
				//					o = (T) ((OAC) inObject);
				//				}
				//				else {
				//					Outputter.errorln("GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.setObjectValue(): unrecognised " +
				//							"type of object: "+inObject.getClass().toString()+". Cannot set the parameter value.");
				//				}
				// Go through all the fields on the object, seeing if there is one that is a set* method for the
				// parameter being tested (SENSITIVITY_TEST_PARAMETER).
				for (Method m:o.getClass().getDeclaredMethods()) {
					//					System.out.println("Method: "+m.getName());
					String methodName = m.getName();
					methodName.toLowerCase();
					// See if field is a set method
					if (methodName.startsWith("set")) {
						//						System.out.println("\t is set method");
						String name = methodName.substring(3).toLowerCase(); // Name of the method in lower case without 'set'
						// See if method name matches the SENSITIVITY_TEST_PARAMETER
						if (name.equals(ENVIRONMENT_SENSITIVITY_TEST_PARAMETER.toLowerCase())) {
							if (setValue) { // Want to set the value, invoke the method and return
								Outputter.debugln("GlobalVars.setObjectValue() invoking method "+m.getName()+" on object "+
										inObject.toString()+" with value "+value, Outputter.DEBUG_TYPES.INIT);
								m.invoke(inObject, value);
								// System.out.println("\t\t names match");
								return true;
							}
							else { // Otherwise just want to see if operaiton would have been successful
								return true;
							}								
						} // if names match
					} // if methodName starts with set
				} // for methods
				// If here then no appropriate method was found
				if (!setValue) { // Doesn't matter, just wanted to know if the operation would have been successful
					return false;
				}
				else { // Does matter, we wanted the operation to succeed
					Outputter.errorln("GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.setObjectValue(): could "+
							"not find an appropriate set method for the field "+ENVIRONMENT_SENSITIVITY_TEST_PARAMETER+" in " +
							"the object of type: "+o.getClass()+". Exiting program.");
					System.exit(1);
					return false;
				}

			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			return false; // If here then operation was unsuccessful, an error might have been caught.
		}

	}

	/** Store all the caches used throughout the simulation so they can be cleared if the sim needs to be restarted.
	 * Cacheable classes must declare themselves by using the add() method.
	 * @author Nick Malleson
	 */
	public static class CACHES {
		private static Map<Cacheable, ?> caches; // Objects which have caches which need to be cleared
		private static Map<Class<? extends Cacheable>, ?> cachedClasses; // Only need to cache one object of each class 

		public static void add(Cacheable c) {
			checkNull();
			// Only add the object if it's class hasn't already been added
			if (!cachedClasses.containsKey(c.getClass())) {
				caches.put(c, null);
				cachedClasses.put(c.getClass(), null);				
			}
		}

		public static void clear() {
			checkNull();
			String cacheString = "GlobalVars.Caches.clear(): ";
			if (caches.keySet().size()==0) {
				cacheString+="nothing to clear.";
			}
			else {
				for (Cacheable c:caches.keySet()) {
					cacheString+=c.getClass().getName().toString()+", ";
					c.clearCaches();
				}
			}
			Outputter.debugln(cacheString, Outputter.DEBUG_TYPES.GENERAL);
			System.out.println(cacheString);
			// Reset the maps used here as well
			caches.clear();
			cachedClasses.clear();
			// Do some garbage collection to try to clear the memory of all these static variables.
			Runtime.getRuntime().gc();
		}

		private static void checkNull() {
			if (caches==null) caches = new HashMap<Cacheable, Object>();
			if (cachedClasses==null) cachedClasses = new HashMap<Class<? extends Cacheable>, Object>();
		}
	}

	/** This method has to be called by Scenario once it has finished updating all fields, which is crap!
	 * Basically because some fields are calculated from other fields (e.g. MINSPERITER)
	 * they have to be recalculated when the fields they are based on are updated. This happens because
	 * all the variables are calculated on initialisation of the GlobalVars class, not dynamically as fields
	 * are updated. I should completely re-design this class but I can't be bothered.
	 */
	public static void recalcDerivedFields() {
		MINS_PER_ITER = (60.0*24)/ITER_PER_DAY;
		//		DETERIORATE_AMOUNT = 1.0/GlobalVars.ITER_PER_DAY;
		//		SLEEP_GAIN = 24.0 / (8*ITER_PER_DAY);
		//		WORK_GAIN = 24.0 / (8*ITER_PER_DAY);
		//		INITIAL_COST_SOCIALISE = WORK_GAIN * SOCIAL_HOURS*(ITER_PER_DAY/24.0);
		//		SOCIAL_GAIN = 24.0 / (SOCIAL_HOURS*ITER_PER_DAY);
		//		COST_SOCIALISE = INITIAL_COST_SOCIALISE / (ITER_PER_DAY * (SOCIAL_HOURS/24.0));

		GIS_PARAMS.BUILDING_FILENAME = GIS_PARAMS.GIS_DATA_ROOT+GIS_PARAMS.BUILDINGS_FILE;
		GIS_PARAMS.ROAD_FILENAME = GIS_PARAMS.GIS_DATA_ROOT+GIS_PARAMS.ROADS_FILE;
		GIS_PARAMS.PEOPLE_FILENAME = GIS_PARAMS.GIS_DATA_ROOT+"one_person.shp";
		GIS_PARAMS.STATIONS_FILENAME = GIS_PARAMS.GIS_DATA_ROOT+GIS_PARAMS.STATIONS_FILE;
		GIS_PARAMS.COMMUNITIES_FILENAME = GIS_PARAMS.GIS_DATA_ROOT+GIS_PARAMS.COMMUNITIES_FILE;

		GIS_PARAMS.BUILDINGS_ROADS_CACHE_LOCATION = GIS_PARAMS.GIS_DATA_ROOT+"buildingsRoadsCache.ser";
		GIS_PARAMS.BUILDINGS_ROADS_COORDS_CACHE_LOCATION = GIS_PARAMS.GIS_DATA_ROOT+"buildingsRoadsCoordsCache.ser";

		//		GIS_PARAMS.TRAVEL_PER_TURN = 100*GlobalVars.MINS_PER_ITER; // 4 mph -> 100 meters per min
		// Increase by 20% to fix problem with agents not being able to satisfy needs.
		GIS_PARAMS.TRAVEL_PER_TURN = 120*GlobalVars.MINS_PER_ITER; 
	}
}