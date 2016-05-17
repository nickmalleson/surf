package burgdsim.environment;

import java.util.List;
import burgdsim.burglars.Burglar;
import burgdsim.burglary.SearchAlg;
import burgdsim.environment.buildings.Building;
import burgdsim.main.GlobalVars;

public class NullRoute extends Route {
	
	private int travelTime = 0;

	/**
	 * See Route() for reason that this constructor is necessary.
	 */
	public NullRoute() {}
	
	public NullRoute(Burglar burglar, Coord destination, Building destinationBuilding, SearchAlg.SEARCH_TYPES type) {
		super(burglar, destination, destinationBuilding, type);
		/* Unlike other Route objects, don't add buildings to the agent's awareness space as they
		 * travel (this doesn't really make sense as the agents don't actually travel anywhere!).
		 * Instead just add all the buildings each time this Route is created. */
		this.passedObjects(GlobalVars.BUILDING_ENVIRONMENT.getAllObjects(Building.class), Building.class);
		this.passedObjects(GlobalVars.COMMUNITY_ENVIRONMENT.getAllObjects(Community.class), Community.class);
	}

	/**
	 * Simulates travelling, will return true once the agent has been travelling for a given amount of time.
	 * 
	 *  @return true if maximum travel time has been reached.
	 */
	@Override
	public boolean atDestination() {
		if (travelTime >= GlobalVars.CONST_TRAVEL_TIME )
			return true;
		return false;
	}

	/**
	 * Simulates travelling around an environment by incrementing the travel time by 1.
	 * @throws Exception 
	 */
	@Override
	public void travel() throws Exception {
		super.travel();
		this.travelTime++;
	}
	
	/**
	 * Just returns 1.
	 */
	@Override
	public double getDistance(Burglar burglar, Coord origin, Coord destination) {
		return 1;
	}

	@Override
	protected List<Coord> setRoute() {
		return null;
	}
	
	/**
	 * Create different types of route, actually just returns null
	 * @throws Exception 
	 */
	protected List<Coord> setRoute(SearchAlg.SEARCH_TYPES type) {
	
		return null;
	}

	
}
