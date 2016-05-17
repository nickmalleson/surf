package burgdsim.scenario;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import burgdsim.burglars.BurglarFactory;
import burgdsim.environment.OAC;
import burgdsim.environment.Sociotype;
import burgdsim.environment.VancouverSociotype;
import burgdsim.main.Outputter;

public class ScenarioGenerator {

	//	private List<Parameter<?>> parameters;		// The list of parameters which will be used to screate scenarios
	private List<Scenario> scenarios;		// The scenarios themselves
	//	private Iterator<Scenario> scenarioIt;		// Used to return next scenario
//	private static int uniqueScenarioID = 0;	// Used to generate a unique name for the scenario

	/* 'Master Parameters' - will be applied to overall run, not single scenarios*/
	private int numSweeps = 1;	// The number of batch sweeps (number of times to run each individual scenario)
	
	/* A BurglaryFactory instance that will be used by the Scenario obejcts to create burglars */
	private BurglarFactory burglarFactory;
	
	/* The type of sociotype to use */
	private Class<? extends Sociotype> sociotypeClass;

	/**
	 * Create a new scenario generator. The scenario generator will a scenarios file to generate scenarios.
	 * It requires the 'scenarios/current_scenario' file to determine which scenarios file to read
	 * Read the parameters file which will be used to generate Scenarios.
	 * @param f THe file containing the model parameters
	 * @throws Exception 
	 * 
	 */
	public ScenarioGenerator() throws Exception {
//		GlobalVars.CACHES.add(this);
		// Find out which scenarios file to read
		File scenariosFile = null;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File("scenarios/current_scenario")));
			String line = reader.readLine();
			scenariosFile = new File("scenarios/"+line);
			Outputter.debugln("ScenarioGenerator() will read scenarios file: "+scenariosFile.getAbsolutePath(),
					Outputter.DEBUG_TYPES.INIT);


		}
		catch (IOException e) {
			Outputter.errorln("ScenarioGenerator() error: problem parsing the file which declares the scenario " +
			"to use: scenarios/current_scenario");
			throw new Exception(e);
		}
		List<Parameter<?>> parameters = this.parseFile(scenariosFile); // parse the file
		this.scenarios = this.generateScenarios(parameters); // generate the list of scenarios (including how to create burglars).
		//		this.scenarioIt = scenarios.iterator(); 

	}

	/**
	 * Read the given parameters file and create a list of parameters.
	 * @param f The input file
	 * @return
	 * @throws Exception If there is a problem reading the input file file
	 */
	private List<Parameter<?>> parseFile(File f) throws Exception {
		// Check the file exists
		if (!f.exists())
			this.error("ScenarioGenerator, cannot parse scenario file: "+f.getName()+" because it doesn't exist");

		List<Parameter<?>> params = new ArrayList<Parameter<?>>();
		// Start reading the file
		BufferedReader reader = new BufferedReader(new FileReader(f));
		int filePart = 1; // Part of file, 1st, 2nd or 3rd (will try to parse differently depending on part)
		String line = StringUtils.strip(reader.readLine()); // Strip whitespace from start/end of line
		int lineCount = 1;
		while (line!=null) { // while there are still lines to read
			if (line.equals("")) { // Ignore empty lines
				line = reader.readLine();
				lineCount++;
				continue;
			}
			else if (line.charAt(0) == '#') { // Ignore lines starting with '#', they're comments
				line = reader.readLine();
				lineCount++;
				continue;

			}
			else if (line.charAt(0) == '*') { // '*' indicates moving onto next part of file
				filePart+=1;
				line = reader.readLine();
				lineCount++;
				continue;
			}

			line = removeComments(line); 	// There might be additional comments at the end of the line
			line = removeWhitespace(line); 	// There might be spaces in the line

			if (filePart==1) {  // Parse first part, information about how many times to run each scenario etc.	
				int splitVal = line.indexOf(':');
				String param = line.substring(0, splitVal);
				int value = Integer.valueOf(line.substring(splitVal+1, line.length()));
				this.setMasterParameter(param, value);
			}

			else if (filePart==2) {  // Parse the second part, all the model parameters
				String[] lineValue = line.split(","); // Split the line on commas
				//				for (int i=0; i<lineValue.length; i++)
				//					lineValue[i] = lineValue[i].replace(" ",""); // strip whitespace
				if (!(lineValue.length==3 || lineValue.length==6)) {
					Outputter.errorln("ScenarioGenerator.parseFile() wrong number of parameters on line "+
							lineCount+": "+line+". Will continue reading file.");
					line = reader.readLine();
					lineCount++;
					continue;					
				} // if lineValue != 3 or 5
				// Create different parameter depending on its type
				Parameter<?> param = null;
				if (lineValue[1].equals("int")) { // An integer parameter
					if (lineValue.length==3)
						param = new NumericParameter<Integer>(lineValue[0],Integer.valueOf(lineValue[2]));
					else 
						param = new NumericParameter<Integer>(lineValue[0],Integer.valueOf(lineValue[2]),Integer.valueOf(lineValue[3]),Integer.valueOf(lineValue[4]),Integer.valueOf(lineValue[5]));
				}
				else if (lineValue[1].equals("double")) { // A double parameter
					if (lineValue.length==3) 
						param = new NumericParameter<Double>(lineValue[0],Double.valueOf(lineValue[2]));
					else 
						param = new NumericParameter<Double>(lineValue[0],Double.valueOf(lineValue[2]),Double.valueOf(lineValue[3]),Double.valueOf(lineValue[4]),Double.valueOf(lineValue[5]));
				}		
				else if (lineValue[1].equals("string")) { // Strings cannot be dynamic, no init or final values
					param = new StringParameter( // Remove quotes from around strings
							lineValue[0].replaceAll("\"", ""),lineValue[2].replaceAll("\"", "")); 
				}
				else {
					Outputter.errorln("ScenarioGenerator.parseFile() unrecognised parameter type on line "+
							lineCount+": "+lineValue[1]);
				}
				Outputter.debugln("ScenarioGenerator.parseFile() found model parameter: "+param.toString(), 
						Outputter.DEBUG_TYPES.SCENARIOS);
				params.add(param);

			} // if filePart==2

			else if (filePart==3) {  // Parse the last part, information for the BurglarFactory.
				// The line should start <keyword>: <other stuff depending on keyword>
				String[] lineSplit = line.split(":");
				if (lineSplit.length!=2) {
					this.error("ScenarioGenerator.parseFile(): error reading burglar parameters. Could not " +
							"split on a colon after a keyword. Line: "+line);
				}
				String keyword = lineSplit[0];
				if (keyword.equals("shapefile")) { // Use a shapefile to create burglars
					this.error("ScenarioGenerator.parseFile error: not implemented shapefile method of reading " +
							"burglar information");
				}
				// The number of burglars to be created and their types are stored with the communities data.
				// Instructions define which types of burglar to create and which columns in community data say
				// how many of that type should be created. The column names must be called BglrCX (where X is a
				// number between 1 and 5, i.e. five possible columns to store burglar numbers in).
				// E.g. create default and professionals:  "communities:DefaultBurglar$Bgclr1,ProfessionalBurglar$Bgclr2"
				if (keyword.equals("communities")) {
					String error = ""; // check of an error has occurred parsing the burglar information.
					String value = lineSplit[1].replaceAll(" ", ""); // Get rid of any spaces
					// If there is a problem print this error

					// Create a list of of burglarType and column heading that contains number to create
					List<String[]> burglarsToCreate = new ArrayList<String[]>();
					for (String argument:value.split(",")) { // Split line on commas (separate arguments)
						// Check the argument is in the form 'BurglarClass$ColumnHeading
						String[] arguments = argument.split("\\$");						
						if (arguments.length != 2) {
							error = "Couldn't split argument into two parts: "+argument;
						}
						// Check that the column names are correct
						else if ( ! (arguments[1].equals("BglrC1") || arguments[1].equals("BglrC2") || arguments[1].
							equals("BglrC3") || arguments[1].equals("BglrC4") || arguments[1].equals("BglrC5"))) {
							error = "Column names which hold burglar numbers must be in 'BglrCX' where X is between 1 and 5" +
									"(inclusive).";
						}
						else { // All correct
							burglarsToCreate.add(new String[]{arguments[0],arguments[1]});
						}
					}
					
					if (!error.equals("")) {
						String errorString = "ScenarioGenerator error: problem parsing part 3 (burglar descriptions), " +
							"reading burglar numbers and types from the communities file. Are you sure the " +
							"instructions have been specified correctly?\n" +
							"The line should be in the form: 'communities:DefaultBurglar$numDefBur,ProfessionalBurglar$numProfBurg'.\n" +
							"I found: "+line+". \nThe error is: "+error;
						Outputter.errorln(errorString);
						throw new Exception(errorString);
					}
					BurglarFactory bf = new BurglarFactory();
					bf.setCreateMethod("communities");
					bf.setBurglarsInCommunities(burglarsToCreate);
					this.burglarFactory = bf;
					String description = "ScenarioGenerator will tell BurglarFactory to create burglars from information " +
						"in the communities file, using the following types and column headers: ";
					for (String[] s:burglarsToCreate) description+=s[0]+"->"+s[1]+", ";					
					Outputter.debugln(description, Outputter.DEBUG_TYPES.INIT);
					
				}
				
				else if (keyword.equals("specific")) { // Call a specific function in BurglarFactory
					// Find the value that describes how the burglars should be created.
					String value = lineSplit[1].replaceAll(" ", ""); // Get rid of any spaces
					// Create a BurglarFactory instance, this will be used by the scenario
					BurglarFactory bf = new BurglarFactory();
					bf.setCreateMethod("specific"); // Tell the bf to use a specific method to create burglars
					bf.setSpecificMethod(value);	// Tell it what that method is
					this.burglarFactory = bf;		// This bf instance is passed to Scenario objects later
					Outputter.debugln("ScenarioGenerator will tell BurglarFactory to use specific method of " +
							"creating burglars: "+value, Outputter.DEBUG_TYPES.INIT);
				}
				
				else if (keyword.equals("text")) { // Describe burglars textually
					this.error("ScenarioGenerator.parseFile error: not implemented text-based method of reading " +
							"burglar information");					
				}
				
				else {
					this.error("ScenarioGenerator.parseFile error: reading burglar information but don't " +
							"understand the keyword: '"+keyword+"'");
				}
			}

			else if (filePart > 3){
				this.error("ScenarioGenerator.parseFile error: have read too many lines starting with " +
				"'*', this denotes moving onto next part of the file but have already moved onto the final " +
				"part (3)");
			}

			// read the next line
			line = reader.readLine();	
			lineCount++;

		} // while line!=null

		return params;
	}

	/** Strip all space characters (' '). */
	private String removeWhitespace(String line) {
		return line = line.replaceAll(" ", "");
	}

	/** Remove any comments that start mid-way through a line */
	private String removeComments(String line) {
		if (!line.contains("#")) { // No '#' so no comments, just return the line
			return line;
		}
		else {
			// Return the part of the line before the comment character
			return line.substring(0, line.indexOf('#'));
		}
	}

	private void setMasterParameter(String param, int value) {
		Outputter.debugln("ScenarioGenerator.parseFile() found master paramter: "+param+" -> "+value, Outputter.DEBUG_TYPES.SCENARIOS);
		if (param.equals("sweeps")) {
			this.numSweeps = value;
		}
		//		else if (param.equals("oNGS")) { // If running on the NGS some database params are slightly different
		//			if (value==0) ScenarioGenerator.ON_NGS = false;
		//			else if (value==1) ScenarioGenerator.ON_NGS = true;	
		//			else Outputter.errorln("ScenarioGenerator.setMasterParameter");
		//			System.out.println("Set ON_NGS to: "+ScenarioGenerator.ON_NGS);
		//		} // if param==ngs
		else if (param.equals("sociotype_class")) {
			if (value == 1) {
				this.sociotypeClass = OAC.class;	
			}
			else if (value == 2) {
				this.sociotypeClass = VancouverSociotype.class;
			}
			 
		}
		else {
			Outputter.errorln("ScenarioGenerator.setMasterParameter(): unrecognised parameter: "+
					param+". (This isn't necessarily a problem).");
		}

	}

	/**
	 * Create Scenarios from the given Parameters. Each time that a dynamic parameter is found (one
	 * which will be incremented) a new Scenario is created for each possible value of the parameter.
	 * @param parameters
	 * @return a collection of scenarios that can be executed in any order
	 */
	private List<Scenario> generateScenarios(List<Parameter<?>> parameters) {
		List<Scenario> scenarios = new ArrayList<Scenario>();
		// Check that there are some dynamic parameters
		List<Parameter<?>> dynamicParams = new ArrayList<Parameter<?>>();
		boolean oneDynamic = false;
		for (Parameter<?> p:parameters) {
			if (p.isDynamic()) {
				oneDynamic = true;
				dynamicParams.add(p);
			}
		}
		if (!oneDynamic) { // No dynamic paramters, just create a single scenario, one for each sweep
			for (int i=0; i<this.numSweeps; i++)
				scenarios.add(new Scenario(parameters));
			Outputter.debugln("No dynamic params, returning one scenario to run "+this.numSweeps+" time(s): "+scenarios.toString(), Outputter.DEBUG_TYPES.SCENARIOS);
		}
		else { // Have at least one dynamic Parameter.
			for (Parameter<?> dp:dynamicParams) { 
				// Can cast because guaranteed that dynamic parameters are Numeric (see constructors).
				NumericParameter<?> np = (NumericParameter<?>) dp;
				// Expand the dynamic parameter into all possible individual params
				for (Parameter<?> p:np.expand()) { 
					// New list of parameters which will be used to create the scenario.
					List<Parameter<?>> params = new ArrayList<Parameter<?>>();
					params.add(p); // Add the expanded parameter
					for (Parameter<?> allOthers:parameters) { // Add all the others
						if (!allOthers.same(p)) { // (But not the parameter just expanded)
							params.add(allOthers);
						}
					} // for allOthers
					for (int i=0; i<this.numSweeps; i++) { // Create a new scenario to run numSweeps times
						scenarios.add(new Scenario(params));
					} // for numSweeps
				} // for expanded dynamic parameter
			} // for dynamicParameter
			String debugString = "Have some dynamic params, returning following scenarios (each to run "+
				this.numSweeps+" times):";
			for (Scenario s:scenarios) {
				debugString+="\n\t"+s.toString();
			}
			Outputter.debugln(debugString, Outputter.DEBUG_TYPES.SCENARIOS);
		} // else dynamicParameters?
		
		// Finally tell the Scenarios how they will create burglars and which sociotype to use
		for(Scenario s:scenarios) {
			s.setBurglarFactory(this.burglarFactory);
			s.setSociotypeClass(this.sociotypeClass);
		}
		return scenarios;
	}

	/** Get the specified sceanrio. Print a message and return null if the index is out of range. */
	public Scenario getScenario(int i) {
		if (i<this.scenarios.size()) { // Check index isn't out of range
			return this.scenarios.get(i);
		}
		else {
			Outputter.errorln("ScenarioGenerator.get(): index "+i+" is out of range, returning null");
			return null;
		}
	}
	
	/** Convenience function, called if an error occurs. Prints a message and throws an Exception.
	 * @param error A string describing the error, this is output and used to create the Exception object.
	 * @throws Exception
	 */
	private void error(String error) throws Exception {
		Outputter.errorln(error);
		throw new Exception(error);
	}

	public int getNumScenarios() {
		return this.scenarios.size();
	}

//	public void clearCaches() {
//		ScenarioGenerator.uniqueScenarioID=0;		
//	}

}
