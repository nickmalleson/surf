package burgdsim.pecs;

import burgdsim.burglars.Burglar;
import burgdsim.main.ContextCreator;
import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;

/**
 * StateVariables control the behaviour of agents. They lead to Motives which the agent will try to satisfy.
 * 
 * @author   Nick Malleson
 */
public abstract class StateVariable {

	protected String name;
	protected Motive motive;
	protected Burglar burglar;
	private double value;		// The value of this state variable (e.g. level of energy)
	
	public StateVariable(Burglar burglar, double initialValue) {
		this.burglar = burglar;
		this.value = initialValue;
		setMotive();
		if (this.motive == null) 
			Outputter.errorln("StateVariable("+this.getClass().getName()+"): motive has not been defined.");
	}
	
	protected abstract void setMotive();
	
	/**
	 * Called at every iteration, deteriorates the values of the state variable. Can be overridden by subclasses
	 * to perform additional actions.
	 */
	public void step() {
		this.changeValue(-GlobalVars.DETERIORATE_AMOUNT);
	}
	
	/**
	 * Get the motive which depends on this state variable.
	 * @return the motive
	 */
	public Motive getMotive() {
		return this.motive;
	}
	
	/**
	 * @return the person
	 */
	public Burglar getBurglar() {
		return this.burglar;
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		if (this.name == null)
			return this.getClass().getSimpleName();
		return this.name;
	}

	/**
	 * Used by subclasses to change the size of the state variable. Checks that value will always be slightly above 0
	 * (given value of Double.MIN_VALUE if value<=0)
	 * @param change The amount to change the state variable value by.
	 */
	protected void changeValue(double change) {
		this.value += change;
		if (this.value <= 0) {
//			Outputter.errorln("WARNING: StateVariable.changeValue(): The value of this state variable (" +
//					this.getClass().getName() + ") for burglar: "+this.burglar.getName() +
//					" has reached its minimum value.");
//			this.value = Double.MIN_VALUE;
//			Outputter.errorln("Halting Simulation.");
//			ContextCreator.haltSim();
			
			// Hack to try to keep an agent working properly if their state variable value drops, this should
			// definitely be fixed if it hapens again.
			Outputter.errorln("WARNING: StateVariable.changeValue(): The value of this state variable (" +
					this.getClass().getName() + ") for burglar: "+this.burglar.getName() +
					" has reached its minimum value, setting it to 0.5 ");
			this.value = 0.5;
		}
	}
	
	/**
	 * Can also be used to set values but not advisable. Used by RandomWalkV (for debugging routing
	 * algorithms) and DebugBurglaryV (for debugging burglary actions).
	 * @param value
	 */
	public void setValue(double value) {
		this.value = value;
	}

	
	/**
	 * @return the value of this state variable (e.g. energy level).
	 */	
	public double getValue() {
		return this.value;
	}
	
	public String toString() {
		if (this.name == null) {
			return this.getClass().getName();
		}
		else return this.name;
	}
	
}

