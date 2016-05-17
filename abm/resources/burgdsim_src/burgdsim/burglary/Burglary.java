package burgdsim.burglary;

import java.util.ArrayList;
import java.util.List;

import burgdsim.burglars.Burglar;
import burgdsim.data_access.DataAccessFactory;
import burgdsim.environment.Cacheable;
import burgdsim.environment.Community;
import burgdsim.environment.Coord;
import burgdsim.environment.EnvironmentFactory;
import burgdsim.environment.buildings.House;
import burgdsim.main.ContextCreator;
import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;
import burgdsim.pecs.Action;
import burgdsim.pecs.Motive;
import burgdsim.pecs.TravelA;

/**
 * The default action to commit a burglary. The burglary 'template' consists of three individual
 * modules: choosing a target, searching and deciding if a house is suitable. Agents can either use
 * pre-defined ones or implement their own.
 * <p>
 * See thesis/model_dev/classes.tex for a description
 * of the different ways this function is used by Motives and Burglar agents. 
 * 
 * @author Nick Malleson
 */
public class Burglary extends Action implements Cacheable {

	private Community targetCommunity;		// The target from which to start the search
	private House targetBuilding;
	private TravelA travelA;				// TravelAction required to move to target 
	private ITargetChooser targetChooser;	// Class to decide who the target should be
	private ISearchAlg searchAlg;			// The algorithm the burglar uses for searching
	private IVictimChooser victimChooser;	// Decide whether or not a suitable victim is in the vicinity

	// Following indicate the state of this Burglary
	private boolean init = true; 			// Just created or agent couldn't find a victim, start again
	private boolean travelToTarget = false;	// Agent is travelling to their chosen target
	private boolean searching = false;		// Agent is searching for a victim.

	private static Object burglaryOccurringLock = new Object(); // Lock used to synchronize burglaryOccuring() 

	// The distance over which a burglary will affect other houses, depends on a weight and is calculate the
	// first time it is called.
	private static double burglaryEffectDist = -1; 

	//	// Lock to determine whether or not a burglary is occurring, stops two burglar threads writing simultaneously
	//	private static boolean burglaryOccuring =false ;  

	//	private int caughtErrors = 0; // Used to try to deal with errors here, if too many happen then bail 

	/**
	 * Default constructor, uses default targetChooser, searchalg and victimChooser objects.
	 * @param motive The motive which requires a burglar
	 */
	public Burglary(Motive motive) {
		super(motive);
		this.description = "Burgling";
		this.searchAlg = new SearchAlg(this.getBurglar());
		this.targetChooser = new TargetChooser(this.getBurglar());
		this.victimChooser = new VictimChooser(this.getBurglar());
		GlobalVars.CACHES.add(this); // To clear the burglaryEffectDist
	}

	/**
	 * Fully customisable burglary action, the constructor takes all the individual parts of the 
	 * burglary template, 
	 * @param motive
	 * @param targetChooser
	 * @param searchAlg
	 * @param victimChooser
	 */
	public Burglary(Motive motive, TargetChooser targetChooser, SearchAlg searchAlg, VictimChooser victimChooser) {
		super(motive);
		this.searchAlg = searchAlg;
		this.targetChooser = targetChooser;
		this.victimChooser = victimChooser;
		GlobalVars.CACHES.add(this); // To clear the burglaryEffectDist
	}


	/**
	 * Partially customisable, the constructor takes in part of the burglary template. 
	 * @param motive
	 * @param targetChooser
	 */
	public Burglary(Motive motive, TargetChooser targetChooser) {
		super(motive);
		this.targetChooser = targetChooser;
		GlobalVars.CACHES.add(this); // To clear the burglaryEffectDist
	}
	/**
	 * Partially customisable, the constructor takes in part of the burglary template.
	 * @param motive
	 * @param searchAlg
	 */
	public Burglary(Motive motive, SearchAlg searchAlg) {
		super(motive);
		this.searchAlg = searchAlg;
		GlobalVars.CACHES.add(this); // To clear the burglaryEffectDist
	}
	/**
	 * Partially customisable, the constructor takes in part of the burglary template.
	 * @param motive
	 * @param victimChooser
	 */
	public Burglary(Motive motive, VictimChooser victimChooser) {
		super(motive);
		this.victimChooser = victimChooser;
		GlobalVars.CACHES.add(this); // To clear the burglaryEffectDist
	}


	/**
	 * Performs the actions required to commit a default burglary (burglars will override this
	 * with their own burglary class).
	 * <p>
	 * This 'default' burglary action simply causes the agent to choose a target from their cognitive
	 * map, travel there, then increase level of wealth when they arrive (won't alter house at all).
	 * @return true if the action is temporary (i.e. the agent can still do more this turn).
	 * @throws Exception 
	 */
	@Override	
	public boolean performAction() throws Exception {
		// Initialise the new burglar action
		if (this.init) {
			//						Outputter.describeln("Burglary.performAction(): new burglary initialised. Pausing sim.");
			//						
			//						RunEnvironment.getInstance().pauseRun();
			// Choose a target community to visit and pick a random building
			this.targetBuilding = null; int counter = 0;
			while (this.targetBuilding==null) { // Some communities might not have any buildings in them, need to loop.
				this.targetCommunity = targetChooser.chooseTarget();
				this.targetBuilding = targetCommunity.getRandomBuilding(House.class);
				if (counter++>1000) Outputter.errorln("Burglary.performAction() in infinite loop looking for target building");
			}

			// Create action which will guide agent to their chosen house.
			this.travelA = new TravelA(this.motive, this.targetBuilding.getCoords(), this.targetBuilding,
			"Travelling to a burglary target");
			this.init = false;
			this.travelToTarget = true;
			Outputter.debugln("Burglary.performAction(): '"+this.getBurglar().toString()+"' initialised new " +
					"Burglary action. Target is: "+this.targetCommunity.toString()+", "+this.targetBuilding.toString(),
					Outputter.DEBUG_TYPES.BURGLARY);
		}
		// Travel to the chosen target
		else if (this.travelToTarget) { 
			if (travelA.isComplete()) { // have reached target
				this.travelToTarget = false;
				this.searching = true;
				Outputter.debugln("Burglary.performAction(): '"+this.getBurglar().toString()+"' has reached " +
						"target, starting search.", Outputter.DEBUG_TYPES.BURGLARY);
			}
			else { // not reached target yet, keep travelling
				try {
					this.travelA.performAction();
					Outputter.debugln("Burglary.performAction(): '"+this.getBurglar().toString()+"' travelling to " +
							"target", Outputter.DEBUG_TYPES.BURGLARY);
				} catch (Exception e) {
					// GISRoute seems to be throwning errors very occasionally, try to catch them there rather than here
					Outputter.errorln("Burglary.performAction: caught an exception travelling to the target.");
					throw e;
				} // catch
			} // else notreachedtarget
		} // elseif traveltotarget
		// Search for a victim
		else if (this.searching) { 
			if (this.searchAlg.finishedSearching()) { // Search was unsuccessful, start a new burglary
				this.searching=false;
				this.init=true;
				this.searchAlg.init(); // Re-initialise the search algorithm ready to start again
				Outputter.debugln("Burglar.performAction() '"+this.getBurglar().toString()+"' couldn't find " +
						"target, will re-initialise this Burglary object.", Outputter.DEBUG_TYPES.BURGLARY);
				//				RunEnvironment.getInstance().pauseRun();
			}
			else {
				Outputter.debugln("Burglar.performAction() '"+this.getBurglar().toString()+"' searching " +
						"for victim ("+GlobalVars.time+").", Outputter.DEBUG_TYPES.BURGLARY);
				searchAlg.step();
			}
		}
		// Look for burglary victim
		List<Double> calculationValues = new ArrayList<Double>(); // Store individual values used in burglary calculation
		House h = this.victimChooser.chooseVictim(this.getBurglar().getBuildingsPassed(), calculationValues);
		if (h!=null) {
			burglaryOccurred(h, calculationValues);
		}
		return false; // Not a temporary action, agent cannot do anything else this turn.

	}

	//	private synchronized void waitForBurglaryToFinish() {
	//		try {
	//			this.wait(10); // Wait a bit
	//		} catch (InterruptedException e) {
	//			Outputter.errorln("Burglary.burglarOccurred() caught Interrupted Exceltion: "+e.getMessage());
	//			e.printStackTrace();
	//		}// Wait until the thread controller has finished	
	//	}

	private void burglaryOccurred(House h, List<Double> calculationValues) throws Exception {
		
		
		
		// Make sure no other burglary is occurring
		//		Outputter.describeln(GlobalVars.getIteration()+" "+this.getBurglar().toString()+" burglaryOccurred...starting");
		synchronized (Burglary.burglaryOccurringLock) {
			GlobalVars.NUM_BURGLARIES++;
			h.burglaryOccurred(); // Tell the building it has been burgled.
			if (ContextCreator.numBurglariesLabel != null) { // Display new number of burglaries (if using GUI)
				ContextCreator.numBurglariesLabel.setText(String.valueOf(GlobalVars.NUM_BURGLARIES));
			}
			this.getBurglar().changeWealth(GlobalVars.BURGLE_GAIN);
			String debugString = "Burglary: '"+this.getBurglar().toString()+"' has just burgled '"+h.toString()+"'.\n\t";
			// Store the information about this burglary
			if (ContextCreator.isTrue(GlobalVars.HISTORY_PARAMS.BURGLARY_OUT)) 
				Burglary.storeBurglaryData(this.getBurglar(), h, calculationValues);
			if (!this.getBurglar().storeFullHistory() ) // If not writing burglar info every iteration 
				Burglar.storeInformation(this.getBurglar());
			// Remember that the agent has just comitted a burglary.
			//		Coord currentCoord = GlobalVars.BURGLAR_ENVIRONMENT.getCoords(this.getBurglar());
			Coord currentCoord = h.getCoords();
			Community burgledCommunity = GlobalVars.COMMUNITY_ENVIRONMENT.getObjectAt(
					Community.class, currentCoord);
			this.getBurglar().getMemory().committedBurglary(h, burgledCommunity);

			// Increase the security of the burgled property
			if (ContextCreator.isTrue(GlobalVars.BURGLARY_BUILDING_PARAMS.INCREASE_SECURITY_AFTER_BURGLARY)) {
				double newSecurity = h.getSecurity()+(h.getSecurity()*GlobalVars.BURGLARY_BUILDING_PARAMS.SECURITY_INCREASE);
				h.setSecurity(newSecurity);
				EnvironmentFactory.addBurgledHouse(h); // Add to list so securty will degrade over time
				//		if (GlobalVars.HISTORY_PARAMS.SECURITY_OUT) {
				//			Burglary.storeSecurityData(h, newSecurity);
				//		}

				/* Increase the security of all buildings in the vicinity */
				if (ContextCreator.isTrue(GlobalVars.BURGLARY_BUILDING_PARAMS.INCREASE_SECURITY_SURROUNDING_BURGLARY)) {
					// Calculate how far the burglary effect will travel, static value so it doesn't have to be recalculated
					// every time there is a burglary.
					double burglaryEffectWeight = GlobalVars.BURGLARY_BUILDING_PARAMS.SECURITY_DISTANCE_W;
					if (burglaryEffectDist == -1) { 			
						double simulatedEffect = burglaryEffectWeight; 
						burglaryEffectDist = 1; // Simulate effect of increased distance on the burglary effect  
						while (simulatedEffect > GlobalVars.BURGLARY_BUILDING_PARAMS.NEGLIDGIBLE_SECURITY_INCREASE) { // While the effect is not negligible
							simulatedEffect = burglaryEffectWeight / burglaryEffectDist;
							burglaryEffectDist+=0.1;
						}
						//			burglaryEffectDist = simulatedEffect;
						debugString+="Simulated burglary effect: burglary will effect houses up to a distance of "+
						(burglaryEffectDist)+" units ("+(burglaryEffectDist*GlobalVars.getDistanceUnit())+"m). ";
					}
					//		debugString+="Burglary is increasing security of houses:\n";
					// Now know how far the effect will reach, calculate the security of all buildings in the vicinity
					for (House house:GlobalVars.BUILDING_ENVIRONMENT.getObjectsWithin(
							currentCoord, House.class, (burglaryEffectDist*GlobalVars.getDistanceUnit()), false)) {
						if (!house.equals(h)) { // Don't include the burgled house, its security has already been increased 
							double oldSec = house.getSecurity();
							// Round to nearest distance unit
							double distance = GlobalVars.BUILDING_ENVIRONMENT.getDistance(h.getCoords(), house.getCoords())/GlobalVars.getDistanceUnit();
							if (distance < 1) distance=1; // Don't want distance < 1 or security increased more than burgled house!
							double increase = burglaryEffectWeight / distance;
							// TODO BUG HERE: The distance used in GISEnvironment.getObjectsWithin() is greater than the one passed in here,
							// so have to check the increase isn't actually negligible (i.e. houses which are too far to feel incras
							if (increase>GlobalVars.BURGLARY_BUILDING_PARAMS.NEGLIDGIBLE_SECURITY_INCREASE) {
								house.setSecurity(oldSec + increase);
								EnvironmentFactory.addBurgledHouse(house); // Add to list so securty will degrade over time
								//					if (GlobalVars.HISTORY_PARAMS.SECURITY_OUT) {
								//						Burglary.storeSecurityData(house, oldSec + increase);
								//					}
								//					debugString+="\t\t'"+house.toString()+"',"+house.getId()+", dist (units):,"+distance+", dist(m):, "+GlobalVars.BUILDING_ENVIRONMENT.getDistance(h.getCoords(), house.getCoords())+
								//						", old sec:,"+oldSec+", new sec:,"+(oldSec+increase)+"\n";
							}

						}
					} // for houses
				} // if increasing security of surrounding properties
			} // if increasing security
			else {
				debugString += "Not increasing security of houses. ";
			}
			this.complete=true; // This will indicate to parent Motive that the action has been completed
			// Re-initialise this Burglary, it might be called again by the parent Motive.
			this.init = true;

			Outputter.debugln(debugString, Outputter.DEBUG_TYPES.BURGLARY);
		} // synchronized
		//		Outputter.describeln(GlobalVars.getIteration()+" "+this.getBurglar().toString()+" burglaryOccurred...finished");
		//		Outputter.describe("Agent "+this.getBurglar().toString()+" just burgled house "+h.toString());
		//		Outputter.describe("Pausing sim because of burglary");
		//		RunEnvironment.getInstance().pauseRun();
	}

	/** Write the new security values of the houses caused by this burglary. NOTE: Not doing this because
	 * security written once a day anyway. 
	 * @throws Exception If there is a problem writing data.*/
	@SuppressWarnings("unused")
	private static void storeSecurityData(House h, double newSecurity) throws Exception {
		String[] names = new String[4];
		Object[] values = new Object[4];
		names[0] = "BuildingID"; values[0] = h.getId();
		names[1] = "ModelID"; values[1] = GlobalVars.MODEL_ID;
		names[2] = "Value"; values[2] = newSecurity;
		names[3] = "Time"; values[3] = GlobalVars.getIteration();
		DataAccessFactory.getDataAccessObject(GlobalVars.HISTORY_PARAMS.RESULTS_DATABASE).writeValues(
				values, GlobalVars.HISTORY_PARAMS.SECURITY, names);
	}

	/** Write out the information about this burglary 
	 * @throws Exception If there is a problem writing data.*/
	private static void storeBurglaryData(Burglar b, House h, List<Double> calculationValues) throws Exception {
		ArrayList<String> namesList = new ArrayList<String>();
		ArrayList<Object> valuesList = new ArrayList<Object>();
		namesList.add("BurglarID"); valuesList.add(b.getID());
		namesList.add("ModelID"); valuesList.add(GlobalVars.MODEL_ID);
		namesList.add("BuildingID"); valuesList.add(h.getId());
		namesList.add("Time"); valuesList.add(GlobalVars.getIteration());
		//		namesList.add("XCoord"); valuesList.add(GlobalVars.BURGLAR_ENVIRONMENT.getCoords(b).getX());
		//		namesList.add("YCoord"); valuesList.add(GlobalVars.BURGLAR_ENVIRONMENT.getCoords(b).getY());
		namesList.add("XCoord"); valuesList.add(GlobalVars.BUILDING_ENVIRONMENT.getCoords(h).getX());
		namesList.add("YCoord"); valuesList.add(GlobalVars.BUILDING_ENVIRONMENT.getCoords(h).getY());
		if (calculationValues != null) { /// Write individual values that went into burglary calculation
			namesList.add("ce"); valuesList.add(calculationValues.get(0));
			namesList.add("occ"); valuesList.add(calculationValues.get(1));
			namesList.add("acc"); valuesList.add(calculationValues.get(2));
			namesList.add("vis"); valuesList.add(calculationValues.get(3));
			namesList.add("sec"); valuesList.add(calculationValues.get(4));
			namesList.add("tv"); valuesList.add(calculationValues.get(5));
			// Also store the attractiveness and sociotype of the community, not part of the burglary decision
			namesList.add("attract"); valuesList.add(h.getCommunity().getSociotype().getAttractiveness());
			namesList.add("sociotype"); valuesList.add(h.getCommunity().getSociotype().getDescription());
			namesList.add("ce_w"); valuesList.add(calculationValues.get(6));
			namesList.add("tv_w"); valuesList.add(calculationValues.get(7));
			namesList.add("occ_w"); valuesList.add(calculationValues.get(8));
			namesList.add("acc_w"); valuesList.add(calculationValues.get(9));
			namesList.add("vis_w"); valuesList.add(calculationValues.get(10));
			namesList.add("sec_w"); valuesList.add(calculationValues.get(11));
			namesList.add("suitability"); valuesList.add(calculationValues.get(12));
			namesList.add("motive_intensity"); valuesList.add(calculationValues.get(13));
			namesList.add("difference"); valuesList.add(calculationValues.get(14));
			namesList.add("probability"); valuesList.add(calculationValues.get(15));
		}


		String[] names = new String[namesList.size()];
		Object[] values = new Object[valuesList.size()];
		for (int i=0; i<names.length; i++) {
			names[i] = namesList.get(i);
			values[i] = valuesList.get(i);
		}
		DataAccessFactory.getDataAccessObject(GlobalVars.HISTORY_PARAMS.RESULTS_DATABASE).writeValues(
				values, GlobalVars.HISTORY_PARAMS.BURGLARY, names);
	}

	/* GETTERS AND SETTERS - useful for setting odd combinations of plug-ins*/

	/**
	 * Customise part of this burglary with a non-default plug-in. This can also be set using the constructor.
	 * @param targetChooser the targetChooser to set
	 */
	public void setTargetChooser(ITargetChooser targetChooser) {
		this.targetChooser = targetChooser;
	}

	/**
	 * Customise part of this burglary with a non-default plug-in. This can also be set using the constructor.
	 * @param searchAlg the searchAlg to set
	 */
	public void setSearchAlg(ISearchAlg searchAlg) {
		this.searchAlg = searchAlg;
	}

	/**
	 * Customise part of this burglary with a non-default plug-in. This can also be set using the constructor.
	 * @param victimChooser the victimChooser to set
	 */
	public void setVictimChooser(IVictimChooser victimChooser) {
		this.victimChooser = victimChooser;
	}

	public void clearCaches() {
		Burglary.burglaryEffectDist = -1;
	}
}