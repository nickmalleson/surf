package burgdsim.burglary;

import burgdsim.burglars.Burglar;
import burgdsim.pecs.Motive;
import burgdsim.pecs.StateVariable;

/**
 * Variable used for debugging burglary, agents just do burglary, don't have any other variables.
 * @author Nick Malleson
 */
public class DebugBurglaryV extends StateVariable {

	public DebugBurglaryV(Burglar burglar, double value) {
		super(burglar, value);
	}

	@Override
	protected void setMotive() {
		this.motive = new DebugBurglaryM(this);
	}

}

class DebugBurglaryM extends Motive {
	
	public DebugBurglaryM(StateVariable s) {
		super(s);
	}

	@Override
	public void buildActionList() {
		Burglary b = this.getBurglar().getSpecificAction(Burglary.class, this);
		if (b!=null) {  // Burglar provides own Burglary action
			this.actionList.add(b);
		}
		else {  // Use the default Burglary action
			this.actionList.add(new Burglary(this));
		}
//		this.actionList.add(new BullsEyeBurglarySearchA(this));
		
	} // setActionList()

	/**
	 * Intensity always 1
	 */
	@Override
	protected double calcIntensity() {
		this.getStateVariable().setValue(1); // stops the state variable deteriorating
		return 1;
	}
}


