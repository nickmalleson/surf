package burgdsim.main;

import java.awt.FlowLayout;
import java.awt.Label;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import burgdsim.burglars.Burglar;
import burgdsim.burglars.BurglarFactory;
import burgdsim.burglars.BurglarMemory;
import burgdsim.data_access.DataAccess;
import burgdsim.data_access.DataAccessFactory;
import burgdsim.environment.Cacheable;
import burgdsim.environment.Community;
import burgdsim.environment.EnvironmentFactory;
import burgdsim.environment.SensitivityTestSociotype;
import burgdsim.environment.TestEnvironment;
import burgdsim.environment.buildings.Building;
import burgdsim.pecs.Action;
import burgdsim.pecs.Motive;
import burgdsim.pecs.StateVariable;
import burgdsim.scenario.Scenario;
import burgdsim.scenario.ScenarioGenerator;

import repast.simphony.context.Context;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.gis.GeographyParameters;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.StrictBorders;
import repast.simphony.ui.RSApplication;
import repast.simphony.util.SimUtilities;

public class ContextCreator implements ContextBuilder<Object>, Cacheable {

	//private static final int DAYS = 50;
	private static Context<Object> mainContext; // A static reference to the root context, useful for other classes.

	private long t1; // Used to measure execution speed

	private boolean burglarsFinishedStepping ; // A flag used to support multi-threading

	// Some parameters that are required to add custom information to the GUI
	private static boolean addedPanel = false; // Used so custom jPanel isn't added to GUI twice
	private static Label timeLabel = null; // A label displaying the real time
	public static Label numBurglariesLabel = null;  // A label displaying the number of burglaries so far.

	// Used to print out the number of burglaries every 100 iterations for information
	private static int burglaries = 0;

	// Used to redirect non-model errors
	private static PrintStream ps = null;

	public Context<Object> build(Context<Object> context) {

		try {

			//			System.out.println("PRINTING CLASSPATH");
			//			ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();
			//			URL[] urls = ((URLClassLoader)sysClassLoader).getURLs();			
			//			for(int i=0; i< urls.length; i++)
			//				System.out.println(urls[i].getFile());
			//			System.out.println("*****");


			ContextCreator.mainContext = context;
			// If the sim has been restarted without restarting simphony the static caches have to be cleared
			// (this class is also responsible for clearing all static variables in GlobalVars).
			GlobalVars.CACHES.add(this);
			GlobalVars.CACHES.clear();

			//			// Add some house data for the grid environment (this can be deleted!!) 
			//			DataAccess da = DataAccessFactory.getDataAccessObject("grid_data");
			//			for (int i=0; i< 900; i++) {
			//				da.writeValues(new Double[]{0.5}, "building_data_sensitivity_test", new String[]{"sec"});
			//			}
			//			System.exit(0);

			Outputter.describeln("Model has "+Runtime.getRuntime().availableProcessors()+" available processors");

			// If the context creator is in direct control of the simulation it will need to set up a scenario,
			// otherwise SCENARIO_INITIALISED will be true and BurgdSimMain will already have set up scenario.
			if (!GlobalVars.SCENARIO_INITIALISED) {
				Outputter.debugln("ContextCreator is initialising Scenarios", Outputter.DEBUG_TYPES.SCENARIOS);
				ScenarioGenerator sg;
				sg = new ScenarioGenerator();
				// Assume only 1 scenario is created, wont be doing batch runs if scenarios haven't been initialised yet
				Scenario s = sg.getScenario(0); 
				s.execute(); // This make scenario change values in GlobalVars
				// Tell simulation when to end, if using Scenarios this will be done programatically in BurgdSimMain
				Outputter.debugln("ConetextCreator.build(): will end run at "+GlobalVars.RUN_TIME, 
						Outputter.DEBUG_TYPES.INIT);
				RunEnvironment.getInstance().endAt(GlobalVars.RUN_TIME);
			}

			// Set up the random seed to make the sim repeatable. NOTE: XXXX this wont work if 
			// ContextCreator.clearCaches() resets the seed to -1 (which it should do) if BurgdSimMain is used
			// it will set the seed before clearCaches() is called (which will reset the seed to -1). So
			// for the moment I've stopped clearCaches() from resetting the seed, but this means that if a lot of
			// simulations are run they will all have the same seed (system time of first run).
			if (GlobalVars.RANDOM_SEED == -1) {
				// Seed hasn't been set in scenario parameters, use system time.
				GlobalVars.RANDOM_SEED = (int)System.nanoTime();	
			}
			RandomHelper.setSeed(GlobalVars.RANDOM_SEED);

			// Can optionally add a jPanel to the GUI to display useful simulation information
			if (!ContextCreator.addedPanel && !isTrue(GlobalVars.ON_NGS)) { // Don't display if already added or on NGS
				try { 
					RSApplication.getRSApplicationInstance().addCustomUserPanel(ContextCreator.createCustomPanel());
				}
				catch (NoSuchMethodError e) {
					Outputter.debugln("ContextCreator.build warning: could not add custom panel to GUI: "+
							e.toString()+"(this isn't a problem).", Outputter.DEBUG_TYPES.INIT);
				}
				catch (Exception e) {
					Outputter.debugln("ContextCreator.build warning: could not add custom panel to GUI: "+
							e.toString(), Outputter.DEBUG_TYPES.INIT);
				}
				ContextCreator.addedPanel = true;
			}

			// Set the environment type and sociotype
			if (GlobalVars.TYPE_OF_ENVIRONMENT==-1) {
				GlobalVars.ENVIRONMENT_TYPE = GlobalVars.ENVIRONMENT_TYPES.NULL;
				Outputter.describeln("ContextCreator setting environment type to "+
						GlobalVars.ENVIRONMENT_TYPES.NULL.toString());
			}
			else if (GlobalVars.TYPE_OF_ENVIRONMENT==0) {
				GlobalVars.ENVIRONMENT_TYPE = GlobalVars.ENVIRONMENT_TYPES.GRID;
				Outputter.describeln("ContextCreator setting environment type to "+
						GlobalVars.ENVIRONMENT_TYPES.GRID.toString());
			}
			else if (GlobalVars.TYPE_OF_ENVIRONMENT==1) {
				GlobalVars.ENVIRONMENT_TYPE = GlobalVars.ENVIRONMENT_TYPES.GIS;
				Outputter.describeln("ContextCreator setting environment type to "+
						GlobalVars.ENVIRONMENT_TYPES.GIS.toString());
			}
			else {
				Outputter.errorln("ContextCreator.build() error: unrecognised TYPE_OF_ENVIRONMENT value: "+
						GlobalVars.TYPE_OF_ENVIRONMENT+". Continuing with the type of environment that was "+
						"compiled by default: "+GlobalVars.ENVIRONMENT_TYPE.toString());
			}
			if (ContextCreator.isTrue(GlobalVars.USE_SENSITIVITY_TEST_SOCIOTYPE)) {
				Outputter.debugln("ContextCreator setting Sociotype class to SensitivityTestSociotype", 
						Outputter.DEBUG_TYPES.INIT);
				GlobalVars.SOCIOTYPE_CLASS = SensitivityTestSociotype.class;
			}


			//			System.out.println("TESTING: SENDING ERROR TO STANDARD OUTPUT STREAM");
			//			System.setErr(System.out);

			// See if errors should be redirected. Outputter wil send all model errors to standard out (not standard
			// error) so standard error can be turned off.
			if (GlobalVars.REDIRECT_ERRORS == 1) {
				Outputter.describeln("Context creator is re-directing errors.");
				// Turn off standard error
				ps = new PrintStream(new NullOutputStream());
				System.setErr(ps);
			}
			else {
				Outputter.debugln("ContextCreator is not redirecting errors.", Outputter.DEBUG_TYPES.INIT);
			}

			// Is this a batch run?
			//			int numBurglars = 50;
			//			if (GlobalVars.BATCH_RUN) {
			//				Outputter.debugOn(false); // Turn all debugging info off
			//				// End at time specified by a batch parameter
			//				RunEnvironment.getInstance().endAt(
			//						(Integer)RunEnvironment.getInstance().getParameters().getValue("max_num_iter"));
			//				numBurglars = (Integer)RunEnvironment.getInstance().getParameters().getValue("num_burglars"); 
			//			} 

			// Need to do same for DataAccess layer (can't implement cacheable becuase abstract).
			DataAccessFactory.init();

			// Create an environment for agents and for their houses
			EnvironmentFactory.init(); // This will make sure static variables do no persist over multiple simulations
			//			Outputter.debugln("ContextCreator creating environments from "+GlobalVars.GIS_PARAMS.GIS_DATA_ROOT,
			//					Outputter.DEBUG_TYPES.INIT);

			GlobalVars.BURGLAR_ENVIRONMENT = EnvironmentFactory.createEnvironment("BurglarEnvironment", context);
			GlobalVars.BUILDING_ENVIRONMENT = EnvironmentFactory.createEnvironment("BuildingEnvironment", context);
			GlobalVars.ROAD_ENVIRONMENT  = EnvironmentFactory.createEnvironment("RoadEnvironment", context);
			GlobalVars.JUNCTION_ENVIRONMENT = EnvironmentFactory.createEnvironment("JunctionEnvironment", context);
			GlobalVars.COMMUNITY_ENVIRONMENT = EnvironmentFactory.createEnvironment("CommunityEnvironment", context);

			// Create communities
			EnvironmentFactory.createCommunities(GlobalVars.COMMUNITY_ENVIRONMENT);

			// Create buildings and road network
			EnvironmentFactory.createBuildingsAndRoads(GlobalVars.BUILDING_ENVIRONMENT,
					GlobalVars.ROAD_ENVIRONMENT, GlobalVars.JUNCTION_ENVIRONMENT, GlobalVars.COMMUNITY_ENVIRONMENT);

			// Create a road network
			EnvironmentFactory.createRoadNetwork(GlobalVars.JUNCTION_ENVIRONMENT, GlobalVars.ROAD_ENVIRONMENT);

			// CAN DELETE THIS:
//			System.out.println("Printing distances between houses 211030 and 204603");			
//			House h1 = null, h2=null;
//			for (House h:GlobalVars.BUILDING_ENVIRONMENT.getAllObjects(House.class)) {
//				if (h.getId()==211030) {
//					h1 = h;
//				}
//				else if (h.getId()==204603) {
//					h2 = h;
//				}
//			}
//			System.out.println("DIST: "+GlobalVars.BUILDING_ENVIRONMENT.getDistance(h1.getCoords(), h2.getCoords()));
//			System.out.println("DIST: "+GISRoute.distanceToMeters(GlobalVars.BUILDING_ENVIRONMENT.getDistance(h1.getCoords(), h2.getCoords())));
//			System.exit(0);
			
			// Create transport networks
			EnvironmentFactory.createTransportRoutes(GlobalVars.JUNCTION_ENVIRONMENT);
			
			if (ContextCreator.isTrue(GlobalVars.PRINT_COMMUNITY_SIMILARITY)) {
			this.printCommunitySimilarity();
			}

			// Remove any communities that don't have any buildings in them
			this.purgeCommunities();

			// Create burglar(s)
			// The BurglarFactory will have already been initialised by the Scenario object created for this model.
			BurglarFactory.getInstance().createBurglars();


			// Have created all objects, now can populate data store
			populateDataStore();

			// Create the schedule and set the time that the simulation should end
			createSchedule();

			// Create a couple of "main projections" which shows all objects in the model
//			context.addProjection(
//					GeographyFactoryFinder.createGeographyFactory(null).createGeography(
//							"MainGeography", context, 
//							new GeographyParameters<Object>())
//			);
//
//			context.addProjection(
//					GridFactoryFinder.createGridFactory(new HashMap<String, Object>()).createGrid(
//							"MainGrid", context, 
//							GridBuilderParameters.multiOccupancy2D(
//									new SimpleGridAdder<Object>(),
//									new StrictBorders(),
//									GlobalVars.GRID_PARAMS.XDIM,
//									GlobalVars.GRID_PARAMS.YDIM))
//			);

			//		// Print info about every agent who has been added to this main context:
			//		System.out.println("Call toString on every agent in context:");
			//		for (Class<Object> c:context.getAgentTypes()) {
			//			for (Object o:context.getObjects(c)) {
			//				System.out.println("\t"+o.toString());
			//			}
			//		}
			//		// Do the same for the main projection
			//		System.out.println("Call toString on every agent in projection:");
			//		for (Object o:(context.getProjection(Grid.class, "MainGrid")).getObjects())	 {
			//			System.out.println("\t"+o.toString());
			//		}
		} catch (Exception e) {
			Outputter.errorln("ContextCreator.build caught an error ("+e.getClass().getName()+") message: "+
					e.getMessage());
			Outputter.errorln(e.getStackTrace());
			ContextCreator.haltSim(); 
		}

		return context;
	}


	private void printCommunitySimilarity() {
		// Print out the similarity of all communities to a given one
		int comID1 = 910 ; // student community
		int comID2 = 420 ; // beeston community
		int comID3 = 1910 ; // easel / harehills community
		int comID4 = 79 ; // rural community
		Community c1 = null; Community c2 = null; Community c3 = null; Community c4 = null;
		// Find the communities using the IDs
		for (Community c:GlobalVars.COMMUNITY_ENVIRONMENT.getAllObjects(Community.class)) {
			if (c.getId()==comID1) c1 = c;
			if (c.getId()==comID2) c2 = c;
			if (c.getId()==comID3) c3 = c;
			if (c.getId()==comID4) c4 = c;
		}
		if (c1==null || c2==null || c3==null || c4==null) {
			System.err.println("Error: null community.");
		}
		else { // Print the similarity of each community to all others
			System.out.println("SIMILARITY OF COMMUNITY 1:"); System.out.println("ComID, Similarity");
			for (Community c:GlobalVars.COMMUNITY_ENVIRONMENT.getAllObjects(Community.class))
				System.out.println(c.getId()+", "+c.getSociotype().compare(c1.getSociotype()));
			System.out.println("\n\n\n");
			System.out.println("SIMILARITY OF COMMUNITY 2:"); System.out.println("ComID, Similarity");
			for (Community c:GlobalVars.COMMUNITY_ENVIRONMENT.getAllObjects(Community.class))
				System.out.println(c.getId()+", "+c.getSociotype().compare(c2.getSociotype()));
			System.out.println("\n\n\n");
			System.out.println("SIMILARITY OF COMMUNITY 3:"); System.out.println("ComID, Similarity");
			for (Community c:GlobalVars.COMMUNITY_ENVIRONMENT.getAllObjects(Community.class))
				System.out.println(c.getId()+", "+c.getSociotype().compare(c3.getSociotype()));
			System.out.println("\n\n\n");
			System.out.println("SIMILARITY OF COMMUNITY 4:"); System.out.println("ComID, Similarity");
			for (Community c:GlobalVars.COMMUNITY_ENVIRONMENT.getAllObjects(Community.class))
				System.out.println(c.getId()+", "+c.getSociotype().compare(c4.getSociotype()));
			System.out.println("\n\n\n");
		}
		System.out.println("printed communities, exitting.");
		System.exit(0);
		
	}


	public Context<Object> getMainContext() {
		return ContextCreator.mainContext;
	}

	private void createSchedule() {
		/* This is the order of the schedule:
		 * priority , method
		 * 
		 * 10		,	this.start()				AT START 
		 * 
		 * 0		,	burglar.step - (this isn't scheduled using Repast scheduler so it can be multithreaded)
		 * -10		,	stateVariable.step()
		 * -15		,	doHousekeeping()
		 * -20		,	History.step()
		 * -30		,	updateHistory()
		 * 
		 * -25		,	this.end()					AT END
		 * -30		,	History.end()				AT END
		 * -50		,	Outputter.closeFiles()		AT END
		 */

		// Schedule the burglar's step() method to be called at every iteration
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		// Burglar step scheduling not actually done by repast scheduler, done directly to make use of threading
		ScheduleParameters burglarStepParams = ScheduleParameters.createRepeating(1, 1, 0);
		schedule.schedule(burglarStepParams, this, "burglarStep");

		//		ScheduleParameters burglarParams = ScheduleParameters.createRepeating(1, 1, 0);
		//		ScheduleParameters stateVariableParams = ScheduleParameters.createRepeating(1, 1, -10);
		//		for (Burglar b:GlobalVars.BURGLAR_ENVIRONMENT.getAllObjects(Burglar.class)) {
		//				schedule.schedule(burglarParams, b, "step"); // schedule every burglar's step() method
		//			// Also need to schedule every StateVariable's step() method, these should be called before the agent's
		//			for (StateVariable s:b.getStateVariables()) {
		//				schedule.schedule(stateVariableParams, s, "step");
		//			}
		//		}

		// Schedule the start() and end() methods of this function (basically just print out the runtime)
		ScheduleParameters endParams = ScheduleParameters.createAtEnd(ScheduleParameters.LAST_PRIORITY);
		schedule.schedule(endParams, this, "end");
		ScheduleParameters startParams = ScheduleParameters.createOneTime(1);
		schedule.schedule(startParams, this, "startSimulation");

		// Schedule the outputter to close all files at the end of the simulation
		schedule.schedule(ScheduleParameters.createOneTime(ScheduleParameters.END, -50),
				Outputter.getInstance(), "closeFiles");

		// Optionally Schedule the History class
		if (GlobalVars.NON_BURGLARY_SENSITIVITY_TEST_PARAMETERS.USE_HISTORY==1) {
			Outputter.debugln("ContextCreator.createSchedule() is scheduling the History class",
					Outputter.DEBUG_TYPES.INIT);
			GlobalVars.NON_BURGLARY_SENSITIVITY_TEST_PARAMETERS.HISTORY = new History();
			ScheduleParameters historyStepParams = 
				ScheduleParameters.createRepeating(1, 1, ScheduleParameters.LAST_PRIORITY);
			schedule.schedule(historyStepParams, GlobalVars.NON_BURGLARY_SENSITIVITY_TEST_PARAMETERS.HISTORY, "step");
			schedule.schedule(endParams, GlobalVars.NON_BURGLARY_SENSITIVITY_TEST_PARAMETERS.HISTORY, "end");
		}


		// Schedule some other housekeeping things which should be done every iteration
		schedule.schedule(ScheduleParameters.createRepeating(1, 1, 20),
				this, "doHousekeeping");


	}

	/** This is called once per iteration and goes through each burglar calling their step method. This
	 * is done (instead of using Repast scheduler) to allow multi-threading (each step method can be
	 * executed on a free core). This method actually just starts a ThreadController thread (which handles
	 * spawning threads to step burglars) and waits for it to finish */
	public synchronized void burglarStep() {

		this.burglarsFinishedStepping = false;
		(new Thread(new ThreadController(this))).start();
		while (!this.burglarsFinishedStepping) {
			try {
				this.wait(); // Wait for the ThreadController to call setBurglarsFinishedStepping().
			} catch (InterruptedException e) {
				Outputter.errorln("ContextCreator.burglarStep() caught Interrupted Exceltion: "+e.getMessage());
				e.printStackTrace();
			}// Wait until the thread controller has finished
		}
	}

	/**
	 * Used to tell the ContextCreator that all burglars have finished their step methods and it can
	 * continue doing whatever it was doing (it will be waiting while burglars are stepping).
	 */
	public synchronized void setBurglarsFinishedStepping() {
		this.burglarsFinishedStepping = true;
		this.notifyAll();
	}


	public void doHousekeeping() throws Exception {
		// increment the decimal time counter
		GlobalVars.time+=(GlobalVars.MINS_PER_ITER)/60; // Increase number of hours by 
		GlobalVars.mins+=GlobalVars.MINS_PER_ITER;
		// Print the speed every 100 iterations (measure time between two point t1 and t2).
		int iter = GlobalVars.getIteration();
		if (iter==0) t1 = System.currentTimeMillis();
		if (iter % 100 == 0) { // Print some info about last 100 iterations
			int totBurgs = GlobalVars.NUM_BURGLARIES;
			Outputter.debugln("Iteration: "+iter+" time taken: "+ (System.currentTimeMillis()-t1)+
					", burglaries: " +(totBurgs-burglaries)+"("+totBurgs+"), "+
					"memory(G): (total,free,max), "+ (Runtime.getRuntime().totalMemory()*0.000000001)+", "+
					(Runtime.getRuntime().freeMemory()*0.000000001)+", "+
					(Runtime.getRuntime().maxMemory()*0.000000001),
					Outputter.DEBUG_TYPES.ITERATIONS);
			burglaries = totBurgs;

			t1 = System.currentTimeMillis();
		}

		// If it's the end of a day do some other stuff
		if (GlobalVars.time >= 24) {
			GlobalVars.time = 0;
			GlobalVars.days++;

			// Reduce the security of any houses that might have been burgled (once per day for efficiency)
			EnvironmentFactory.deteriorateSecurtyValues();

			// Add some model information about the model to the results data store and flush it (not always necessary).
			DataAccessFactory.getDataAccessObject(GlobalVars.HISTORY_PARAMS.RESULTS_DATABASE).flush();
			saveDailyHistory();
			DataAccessFactory.getDataAccessObject(GlobalVars.HISTORY_PARAMS.RESULTS_DATABASE).flush();

			// Print the day
			Outputter.debugln("Day "+GlobalVars.days, Outputter.DEBUG_TYPES.ITERATIONS);
		}
		// Set the time display
		if (ContextCreator.timeLabel != null) {
			ContextCreator.timeLabel.setText(String.valueOf(GlobalVars.time));
		}
	}

	/** 
	 * Function called at the beginning of the simulation
	 */
	public void startSimulation() {

		new TestEnvironment();

		Outputter.describeln(
				"ContextCreator.start(): Sim starting with seed '"+GlobalVars.RANDOM_SEED+
				"' and mem (tot,fr,max), "+ (Runtime.getRuntime().totalMemory()*0.000000001)+", "+
				(Runtime.getRuntime().freeMemory()*0.000000001)+", "+
				(Runtime.getRuntime().maxMemory()*0.000000001) );
		GlobalVars.START_TIME = System.currentTimeMillis();
	}

	/**
	 * Function called at the end of the simulation
	 * @throws SQLException 
	 */
	public void end() throws SQLException {

		// Write the final bit of information about burglaries and security values.
		try {
			this.writeSecurityHistory(DataAccessFactory.getDataAccessObject(GlobalVars.HISTORY_PARAMS.RESULTS_DATABASE));
		}
		catch (Exception e) {
			Outputter.errorln("ContextCreator.end() caught an exception trying to write final house security values. " +
					"Possible that they were already written, by conincidence, this iteration already. This isn't really " +
					"a problem but printing stack trace anyway: "+e.getClass());
			Outputter.errorln(e.getStackTrace());
		}

		// Close any open DataAccess objects (flush caches, close db connections etc).
		try {
			DataAccessFactory.closeAllObjects();
		}
		catch (Exception e) {
			Outputter.errorln("ContextCreator.end() caught an error trying to close data access objects: "+
					e.getClass().toString()+", "+ e.getMessage());
			Outputter.errorln(e.getStackTrace());
		}

		// Do some garbage collection (might be about to start a new simulation).
		Runtime.getRuntime().gc();

		double runTime = System.currentTimeMillis()-GlobalVars.START_TIME;
		Outputter.describeln("ContextCreator.end(). Iterations: "+GlobalVars.getIteration()+
				"Simulation time: "+runTime+"(ms) time per iteration:"+
				(runTime/RunEnvironment.getInstance().getCurrentSchedule().getTickCount()));
	}

	/** Returns a JPanel which displays some useful model information and can be added to the "User Panel"
	 * on the GUI. */
	private static JPanel createCustomPanel() {
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));

		JPanel timePanel = new JPanel();
		timePanel.setLayout(new FlowLayout());
		ContextCreator.timeLabel = new Label(String.valueOf(GlobalVars.time));
		timePanel.add(new Label("Time:"));
		timePanel.add(ContextCreator.timeLabel);
		mainPanel.add(timePanel);

		JPanel burglariesPanel = new JPanel();
		burglariesPanel.setLayout(new FlowLayout());
		ContextCreator.numBurglariesLabel = new Label(String.valueOf(GlobalVars.NUM_BURGLARIES));
		burglariesPanel.add(new Label("Burglaries:"));
		burglariesPanel.add(ContextCreator.numBurglariesLabel);
		mainPanel.add(burglariesPanel);

		return mainPanel;
	}

	public void clearCaches() {
		GlobalVars.time = 0;
		GlobalVars.days = 0;
		GlobalVars.mins = 0;
		Outputter.describeln("ContextCreator.clearCaches() not resetting model seed.");
		//GlobalVars.RANDOM_SEED = -1;
		EnvironmentFactory.init(); // This will make sure static variables do no persist over multiple simulations
		GlobalVars.BURGLAR_ENVIRONMENT = null;
		GlobalVars.BUILDING_ENVIRONMENT = null;
		GlobalVars.ROAD_ENVIRONMENT  = null;
		GlobalVars.JUNCTION_ENVIRONMENT = null;
		GlobalVars.COMMUNITY_ENVIRONMENT = null;
		GlobalVars.NON_BURGLARY_SENSITIVITY_TEST_PARAMETERS.HISTORY = null;
		//		GlobalVars.SCENARIO_INITIALISED = false;
		//		GlobalVars.MODEL_NAME = null;
	}

	/**
	 * Convenience function to remove all communities from the environment that don't have a building in them.
	 */
	private void purgeCommunities() {
		List<Community> toRemove = new ArrayList<Community>();
		for (Community c:GlobalVars.COMMUNITY_ENVIRONMENT.getAllObjects(Community.class)) {
			if (c.getBuildings()==null || c.getBuildings().size()==0) {
				toRemove.add(c);
			}
		}
		Outputter.debugln("ContextCreator.build(): removing the following communities that don't have " +
				"any buildings in them: "+toRemove.toString(), Outputter.DEBUG_TYPES.INIT);
		for (Community c:toRemove) {
			GlobalVars.COMMUNITY_ENVIRONMENT.remove(c);
		}
	}

	/**
	 * Will populate the results store with data representing the initial state of the model. The
	 * same data store is used by multiple models so once buildings and communities have been added
	 * they don't need to be added again by subsequent models. Therefore the first thing the function
	 * does is look for a buildings and communities data, if it finds it then it just added the rest
	 * of the model information (about burglars, security levels etc) and if not then it adds buildings
	 * and communities data first.
	 * @throws Exception
	 */
	private void populateDataStore() throws Exception {

		//		if (! (isTrue(GlobalVars.HISTORY_PARAMS.COMMUNITIES_OUT) || isTrue(GlobalVars.HISTORY_PARAMS.SCENARIO_PARAMETERS_OUT)
		//				|| isTrue(GlobalVars.HISTORY_PARAMS.BUILDINGS_OUT) || isTrue(GlobalVars.HISTORY_PARAMS.SECURITY_OUT)
		//				|| isTrue(GlobalVars.HISTORY_PARAMS.BURGLARS_OUT) || isTrue(GlobalVars.HISTORY_PARAMS.BURGLAR_INFO_OUT)
		//				|| isTrue(GlobalVars.HISTORY_PARAMS.BURGLAR_MEMORY_OUT) || isTrue(GlobalVars.HISTORY_PARAMS.STATE_VARIABLES_OUT)
		//				|| isTrue(GlobalVars.HISTORY_PARAMS.MOTIVES_OUT) || isTrue(GlobalVars.HISTORY_PARAMS.ACTIONS_OUT)
		//				|| isTrue(GlobalVars.HISTORY_PARAMS.BURGLARY_OUT) ) ) {
		//			Outputter.debugln("ContextCreator.populateDatastore(): not writing any history data at all",
		//					Outputter.DEBUG_TYPES.INIT);
		//			return;
		//		}
		if (GlobalVars.HISTORY_PARAMS.ALL_OUTOUT_ONOFF == 0) { // Could be part of previous if statement but I can't work out logic!
			Outputter.debugln("ContextCreator.populateDatastore(): not writing any history data at all",
					Outputter.DEBUG_TYPES.INIT);
			return;
		}

		// Datastore to save results, this schema should have been created already
		DataAccess da = DataAccessFactory.getDataAccessObject(GlobalVars.HISTORY_PARAMS.RESULTS_DATABASE);

		// See if the data store has already been populated with buildings/communities data by comparing numbers
		int numBuildings = GlobalVars.BUILDING_ENVIRONMENT.getAllObjects(Building.class).size();
		int numCommunities = GlobalVars.COMMUNITY_ENVIRONMENT.getAllObjects(Community.class).size();
		int numDataBuildings = da.getNumRecords(GlobalVars.HISTORY_PARAMS.BUILDINGS);
		int numDataCommunities = da.getNumRecords(GlobalVars.HISTORY_PARAMS.COMMUNITIES);

		// (One exception: if using Grid environment then the number of communities doesn't have to match)
		if ( numBuildings==numDataBuildings && numBuildings!=0 && numCommunities!=0 && 
				GlobalVars.ENVIRONMENT_TYPE.equals(GlobalVars.ENVIRONMENT_TYPES.GRID)) {
			Outputter.debugln("ContextCreator.populateDataStore() right number of buildings ("+numBuildings+") " +
					"but wrong number of communities ("+numCommunities+")." +
					"This is OK because using Grid environment.", Outputter.DEBUG_TYPES.INIT);
		}
		// Otherwise datastore has not been populated correctly, need to add communities and buildings for this scenario
		else if (! (numBuildings==numDataBuildings && numCommunities==numDataCommunities) ){
			Outputter.debugln("ContextCreator didn't find an appropriate data store for model results. Numbers:" +
					"buildings ("+numBuildings+","+numDataBuildings+"), communities ("+numCommunities+","+
					numDataCommunities+").", Outputter.DEBUG_TYPES.INIT);
			// If the datastore isn't empty ask the user to clear it and try again
			if (!(numDataBuildings==0 && numDataCommunities==0)) {

				Outputter.errorln("THE DATASTORE IS NOT EMPTY. PLEASE REMOVE ALL COMMUNITIES AND BUILDINGS" +
				"AND THEN RE-RUN THE MODEL. CLOSING DATABASE CONNETIONS AND EXITTING NOW.");
				DataAccessFactory.closeAllObjects();
				System.exit(1);
				// For some reason Oracle is having problems deleting values from the database, hangs
				// on the clear() line. Otherwise previous lines are unnecessary and following should clear the
				// database.
				//				List<String> tableNames = new ArrayList<String>();
				//				tableNames.add(GlobalVars.HISTORY_PARAMS.COMMUNITIES);
				//				tableNames.add(GlobalVars.HISTORY_PARAMS.BUILDINGS);
				//				tableNames.add(GlobalVars.HISTORY_PARAMS.ACTIONS);
				//				tableNames.add(GlobalVars.HISTORY_PARAMS.BURGLAR_INFO);
				//				tableNames.add(GlobalVars.HISTORY_PARAMS.BURGLAR_MEMORY);
				//				tableNames.add(GlobalVars.HISTORY_PARAMS.BURGLARS);
				//				tableNames.add(GlobalVars.HISTORY_PARAMS.BURGLARY);
				//				tableNames.add(GlobalVars.HISTORY_PARAMS.MODEL_NAMES);
				//				tableNames.add(GlobalVars.HISTORY_PARAMS.MOTIVES);
				//				tableNames.add(GlobalVars.HISTORY_PARAMS.SCENARIO_PARAMETERS);
				//				tableNames.add(GlobalVars.HISTORY_PARAMS.SECURITY);
				//				tableNames.add(GlobalVars.HISTORY_PARAMS.STATE_VARIABLES);
				//				da.clear(tableNames); // Delete everything from the old database

			}
			// If get here then database is empty so can repopulate

			// Now add the communities and buildings
			Outputter.describeln("ContextCreator: have an empty database to store history in, now will populate " +
					"with buildings and communities. (This might take some time but unless Outputter.debugInit is " +
			"true you wont see any output.");
			/* Communities */
			if (isTrue(GlobalVars.HISTORY_PARAMS.COMMUNITIES_OUT)) {
				List<Community> communities = GlobalVars.COMMUNITY_ENVIRONMENT.getAllObjects(Community.class);
				Outputter.debugln("\tpopulating "+communities.size()+" communities.", Outputter.DEBUG_TYPES.INIT);
				int counter = 0 ; // used to output progress of loading data into databases
				for (Community c:communities) {
					Object[] values = new Object[2]; 	// Array of values to be added to database (id, name)
					String[] names = new String[2];		// Column names for each value 
					names[0] = "CommunityID"; values[0] = c.getId();
					names[1] = "Name"; values[1] = "no name";
					try { // (There will be duplicated communities in Grid environment because many Community objects represent contiguous community)
						da.writeValues(values, GlobalVars.HISTORY_PARAMS.COMMUNITIES, names);
					} catch (SQLException e) {
						if (GlobalVars.ENVIRONMENT_TYPE.equals(GlobalVars.ENVIRONMENT_TYPES.GRID)) {
							Outputter.errorln("ContextCreator.populateDataStore() warning, caught SQLException trying to " +
							"add communities, but as using Grid environment this is probably ok, ignoring."); 
						}
						else {
							throw e;
						}
					}
					if (counter++ % 10 == 0) Outputter.debug((counter-1)+"...", Outputter.DEBUG_TYPES.INIT);
				}
				Outputter.debugln("", Outputter.DEBUG_TYPES.INIT);	
			} // if communitiesOut

			/* Buildings */
			if (isTrue(GlobalVars.HISTORY_PARAMS.BUILDINGS_OUT)) {
				List<Building> buildings = GlobalVars.BUILDING_ENVIRONMENT.getAllObjects(Building.class); 
				Outputter.debugln("\tpopulating "+buildings.size()+" buildings", Outputter.DEBUG_TYPES.INIT);
				int counter = 0 ; // used to output progress of loading data into databases
				for (Building b:buildings) {
					Object[] values = new Object[5];
					String[] names = new String[5];
					names[0] = "BuildingID"; values[0] = b.getId();
					names[1] = "CommunityID"; values[1] = b.getCommunity().getId();
					names[2] = "Type"; values[2] = b.getType();
					names[3] = "XCoord"; values[3] = GlobalVars.BUILDING_ENVIRONMENT.getCoords(b).getX();
					names[4] = "YCoord"; values[4] = GlobalVars.BUILDING_ENVIRONMENT.getCoords(b).getY();
					da.writeValues(values, GlobalVars.HISTORY_PARAMS.BUILDINGS, names);
					if (counter++ % 1000 == 0) Outputter.debug((counter-1)+"...", Outputter.DEBUG_TYPES.INIT);
				}
				Outputter.debugln("", Outputter.DEBUG_TYPES.INIT);
			} // if buildingsOut

			Outputter.describeln("HAVE JUST POPULATED THE DATABASE WITH COMMUNITIES AND BUILDINGS. " +
			"Assuming that simulation shoudn't continue, exitting now with status 0.");
			System.exit(0);
		} // if datastore not populated
		else {
			Outputter.debugln("ContextCreator.populateDataStore() found existing datastore to use. It has " +
					numDataBuildings+" buildings and "+numDataCommunities+" communities.", Outputter.DEBUG_TYPES.INIT); 
		} // if datastore populated

		// Get a model id to distinguish this model from others in the db by adding this model's name to the 
		// database then finding the unique id generated (also add the date/time the model was created).
		Calendar calendar = new GregorianCalendar();
		String dateadded = calendar.get(Calendar.DATE)+"/"+(calendar.get(Calendar.MONTH)+1)+"/"+calendar.get(Calendar.YEAR)+"-"+calendar.get(Calendar.HOUR_OF_DAY)+":"+calendar.get(Calendar.MINUTE)+":"+calendar.get(Calendar.SECOND);
		da.writeValues(new String[]{GlobalVars.MODEL_NAME, dateadded}, 
				GlobalVars.HISTORY_PARAMS.MODEL_NAMES, new String[]{"modelname", "datecreated"});
		GlobalVars.MODEL_ID = da.getValue(GlobalVars.HISTORY_PARAMS.MODEL_NAMES, "ModelID", Integer.class, 
				"ModelName", String.class, GlobalVars.MODEL_NAME);
		Outputter.describeln("ContextCreator.populateDataStore() found unique id for this model ("+
				GlobalVars.MODEL_NAME+"): "+GlobalVars.MODEL_ID);

		// Now add all other data

		/* ScenarioParameters (Name, Type, Value) */
		if (isTrue(GlobalVars.HISTORY_PARAMS.SCENARIO_PARAMETERS_OUT)) {
			Outputter.debugln("\tpopulating scenario parameters.", Outputter.DEBUG_TYPES.INIT);
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
			for (Field f:fieldList) {
				Object[] values = new Object[4];
				String[] names = new String[4];
				names[0] = "Name"; values[0] = f.getName().toString();
				names[1] = "ModelID"; values[1] = GlobalVars.MODEL_ID;
				names[2] = "Type"; values[2] = f.getType().toString();
				String val = "null"; // Need to check for nulls and empty strings
				if (f.get(null) != null && f.get(null).toString() != null && f.get(null).toString() != "")
					val = f.get(null).toString();
				names[3] = "Value"; values[3] = val;
				//				names[3] = "Value"; values[3] = (f.get(null) == null ? "null" : f.get(null).toString()); // Check for null value
				da.writeValues(values, GlobalVars.HISTORY_PARAMS.SCENARIO_PARAMETERS, names);
			}
		}


		/* Security (BuildingID, Value, Time) */
		this.writeSecurityHistory(da); // Use a separate method because this is also written when sim ends

		// Iterate over burglars, there are a few different tables to fill
		if (isTrue(GlobalVars.HISTORY_PARAMS.BURGLARS_OUT)) {
			Outputter.debugln("\tpopulating burglars.", Outputter.DEBUG_TYPES.INIT);
			for (Burglar b:GlobalVars.BURGLAR_ENVIRONMENT.getAllObjects(Burglar.class)) {
				/* Burglars (id, name, home) */
				Object[] values = new Object[4]; 
				String[] names = new String[4];
				names[0] = "BurglarID"; values[0] = b.getID();
				names[1] = "ModelID"; values[1] = GlobalVars.MODEL_ID;
				names[2] = "Name"; values[2] = b.getName().replaceAll("[([)]]", "");
				names[3] = "Home"; values[3] = b.getHome().getId();
				da.writeValues(values, GlobalVars.HISTORY_PARAMS.BURGLARS, names);

				if (isTrue(GlobalVars.HISTORY_PARAMS.BURGLAR_INFO_OUT)) {
					writeBurglarHistory(da, b);
				}

				/* BurglarMemory (BurglarID, BuildingID, NumVisits, NUmBurglaries, Time) */
				if (isTrue(GlobalVars.HISTORY_PARAMS.BURGLAR_MEMORY_OUT)) {
					Map<Building, List<Integer>> bm = b.getMemory().getFromMemory(Building.class);  
					for (Building building:bm.keySet()) {
						values = new Object[6]; 
						names = new String[6];
						names[0] = "BurglarID"; values[0] = b.getID();
						names[1] = "BuildingID"; values[1] = building.getId();
						names[2] = "ModelID"; values[2] = GlobalVars.MODEL_ID;
						names[3] = "NumVisits"; values[3] = bm.get(building).get(BurglarMemory.VISITS_INDEX); 
						names[4] = "NumBurglaries"; values[4] = bm.get(building).get(BurglarMemory.BURGLARIES_INDEX);
						names[5] = "Time"; values[5] = GlobalVars.getIteration() ;
						da.writeValues(values, GlobalVars.HISTORY_PARAMS.BURGLAR_MEMORY, names);
					} // for BurglarMemory
				}

				// Add the initial values of state variables, motives and current action
				/* StateVariables (idStateVariables, BurglarID, Name, Time, Value) */
				if (isTrue(GlobalVars.HISTORY_PARAMS.STATE_VARIABLES_OUT)) {
					for (StateVariable s:b.getStateVariables()) {
						values = new Object[5]; 
						names = new String[5];
						names[0] = "BurglarID"; values[0] = b.getID();
						names[1] = "ModelID"; values[1] = GlobalVars.MODEL_ID;
						names[2] = "Name"; values[2] = s.getName();
						names[3] = "Time"; values[3] = GlobalVars.getIteration();
						names[4] = "Value"; values[4] = s.getValue();
						da.writeValues(values, GlobalVars.HISTORY_PARAMS.STATE_VARIABLES, names);

						/* Motives (idMotives, BurglarID, Name, Time, Value) */ // NOTE: if change system so multiple motives this loop can be nester withing statevar loop
						Motive m;
						if (isTrue(GlobalVars.HISTORY_PARAMS.MOTIVES_OUT)) {
							m = s.getMotive();
							values = new Object[5]; 
							names = new String[5];
							names[0] = "BurglarID"; values[0] = b.getID();
							names[1] = "ModelID"; values[1] = GlobalVars.MODEL_ID;
							names[2] = "Name"; values[2] = m.getName();
							names[3] = "Time"; values[3] = GlobalVars.getIteration();
							names[4] = "Value"; values[4] = m.getIntensity();
							da.writeValues(values, GlobalVars.HISTORY_PARAMS.MOTIVES, names);
						}
					} // for statevariables			
				} // if STATEVARIABLES_OUT
				/* Action */
				if (isTrue(GlobalVars.HISTORY_PARAMS.ACTIONS_OUT)) {
					Action a = b.getCurrentAction();
					if (a!=null) { // can be null if the simulation is starting and
						values = new Object[5]; 
						names = new String[5];
						names[0] = "BurglarID"; values[0] = b.getID();
						names[1] = "ModelID"; values[1] = GlobalVars.MODEL_ID;
						names[2] = "Name"; values[3] = a.getClass().getName();
						names[3] = "Time"; values[4] = GlobalVars.getIteration();
						names[4] = "Value"; values[5] = -1; // Actions don't have values like Motives/StateVariables
						da.writeValues(values, GlobalVars.HISTORY_PARAMS.ACTIONS, names);
					} // if action!=null
				} // if ACTIONS_OUT
			} // for burglars
		} // if BURGLARS_OUT

		// Decide which burglars we will write information about at each iteration
		List<Burglar> allBurglars = GlobalVars.BURGLAR_ENVIRONMENT.getAllObjects(Burglar.class);
		if (GlobalVars.STORE_NUM_BURGLARS > 0) {
			SimUtilities.shuffle(allBurglars, RandomHelper.getUniform());
		}
		// The number of burglars to write info about, if -1 then write about all.
		int num = (GlobalVars.STORE_NUM_BURGLARS == -1 ? allBurglars.size() : GlobalVars.STORE_NUM_BURGLARS);
		// check num isn't greater than the number of burglars
		if (num > allBurglars.size()) {
			num = allBurglars.size();
		}
		Outputter.debugln("ContextCreator.populateDataStore(), will write info about "+num+" burglars", 
				Outputter.DEBUG_TYPES.INIT);
		for (int i=0; i<num; i++) {
			allBurglars.get(i).storeFullHistory(true);
			Outputter.debugln("ContextCreator: Will write info about burglar: "+allBurglars.get(i).toString(),
					Outputter.DEBUG_TYPES.INIT);
		}

		//		/* Burglary (BurglarID, BuildingID, Time)*/
		//		// Can't populate this table with initial because no burglaries happened, but can test it works.
		//		if (GlobalVars.HISTORY_PARAMS.BURGLARY_OUT) {
		//			Object[] values = new Object[3]; 
		//			String[] names = new String[3];
		//			names[0] = "BurglarID"; values[0] = GlobalVars.BURGLAR_ENVIRONMENT.getRandomObject(Burglar.class).getID();
		//			names[1] = "BuildingID"; values[1] = GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(Building.class).getId();
		//			names[2] = "Time"; values[2] = -1;
		//			da.writeValues(values, GlobalVars.HISTORY_PARAMS.BURGLARY, names);
		//		}
	}

	/**
	 * Add some information to the history. This is called once per day from doHousekeeping(). It will do the
	 * following:
	 * <ul>
	 * <li>Record the security of each property</li>
	 * <li>Record each burglars' memory</li>
	 * <li>Record burglar information (if not doing this at every iteration for the burglar).</li>
	 * <li></li>
	 * </ul>
	 * @throws Exception  
	 */
	private void saveDailyHistory() throws Exception {
		/* Store building security  - should be House security?*/
		DataAccess da = DataAccessFactory.getDataAccessObject(GlobalVars.HISTORY_PARAMS.RESULTS_DATABASE);
		if (isTrue(GlobalVars.HISTORY_PARAMS.SECURITY_OUT)) {
			String[] names = new String[4]; 
			names[0] = "BuildingID"; names[1] = "ModelID"; names[2] = "Value"; names[3] = "Time";   
			Object[] values = new Object[4];
			for (Building b:GlobalVars.BUILDING_ENVIRONMENT.getAllObjects(Building.class)) {
				values[0] = b.getId();
				values[1] = GlobalVars.MODEL_ID;
				values[2] = b.getSecurity();
				values[3] = GlobalVars.getIteration();
				da.writeValues(values, GlobalVars.HISTORY_PARAMS.SECURITY, names);
			}
		}

		// Store some burglar information (only if writing memory or info, if neither then skip this)
		if (isTrue(GlobalVars.HISTORY_PARAMS.BURGLAR_MEMORY_OUT) || isTrue(GlobalVars.HISTORY_PARAMS.BURGLAR_INFO_OUT)) {
			Object[] values; String[] names;
			for (Burglar burglar:GlobalVars.BURGLAR_ENVIRONMENT.getAllObjects(Burglar.class)) {

				/* Store BurglarMemory if not writing about this burglar at every iteration anyway */
				// Not writing burglar memory at every iteration at the moment so write it for every burglar
				//				if (isTrue(GlobalVars.HISTORY_PARAMS.BURGLAR_MEMORY_OUT) && !burglar.storeFullHistory()) {
				if (isTrue(GlobalVars.HISTORY_PARAMS.BURGLAR_MEMORY_OUT)) {
					values = new Object[6];
					names = new String[6];
					Map<Building, List<Integer>> bm = burglar.getMemory().getFromMemory(Building.class);  
					for (Building building:bm.keySet()) {
						names[0] = "BurglarID"; values[0] = burglar.getID();
						names[1] = "ModelID"; values[1] = GlobalVars.MODEL_ID;
						names[2] = "BuildingID"; values[2] = building.getId();
						names[3] = "NumVisits"; values[3] = bm.get(building).get(BurglarMemory.VISITS_INDEX); 
						names[4] = "NumBurglaries"; values[4] = bm.get(building).get(BurglarMemory.BURGLARIES_INDEX);
						names[5] = "Time"; values[5] = GlobalVars.getIteration();
						da.writeValues(values, GlobalVars.HISTORY_PARAMS.BURGLAR_MEMORY, names);
					} // for BurglarMemory
				}// if BurglarMemoryOut

				/* Store BurglarInfo if not writing about this burglar at every iteration anyway */
				if (isTrue(GlobalVars.HISTORY_PARAMS.BURGLAR_INFO_OUT) && !burglar.storeFullHistory()) {
					writeBurglarHistory(da, burglar);
				} // if BurglarInfoOut

			} // for Burglars

		} // if BurglarMemoryOut or BurglarInfoOut

	}

	/** Print some information about all communities that cannot be established directly from input data. Will
	 * output (in csv format) the similarity to some communities and occupancy estimates at different times of
	 * day. Note that IDs of communities must be entered manually.
	 */
	@SuppressWarnings("unused")
	private void printSomeInfo() {
		System.out.println("WILL PRINT SOME COMMUNITY INFO IN CSV FORMAT");
		// Map to store buildings and all the info
		Map<Community, List<Double>> communityMap = new Hashtable<Community, List<Double>>();

		// Four communities and IDs which will have their similarity compared to all others
		Community similarCommunity1=null, similarCommunity2=null, similarCommunity3=null, similarCommunity4=null;
		// These ID's are for the Vancouver_City dataset
		int c1ID = 1371; 	// A west-side community
		int c2ID = 436;		// A downtown community
		int c3ID = 1034;	// An east-van community
		int c4ID = 569;		// A downtown east-side community
		// Five times of day which will be used to output occupancy levels
		double time1 = 8.0, time2 = 13.0, time3 = 18.0, time4 = 11.00, time5 = 3.00;
		// Build the map and find the similar communities
		for (Community c:GlobalVars.COMMUNITY_ENVIRONMENT.getAllObjects(Community.class)) {
			communityMap.put(c, new ArrayList<Double>());
			if (c.getId()==c1ID) { 
				similarCommunity1 = c;
				System.out.println("Found community 1:"+c.toString());
			}
			else if (c.getId() == c2ID) { 
				similarCommunity2 = c;
				System.out.println("Found community 2:"+c.toString());
			}
			else if (c.getId() == c3ID) {
				System.out.println("Found community 3:"+c.toString());
				similarCommunity3 = c;
			}
			else if (c.getId() == c4ID) {
				System.out.println("Found community 4:"+c.toString());
				similarCommunity4 = c; 
			}
		}

		// Now fill the map with information
		try {
			for (Community c:GlobalVars.COMMUNITY_ENVIRONMENT.getAllObjects(Community.class)) {
				List<Double> list = communityMap.get(c);
				list.add(c.getSociotype().compare(similarCommunity1.getSociotype()));
				list.add(c.getSociotype().compare(similarCommunity2.getSociotype()));
				list.add(c.getSociotype().compare(similarCommunity3.getSociotype()));
				list.add(c.getSociotype().compare(similarCommunity4.getSociotype()));
				list.add(c.getSociotype().getOccupancy(time1));
				list.add(c.getSociotype().getOccupancy(time2));
				list.add(c.getSociotype().getOccupancy(time3));
				list.add(c.getSociotype().getOccupancy(time4));
				list.add(c.getSociotype().getOccupancy(time5));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Print the header
		System.out.println("CommunityID, Sim"+c1ID+", Sim"+c2ID+", Sim"+c3ID+", Sim"+c4ID+
				", Occ"+time1+", Occ"+time2+", Occ"+time3+", Occ"+time4+", Occ"+time5);
		// Now print info for each community
		for (Community c:communityMap.keySet()) {
			List<Double> l = communityMap.get(c);
			System.out.print(c.getId()+", ");
			for (Double d:l) {
				System.out.print(d+", ");
			}
			System.out.println();
		}

		System.out.println("\nFINISHED OUTPUTTING INFO, EXITTING");
		System.exit(0);

	}

	/** Useful function which will sort a map on the values. Adapted from
	 *  http://forums.sun.com/thread.jspa?threadID=583931&start=15
	 * 
	 * @param map The map to be sorted 
	 * @return A map sorted on the size of the values.
	 */

	public static <S,T> Map<S,T> sortByValue(Map<S,T> map) {

		List<Entry<S,T>> list = new LinkedList<Entry<S,T>>(map.entrySet());

		Collections.sort(list, new Comparator<Entry<S,T>>() {
			@SuppressWarnings("unchecked")
			public int compare(Entry<S, T> o1, Entry<S, T> o2) {
				return ((Comparable<T>)o1.getValue()).compareTo(o2.getValue());
			}
		});

		// logger.info(list);

		Map<S,T> result = new LinkedHashMap<S,T>();		
		for (Iterator<Entry<S,T>> it = list.iterator(); it.hasNext();) {
			Entry<S,T> entry = (Entry<S,T>)it.next();
			result.put(entry.getKey(), entry.getValue());
		}
		return result;

	}

	private void writeSecurityHistory(DataAccess da) throws Exception {
		if (isTrue(GlobalVars.HISTORY_PARAMS.SECURITY_OUT)) {
			Outputter.debugln("\tpopulating building security", Outputter.DEBUG_TYPES.INIT);
			for (Building b:GlobalVars.BUILDING_ENVIRONMENT.getAllObjects(Building.class)) {
				Object[] values = new Object[3];
				String[] names = new String[3];
				//					names[0] = "BuildingID"; values[0] = b.getId();
				//					names[1] = "CommunityID"; values[1] = b.getCommunity().getId();
				//					names[2] = "Type"; values[2] = b.getType();
				//					da.writeValues(values, GlobalVars.HISTORY_PARAMS.BUILDINGS, names);


				values = new Object[4]; 
				names = new String[4];
				names[0] = "BuildingID"; values[0] = b.getId();
				names[1] = "ModelID"; values[1] = GlobalVars.MODEL_ID;
				names[2] = "Value"; values[2] = b.getSecurity();
				names[3] = "Time"; values[3] = GlobalVars.getIteration();
				da.writeValues(values, GlobalVars.HISTORY_PARAMS.SECURITY, names);
			}
		}
	}

	/** Convenience function to write burglar info (called a few times so I put the code here) */
	public static void writeBurglarHistory(DataAccess da, Burglar b) throws Exception {
		/* BurglarInfo (BurglarID, xcoord, ycoord, wealth, time) */
		Object[] values = new Object[10]; 
		String[] names = new String[10];
		names[0] = "BurglarID"; values[0] = b.getID();
		names[1] = "ModelID"; values[1] = GlobalVars.MODEL_ID;
		names[2] = "XCoord"; values[2] = GlobalVars.BURGLAR_ENVIRONMENT.getCoords(b).getX();
		names[3] = "YCoord"; values[3] = GlobalVars.BURGLAR_ENVIRONMENT.getCoords(b).getY();
		names[4] = "Wealth"; values[4] = b.getWealth();
		names[5] = "Workplace"; values[5] = b.getWork().getId();
		names[6] = "Time"; values[6] = GlobalVars.getIteration() ;
		names[7] = "DrugDealer"; values[7] = b.getDrugDealer().getId();
		names[8] = "Social"; values[8] = b.getSocial().getId();
		// Action is included so it doesn't have to be done in the Actions table as well, fewer database calls
		names[9] = "Action"; values[9] = 
			b.getActionGuidingMotive()==null || b.getActionGuidingMotive().getCurrentAction()==null ? 
				"null" :
				b.getActionGuidingMotive().getCurrentAction().toString();
		da.writeValues(values, GlobalVars.HISTORY_PARAMS.BURGLAR_INFO, names);		
	}

	/** Convert an iteration count to real time, readable by CrimeAnalyst (assumes simulation starts at midnight on
	 * 1st Janurary. */
	public static String convertToRealTime(int iteration, int iterPerDay) {

		// Create a date object from the number of iterations by calculating how many milliseconds in an
		// iteration and then using the normal Date constructor (no. milliseconds from 1970)
		double millisPerIter = (24 * 60 * 60 * 1000) / iterPerDay;
		Calendar c = new GregorianCalendar();
		Date d = new Date((long)(millisPerIter * iteration));
		c.setTime(d);

		// Now format the date properly and return a string
		return ContextCreator.checkDigits(c.get(Calendar.DAY_OF_MONTH))+"/"+
		ContextCreator.checkDigits(c.get(Calendar.MONTH)+1)+"/"+c.get(Calendar.YEAR)+" "+
		ContextCreator.checkDigits(c.get(Calendar.HOUR_OF_DAY))+":"+
		ContextCreator.checkDigits(c.get(Calendar.MINUTE));

	}
	/** Make sure the input has enough digits, e.g. if input = 1, output = 01 */
	private static String checkDigits(int input) {
		String output = String.valueOf(input);
		if (output.length()==1) output = "0"+output;
		return output;
	}

	/** Stops the simulation. Attempts to call stop methods for GUI and if model is running programatically
	 * or on NGS */
	public static void haltSim() {
		RunEnvironment.getInstance().endRun(); // stop sim us using gui
		BurgdSimMain.stopSim(); // stop sim if running programatically
		BurgdSimMainMPJ.stopSim();	// stop sim if on NGS
	}

	/**
	 * Convenience paramter to convert integers to booleans.
	 * @param input The input integer
	 * @return False if the input is 0, true otherwise.
	 */
	public static final boolean isTrue(int input) {
		if (input == 0) {
			return false;
		}
		else {
			return true;
		}
	}


}

/** Used to redirect standard error to nowhere */
class NullOutputStream extends OutputStream {

	private static int numCalls = 0 ; // The number of times this outputstream has been called
	private static final int cutoff = 10000; // When to stop reporting errors

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (write()) {
			super.write(b, off, len);
		}
	}

	@Override
	public void write(byte[] b) throws IOException {
		if (write()) {
			super.write(b);
		}
	}

	@Override
	public void write(int value) throws IOException {
		//		if (write()) {
		//			// Convert integet to a byte array
		//	        byte[] ba = new byte[4];
		//	        for (int i = 0; i < 4; i++) {
		//	            int offset = (ba.length - 1 - i) * 8;
		//	            ba[i] = (byte) ((value >>> offset) & 0xFF);
		//	        }
		//			super.write(ba);
		//		}

	}

	private static boolean write() {
		//		return true;

		numCalls+=1;
		if (numCalls < cutoff) {
			Outputter.describeln("NullOutputStream.write() called "+numCalls);
			return true;
		}
		else if (numCalls == cutoff){
			Outputter.errorln("ContextCreator.NullOutputStream: got too many non-model errors (errors that" +
					"aren't being caught by the model) Will stop displaying them and won't display this message " +
			"again.");
			return false;
		}
		else {
			return false;
		}
	}

}

/** Controls the allocation of <code>BurglarThread</code>s to free CPUs */
class ThreadController implements Runnable {

	private ContextCreator cc; 		// A pointer to ContextCreator, used to inform cc when it can wake up
	private int numCPUs; 			// The number of CPUs which can be utilised
	private boolean[] cpuStatus;	// Record which cpus are free (true) or busy (false)

	public ThreadController(ContextCreator cc) {
		this.cc = cc;
		this.numCPUs = Runtime.getRuntime().availableProcessors();
		// Set all CPU status to 'free'
		this.cpuStatus = new boolean[this.numCPUs];
		for (int i=0; i<this.numCPUs; i++) {
			this.cpuStatus[i] = true;
		}
		//		System.out.println("ThreadController found "+this.numCPUs+" CPUs");
	}

	/** Start the ThreadController. Iterate over all burglars, starting <code>BurglarThread</code>s 
	 * on free CPUs. If no free CPUs then wait for a BurglarThread to finish.
	 */
	public void run() {

		//		System.out.print("Thread controller is stepping burglars...");
		List<Burglar> burglars = GlobalVars.BURGLAR_ENVIRONMENT.getAllObjects(Burglar.class);
		SimUtilities.shuffle(burglars, RandomHelper.getUniform());
		for (Burglar b:burglars) {
			// Find a free cpu to exectue on
			boolean foundFreeCPU = false; // Determine if there are no free CPUs so thread can wait for one to become free
			while (!foundFreeCPU) {
				synchronized (this) {
					//					System.out.println("ThreadController looking for free cpu for burglar "+b.toString()+", "+Arrays.toString(cpuStatus));
					cpus: for (int i=0; i<this.numCPUs; i++) {
						if (this.cpuStatus[i]) {
							// Start a new thread on the free CPU and set it's status to false
							//							System.out.println("ThreadController running burglar "+b.toString()+" on cpu "+i+". ");
							foundFreeCPU = true;
							this.cpuStatus[i] = false;
							(new Thread(new BurglarThread(this, i, b))).start();
							break cpus; // Stop looping over CPUs, have found a free one for this burglar
						}
					} // for cpus
				if (!foundFreeCPU) {
					this.waitForBurglarThread();
				} // if !foundFreeCPU
				}
			} // while !freeCPU
		} // for burglars

		//		System.out.println("ThreadController finished looping burglars");

		// Have started stepping over all burglars, now wait for all to finish.
		boolean allFinished = false;
		while (!allFinished) {
			allFinished = true;
			synchronized (this) {
				//				System.out.println("ThreadController checking CPU status: "+Arrays.toString(cpuStatus));
				cpus: for (int i=0; i<this.cpuStatus.length; i++) {
					if (!this.cpuStatus[i]) {
						allFinished = false;
						break cpus;
					}
				} // for cpus
			if (!allFinished) {
				this.waitForBurglarThread();
			}
			}
		} // while !allFinished
		// Finished, tell the context creator to start up again.
		//		System.out.println("ThreadController finished stepping all burglars (iteration "+GlobalVars.getIteration()+")"+Arrays.toString(cpuStatus));
		this.cc.setBurglarsFinishedStepping();
	}

	/**
	 * Causes the ThreadController to wait for a BurglarThred to notify it that it has finished and a CPU
	 * has become free.
	 */
	private synchronized void waitForBurglarThread() {
		try {
			//			System.out.println("ThreadController got no free cpus, waiting "+Arrays.toString(cpuStatus));
			this.wait();
			//			System.out.println("NOTIFIED");
		} catch (InterruptedException e) {
			Outputter.errorln("ContextCreator.ThreadController caught Interrupted Exceltion: "+e.getMessage());
			e.printStackTrace();
		}// Wait until the thread controller has finished

	}

	/** Tell this <code>ThreadController</code> that one of the CPUs is no free and it can stop waiting
	 * @param cpuNumber The CPU which is now free
	 */
	public synchronized void setCPUFree(int cpuNumber) {
		//		System.out.println("ThreadController has been notified that CPU "+cpuNumber+" is now free");
		this.cpuStatus[cpuNumber] = true;
		this.notifyAll();
	}

}

/** Single thread to call a Burglar's step method */
class BurglarThread implements Runnable {

	private Burglar theburglar; // The burglar to step
	private ThreadController tc;
	private int cpuNumber; // The cpu that the thread is running on, used so that ThreadController
	//	private static int uniqueID = 0;
	//	private int id;


	public BurglarThread(ThreadController tc, int cpuNumber, Burglar b) {
		this.tc = tc;
		this.cpuNumber = cpuNumber;
		this.theburglar = b;
		//		this.id = BurglarThread.uniqueID++;
	}

	public void run() {
		//		System.out.println("BurglarThread "+id+" stepping burglar "+this.theburglar.toString()+" on CPU "+this.cpuNumber);
		this.theburglar.step();
		for (StateVariable s:this.theburglar.getStateVariables()) {
			s.step();
		}
		//		System.out.println("BurglarThread "+id+" finished burglar "+this.theburglar.toString()+" on CPU "+this.cpuNumber);
		// Tell the ThreadController that this thread has finished
		tc.setCPUFree(this.cpuNumber); // Tell the ThreadController that this thread has finished
	}

}