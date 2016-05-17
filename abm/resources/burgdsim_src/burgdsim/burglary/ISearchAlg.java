package burgdsim.burglary;

public interface ISearchAlg {
	
	/**
	 * Control the agent and search around the local area. 
	 * @throws Exception 
	 */
	void step() throws Exception;
	
	/**
	 * Determine when this search algorithm has finished.
	 * @return True when the agent has finished searching.
	 */
	boolean finishedSearching();
	
	/**
	 * Reinitialise the search algorithm. If the burglar finishes searching but hasn't found a
	 * victim this can be re-initialised to start a new search.
	 */
	public void init();
}
