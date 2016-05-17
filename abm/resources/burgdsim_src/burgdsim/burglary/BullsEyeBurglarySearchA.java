package burgdsim.burglary;

import burgdsim.environment.EnvironmentFactory;
import burgdsim.environment.Route;

import burgdsim.main.GlobalVars;
import burgdsim.pecs.Action;
import burgdsim.pecs.Motive;

class BullsEyeBurglarySearchA extends Action {
	
//	private int numIterations = 0; 	// The number of iterations which the agent has spend searching
	private double searchStartTime;	// The amount of time (mins) that the agent has spent searching
	private Route route ;			// A route object to perform a bulls eye search

	public BullsEyeBurglarySearchA(Motive m) {
		super(m);
		this.description = "DebugBurglary action";
	}

	@Override
	public boolean performAction() throws Exception {
		// System.out.println("Time searching (mins): "+(GlobalVars.mins-this.searchStartTime));
		// Assume agent starts at victim's house, just create a search.
		if (this.route == null) {
			this.route = EnvironmentFactory.createRoute(this.getBurglar(), null, null, SearchAlg.SEARCH_TYPES.BULLS_EYE);
			this.searchStartTime=GlobalVars.mins;
		}
		// If spend too long searching (total mins > bulls eys search time) then start again with a new target
		else if (GlobalVars.mins-this.searchStartTime > GlobalVars.BULLS_EYE_SEARCH_TIME ){ // Start again from current possition
			this.route = EnvironmentFactory.createRoute(this.getBurglar(), null, null, SearchAlg.SEARCH_TYPES.BULLS_EYE);
			this.searchStartTime=GlobalVars.mins;
			
		}
		else if (this.route.atDestination()) { // Have finished search
			this.route = EnvironmentFactory.createRoute(this.getBurglar(), null, null, SearchAlg.SEARCH_TYPES.BULLS_EYE);
			this.searchStartTime=GlobalVars.mins;
		}
		this.route.travel();
		return false; // Not a temporary action, takes a whole iteration.
		// Note; this action will never complete
	}

	
	
}