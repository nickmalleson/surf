package burgdsim.burglary;

import burgdsim.environment.Community;

public interface ITargetChooser {
	
	/**
	 * Examines the agent's cognitive map and chooses the community which they will visit to search
	 * for a burglary victim.
	 * @return
	 */
	Community chooseTarget();

}
