package burgdsim.scenario;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import repast.simphony.random.RandomHelper;

import burgdsim.burglars.BurglarFactory;
import burgdsim.data_access.DataAccess;
import burgdsim.data_access.DataAccessFactory;
import burgdsim.environment.Sociotype;
import burgdsim.main.GlobalVars;
import burgdsim.main.Logger;
import burgdsim.main.Outputter;

public class Scenario implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private List<Parameter<?>> parameters;
	private String name = null;
	
	private BurglarFactory burglarFactory; // A burglar factory instance that will be used in this Scenario
	
	private Class<? extends Sociotype> sociotypeClass; // The type of sociotype to use

	private Logger logger = null; // Can be set by an MPJ slave, tells Outputter to log data
	
	/**
	 * Generate a new Scenario based on the given parameters. This will set all the GlobalVars values.
	 * @param parameters A list of Parameter objects which describe the scenario
	 */
	public Scenario(List<Parameter<?>> parameters) {
		this.parameters = parameters;
	}
	
//	/**
//	 * Create a new scenario wih the same name and list of parameters as the given scenario.
//	 * @param t
//	 */
//	public Scenario(Scenario t) {
//		this.name = t.name;
//		this.parameters = t.parameters;
//	}

	/**
	 * Make this Scenario set the GlobalVars values according to its list of Parameters.
	 */
	public void execute() throws Exception {
		this.name = generateScenarioName();	
		GlobalVars.MODEL_NAME = this.name;
		this.setGlobalVars(); // Update the GlobalVars values from the parameters
//		this.initialiseDataStore(); // Not doing this now that all results saved to same database
		this.initialiseOutput(); // Check the value of the ALL_OUTOUT_ONOFF to turn all output on or off
		if (this.sociotypeClass!=null) {
			GlobalVars.SOCIOTYPE_CLASS = this.sociotypeClass;	
		}
		BurglarFactory.setInstance(this.burglarFactory); // Set the BurglarFactory which will be used to create burglars
		
		// Set the logger, this might have been set by an MPJ slave, otherwise it will be null (and wont do anything).
		GlobalVars.logger = this.logger; 
		
		GlobalVars.SCENARIO_INITIALISED = true; // This will stop ContextCreator generating any (more) Scenarios
	}
	/** Iterate over all the Parameters and look for a GlobalVars field that matches it, setting the new 
	 * GlobalVars value if one is found.
	 *  
	 * @throws IllegalAccessException If one of the variables cannot be written
	 */
	private void setGlobalVars() throws IllegalAccessException {
	
		Field debugField = null; // Used so that if errors are caught the associated field can be found
		try {
			// Build up a list of fields in global vars and its inner classes
			List<Field> fieldList = new ArrayList<Field>(); 
			for (Field f:GlobalVars.class.getFields()) {
				fieldList.add(f);
			}
			for(Class<?> c:GlobalVars.class.getClasses()) {
				for (Field f:c.getFields()) {
					fieldList.add(f);
				}
			}
			// Now look through parameters to find matching fields
			for (Parameter<?> p:this.parameters) {
				boolean foundField = false;
				for (Field f:fieldList) {
					debugField = f;
					if (f.getName().equals(p.getName())) {
						f.set(null, p.getValue());
						Outputter.debugln("Scenario.setGlobalVars() set field "+f.getName()+" to "+p.getValue(),
								Outputter.DEBUG_TYPES.INIT);
						foundField = true;
					} // if name
				} // for field
				if (!foundField) {
					Outputter.errorln("Scenario.setGlobalVars() warning: couldn't find a field in GlobalVars " +
							"that matches the paramter: "+p.getName()+". This might not be a problem, but the " +
							"parameter should be removed from the scenarios file if it isn't being used any more.");
				} // if foundField
			} // for paramter
			// Some derived variables will need to be recalculated
			GlobalVars.recalcDerivedFields();
		} catch (IllegalArgumentException e) {
			Outputter.errorln("Scenario.setGlobalVars(): caught IllegalArgumentException trying to set a field. Possible cause is field: "+debugField);
			throw e;
		} catch (IllegalAccessException e) {
			Outputter.errorln("Scenario.setGlobalVars(): caught IllegalAccessException trying to set a field. Possible cause is field: "+debugField);
			throw e;
		}
	}
	
	public void setSociotypeClass(Class<? extends Sociotype> s) {
		this.sociotypeClass = s;
	}
	
	public void setBurglarFactory(BurglarFactory bf) {
		this.burglarFactory = bf;
	}

	/**
	 * Initialise a new data store which will store the model history and results. Create all the tables
	 * and relationships between them.  NOTE: this doesn't use the DataAccess hierarchy properly, it
	 * generates SQL which (obviously) wouldn't work with flat files. NOTE: this isn't used any more, now
	 * I create the databases beforehand and all model results are stored in same db.
	 * @throws Exception  If there are other problems creating the new database (e.g. thrown by DataAccessFactory). 
	 * @throws IllegalArgumentException 
	 * @throws UnsupportedOperationException 
	 * @throws FileNotFoundException If there are problems reading the required SQL file which dictates the
	 * tables and relationships to be created
	 */
	@SuppressWarnings("unused")
	private void initialiseDataStore() throws UnsupportedOperationException, IllegalArgumentException, Exception  {
		// Create a new data store object to store model results to
		DataAccess resultsStore = DataAccessFactory.getDataAccessObject(GlobalVars.HISTORY_PARAMS.RESULTS_DATABASE);
		// Create tables required for storing results
		String dataSchema = "data/create_tables2.sql";
		String fullString = this.readSQLFile(dataSchema); // SQL which will create all required tables and relations etc.
		Outputter.debugln("Scenario.initialiseDataStore() creating database schema from file: "+
				new File(dataSchema).getAbsoluteFile(), Outputter.DEBUG_TYPES.SCENARIOS);
		String[] sqlStrings = fullString.split(";"); // Split on ';' to create different tables.
		for (String sql:sqlStrings) {
			resultsStore.createCustomFiles(sql);
		}
	}
	
	/** Used to turn on/off all database output. This could be done by setting every individual GlobalVars
	 * parameter in the scenario file, but writing one line is easier! */
	private void initialiseOutput() {
		int value;
		if (GlobalVars.HISTORY_PARAMS.ALL_OUTOUT_ONOFF == 0) { // turn all output off
			Outputter.debugln("Scenario turning all history output off (nothing to be written to database)", 
					Outputter.DEBUG_TYPES.INIT);
			value = 0;
		}
		else if (GlobalVars.HISTORY_PARAMS.ALL_OUTOUT_ONOFF == 1) { // turn all output on
			Outputter.debugln("Scenario turning all history output on (everything to be written to database)", 
					Outputter.DEBUG_TYPES.INIT);
			value = 1;
		}
		else if (GlobalVars.HISTORY_PARAMS.ALL_OUTOUT_ONOFF == -1) { // levae the values as they are
			return;
		}
		else {
			Outputter.errorln("Scenario.initialiseOutput(): unrecognised ALL_OUTPUT_ONOFF value: "+
					GlobalVars.HISTORY_PARAMS.ALL_OUTOUT_ONOFF+". Should be 0, 1 or -1.");
			return;
		}
		// Set the history params
		GlobalVars.HISTORY_PARAMS.COMMUNITIES_OUT = value;
		GlobalVars.HISTORY_PARAMS.SCENARIO_PARAMETERS_OUT = value;
		GlobalVars.HISTORY_PARAMS.BUILDINGS_OUT = value;
		GlobalVars.HISTORY_PARAMS.SECURITY_OUT = value;
		GlobalVars.HISTORY_PARAMS.BURGLARS_OUT = value;
		GlobalVars.HISTORY_PARAMS.BURGLAR_INFO_OUT = value;
		GlobalVars.HISTORY_PARAMS.BURGLAR_MEMORY_OUT = value;
		GlobalVars.HISTORY_PARAMS.STATE_VARIABLES_OUT = value;
		GlobalVars.HISTORY_PARAMS.MOTIVES_OUT = value;
		GlobalVars.HISTORY_PARAMS.ACTIONS_OUT = value;
		GlobalVars.HISTORY_PARAMS.BURGLARY_OUT = value;
	}
	
	private String readSQLFile(String fileLocation) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(fileLocation));
		String sqlString = "";
		String line = br.readLine();	
		while (line!=null) {
			if (! (line.equals("") || line.charAt(0)=='-' ) ) { // indicate start of comments or an empty line
				sqlString += line+"\n";
			}
			line = br.readLine();
		}
		br.close();
		return sqlString;
	}
	
	/**
	 * Generate a unique scenario name. This is important because the data store which will hold the results
	 * of the model which is configured to represent this Scenario will have this name. It is set to "MDL" 
	 * with current date/time (in format YY-MM-DD-HH_MM_SS) and a unique id (e.g. MDL-08-05-15-13_47-9).  
	 * @return
	 */
	private static String generateScenarioName() {
		Calendar c = new GregorianCalendar();
		String name = "MDL-";
		String year = String.valueOf(c.get(Calendar.YEAR)).substring(2, 4);
		String month = make2num(c.get(Calendar.MONTH)+1);
		String day = make2num(c.get(Calendar.DAY_OF_MONTH));
		String hour = make2num(c.get(Calendar.HOUR_OF_DAY));
		String min = make2num(c.get(Calendar.MINUTE));
		String sec = make2num(c.get(Calendar.SECOND));
		String millisec = String.valueOf(c.get(Calendar.MILLISECOND));
		return name+year+"-"+month+"-"+day+"-"+hour+"_"+min+"_"+sec+"_"+millisec+"_"+make2num(RandomHelper.nextInt());
	}
	
	/* make strings 2 number format, e.g. 8 -> 08 and  2006 -> 06, or any length -> 2 (e.g. 213837934 -> 21)*/
	private static String make2num(int in) {
		String str = String.valueOf(in); 
		if (str.length()==1) 
			str = "0"+str;
		else if (str.length()==4)
			str = str.substring(2, 4);
		else
			str = str.substring(0,2);
		return str;
	}

	public String toString() {
		if (this.name == null) {
			return "Scenario (no name yet): "+parameters.toString();
		}
		else {
			return "Scenario ("+name+"): "+parameters.toString();
		}
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}
}
