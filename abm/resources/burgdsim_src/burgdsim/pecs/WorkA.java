package burgdsim.pecs;

import burgdsim.main.GlobalVars;

/**
 * Action allows agents to work to generate wealth.
 * @author Nick Malleson
 *
 */
public class WorkA extends Action {

	private double desiredWealth ; // The amount of wealth this agent needs to generate
	
	public WorkA(Motive motive, String description, double desiredWealth) {
		super(motive);
		this.description = description;
		this.desiredWealth = desiredWealth;
	}

	@Override
	public boolean performAction() {
		if (this.getBurglar().getWealth() > this.desiredWealth) {
			this.complete = true;
			return true; // A temporary action, don't need to do any work this iteration so move onto the next action
		}
		else if (!this.getBurglar().canWork()) {
			this.complete = true;
			// The agent can't work so this is a temporary action
			return true; 
		}
		else {
			this.getBurglar().changeWealth(GlobalVars.WORK_GAIN);
			return false;
		}
		
	} // performAction()

}
