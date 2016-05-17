package burgdsim.burglars;

import java.util.List;

import burgdsim.pecs.StateVariable;


/**
 * Simple burglar who uses default Burglary template
 * @author Nick Malleson
 *
 */
public class SimpleDefaultBurglar extends Burglar {

	public SimpleDefaultBurglar(List<StateVariable> stateVariables) {
		super(stateVariables);
	}

	@Override
	public boolean canWork() {
		return false;
	}
	
	public int getDummy() {
		return this.dummy;
	}
	private int dummy;
	
}