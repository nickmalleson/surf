package burgdsim.burglars;

import java.util.List;

import burgdsim.burglary.Burglary;
import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;
import burgdsim.pecs.Action;
import burgdsim.pecs.Motive;
import burgdsim.pecs.StateVariable;

import repast.simphony.random.RandomHelper;

/**
 * Simple burglar example who implements their own internal Burglary action.
 * @author Nick Malleson
 *
 */
public class SimplePersonalisedBurglar extends Burglar {

	public SimplePersonalisedBurglar(List<StateVariable> stateVariables) {
		super(stateVariables);
	}

	@Override
	public boolean canWork() {
		// Simple burglar can only work half the time
		return (5<RandomHelper.nextIntFromTo(0, 10));
	}
	
	
	/* (non-Javadoc)
	 * @see burglars.Burglar#getSpecificAction(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Action> T getSpecificAction(Class<T> actionClass, Motive motive) {
		T obj = null;
		if (actionClass.isAssignableFrom(SimpleBurgleA.class)) {
			obj = (T) new SimpleBurgleA(motive);
		}
		return obj;
	}
	

	public int getDummy() {
		return this.dummy;
	}
	private int dummy;
	
}

class SimpleBurgleA extends Burglary {

	public SimpleBurgleA(Motive m) {
		super(m);
		this.description = "Burgling (SimpleBurgleA)";
	}

	@Override
	public boolean performAction() {
		this.getBurglar().changeWealth(GlobalVars.BURGLE_GAIN);
		Outputter.debugln("Burglary: "+this.getBurglar().toString()+" has just burgled, using SimpleBurgleA",
				Outputter.DEBUG_TYPES.GENERAL);
		// XXXX Make changes to house which has just been burgled.
		this.complete = true;
		return false;
	}
	
}