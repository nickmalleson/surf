package burgdsim.burglars;

import java.util.List;

import burgdsim.burglary.Burglary;
import burgdsim.burglary.SearchAlg;
import burgdsim.pecs.Action;
import burgdsim.pecs.Motive;
import burgdsim.pecs.StateVariable;

import repast.simphony.random.RandomHelper;

/**
 * Simple burglar example who 'plugs-in' their own modules to the deault burglary template.
 * NOTE: At the moment only plugs-in the default search algorithm, so does exactly the same thing
 * as a burglar who uses the entire default template but useful for debugging. 
 * @author Nick Malleson
 *
 */
public class SimplePlugInBurglar extends Burglar {

	public SimplePlugInBurglar(List<StateVariable> stateVariables) {
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
		if (actionClass.isAssignableFrom(Burglary.class)) {
			Burglary b = new Burglary(motive);
			b.setSearchAlg(new SearchAlg(this));
			obj = (T) b;
		}
		return obj;
	}
	

	public int getDummy() {
		return this.dummy;
	}
	private int dummy;
	
}