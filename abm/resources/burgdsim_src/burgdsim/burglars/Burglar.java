package burgdsim.burglars;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import burgdsim.data_access.DataAccess;
import burgdsim.data_access.DataAccessFactory;
import burgdsim.environment.Cacheable;
import burgdsim.environment.Community;
import burgdsim.environment.buildings.Building;
import burgdsim.main.ContextCreator;
import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;
import burgdsim.pecs.Action;
import burgdsim.pecs.Motive;
import burgdsim.pecs.StateVariable;

public abstract class Burglar implements Cacheable {

	private List<StateVariable> stateVariables; // The list of state variables which control the person
	private Action currentAction;	// The current action the agent is performing
	private Motive actionGuidingMotive = null; // The agents action-guiding motive
	protected String name;
	private int id;
	private static int uniqueID = 0;
	public static int burglarNumber = 0;

	private Building home; // This person's home
	private Building work; // This person's work
	private Building drugDealer; // The person's drug dealer
	private Building social; // Where the person goes to socialise
	//protected DiffTypeHashMap<String, ? extends Building> cognitiveMap; // Use a Hashtable for efficiency
	protected double wealth = 1;

	/* Memory object used to store agent's internal information about the environment */
	protected BurglarMemory memory;
	protected List<Building> buildingsPassed; // List of houses passed in this iteration

	/* Certain actions wont allow the agent to change their behaviour until they have been completed (e.g.
	 * travelling on a bus, the agent can't just get off!) so these locks can be used. */
	private boolean locked = false;		// Indicates motive can't be changed yet
	private Action locker = null;			// The Action which has locked this agent (useful for debugging)
	private boolean awaitingUnlock = false; // Indicates to the locking action that it should stop ASAP so the agent can change action
	
	/* Can try to predict if the agent's current action won't change their behaviour for a while (e.g. sleeping in
	 * the middle of the night). If this is the case then don't need to run through whole step() method. NOTE: this
	 * shouldn't be confused with the SleepA Action. */
	private int sleeping = 0; // The number of iterations to remain asleep, if 0 then not sleeping
	// Store the intensities of each motive, required to calculate 2nd highest intensity (if big difference agent can sleep).
	private List<Double> intensities;
	// Whether or not to store information about this burglar at every iteration (default not).
	private boolean storeFullHistory = false;
	
	
	protected List<String> transportAvailable; // The possible methods an agent can use to get around

	/**
	 * 
	 * @param stateVariables a list of the state variables which will determine the agent's behaviour
	 */
	public  Burglar (List<StateVariable> stateVariables) {
		this.stateVariables = stateVariables;
		this.transportAvailable = new ArrayList<String>();
		// TODO Set transport method available to the agent correctly.
//		this.transportAvailable.addAll(GlobalVars.TRANSPORT_PARAMS.ALL_PARAMS);
		this.transportAvailable.add(GlobalVars.TRANSPORT_PARAMS.WALK); // Everyone can walk.
		this.intensities = new ArrayList<Double>(this.stateVariables.size());
		this.id = Burglar.uniqueID++;
	}

	/**
	 * Controls the agent's behaviour. Also changes the values of the agent's state variables
	 */
	public void step() {
		
		try{
//			Outputter.describeln(GlobalVars.getIteration()+" Burglar "+this.toString()+" stepping...");
			if (this.sleeping>0) { // Agent is asleep, don't do anything
				this.sleeping--;
				this.currentAction.performAction();
				Outputter.debugln("Burglar "+this.name+" is sleeping for "+sleeping+" more iterations. " +
						"Performing action "+this.currentAction.toString(), Outputter.DEBUG_TYPES.GENERAL);
			}
			else {
				//			long startTime = System.nanoTime(); long time = startTime; long ticks = (long) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
				//			if (ticks%1000 == 0) System.out.print(ticks+" step times ("+this.toString()+"): ");
				/* Go through the state variables, and find the action guiding motive. */
				Outputter.debugln(GlobalVars.getIteration()+"("+GlobalVars.time+")"+"Burglar "+name+" step: " +
						"\n\twealth: "+this.wealth,
						Outputter.DEBUG_TYPES.GENERAL);
				// Motives are persistent objects, so if a new motive becomes the actionGuidingMotive it should re-build
				// its actionList otherwise it will continue using the actionList created when sim. initialised.
				Motive oldMotive = this.actionGuidingMotive;
				Motive newMotive = oldMotive;
				double highestIntensity = Double.NEGATIVE_INFINITY;
				double intensity;	// Used to store intensity of each Motive, don't want to have to calculate > once
				Motive m;
				this.intensities.clear();
				for (StateVariable s:this.stateVariables) {
					m = s.getMotive();
					intensity = m.getIntensity();
					this.intensities.add(intensity);
					Outputter.debugln("\tStateVariable: "+s.toString()+" value: "+s.getValue()+
							". Motive: "+s.getMotive()+ ". Intensity: "+
							intensity+".", Outputter.DEBUG_TYPES.GENERAL);
					if (intensity > highestIntensity) {
						newMotive = m;
						highestIntensity = intensity;
					}
				}
				//			if (ticks%1000 == 0) {System.out.print((System.nanoTime()-time)+"\t\t"); time = System.nanoTime(); }
				

				// See if the motive has changed. Also check that the intensity of the new motive is significantly
				// higher than the old motive (to prevent agents swapping between two similer motives)
				if (oldMotive==null || 
						( (!oldMotive.getClass().isInstance(newMotive)) && ( highestIntensity > (oldMotive.getIntensity()+GlobalVars.INTENSITY_DIFFERENCE) ) )
				) {
					// The motive should be changed, but need to check if the agent is locked
					if (this.locked) {
						this.awaitingUnlock = true;
						Outputter.debugln("\tMotive has changed to: "+newMotive.toString()+" but agent is locked by " +
								this.locker.toString()+" so motive is still "+this.actionGuidingMotive.toString()
								+". Pausing sim.",
								Outputter.DEBUG_TYPES.GENERAL);
//						System.out.println("Burglar.java Pausing sim because locking condition");
//						RunEnvironment.getInstance().pauseRun();
					}
					else {
						this.awaitingUnlock = false;
						this.actionGuidingMotive = newMotive;
						/* If the actionGuidingMotive has changed then the new one needs to be reinitialised (the actionList
						 * needs to be rebuild and new action listeners will need to be created etc). */
						this.actionGuidingMotive.initialise();
						Outputter.debugln("\taction-guiding motive changed to: "+actionGuidingMotive.toString()+".",
								Outputter.DEBUG_TYPES.GENERAL);
					}
				}
				else { // Motive hasn't changed so don't need to worry about changing action if agent is locked
					this.awaitingUnlock = false;
					
					Outputter.debugln("\taction-guiding motive unchanged from "+actionGuidingMotive.toString()+").",
							Outputter.DEBUG_TYPES.GENERAL);
				}
				//			if (ticks%1000 == 0) { System.out.print((System.nanoTime()-time)+"\t\t");time = System.nanoTime(); }

				/* Look through actions until the agent finds one which will take more than one iteration to
				 * complete. (e.g. "travel home" can be completed instantly if the agent is already at home, so
				 * move on to the next Action) */
				boolean temporaryAction = true;		
				while (temporaryAction) {
					// Ask this motive for the action to perform.
					this.currentAction = actionGuidingMotive.getCurrentAction();
					// Perform the action
					//				Outputter.debug("\tPerforming action: "+this.currentAction.toString()+".", Outputter.DEBUG_TYPES.GENERAL);
					temporaryAction = this.currentAction.performAction();
				} // while !completed
				//			if (ticks%1000 == 0) System.out.print((System.nanoTime()-time)+"\t\t"); {time = System.nanoTime(); }
				Outputter.debugln("\taction: "+this.currentAction.toString()+".",
						Outputter.DEBUG_TYPES.GENERAL);

				// Check the cognitive map is working
				if (Outputter.debugAwarenessSpace) {
					Outputter.debug("Burglar('"+toString()+"').step(). Memory (visits,burglaries): ", Outputter.DEBUG_TYPES.AWARENESS_SPACE);
					Map<Building, List<Integer>> buildingsInMemory = this.memory.getFromMemory(Building.class);
					for (Building b:buildingsInMemory.keySet()) {
						Outputter.debug(b.toString()+":"+
								buildingsInMemory.get(b).get(0)+","+
								buildingsInMemory.get(b).get(1)+". ", 
								Outputter.DEBUG_TYPES.AWARENESS_SPACE);
					}
					Map<Community, List<Integer>> communitiesInMemory = this.memory.getFromMemory(Community.class);
					for (Community c:communitiesInMemory.keySet()) {
						Outputter.debug(c.toString()+":"+
								communitiesInMemory.get(c).get(0)+", "+
								communitiesInMemory.get(c).get(1)+". ", 
								Outputter.DEBUG_TYPES.AWARENESS_SPACE);
					}
					Outputter.debugln("",Outputter.DEBUG_TYPES.AWARENESS_SPACE);
				}
				
				// See if the agent can be put to sleep.
				if (this.currentAction.isSleepable()) {
					// Find the second-highest intensity of all the motives (these were added to an array earlier)
					double secondHighestIntensity = Double.MIN_VALUE;
					for (Double inten:this.intensities) {
						if ( (inten > secondHighestIntensity) && (inten != highestIntensity)) {
							secondHighestIntensity = inten;
						}
					} // for intensities
 					if ((secondHighestIntensity+GlobalVars.INTENSITY_DIFFERENCE) < highestIntensity) {
 						// The secondHighestIntensity is significantly smaller than the highest one, put agent to sleep
 						Outputter.debugln("\tmotive unlikely to change for a while, agent is being put to sleep for an " +
 								"hour. ("+highestIntensity+", "+secondHighestIntensity+")",	Outputter.DEBUG_TYPES.GENERAL);
 						this.sleeping = (int) GlobalVars.ITER_PER_DAY / 24;
 					}
 					else {
 						Outputter.debugln("\tagent could sleep, but highest intensity not great enough ("+
 								highestIntensity+", "+secondHighestIntensity+")", Outputter.DEBUG_TYPES.GENERAL);
 					}
 					
				} // if agent can be put to sleep
				
				// Possibly write out this agent's information
				if ((ContextCreator.isTrue(GlobalVars.HISTORY_PARAMS.BURGLAR_INFO_OUT) || 
						ContextCreator.isTrue(GlobalVars.HISTORY_PARAMS.ACTIONS_OUT) || 
						ContextCreator.isTrue(GlobalVars.HISTORY_PARAMS.MOTIVES_OUT) || 
						ContextCreator.isTrue(GlobalVars.HISTORY_PARAMS.STATE_VARIABLES_OUT) ||
						ContextCreator.isTrue(GlobalVars.HISTORY_PARAMS.BURGLAR_MEMORY_OUT)
				) && this.storeFullHistory()) {
					Burglar.storeInformation(this);
				}

			} // else if sleeping
//			Outputter.describeln("..."+GlobalVars.getIteration()+" Burglar "+this.toString()+" finished stepping.");
//			if (ticks%1000 == 0) System.out.println("total: "+(System.nanoTime()-startTime)); 
		} catch (Exception e) {
			Outputter.errorln("Burglar '"+toString()+"' threw an exception while stepping. ENDING THIS RUN");
			Outputter.errorln("Type: "+e.getClass().getName());
			Outputter.errorln("Message: "+e.getMessage());
			Outputter.errorln(e.getStackTrace());
			ContextCreator.haltSim();
			// TODO XXXX need to check that this signals to the runner that the model has failed and should
			// not continue, cannot throw this error further because I'm not sure what the calling class is.
		}

	} // step()

	/**
	 * This function allows for extra heterogeneity in the burglars. If a particular subclass
	 * of burglar will use a unique search routine to look for a burglary target, for example. it
	 * can override this method method, returning it's unique Action subclass.
	 * <p>
	 * When a motive adds a Burglary action to it's action list, the burglary action will ask the
	 * agent if it has implemented it's own burglary action via this function. If this function returns
	 * null then the agent doesn't have a personalised burglary strategy so the Burglary action
	 * wil use a default.  
	 * @param <T> The type of action which this agent might have personalised, e.g. "Search".
	 * @param actionClass The superclass of action the agent might have extended (e.g. TearDropSearch).
	 * @param motive The motive which is looking for the personalised action
	 * @return The personalised action to be added to the agent's actionList (e.g. TearDropSearch)
	 */
	public <T extends Action> T getSpecificAction(Class<T> actionClass, Motive motive) {
		return null;
	}

	/**
	 * Whether or not this Burglar can do any temporary work today.
	 * @return true if the burglar can work, false otherwise.
	 */
	public abstract boolean canWork();
	
	/**
	 * Initialises this Burglar's memory. Cannot be done at the same time that the agent is created because
	 * it needs them to exist in an environemnt, but the agent can only be added to an environment and moved
	 * to their starting position once they have actually been created!
	 */
	public void initMemory() {
		this.memory = new BurglarMemory(this);
	}

	public List<StateVariable> getStateVariables() {
		return this.stateVariables;
	}

	public double getWealth() {
		return this.wealth;
	}	
	public void changeWealth(double change) {
		this.wealth += change;
	}
	public Building getHome() {
		return this.home;
	}
	public void setHome(Building home) {
		this.home = home;
	}
	public Building getWork() {
		return work;
	}
	public void setWork(Building work) {
		this.work = work;
	}
	public Building getDrugDealer() {
		return drugDealer;
	}
	public void setDrugDealer(Building drugDealer) {
		this.drugDealer = drugDealer;
	}
	public Building getSocial() {
		return social;
	}
	public void setSocial(Building social) {
		this.social = social;
	}
	public String getName() {
		return this.name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Action getCurrentAction() {
		return this.currentAction;
	}
	public Motive getActionGuidingMotive() {
		return this.actionGuidingMotive;
	}
	public int getID() {
		return this.id;
	}
	/**
	 * Sets the burglar's id. Not commonly used, usually ID will be set automatically (auto-increment) or
	 * from the input data. This is used in sensitivity tests. Call 'setTheID' rather than 'setID' so that
	 * it doesn't confuse the Shapefile loader (possible that a burglar input GIS file has an ID column).
	 * @param id
	 */
	protected void setTheID(int id) {
		this.id = id;
	}
	public boolean storeFullHistory() {
		return this.storeFullHistory;
	}
	public void storeFullHistory(boolean store) {
		this.storeFullHistory = store;
	}
	public String toString() {
		return this.getClass().getCanonicalName()+" "+this.id ;
	}
	
	/**
	 * Tells if the agent has been locked by an action so cannot change action temporarily (i.e. agent
	 * is on a bus, can't get off until next stop).
	 * @return true if this burglar has been locked
	 */
	public boolean isLocked() {
		return locked;
	}
	/**
	 * Used to lock/unlock the agent
	 * @param locked Whether the agent should be locked or not.
	 * @param action The action which is (un)locking the agent, useful for debugging.
	 */
	public void setLocked(boolean locked, Action action) {
		this.locked = locked;
		this.locker = action;
	}

	/**
	 * Used to tell whether or not the agent wants to change it's action but is locked. If this is true the
	 * Action which has locked the agent will stop asap so the agent can perform a new action (i.e. agent is
	 * on a bus but now wants to do something else, awaitingUnlock is set to true so as soon as agent reaches
	 * a bus stop, TravelA will stop and unlock the agent so another Motive/Action can take over.
	 * @return true if the agent wants to change its Action and needs to be unlocked.
	 */
	public boolean isAwaitingUnlock() {
		return awaitingUnlock;
	}

	public void setAwaitingUnlock(boolean changeAction) {
		this.awaitingUnlock = changeAction;
	}
	
	/**
	 * Get a list of all the transport methods which this agent can utilise at the moment.
	 */
	public List<String> getTransportAvailable() {
		return this.transportAvailable;
	}
	
	/**
	 * Add a new method of transport for this agent (i.e. just bought a car).
	 * @param str The method of transport to add, see GlobalVars.TRANSPORT_PARAMS for possibilities
	 */
	public void addToTransportAvailable(String str) {
		if (!GlobalVars.TRANSPORT_PARAMS.ALL_PARAMS.contains(str)) {
			Outputter.errorln("Burglar: addToTransportAvailable: '"+str+"' isn't a recognised transportation method");
			return;
		}
		if (this.transportAvailable.contains(str)) {
			Outputter.errorln("Burglar: addToTransportAvailable: this agent already has '"+str+"' registered" +
					"as available transport: "+this.transportAvailable.toString());
		}
		else {
			this.transportAvailable.add(str);	
		}
	}
	
	/**
	 * Stop the agent being able to utilise a method of transport.
	 * @param str
	 */
	public void removeFromTransportAvailable(String str) {
		if (!GlobalVars.TRANSPORT_PARAMS.ALL_PARAMS.contains(str)) {
			Outputter.errorln("Burglar: removeFromTransportAvailable: '"+str+
					"' isn't a recognised transportation method");
			return;
		}
		if (!this.transportAvailable.contains(str)) {
			Outputter.errorln("Burglar: removeFromTransportAvailable: this agent doesn't have '"+str+
					"' registered as available transport: "+this.transportAvailable.toString());
		}
		else {
			this.transportAvailable.remove(str);	
		}
	}
	
	/**
	 * Check if the given transport method is available to this agent at the moment.
	 * @param str The transport method to check, see GlobalVars.TRANSPORT_PARAMS for possibilities (car,
	 * bus, train etc).
	 * @return True if they can make use of the transport method (i.e. have a car), false otherwise.
	 */
	public boolean isAvailableTransport(String str) {
		if (!GlobalVars.TRANSPORT_PARAMS.ALL_PARAMS.contains(str)) {
			Outputter.errorln("Burglar: isAvailableTransport: '"+str+"' isn't a recognised transportation method");
		}
		if (this.transportAvailable.contains(str))
			return true;
		return false;
	}

	/**
	 * Adds all the given objects to this agent's awareness space (memory). 
	 * <p>
	 * The Route class is responsible for adding buildings to the agent's awareness space as it
	 * moves the agent around the environment (i.e. Route will call this function each time it steps).
	 * @param <T> The type of objects to add to the memory
	 * @param objects The objects to add
	 * @param clazz The class of T
	 */
	public <T> void addToMemory(List<T> objects, Class<T> clazz) {
		checkMemory();
		this.memory.addToMemory(objects, clazz);
	}
	
	/**
	 * Set the buildings which the agent has just passed.
	 * <p>
	 * The Route class is responsible for doing this as it moves the agent around the environment. If the
	 * agent doesn't pass any buildings in an iteration (e.g. could be on a transport route or on a stretch
	 * of road without any buildings) then Route should pass null to this function.
	 * @param buildings
	 */
	public void buildingsPassed(List<Building> buildings) {
//		System.out.println("Burglar.passedBuildings: "+buildings.toString());
		this.buildingsPassed = buildings;
	}
	
	/**
	 * Get the buildings which have been passed by this agent in this iteration.
	 * @return The buildings or null if none were passed.
	 */
	public List<Building> getBuildingsPassed() {
		return this.buildingsPassed;
	}
	
	public BurglarMemory getMemory() {
		checkMemory();
		return this.memory;
	}
	
	private void checkMemory() {
		if (this.memory==null) {
			Outputter.errorln("Burglar error. BurglarMemory is being accessed  but hasn't been initialised " +
					"yet. Once a Burglar has been created, added to an environment and moved to their starting " +
					"position, initMemory() must be called");
		}
	}
	
	/**
	 * Store the given burglar's information in a data store. Will write the values of their state variables,
	 * motives and current action  along with wealth and x,y position. This will only be called by step() if
	 * we're writing some burglar information in the first place and the agent's
	 * storeFullInfo boolean is true (possible to only write information about some burglars to
	 * save time).
	 * @param burglar The burglar to store information about.
	 * @throws Exception If there is a problem writing the data.
	 */
	public static void storeInformation(Burglar burglar) throws Exception {
		DataAccess da = DataAccessFactory.getDataAccessObject(GlobalVars.HISTORY_PARAMS.RESULTS_DATABASE);
		/* BurglarInfo  */
		int time = GlobalVars.getIteration();
		if (ContextCreator.isTrue(GlobalVars.HISTORY_PARAMS.BURGLAR_INFO_OUT)) {
			ContextCreator.writeBurglarHistory(da, burglar); // Moved code to a convenience function
		}
		
		// Not storing burglar memory at every iteration, only once per day.
//		/* BurglarMemory */
//		if (ContextCreator.isTrue(GlobalVars.HISTORY_PARAMS.BURGLAR_MEMORY_OUT)) {
//			Object[] values = new Object[6];
//			String[] names = new String[6];
//			Map<Building, List<Integer>> bm = burglar.getMemory().getFromMemory(Building.class);  
//			for (Building building:bm.keySet()) {
//				names[0] = "BurglarID"; values[0] = burglar.getID();
//				names[1] = "ModelID"; values[1] = GlobalVars.MODEL_ID;
//				names[2] = "BuildingID"; values[2] = building.getId();
//				names[3] = "NumVisits"; values[3] = bm.get(building).get(BurglarMemory.VISITS_INDEX); 
//				names[4] = "NumBurglaries"; values[4] = bm.get(building).get(BurglarMemory.BURGLARIES_INDEX);
//				names[5] = "Time"; values[5] = GlobalVars.getIteration();
//				da.writeValues(values, GlobalVars.HISTORY_PARAMS.BURGLAR_MEMORY, names);
//			} // for BurglarMemory
//		}// if BurglarMemoryOut


		/* StateVariables, Motives and current Action */
		if (ContextCreator.isTrue(GlobalVars.HISTORY_PARAMS.STATE_VARIABLES_OUT)) {
			Object[] values = new Object[4]; 
			String[] names = new String[4];
			for (StateVariable s:burglar.getStateVariables()) {
				values = new Object[5]; 
				names = new String[5];
				names[0] = "BurglarID"; values[0] = burglar.getID();
				names[1] = "ModelID"; values[1] = GlobalVars.MODEL_ID;
				names[2] = "Name"; values[2] = s.getName();
				names[3] = "Time"; values[3] = time;
				names[4] = "Value"; values[4] = s.getValue();
				da.writeValues(values, GlobalVars.HISTORY_PARAMS.STATE_VARIABLES, names);

				/* Motives (idMotives, BurglarID, Name, Time, Value) */ // NOTE: if change system so multiple motives this loop can be nester withing statevar loop
				Motive m;
				if (ContextCreator.isTrue(GlobalVars.HISTORY_PARAMS.MOTIVES_OUT)) {
					m = s.getMotive();
					values = new Object[5]; 
					names = new String[5];
					names[0] = "BurglarID"; values[0] = burglar.getID();
					names[1] = "ModelID"; values[1] = GlobalVars.MODEL_ID;
					names[2] = "Name"; values[2] = m.getName();
					names[3] = "Time"; values[3] = time;
					names[4] = "Value"; values[4] = m.getIntensity();
					da.writeValues(values, GlobalVars.HISTORY_PARAMS.MOTIVES, names);
				}
			} // for statevariables
			/* Action */
			if (ContextCreator.isTrue(GlobalVars.HISTORY_PARAMS.ACTIONS_OUT)) {
				Action a = burglar.getCurrentAction();
				values = new Object[5]; 
				names = new String[5];
				names[0] = "BurglarID"; values[0] = burglar.getID();
				names[1] = "ModelID"; values[1] = GlobalVars.MODEL_ID;
				names[2] = "Name"; values[2] = a.getClass().getName();
				names[3] = "Time"; values[3] = time;
				names[4] = "Value"; values[4] = -1; // Actions don't have values like Motives/StateVariables
				da.writeValues(values, GlobalVars.HISTORY_PARAMS.ACTIONS, names);
			}

		} // if STATEVARIABLES_OUT
		
		
	}
	
	public void clearCaches() {
		Burglar.burglarNumber = 0;
		Burglar.uniqueID=0;
	}

}
