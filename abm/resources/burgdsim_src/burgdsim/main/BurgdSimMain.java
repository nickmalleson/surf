package burgdsim.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import burgdsim.scenario.Scenario;
import burgdsim.scenario.ScenarioGenerator;


/**
 * Class can be used to run a model without using the repast GUI by initialising and calling the
 * BurgdSimRunner class. There's a discussion about this on the repast email list: 
 * <url>http://www.nabble.com/How-to-programmatically-start-RepastS---to14351077.html</url>
 * @see BurgdSimRunner
 * @author Nick Malleson
 */

public class BurgdSimMain {

	public BurgdSimMain() {}

	private static boolean stopSim = false;	// Used by other classes to signal model should stop (i.e. error)

	private static BufferedWriter summaryWriter = null; // Used to save a summary table for the run to csv file
	private static Map<String, Double> actionTimes = null; // Store amount of time spend on each Action (for summary)

	public static void main(String[] args){

		//		System.out.println("***************** PRINT THE CLASSPATH ****************");
		//        //Get the System Classloader
		//        ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();
		//        //Get the URLs
		//        URL[] urls = ((URLClassLoader)sysClassLoader).getURLs();
		//        for(int i=0; i< urls.length; i++)
		//            System.out.println(urls[i].getFile());
		//        System.out.println("*************************** ***************************");
		//		System.out.println("***************** PRINT AGAIN FROM SYSTEM PROPERTY ****************");
		//        System.out.println(System.getProperties().get("java.class.path").toString());
		//        System.out.println("*************************** ***************************");


		File file = new File("burgdsim.rs"); // the scenario dir

		BurgdSimRunner runner = new BurgdSimRunner();
		GlobalVars.RUNNER = runner;

		try {
			runner.load(file);     // load the repast scenario

			// Find out how many times each simulation needs to be run for.
			//			int numRuns = 1;
			ScenarioGenerator sg = new ScenarioGenerator();
			System.out.println("Num scenarios generated: "+sg.getNumScenarios());
			for (int i=0; i<sg.getNumScenarios(); i++) {
				Scenario s = sg.getScenario(i);	
				//		for(int i=0; i<numRuns; i++){
				System.out.println("*********************** STARTING NEW SIM ("
						+(i+1)+"/"+sg.getNumScenarios()+")*********************");
				/* INITIALISE THE SIMULATION */
				s.execute(); 				// This will set the GlobalVars
				runner.runInitialize();  	// ContextCreator.build() called here.
				System.out.println("*********************** INITIALISED *********************");
				// For some reason this next line doesn't cause sim to terminate properly, the "end" functions are called
				// but sim keeps running and breaks. So manually check if tickCount>endTime in while loop
				//			RunEnvironment.getInstance().endAt(endTime);			
				double endTime = GlobalVars.RUN_TIME;
				Outputter.debugln("BurgdSimMain: will end runs at "+GlobalVars.RUN_TIME+" iterations", 
						Outputter.DEBUG_TYPES.INIT);

				/* RUN THE SIMULATION */				
				//				while (runner.getActionCount() > 0){  // loop until last action is left
				//					double ticks = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
				//					if (runner.getModelActionCount() == 0 || ticks > endTime) {
				//						runner.setFinishing(true);
				//					}
				//					runner.step();  // execute all scheduled actions at next tick
				//					if (ticks % 500 == 0) {
				//						System.out.println(ticks+", ");
				//					}
				//				}
				// Use a tick counter to determine when to stop sim rather than checking how many actions remain
				double ticks = 0;

				while (ticks <= endTime){  // loop until last action is left
					if (BurgdSimMain.stopSim) { // Another class has set this, probably because an error has occurred
						ticks=endTime;
						System.out.println("BurgdSimMain has been told to stop, terminating this run.");
						BurgdSimMain.stopSim = false; // reset the boolean ready for the next run
					}
					if (ticks == endTime) {
						System.out.println("BurgdSimMain, last tick, setting finishing");
						runner.setFinishing(true);
					}
					runner.step();  // execute all scheduled actions at next tick
					ticks++;
				}
				System.out.println();

				System.out.println("***********************STOPPING CURRENT SIM*********************");
				runner.stop();          // execute any actions scheduled at run end
				runner.cleanUpRun();
				if (ContextCreator.isTrue(GlobalVars.NON_BURGLARY_SENSITIVITY_TEST_PARAMETERS.PRINT_AGENT_SUMMARY_TABLE)) {
					BurgdSimMain.addToSummaryTable();
				}
			}
			if (BurgdSimMain.summaryWriter != null) {
				BurgdSimMain.summaryWriter.close();	
			}
			System.out.println("*********************** FINISHING RUN *********************");
			// If appropriate, summarise all agent's state variables and motive values
			runner.cleanUpBatch();    // run after all runs complete

		} catch (Exception e) {
			System.err.println("BurgdSimMain caught exception, printing stack trace and exitting:");
			e.printStackTrace();
			System.exit(0);
		}
	}// main

	/**
	 * Go through the motive / intensity values stored in this simulation's History object and save a table
	 * (in csv format) showing average motive and state variable values. Will be saved to file 'summary.csv'
	 * in root model directory. This function is called repeatedly after each simulation, adding results to
	 * the file each time.
	 */
	private static void addToSummaryTable() {
		// Check that a test variable has been defined and a History object has been populated
		if (GlobalVars.NON_BURGLARY_SENSITIVITY_TEST_PARAMETERS.TEST_VARIABLE.equals("")) {
			System.err.println("BurgdSimMain.printSummaryTable(): the variable being tested hasn't been "+
			"defined in the scenario file, cannot produce table.");
			return;
		}
		else if (GlobalVars.NON_BURGLARY_SENSITIVITY_TEST_PARAMETERS.HISTORY == null) {
			System.err.println("BurgdSimMain.printSummaryTable(): no History object has been created "+
			"so cannot analyse model results.");
			return;
		}
		// See if the output file has been created
		File file = new File("summary.csv");
		try {
			if (BurgdSimMain.summaryWriter==null) {
				System.out.println("Creating new summary table to store all simulations in: "+file.toString());
				if (file.exists())
					file.delete();
				BurgdSimMain.summaryWriter = new BufferedWriter(new FileWriter(file));
				summaryWriter.write(
						"TestVariable, TestVariableValue, BurglarNumber, NumIter, " +
						"AvgSleepV, AvgSleepM, AvgSocialV, AvgSocialM, AvgDrugsV, AvgDrugsM, " +
						"AvgTravelTime, AvgBurgleTime, AvgWorkTime, AvgSocialTime, AvgSleepTime, AvgDoNothingTime, " +
						"AvgDrugTake\n");
			}
			// Get the history, a multi-dimensional list: burglarid, iteration and "name" / "value" pairs of variables
			List<List<List<String[]>>> hist = GlobalVars.NON_BURGLARY_SENSITIVITY_TEST_PARAMETERS.HISTORY.getBurglarHist();
			// Get the name and value of the test variable (iteratae over all fields in GlobalVars)
			String testVar = GlobalVars.NON_BURGLARY_SENSITIVITY_TEST_PARAMETERS.TEST_VARIABLE;
			double testVarVal = -1; boolean foundTestVar = false;
			List<Field> fieldList = new ArrayList<Field>();
			for (Field f:GlobalVars.class.getFields()) 
				fieldList.add(f);		
			for(Class<?> c:GlobalVars.class.getClasses()) 
				for (Field f:c.getFields()) 
					fieldList.add(f);
			for (Field f:fieldList) {
				if (f.getName().equals(testVar)) {

					testVarVal = f.getDouble(null);
					foundTestVar = true;
					break;
				}
			}
			if (!foundTestVar) {
				System.err.println("BurgdSimMain.printSummaryTable() error: couldn't find the test variable in "+
				"GlobalVars, are you sure its name (the TEST_VARIABLE parameter) is correct?");
				return;
			}


			// Iterate over history
			for (int burglarid=0; burglarid<hist.size(); burglarid++) {
				// Totals for calculating average state variables or motives
				double totDrugsV = 0, totDrugsM = 0, totSleepV = 0, totSleepM = 0,
					totSocialV = 0, totSocialM = 0, totDoNothingV = 0, totDoNothingM = 0;
				// Totals for calculating the proportion of time spent performing each action
				double totTravelTime = 0, totBurgleTime = 0, totWorkTime = 0, totSocialTime = 0, 
					totSleepTime = 0, totDoNothingTime = 0, totDrugTaken = 0;
				double numIter = hist.get(burglarid).size(); // Number of iterations for the model
				//			System.out.println(burglarid);
				for (int iteration=0; iteration<numIter; iteration++) {
					//				System.out.println("\t"+iteration);
					for (int nameval = 0; nameval < hist.get(burglarid).get(iteration).size(); nameval++) {
						String[] values = hist.get(burglarid).get(iteration).get(nameval);
//						System.out.println("\t\t"+values[0]+", "+values[1]);
						// Find values of PECS variable
						if      (values[0].equals("SleepV")) totSleepV += Double.parseDouble(values[1]);
						else if (values[0].equals("SleepM")) totSleepM += Double.parseDouble(values[1]);
						else if (values[0].equals("DrugsV")) totDrugsV += Double.parseDouble(values[1]);
						else if (values[0].equals("DrugsM")) totDrugsM += Double.parseDouble(values[1]);
						else if (values[0].equals("SocialV")) totSocialV += Double.parseDouble(values[1]);
						else if (values[0].equals("SocialM")) totSocialM += Double.parseDouble(values[1]);
						else if (values[0].equals("DoNothingV")) totDoNothingV += Double.parseDouble(values[1]);
						else if (values[0].equals("DoNothingM")) totDoNothingM += Double.parseDouble(values[1]);
						// Find action being undertaken
						else if (values[0].equals("CurrentAction")) {
							// (Have checked that all actions add the word "Travelling" to their description)
							if (values[1].contains("Travelling")) totTravelTime++;
							else if (values[1].equals("Sleeping")) totSleepTime++;
							else if (values[1].equals("Doing nothing")) totDoNothingTime++;
							else if (values[1].equals("Socialising")) totSocialTime++;
							else if (values[1].contains("Working")) totWorkTime++;
							else if (values[1].contains("Burgling")) totBurgleTime++;
							else if (values[1].contains("drugs")) totDrugTaken++;
							else { 
								System.err.println("BurgdSimMain.addToSummaryTable() warning: not sure what this " +
										"action is: "+values[0]);
							} // else
						} // if currentaction

					} // for values
				} // for iteration
				
//				"TestVariable, TestVariableValue, BurglarNumber, NumIter, " +
//				"AvgSleepV, AvgSleepM, AvgSocialV, AvgSocialM, AvgDrugsV, AvgDrugsM, " +
//				"AvgTravelTime, AvgBurgleTime, AvgWorkTime, AvgSocialTime, AvgSleepTime, AvgDoNothingTime 
//				AvgDrugTake\n");
				BurgdSimMain.summaryWriter.write(
						testVar+", "+testVarVal+", "+burglarid+", "+numIter+", "+
						(totSleepV/numIter)+", "+(totSleepM/numIter)+", "+
						(totSocialV/numIter)+", "+(totSocialM/numIter)+", "+
						(totDrugsV/numIter)+", "+(totDrugsM/numIter)+", "+
						(totTravelTime/numIter)+", "+(totBurgleTime/numIter)+", "+(totWorkTime/numIter)+", "+
						(totSocialTime/numIter)+", "+(totSleepTime/numIter)+", "+(totDoNothingTime/numIter)+", "+
						(totDrugTaken)+						
						"\n");
				BurgdSimMain.summaryWriter.flush();
			} // for history			
		} catch (IOException e) {
			System.err.println("BurgdSimMain.writeSummaryTable() error creating new summary file: "+file.toString());
			e.printStackTrace();

		} catch (IllegalArgumentException e) {
			System.err.println("BurgdSimMain.printSummaryTable() error parsing GlobalVars class");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			System.err.println("BurgdSimMain.printSummaryTable() error parsing GlobalVars class");
		}

	}



	/** Tells this main class to stop the current sim. Can be used by other classes to indicate that an error has
	 * occurred and the sim must terminate.  */
	public static void stopSim() {
		BurgdSimMain.stopSim = true;
	}
}
