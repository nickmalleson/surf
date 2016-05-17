package burgdsim.burglary;

import burgdsim.burglars.Burglar;
import burgdsim.environment.EnvironmentFactory;
import burgdsim.environment.Route;
import burgdsim.main.GlobalVars;


/**
 * Default class which will cause an agent to search for a target. The SearchAlg object
 * guarantees that a step() method is provided which moves the agent along their search route.
 * When a SearchAlg object is created the route should be pre-planned so the searchTime variable
 * can be used to determine the length of the route which should be planned for (i.e. plan a
 * route that is long enough so that the agent always has somewhere to go until they decide to
 * stop searching).
 *  
 * @author Nick Malleson
 */
public class SearchAlg implements ISearchAlg {
	
	public static enum SEARCH_TYPES { BULLS_EYE };
	protected SEARCH_TYPES searchType;
	private double searchStartTime;	// The amount of time (mins) that the agent has spent searching
	private boolean finishedSearching;
	private Route route ;			// A route object to perform a bulls eye search
	
	private Burglar burglar;
	
	/**
	 * Default search algorithm. Implements 'bulls eye' search for 30 mins.
	 */
	public SearchAlg(Burglar burglar) {
//		System.out.println("SEARCHALG Constructor");
		this.burglar = burglar;
		this.searchType = SearchAlg.SEARCH_TYPES.BULLS_EYE;
		init();
	}
	
	public SearchAlg(Burglar burglar, SearchAlg.SEARCH_TYPES searchType) {
//		System.out.println("SEARCHALG Constructor");
		this.burglar = burglar;
		this.searchType = searchType;
		init();
	}

	public void step() throws Exception {
//		System.out.println("Time searching (mins): "+(GlobalVars.mins-this.searchStartTime));
		// Assume agent starts at victim's house, just create a search.
		// If spend too long searching then stop
		if (GlobalVars.mins-this.searchStartTime > GlobalVars.BULLS_EYE_SEARCH_TIME*60 ){ 
			// Have finished searching
			this.finishedSearching = true;
			return;
			
		}
		// If get here then just keep searching	
		this.route.travel();
	}

	public boolean finishedSearching() {
		return this.finishedSearching;
	}
	
	public void init() {
		this.route = EnvironmentFactory.createRoute(this.burglar, null, null, SearchAlg.SEARCH_TYPES.BULLS_EYE);
		this.searchStartTime=GlobalVars.mins;
		this.finishedSearching = false;
	}
	
	
}
