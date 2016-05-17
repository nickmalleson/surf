package burgdsim.burglars;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import repast.simphony.context.Context;
import repast.simphony.context.DefaultContext;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import repast.simphony.space.gis.ShapefileLoader;
import repast.simphony.space.gis.SimpleAdder;


import burgdsim.burglary.DebugBurglaryV;
import burgdsim.environment.Community;
import burgdsim.environment.TempCommunity;
import burgdsim.environment.VancouverSociotype;
import burgdsim.environment.buildings.Building;
import burgdsim.environment.buildings.DrugDealer;
import burgdsim.environment.buildings.House;
import burgdsim.environment.buildings.Social;
import burgdsim.environment.buildings.Workplace;

import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;
import burgdsim.pecs.DoNothingV;
import burgdsim.pecs.DrugsV;
import burgdsim.pecs.RandomWalkV;
import burgdsim.pecs.SleepV;
import burgdsim.pecs.SocialV;
import burgdsim.pecs.StateVariable;

/**
 * Used to create burglars. Adds the burglar to the BURGLAR_ENVIRONMENT and also returns it.<br>
 * 
 * The BurglarFactory is initialised in a very long-winded way. ScenarioGenerator reads how the burgars
 * should be created from the scenario parameters file. It then creates an instance of BurglarFactory
 * which is initialised properly, depending on what was put into the scenario file. Then the ScenarioGenerator
 * passes the instance of BurglaryFactory to all the Scenario objects it creates. Finally, in execute()
 * each scenario will call BurglarFactory.setInstance(), passing the original instance created by
 * ScenarioGenerator. When ContextCreator is setting up the simulation it will call 
 * BurglarFactory.getInstance().createBurglars(), thus creating burglars appropriately.
 * 
 * @author Nick Malleson
 */
public class BurglarFactory implements Serializable {

	private static final long serialVersionUID = 1L;

	private static int burglarNumber = 0;

	/** The different ways that burglars can be created (as specified in the scenario parameters file). */
	private enum CREATION_METHOD {SHAPEFILE, SPECIFIC, TEXT, COMMUNITIES};
	private CREATION_METHOD method = null;

	/** Call a specific implementation method to create burglars*/
	private String specificMethod = null;

	/** If creating burglars from information stored in the communities file */
	private List<String[]> burglarsFromCommunities; 

	private static BurglarFactory instance;

	public static void setInstance(BurglarFactory instance) {
		BurglarFactory.instance = instance;
	}
	public static BurglarFactory getInstance() {
		return BurglarFactory.instance;
	}

	/** Once this BurglarFactory has been properly initialised, (all the required variables set by Scenario) 
	 * this function can be used to create the required burglars. Called in ContextCreator once the
	 * environments have been created properly */
	public void createBurglars() throws Exception {
		if (this.method.equals(CREATION_METHOD.SPECIFIC)) {
			if (this.specificMethod == null) {
				BurglarFactory.error("BurglarFactory.createBurglars() error: this burglar factory is going to use " +
						"a specific method to create burglars but the method hasn't been set. The setSpecificMethod() " +
				"function should have been called.");
			}
			Outputter.debugln("BurglarFactory using a specific implementation to create burglars: "+
					this.specificMethod, Outputter.DEBUG_TYPES.INIT);
			this.createSpecificBurglars();
		}

		else if (this.method.equals(CREATION_METHOD.SHAPEFILE)) {
			BurglarFactory.error("BurglarFactory.createBurglars() error: haven't implemented the shapefile method to" +
			" create burglars.");

		}

		else if (this.method.equals(CREATION_METHOD.TEXT)) {
			BurglarFactory.error ("BurglarFactory.createBurglars() error: haven't implemented the textual method to" +
			" create burglars.");		
		}

		else if (this.method.equals(CREATION_METHOD.COMMUNITIES)) {
			if (this.burglarsFromCommunities==null) {
				BurglarFactory.error("BurglarFactory.createBurglars() error: this burglar factory is going to create " +
				"burglars from communities but the setBurglarsInCommunities() method should have been called.");
			}
			Outputter.debugln("BurglarFactory creating burglars from information in the Communities shapefile",
					Outputter.DEBUG_TYPES.INIT);
			this.createBurglarsFromCommunities();
		}		

		else {
			BurglarFactory.error("Unrecognised creation method: "+this.method.toString()+". No idea " +
			"how this could happen!");
		}
	}

	/** Convenience function, prints the error and throws a new exception */
	private static void error(String error) throws Exception {
		Outputter.errorln(error);
		throw new Exception(error);
	}

	private void createSpecificBurglars() throws Exception  {
		if (this.specificMethod.equals("vancouver_skytrain1")) { // Used in VancouverSkytrain1 scenario
			/* Will go through each community, read it's VANDIX (deprivation) and create burglars such that,
			 * on average, the number of burglars will correspond to the VANDIX value. E.g. if VANDIX = 0.2 for
			 * a community, then (on average) 0.2 burglars created for that community.
			 */
			int numBurglars = 0; // the number of burglars created eventually
			double pctBurgs = 0.1;	// Reduce/increase number or burglars by multiplying probability by this amount
			for (Community c:GlobalVars.COMMUNITY_ENVIRONMENT.getAllObjects(Community.class)) {
				VancouverSociotype vc = (VancouverSociotype) c.getSociotype();
				if ((vc.getVandix()*pctBurgs) > RandomHelper.nextDouble()) { // Create a burglar
					House home = c.getRandomBuilding(House.class);
					if (home==null) {
						//						System.out.println("Not creating because no houses: for sociotype "+vc.toString()+" ("+vc.getVandix()+")");
						Outputter.errorln("BurglarFactory.createSpecificBurglars(): didn't find a house in community "+
								c.toString()+". Doesn't matter, just ignoring this community (don't worry about the previous" +
						" error about returning a null value).");
						continue;
					}
					//					System.out.println("Creating a burglar for sociotype "+vc.toString()+" ("+vc.getVandix()+")");
					List<StateVariable> stateVariables = new ArrayList<StateVariable>();
					Burglar burglar = new VancouverSkyTrainBurglar(stateVariables);
					GlobalVars.BURGLAR_ENVIRONMENT.add(burglar);
					// Allow agent to get bus and train but not car
					burglar.addToTransportAvailable(GlobalVars.TRANSPORT_PARAMS.BUS);
					burglar.addToTransportAvailable(GlobalVars.TRANSPORT_PARAMS.TRAIN);
					//					burglar.addToTransportAvailable(GlobalVars.TRANSPORT_PARAMS.CAR);
					burglar.setName("SimpleDrugsSocialBurglar(Default)"+burglarNumber++);
					// Have to set up their homes etc first because these are required by actions.
					burglar.setHome(home);
					GlobalVars.BURGLAR_ENVIRONMENT.move(burglar, GlobalVars.BUILDING_ENVIRONMENT.getCoords(home));
					burglar.initMemory();
					//					burglar.setWork(GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(Workplace.class));
					burglar.setDrugDealer(GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(DrugDealer.class));
					//					burglar.setSocial(GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(Social.class));

					StateVariable sleep = new SleepV(burglar, 0.5); stateVariables.add(sleep);
					StateVariable doNothing = new DoNothingV(burglar); stateVariables.add(doNothing);
					StateVariable drugs = new DrugsV(burglar, 1.0); stateVariables.add(drugs);
					StateVariable social = new SocialV(burglar, 1.0); stateVariables.add(social);

					Outputter.debugln("BurglarFactory created new burglar: "+burglar.getName()+" with following: \n" +
							"\thome: "+home.toString()+" "+GlobalVars.BUILDING_ENVIRONMENT.getCoords(home).toString()+"\n" +
							"\twork: "+burglar.getWork().toString()+" "+GlobalVars.BUILDING_ENVIRONMENT.getCoords(burglar.getWork()).toString()+"\n" +
							"\tsocial: "+burglar.getSocial().toString()+" "+GlobalVars.BUILDING_ENVIRONMENT.getCoords(burglar.getSocial()).toString()+"\n" +
							"\tdrug: "+burglar.getDrugDealer().toString()+" "+GlobalVars.BUILDING_ENVIRONMENT.getCoords(burglar.getDrugDealer()).toString()+"\n",
							Outputter.DEBUG_TYPES.GENERAL);
					numBurglars++;
				}// if create burglar
				//				else {
				//					System.out.println("NOT creating a burglar for sociotype "+vc.toString()+" ("+vc.getVandix()+")");
				//				}
			} // for communities
			Outputter.debugln("BurglarFactory.createSpecificBurglars() created "+numBurglars+" burglars", 
					Outputter.DEBUG_TYPES.INIT);
		}

		else if (this.specificMethod.equals("default")) { // Old, deprecated, way of creating burglars (now just used for testing)
			// Check to see if running a sensitivity test for which a number of burglars need to be created with
			// specific behaviour parameters
			if (!GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.BURGLAR_SENSITIVITY_TEST_PARAMETER.equals("")) {
				// We are running a sensitivity tests but there are two types: null and grid tests. For null environment
				// 11 burglars should be created with ascending parameter values, in grid environment only 1 burglar is
				// created with the given parameter value. 
				if (GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.BURGLAR_PARMETER_VALUE==-1.0) {
					// As BURGLAR_PARMETER_VALUE hasn't been set then it is the null test.
					Outputter.debugln("BurglarFactory.createSpecificBurglars(): BURGLAR_SENSITIVITY_TEST_PARAMETER has " +
							"been set so will try to create 11 SensitivityTestBurglars with increasing values for " +
							"parameter "+GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.BURGLAR_SENSITIVITY_TEST_PARAMETER, 
							Outputter.DEBUG_TYPES.INIT);
					// Create 11 default burglars, and set their parameter values
					int counter = 0; // For ascending burglar id's
					for (double d=0.0; d<=1.0; d+=0.1 ) {
						createSensitivityTestBurglar(d, counter++);
					}

				} // if BURGLAR_PARMETER_VALUE==-1.0)
				else { // Must be running a grid sensitivity test, only create one burglar the a parameter value.
					Outputter.debugln("BurglarFactory.createSpecificBurglars(): BURGLAR_SENSITIVITY_TEST_PARAMETER and" +
							"BURGLAR_PARMETER_VALUE have been set. Will create a sensitivity test burglar with value " +
							GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.BURGLAR_PARMETER_VALUE+" for parameter "+ 
							GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.BURGLAR_SENSITIVITY_TEST_PARAMETER, 
							Outputter.DEBUG_TYPES.INIT);
					
					createSensitivityTestBurglar(GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.BURGLAR_PARMETER_VALUE, 1);

				}
			}
			else {
				Outputter.debugln("BurglarFactory.createSpecificBurgalrs() creating "+GlobalVars.NUM_BURGLARS+
						" burglars", Outputter.DEBUG_TYPES.INIT);
				for (int i=0; i<GlobalVars.NUM_BURGLARS; i++) { 
					BurglarFactory.createBurglar("SIMPLE_DRUGS_SOCIAL_BURGLAR");
					// BurglarFactory.createBurglar("SIMPLE_DRUGS_SOCIAL");
				}
			}
		}

		// Used in sensitivity tests. Agent has full-time work so doesn't need to burgle
		else if (this.specificMethod.equals("working_burglar")) { // Old, deprecated, way of creating burglars (now just used for testing)
			Outputter.debugln("BurglarFactory.createSpecificBurgalrs() creating "+GlobalVars.NUM_BURGLARS+
					" working agents.", Outputter.DEBUG_TYPES.INIT);
			for (int i=0; i<GlobalVars.NUM_BURGLARS; i++) {
				List<StateVariable> stateVariables = new ArrayList<StateVariable>();
				Burglar agent = new Worker(stateVariables);
				GlobalVars.BURGLAR_ENVIRONMENT.add(agent);
				agent.setName("Worker "+burglarNumber++);
				// Have to set up their homes etc first because these are required by actions.
				House home = (House)GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(House.class);
				agent.setHome(home);
				GlobalVars.BURGLAR_ENVIRONMENT.move(agent, GlobalVars.BUILDING_ENVIRONMENT.getCoords(home));
				agent.initMemory();
				agent.setWork(GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(Workplace.class));
				agent.setDrugDealer(GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(DrugDealer.class));
				agent.setSocial(GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(Social.class));

				StateVariable sleep = new SleepV(agent, 0.5);
				stateVariables.add(sleep);
				StateVariable doNothing = new DoNothingV(agent);
				stateVariables.add(doNothing);
				StateVariable drugs = new DrugsV(agent, 1.0);
				stateVariables.add(drugs);
				StateVariable social = new SocialV(agent, 1.0);
				stateVariables.add(social);
				Outputter.debugln(agent.toString(), Outputter.DEBUG_TYPES.INIT);
			}
		}
		
		// Used in GIS sensitivity tests. Burglar created at specific home address with/without access to transport
		else if (this.specificMethod.equals("gis_sensitivity_test")) {
			Outputter.debugln("BurglarFactory.createSpecificBurgalrs('gis_sensitivity_test') creating "+
					GlobalVars.NUM_BURGLARS+ " BaseBuglar agents.", Outputter.DEBUG_TYPES.INIT);
			for (int i=0; i<GlobalVars.NUM_BURGLARS; i++) {
				List<StateVariable> stateVariables = new ArrayList<StateVariable>();
				Burglar agent = new BaseBurglar(stateVariables);
				agent.setName("BaseBurglar "+burglarNumber++);
				
				// All agents live at a particular house
				int houseid = 211080;
				House home = null;
				for (House h:GlobalVars.BUILDING_ENVIRONMENT.getAllObjects(House.class)) {
					if (h.getId()==houseid) {
						home = h;
						break;
					}
				}
				if (home==null) {
					Outputter.errorln("BurglarFactory.createSpecificBurgalrs('gis_sensitivity_test') could not "+
							"find a house with id "+houseid+" for the agents to live in. This is going to break things.");
				}
				// Have to set up their homes etc first because these are required by actions.
				agent.setHome(home);
				GlobalVars.BURGLAR_ENVIRONMENT.add(agent);
				GlobalVars.BURGLAR_ENVIRONMENT.move(agent, GlobalVars.BUILDING_ENVIRONMENT.getCoords(home));
				agent.initMemory();
//				agent.setWork(GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(Workplace.class));
				// Only one drug dealer and social location should exist in the environment for the sensitivity tests
				agent.setDrugDealer(GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(DrugDealer.class));
				agent.setSocial(GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(Social.class));

				// Might have access to transport
				if (GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.GIS_SENSITIVITY_TEST_TYPE == 0) {
					Outputter.errorln("BurglarFactory.createBurglars() warning: GIS_SENSITIVITY_TEST_TYPE is " +
							"0, but I've been told to create burglars using the 'gis_sensitivity_type' method " +
							"so this should say whether or not they have availability to transport.");
				}
				else if (GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.GIS_SENSITIVITY_TEST_TYPE == 1) {
					Outputter.debugln("BurglarFactory.createBurglars() Creating burglars without access to " +
							"transport.", Outputter.DEBUG_TYPES.INIT);
					// Don't do anything, by default burglars don't have access to transport.
				}
				else if (GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.GIS_SENSITIVITY_TEST_TYPE == 2) {
					Outputter.debugln("BurglarFactory.createBurglars() Creating burglars with access to " +
							"public transport.", Outputter.DEBUG_TYPES.INIT);
					agent.addToTransportAvailable(GlobalVars.TRANSPORT_PARAMS.TRAIN);
				}
				else {
					Outputter.errorln("BurglarFactory.createBurglars() warning: GIS_SENSITIVITY_TEST_TYPE is not "+
							"recognised: "+GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.GIS_SENSITIVITY_TEST_TYPE);
				}
				
				StateVariable sleep = new SleepV(agent, 0.5);
				stateVariables.add(sleep);
				StateVariable doNothing = new DoNothingV(agent);
				stateVariables.add(doNothing);
				StateVariable drugs = new DrugsV(agent, 1.0);
				stateVariables.add(drugs);
				StateVariable social = new SocialV(agent, 1.0);
				stateVariables.add(social);
				Outputter.debugln(agent.toString(), Outputter.DEBUG_TYPES.INIT);
			}
		}


		else {
			BurglarFactory.error("BurglarFactory.createSpecificBurglars(): unrecognised specific type of implementation: "+
					this.specificMethod);
		}
	}

	/**
	 * Convenience method to create a SensitivityTestBurglar
	 * @param parameterValue The value of a given parameter (see calling function for information).
	 * @param agentID The agent's id
	 */
	private void createSensitivityTestBurglar(double parameterValue, int agentID) {
		List<StateVariable> svList = new ArrayList<StateVariable>();
		Burglar burglar = new SensitivityTestBurglar(svList, parameterValue, agentID);
		GlobalVars.BURGLAR_ENVIRONMENT.add(burglar);
		burglar.setName("SensitivityTestBurglar"+burglarNumber++);
		House home = null;
		// Sometimes (in sensitivity tests) can set the burglar starting location.
		if (GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.BURGLAR_HOME_X != -1 && 
				GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.BURGLAR_HOME_Y != -1) {
			if (GlobalVars.NUM_BURGLARS > 1) {
				Outputter.errorln("Warning: BUrglarFactory.createSpecificBurglars: the BURGLAR_HOME_X and " +
						"BURGLAR_HOME_Y variables are not -1, but the number of burglars to be created " +
						"is > 1, this means that lots of burglars are going to be created in same house. " +
				"This is probably a mistake.");
			}
			// Find the house with the specified coordinates
			for (House h:GlobalVars.BUILDING_ENVIRONMENT.getAllObjects(House.class)) {
				if (h.getCoords().getX() == GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.BURGLAR_HOME_X && 
						h.getCoords().getY() == GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.BURGLAR_HOME_Y){
					home = h;
					Outputter.debugln("BurglarFactory.createSpecificBurglars: all burglars starting at house: "+
							h.toString(), Outputter.DEBUG_TYPES.INIT) ;
					break;
				}
			} // for houses
			if (home == null) {
				Outputter.errorln("BurglarFactory.createSpecificBurglars() error: couldn't find a house with " +
						"coordinates ("+GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.BURGLAR_HOME_X+","+
						GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.BURGLAR_HOME_Y+") for burglar to live in.");
				return;
			}
		}
		else { // Not specifying individual coords, find a random house
			home = (House)GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(House.class);
		}
		burglar.setHome(home);
		GlobalVars.BURGLAR_ENVIRONMENT.move(burglar, GlobalVars.BUILDING_ENVIRONMENT.getCoords(home));
		burglar.initMemory();
		burglar.setWork(GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(Workplace.class));
		burglar.setDrugDealer(GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(DrugDealer.class));
		burglar.setSocial(GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(Social.class));

		StateVariable sleep = new SleepV(burglar, 0.5); svList.add(sleep);
		StateVariable doNothing = new DoNothingV(burglar); svList.add(doNothing);
		StateVariable drugs = new DrugsV(burglar, 1.0); svList.add(drugs);
		StateVariable social = new SocialV(burglar, 1.0); svList.add(social);
		agentID++;
	}
		
	
	public void setSpecificMethod(String specificMethod) {
		this.specificMethod = specificMethod;
	}

	/** 
	 * Used to tell this burglary factory how the burglars will be created
	 * @param method Either 'shapefile' (read types and locations from a shapefile), 'specific'
	 * (call a particular, specific, implementation in this factory) or 'text' (textually define
	 * the types, numbers and starting locations of burglars).
	 */
	public void setCreateMethod(String method) {
		if (method.equals("shapefile")) {
			this.method = CREATION_METHOD.SHAPEFILE;
		}
		else if (method.equals("specific")) {
			this.method = CREATION_METHOD.SPECIFIC;
		}
		else if (method.equals("text")) {
			this.method = CREATION_METHOD.TEXT;
		}
		else if (method.equals("communities")) {
			this.method = CREATION_METHOD.COMMUNITIES;	
		}
		else {
			Outputter.errorln("BurglarFactory.setCreateMethod() error: don't recognise the given method" +
					" of creating burglars: "+method+". The factory will fail to create burglars.");
			this.method = null;
		}
	}

	/** DEPRECATED. Used to manually create burglars before ScenarioGenerator did this. Now, ScenarioGenerator
	 * creates an instance of BurglarFactory which is configured to represent a particular scenario. This instance
	 * is passed to all Scenarios, they just need to get ContextCreator to call createBurglars(). 
	 * NOTE: this still used with sensitivity tests.*/
	public static Burglar createBurglar(String type) {
		if (type.equals("SIMPLE_PERSON")) {
			List<StateVariable> simpleList = new ArrayList<StateVariable>();
			Burglar simpleBurglar = new SimpleDefaultBurglar(simpleList);
			GlobalVars.BURGLAR_ENVIRONMENT.add(simpleBurglar);
			simpleBurglar.setName("SimpleBurglar"+burglarNumber++);

			House home = (House)GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(House.class);
			simpleBurglar.setHome(home);
			GlobalVars.BURGLAR_ENVIRONMENT.move(simpleBurglar, GlobalVars.BUILDING_ENVIRONMENT.getCoords(home));
			simpleBurglar.initMemory();
			simpleBurglar.setWork(GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(Workplace.class));
			simpleBurglar.setDrugDealer(GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(DrugDealer.class));
			simpleBurglar.setSocial(GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(Social.class));

			StateVariable sleep = new SleepV(simpleBurglar, 0.5); 
			simpleList.add(sleep);
			StateVariable doNothing = new DoNothingV(simpleBurglar);
			simpleList.add(doNothing);
			return simpleBurglar;
		}
		else if (type.equals("RANDOM_WALKER")) {
			List<StateVariable> walkerList = new ArrayList<StateVariable>();
			Burglar walker = new SimpleDefaultBurglar(walkerList);
			GlobalVars.BURGLAR_ENVIRONMENT.add(walker);
			walker.setName("RandomWalking person "+burglarNumber++);

			House home = (House)GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(House.class);
			walker.setHome(home);
			GlobalVars.BURGLAR_ENVIRONMENT.move(walker, GlobalVars.BUILDING_ENVIRONMENT.getCoords(home));
			walker.initMemory();

			StateVariable randomWalk = new RandomWalkV(walker, 1); 
			walkerList.add(randomWalk);
			return walker;
		}
		else if (type.equals("RANDOM_DRIVER")) {
			List<StateVariable> driverList = new ArrayList<StateVariable>();
			Burglar driver= new SimpleDefaultBurglar(driverList);
			driver.addToTransportAvailable(GlobalVars.TRANSPORT_PARAMS.CAR);
			driver.removeFromTransportAvailable(GlobalVars.TRANSPORT_PARAMS.WALK);
			GlobalVars.BURGLAR_ENVIRONMENT.add(driver);
			driver.setName("RandomDriving  person "+burglarNumber++);

			House home = (House)GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(House.class);
			driver.setHome(home);
			GlobalVars.BURGLAR_ENVIRONMENT.move(driver, GlobalVars.BUILDING_ENVIRONMENT.getCoords(home));
			driver.initMemory();

			StateVariable randomWalk = new RandomWalkV(driver, 1); 
			driverList.add(randomWalk);
			return driver;
		}

		// A person who needs to sleep, socialise and take drugs but who can work to do this
		else if (type.equals("SIMPLE_DRUGS_SOCIAL")) {
			List<StateVariable> simpleList = new ArrayList<StateVariable>();
			Burglar burglar = new SimpleDefaultBurglar(simpleList);
			GlobalVars.BURGLAR_ENVIRONMENT.add(burglar);
			burglar.setName("SimpleBurglar"+burglarNumber++);
			// Have to set up their homes etc first because these are required by actions.
			House home = (House)GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(House.class);
			//			House home = (House)GlobalVars.BUILDING_ENVIRONMENT.getObjectAt(new Coord(5,6));
			burglar.setHome(home);
			GlobalVars.BURGLAR_ENVIRONMENT.move(burglar, GlobalVars.BUILDING_ENVIRONMENT.getCoords(home));
			burglar.initMemory();
			burglar.setWork(GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(Workplace.class));
			burglar.setDrugDealer(GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(DrugDealer.class));
			burglar.setSocial(GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(Social.class));

			StateVariable sleep = new SleepV(burglar, 1.0);
			simpleList.add(sleep);
			StateVariable doNothing = new DoNothingV(burglar);
			simpleList.add(doNothing);
			StateVariable drugs = new DrugsV(burglar, 1.0);
			simpleList.add(drugs);
			StateVariable social = new SocialV(burglar, 1.0);
			simpleList.add(social);

			Outputter.describeln("BurglarFactory created new burglar: "+burglar.getName()+" with following: \n" +
					"\thome: "+home.toString()+" "+GlobalVars.BUILDING_ENVIRONMENT.getCoords(home).toString()+"\n" +
					"\twork: "+burglar.getWork().toString()+" "+GlobalVars.BUILDING_ENVIRONMENT.getCoords(burglar.getWork()).toString()+"\n" +
					"\tsocial: "+burglar.getSocial().toString()+" "+GlobalVars.BUILDING_ENVIRONMENT.getCoords(burglar.getSocial()).toString()+"\n" +
					"\tdrug: "+burglar.getDrugDealer().toString()+" "+GlobalVars.BUILDING_ENVIRONMENT.getCoords(burglar.getDrugDealer()).toString()+"\n" );
			return burglar;

		}
		// Needs to sleep and take drugs and can also burgle
		else if (type.equals("SIMPLE_DRUGS_SOCIAL_BURGLAR")) {
			List<StateVariable> simpleList = new ArrayList<StateVariable>();
			Burglar burglar = new SimpleDefaultBurglar(simpleList);
			// Allow agent to get bus and train but not car
			burglar.addToTransportAvailable(GlobalVars.TRANSPORT_PARAMS.BUS);
			burglar.addToTransportAvailable(GlobalVars.TRANSPORT_PARAMS.TRAIN);
			GlobalVars.BURGLAR_ENVIRONMENT.add(burglar);
			burglar.setName("SimpleDrugsSocialBurglar(Default)"+burglarNumber++);
			House home = null;
			// Sometimes (in sensitivity tests) can set the burglar starting location.
			if (GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.BURGLAR_HOME_X != -1 && 
					GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.BURGLAR_HOME_Y != -1) {
				if (GlobalVars.NUM_BURGLARS > 1) {
					Outputter.errorln("Warning: BUrglarFactory.createSpecificBurglars: the BURGLAR_HOME_X and " +
							"BURGLAR_HOME_Y variables are not -1, but the number of burglars to be created " +
							"is > 1, this means that lots of burglars are going to be created in same house. " +
					"This is probably a mistake.");
				}
				// Find the house with the specified coordinates
				for (House h:GlobalVars.BUILDING_ENVIRONMENT.getAllObjects(House.class)) {
					if (h.getCoords().getX() == GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.BURGLAR_HOME_X && 
							h.getCoords().getY() == GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.BURGLAR_HOME_Y){
						home = h;
						Outputter.debugln("BurglarFactory.createSpecificBurglars: all burglars starting at house: "+
								h.toString(), Outputter.DEBUG_TYPES.INIT) ;
						break;
					}
				} // for houses
				if (home == null) {
					Outputter.errorln("BurglarFactory.createSpecificBurglars() error: couldn't find a house with " +
							"coordinates ("+GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.BURGLAR_HOME_X+","+
							GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.BURGLAR_HOME_Y+") for burglar to live in.");
					return null;
				}
			}
			else { // Not specifying individual coords, find a random house
				home = (House)GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(House.class);
			}			burglar.setHome(home);
			GlobalVars.BURGLAR_ENVIRONMENT.move(burglar, GlobalVars.BUILDING_ENVIRONMENT.getCoords(home));
			burglar.initMemory();
			burglar.setWork(GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(Workplace.class));
			burglar.setDrugDealer(GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(DrugDealer.class));
			burglar.setSocial(GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(Social.class));

			StateVariable sleep = new SleepV(burglar, 0.5);
			simpleList.add(sleep);
			StateVariable doNothing = new DoNothingV(burglar);
			simpleList.add(doNothing);
			StateVariable drugs = new DrugsV(burglar, 1.0);
			simpleList.add(drugs);
			StateVariable social = new SocialV(burglar, 1.0);
			simpleList.add(social);

			Outputter.debugln("BurglarFactory created new burglar: "+burglar.getName()+" with following: \n" +
					"\thome: "+home.toString()+" "+GlobalVars.BUILDING_ENVIRONMENT.getCoords(home).toString()+"\n" +
					"\twork: "+burglar.getWork().toString()+" "+GlobalVars.BUILDING_ENVIRONMENT.getCoords(burglar.getWork()).toString()+"\n" +
					"\tsocial: "+burglar.getSocial().toString()+" "+GlobalVars.BUILDING_ENVIRONMENT.getCoords(burglar.getSocial()).toString()+"\n" +
					"\tdrug: "+burglar.getDrugDealer().toString()+" "+GlobalVars.BUILDING_ENVIRONMENT.getCoords(burglar.getDrugDealer()).toString()+"\n",
					Outputter.DEBUG_TYPES.GENERAL);
			return burglar;

		}
		else if (type.equals("TESTING_BURGLAR-PERSONALISED")) {
			// Just burgles, no real needs
			List<StateVariable> testerList = new ArrayList<StateVariable>();
			Burglar tester = new SimplePersonalisedBurglar(testerList);
			GlobalVars.BURGLAR_ENVIRONMENT.add(tester);
			tester.setName("PersonalisedBurglar "+burglarNumber++);
			House home = (House)GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(House.class);
			tester.setHome(home);
			//			House home = null;
			//			for (House h:GlobalVars.BUILDING_ENVIRONMENT.getAllObjects(House.class)) {
			//				if (h.getId()==240) {
			//					home = h;
			//					tester.setHome(home);
			//					System.out.println("BurglarFactory: Test buglar set home 240");
			//				}
			//			}			
			GlobalVars.BURGLAR_ENVIRONMENT.move(tester, GlobalVars.BUILDING_ENVIRONMENT.getCoords(home));
			tester.initMemory();
			StateVariable justBurglary = new DebugBurglaryV(tester, 1); 
			testerList.add(justBurglary);
			return tester;
		}
		else if (type.equals("TESTING_BURGLAR-PLUGIN")) {
			// Just burgles, no real needs
			List<StateVariable> testerList = new ArrayList<StateVariable>();
			Burglar tester = new SimplePlugInBurglar(testerList);
			GlobalVars.BURGLAR_ENVIRONMENT.add(tester);
			tester.setName("PluginBurglar "+burglarNumber++);
			House home = (House)GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(House.class);
			tester.setHome(home);
			//			House home = null;
			//			for (House h:GlobalVars.BUILDING_ENVIRONMENT.getAllObjects(House.class)) {
			//				if (h.getId()==240) {
			//					home = h;
			//					tester.setHome(home);
			//					System.out.println("BurglarFactory: Test buglar set home 240");
			//				}
			//			}			
			GlobalVars.BURGLAR_ENVIRONMENT.move(tester, GlobalVars.BUILDING_ENVIRONMENT.getCoords(home));
			tester.initMemory();

			StateVariable justBurglary = new DebugBurglaryV(tester, 1); 
			testerList.add(justBurglary);
			return tester;
		}
		else if (type.equals("TESTING_BURGLAR-DEFAULT")) {
			// Just burgles, no real needs
			List<StateVariable> testerList = new ArrayList<StateVariable>();
			Burglar tester = new SimpleDefaultBurglar(testerList);
			GlobalVars.BURGLAR_ENVIRONMENT.add(tester);
			tester.setName("DefaultBurglar "+burglarNumber++);

			House home = (House)GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(House.class);
			tester.setHome(home);
			//			House home = null;
			//			for (House h:GlobalVars.BUILDING_ENVIRONMENT.getAllObjects(House.class)) {
			//				if (h.getId()==240) {
			//					home = h;
			//					tester.setHome(home);
			//					System.out.println("BurglarFactory: Test buglar set home 240");
			//				}
			//			}			
			GlobalVars.BURGLAR_ENVIRONMENT.move(tester, GlobalVars.BUILDING_ENVIRONMENT.getCoords(home));
			tester.initMemory();

			StateVariable justBurglary = new DebugBurglaryV(tester, 1); 
			testerList.add(justBurglary);
			return tester;
		}		

		Outputter.errorln("BurglarFactory: unrecognised burglar type: "+type);
		return null;
	}

	/**
	 * Used for testing the environment, create a burglar who lives at the given building, used to test
	 * route generation and travel
	 * @param type
	 * @param b
	 */
	public static Burglar createBurglar(Building b) {
		List<StateVariable> walkerList = new ArrayList<StateVariable>();
		Burglar walker = new SimpleDefaultBurglar(walkerList);
		GlobalVars.BURGLAR_ENVIRONMENT.add(walker);
		walker.setName("EnvironmentTesting person "+burglarNumber++);

		walker.setHome(b);
		GlobalVars.BURGLAR_ENVIRONMENT.move(walker, GlobalVars.BUILDING_ENVIRONMENT.getCoords(b));
		walker.initMemory();

		//		StateVariable randomWalk = new RandomWalkV(walker, 1); 
		//		walkerList.add(randomWalk);
		StateVariable doNothing = new DoNothingV(walker);
		walkerList.add(doNothing);
		return walker;
	}

	/**
	 * Used to read information about the number of burglars to create and their type(s) from the communities file.
	 * Requirements: 
	 * <ol>
	 * <li>The Burglar to be created must have a static method: <code>createBurglar(House homePlace, 
	 * Workplace workPlace, Social socialPlace, DrugDealer drugDealerPlace)</code> which is
	 * used to create the burglar. </li>
	 * <li>The <code>createBurglar</code> method must also add the burglar to the environment and move it to it's
	 * starting location (this cannot be done here because the burglar must be given a place in the environment
	 * for it's memory to be initialised).</li>
	 * <li>The number of burglars in each community are stored in columns entitled BglrCX were X is a number between
	 * 1 and 5 (i.e. up to 5 columns to store different burglar numbers). This is a requirement because in the Shapefile
	 * loader is used to query the communities and I can't work out how to dynamically create a class to be populated
	 * with the values (i.e. dynamically create get/set methods for the collumn headings).
	 * <li>The GIS environment must be used because this method replies upon reading information from the communities
	 * shapefile.</li>
	 * </ol>
	 * <p>
	 * The burglars are actually created by the private <code>createBurglarsFromCommunities()</code> method while the
	 * model is being initialised. 
	 * 
	 * @param burglarsToCreate This is a list of String[], each with two elements:
	 * <ol><li>The name of the type of burglar to create, this must match a class in the <code>burgdsim.burglars</code>
	 * package.</li>
	 * <li>The column in the communities file which holds the number of these burglars to create for each community.
	 * The burglars will be created in random houses in the community. If there are more burglars than there are houses
	 * an error will be printed and one burglar will be created for each house.</li>
	 * </ol> 
	 */
	public void setBurglarsInCommunities(List<String[]> burglarsToCreate) {
		this.burglarsFromCommunities = burglarsToCreate;		
	}

	// See setBurglarsInCommunities for a description of how this function creates burglars
	@SuppressWarnings("unchecked")
	private void createBurglarsFromCommunities() throws Exception {
		for (String[] s:this.burglarsFromCommunities ) {

			String burglaryClass = s[0];
			String communitiesColumn = s[1];
			Class<? extends Burglar> classDefinition;
			try {
				classDefinition = (Class<? extends Burglar>) Class.forName("burgdsim.burglars."+burglaryClass);
				Outputter.debugln("BurglarFactory: creating burglars of type: "+classDefinition.getName(), 
						Outputter.DEBUG_TYPES.INIT);

				// Read the number of burglars in each community
				List<int[]> numBurglars = new ArrayList<int[]>();
				numBurglars = this.getNumBurglarsInCommunity(communitiesColumn);
				//				System.out.println("*****");
				//				for (int[] i:numBurglars) {
				//					System.out.println(i[0]+"->"+i[1]);
				//				}
				for (int[] com:numBurglars) { // iterate over every community
					// Find the community with the correct ID
					Community community = null;
					for (Community c:GlobalVars.COMMUNITY_ENVIRONMENT.getAllObjects(Community.class)) {
						if (c.getId()==com[0]) {
							community = c;
							break;
						}
					}
					if (community == null) {
						Outputter.errorln("BurglarFactory.createBurglarsFromCommunities() error: unrecognised " +
								"community ID read in from the shapefile, it doesn't match any of the communities in" +
								"the environment '"+com[0]+"'.\nThis might be because sometimes communities are removed" +
						"if they don't have any houses in them). Not creating any burglars in this community.");
						continue; // Move onto the next community
					}
					// Find all the houses in the community
					List<House> houses = new ArrayList<House>();
					for (Building b:community.getBuildings()) {
						if (b instanceof House) {
							houses.add((House)b);
						}
					}
					// Check there aren't more burglars than houses
					int numB = com[1];
					if (houses.size() < numB) {
						Outputter.errorln("BurglarFactory.createBurglarsFromCommunities Warning: there are "+
								houses.size()+" houses in community "+community.getId()+" but I want to create " +
								com[1]+" burglars there. Only creating one burglar per house.");
						numB = houses.size();
					}
					// Now create the burglars, putting them into one of the given houses.
					for (int i=0; i<numB; i++) { // iterate once for each burglar to be created
						// Invoke the createBurglar() method, guaranteed to exist because Class implements Reflective interface
						House house = houses.get(i);
						Method createMethod = classDefinition.getMethod(
								"createBurglar", new Class<?>[] {House.class, Workplace.class, Social.class, DrugDealer.class});
						createMethod.invoke(null, new Object[] { house, null, null, null });
						BurglarFactory.burglarNumber++;
						//						Burglar newBurglar = (Burglar) createMethod.invoke(null, new Object[] { house, null, null, null });
						//						System.out.println("BurglarFactory created a new burglar: "+newBurglar.toString());
					} // for i
				} // for com
				Outputter.debugln("BurglarFactory created "+BurglarFactory.burglarNumber+" burglars",
						Outputter.DEBUG_TYPES.INIT);

			} catch (ClassNotFoundException e) {
				BurglarFactory.error("BurglarFactory.createBurglarsFromCommunities() error: ClassNotFoundException" +
						"trying to create class for burglars: "+burglaryClass);
			} catch (IllegalAccessException e) {
				BurglarFactory.error("BurglarFactory.createBurglarsFromCommunities() error: IllegalAccessException" +
						"trying to create class for burglars: "+burglaryClass);
			} catch (NoSuchMethodException e) {
				BurglarFactory.error("BurglarFactory.createBurglarsFromCommunities() error: trying to create burglars "+
						"of type '"+burglaryClass+"' but can't because that type of burglar doesn't have a static '" +
				"createBurglar(House, Workplace, Social, DrugDealer)' method.");
			} catch (InvocationTargetException e) {
				BurglarFactory.error("BurglarFactory.createBurglarsFromCommunities() error: caught an " +
						"InvocationTargetException while trying to create burglars '"+burglaryClass+"'. This" +
				"means that the createBurglar() method of the burglar class threw an error.");
			}
		}
	} // createBUrglarsFromCommunities

	/* Uses shapefileloader to find out how many offenders are in each community */
	private Context<TempCommunity> tempContext;
	private List<int[]> getNumBurglarsInCommunity(String communitiesColumn) throws Exception {
		List<int[]> burglarsInCommunities = new ArrayList<int[]>();
		// Use a temporary projection and context to read the communities shapefile
		try {
			if (this.tempContext==null) { // First time this has been called, read the communities
				this.tempContext = new DefaultContext<TempCommunity>("TempContext");
				GeographyParameters<TempCommunity> geoParams = new GeographyParameters<TempCommunity>(new SimpleAdder<TempCommunity>());
				Geography<TempCommunity> tempProj = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
						"TempGeography", this.tempContext, geoParams);
				ShapefileLoader<TempCommunity> loader = new ShapefileLoader<TempCommunity>(TempCommunity.class, 
						new File(GlobalVars.GIS_PARAMS.COMMUNITIES_FILENAME).toURL(), tempProj, tempContext);
				System.out.println(new File(GlobalVars.GIS_PARAMS.COMMUNITIES_FILENAME).toURL().toString());
				while (loader.hasNext()) loader.load();
			}
			for (TempCommunity c:this.tempContext.getObjects(TempCommunity.class)) {
				if (communitiesColumn.equals("BglrC1") && (c.getBglrC1() > 0)) {
					burglarsInCommunities.add(new int[]{c.getId(), c.getBglrC1()});
				}
				else if (communitiesColumn.equals("BglrC2") && (c.getBglrC2() > 0)) {
					burglarsInCommunities.add(new int[]{c.getId(), c.getBglrC2()});
				}
				else if (communitiesColumn.equals("BglrC3") && (c.getBglrC3() > 0)) {
					burglarsInCommunities.add(new int[]{c.getId(), c.getBglrC3()});
				}
				else if (communitiesColumn.equals("BglrC4") && (c.getBglrC4() > 0)) {
					burglarsInCommunities.add(new int[]{c.getId(), c.getBglrC4()});
				}
				else if (communitiesColumn.equals("BglrC5") && (c.getBglrC5() > 0)) {
					burglarsInCommunities.add(new int[]{c.getId(), c.getBglrC5()});
				}	
			}

		} catch (MalformedURLException e) {
			BurglarFactory.error("BurglarFactory.getNumBurglarsInCommunity() caught a MalformedURLException");
		}

		return burglarsInCommunities;
	}

}
