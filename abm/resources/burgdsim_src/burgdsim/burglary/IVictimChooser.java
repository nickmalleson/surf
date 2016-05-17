package burgdsim.burglary;

import java.util.List;

import burgdsim.environment.buildings.Building;
import burgdsim.environment.buildings.House;


public interface IVictimChooser {
	
	/**
	 * From the Collection of given houses see if any are suitable burglary victims.  
	 * @param buildings The Buildings that the agent is passing during this iteration
	 * @param calculationValues An optional array of doubles. If this is not null, all 
	 * the values of the variables used in the calculation will be put in here. For more
	 * information about what the variables do see the VictimChooser class.  
	 * @return the house which is suitable or null if none are.
	 * @throws Exception 
	 */
	House chooseVictim(List<Building> buildings, List<Double> calculationValues) throws Exception;

}
