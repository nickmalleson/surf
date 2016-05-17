package burgdsim.pecs;

import burgdsim.burglars.Burglar;
import burgdsim.main.GlobalVars;


/**
 * State variable which causes agent to do nothing. Only purpose of this variable is to point to the
 * DoNothingM motive.
 * @author Nick Malleson
 *
 */
public class DoNothingV extends StateVariable {

	/**
	 * Unlike other state variables DoNothing doesn't have a value, it isn't relevant (passes -1 to parent).
	 * The only purpose of this state variable is to point to the DoNothingM motive.
	 * @param burglar
	 */
	public DoNothingV(Burglar burglar) {
		super(burglar, -1);
	}

	@Override
	protected void setMotive() {
		this.motive = new DoNothingM(this);		
	}
	
	/**
	 * Override the StateVariable step() method becasue this state variable doesn't need to be reduced.
	 */
	@Override
	public void step() {
		
	}
}

/**
 * Motive which will cause agent to do nothing. Must travel home first.
 * @author Nick Malleson
 *
 */
class DoNothingM extends Motive {

	public DoNothingM(StateVariable s) {
		super(s);
	}

	/**
	 * Return a constant intensity defined in GlobalVars.DO_NOTHING_THRESHOLD, will only DoNothing if
	 * all other motive intensities are below this value.
	 * 
	 * @return GlobalVars.DO_NOTHING_THRESHOLD (constant value)
	 * @return {@link }
	 */
	@Override
	protected double calcIntensity() {
//		for (StateVariable s:this.getBurglar().getStateVariables()) {
//			if ((!s.getMotive().equals(this)) && (s.getMotive().getIntensity() > GlobalVars.DO_NOTHING_THRESHOLD) )
//				return 0;
//		}
//		// No motives are above the threshold so return infinity (definitely perform this action).
		return GlobalVars.DO_NOTHING_THRESHOLD;
	}

	@Override
	public void buildActionList() {
		/* BUILD UP THE ACTIONLIST FROM THE BOTTOM UP */
		this.actionList.add(new DoNothingA(this));
		this.actionList.add(new TravelA(
				this, this.getBurglar().getHome().getCoords(), 
				this.getBurglar().getHome(),
				"Travelling to Home to Do Nothing"));
	}
}

/**
 * Agents will do nothing if none of their motives are sufficiently high. This could be used to
 * represent other motives which will be done at home but are not included in the model such as 
 * "read a book" or "tidy the house". 
 * 
 * @author Nick Malleson
 */
class DoNothingA extends Action {

	public DoNothingA(Motive motive) {
		super(motive);
		this.description = "Doing nothing";
		this.sleepable = true; // It is ok for the agent to be put to 'sleep' if they are performing this action
	}

	/**
	 * This action causes the agent to do nothing, so simply returns false.
	 * @return false because this is not a temporary action (it will take an entire iteration to complete). 
	 */
	@Override
	public boolean performAction() {
		return false;
	}


}
