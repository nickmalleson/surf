package burgdsim.pecs;

import burgdsim.burglars.Burglar;
import burgdsim.environment.buildings.Building;
import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;

/**
 * A variable which can be used to make agents walk randomly around environment. Usefull
 * for debugging. Agent chooses a building at random, travels there, then chooses another
 * building etc.
 * @author Nick Malleson
 */
public class RandomWalkV extends StateVariable {

	public RandomWalkV(Burglar burglar, double value) {
		super(burglar, value);
	}

	@Override
	protected void setMotive() {
		this.motive = new RandomWalkM(this);
	}

}

class RandomWalkM extends Motive {
	
	public RandomWalkM(StateVariable s) {
		super(s);
	}

	@Override
	public void buildActionList() {
		// Choose a random building to travel to.
		Building b = GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(Building.class);
		Outputter.debugln("RandomWalkV: creating a route to: "+b.toString(), Outputter.DEBUG_TYPES.GENERAL);
		this.actionList.add(new RandomWalkA(this));
		this.actionList.add(new TravelA(this,
				b.getCoords(), 
				b, "Travelling to A Random Building: "+b.toString()));
	} // setActionList()

	/**
	 * Intensity always 1
	 */
	@Override
	protected double calcIntensity() {
		return 1;
	}
}

/**
 * Doesn't actually do anything, just sets 'completed' to true so the agent will create a 
 * new RandomWalkA which will take them to another building
 * @author Nick Malleson
 *
 */
class RandomWalkA extends Action {

	public RandomWalkA(Motive m) {
		super(m);
		this.description = "RandomWalk action";
	}

	@Override
	public boolean performAction() {
		this.complete = true;
		this.getStateVariable().setValue(1);
		return false; // Not a temporary action, takes a whole iteration.
	}
	
}