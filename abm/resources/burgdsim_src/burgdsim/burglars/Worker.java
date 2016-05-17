package burgdsim.burglars;

import java.util.List;

import burgdsim.pecs.StateVariable;


/**
 * An agent who can work so has no need to burgle. This is used for some sensitivity tests
 * @author Nick Malleson
 *
 */
public class Worker extends Burglar {

	public Worker(List<StateVariable> stateVariables) {
		super(stateVariables);
	}

	@Override
	public boolean canWork() {
		return true;
	}
	
	public int getDummy() {
		return this.dummy;
	}
	private int dummy;
	
}