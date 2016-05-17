package burgdsim.pecs;

import java.util.Hashtable;

import burgdsim.burglars.Burglar;
import burgdsim.main.GlobalVars;

public class SleepV extends StateVariable {

	public SleepV(Burglar burglar, double initValue) {
		super(burglar, initValue);
	}

	@Override
	protected void setMotive() {
		this.motive = new SleepM(this);
	}
	
	/** Convenience function provided to allow easy graph creation. See main.Grapher.java for example usage.
	 *
	 * @return a HashTable containing the different values of the TOD component of this sleep motive
	 */
	public static Hashtable<Integer, Double> getMotiveLookupTable() {
		return SleepM.getLookupTable();
	}
}

class SleepM extends Motive {
	
	// The intensities of this motive at different times of day, stored for quick lookup
	private static Hashtable<Integer, Double> times = null; // static because shared over all SleepMotives
	private static boolean builtLookupTable = false;
	
	public SleepM(StateVariable s) {
		super(s);
		if (!builtLookupTable) {
			buildLookupTable();
			SleepM.builtLookupTable = true;
		}
	}

	@Override
	public void buildActionList() {
		this.actionList.add(new SleepA(this));
		this.actionList.add(new TravelA( this, 
			this.getBurglar().getHome().getCoords(), this.getBurglar().getHome(), "Travelling home to sleep"));	
	}

	/**
	 * Sleep intensity is calculated by  (1/sleepLevel)*((factor+TOD)/2)
	 */
	@Override
	protected double calcIntensity() {
		return 0.5 * (this.factor + this.getTODIntensityC()) * (1/this.stateVariable.getValue());
	}
	
	/**
	 * Calculates the time-of-day component of this motive (agents feel need to sleep at night more strongly
	 * than during the day).
	 * @return the intensity
	 */
	private double getTODIntensityC () {
		// Interpolate between two values (upper and lower)in the TOD table
		double lower = times.get((int) GlobalVars.time); // When casting to int will round down
		double upper = times.get((int) GlobalVars.time + 1);
		double interpolateAmount = GlobalVars.time - (int) GlobalVars.time; // The amount to interpolate between
//		Outputter.describeln("SLEEPM: lower:"+lower+". upper: "+upper+". interpolate: "+interpolateAmount+" . return: "+(lower + interpolateAmount*(upper-lower)));
		return lower + interpolateAmount*(upper-lower);
		
	}
	
	/** Build a hashtable for quick look-up of different intensities depending on time of day. Find intensity by
	 * interpolating between values at different hours. E.g. to find intensity at 3:30 (decimal value 3.50)
	 * find half way between values of keys '3' and '4': 0.8+(0.
	 */
	private static void buildLookupTable() {
		SleepM.times = new Hashtable<Integer, Double>(25);
		times.put(0,  0.95); // Midnight (00:00)
		times.put(1,  1.00); // MAX
		times.put(2,  1.00); // MAX
		times.put(3,  0.95);
		times.put(4,  0.90);
		times.put(5,  0.80);
		times.put(6,  0.50);
		times.put(7,  0.30);
		times.put(8,  0.25);
		times.put(9,  0.20);
		times.put(10, 0.175);
		times.put(11, 0.15);
		times.put(12, 0.125); // Midday (12:00)
		times.put(13, 0.09);
		times.put(14, 0.075); // MIN
		times.put(15, 0.09);
		times.put(16, 0.125);
		times.put(17, 0.15);
		times.put(18, 0.175);
		times.put(19, 0.20);
		times.put(20, 0.25);
		times.put(21, 0.50);
		times.put(22, 0.80);
		times.put(23, 0.90);
		times.put(24, 0.95); // Midnight (00:00) - included because 23:00 needs higher number to interpolate between.
	}
	
	/** Convenience function provided to allow easy graph creation. See main.Grapher.java for example usage.
	 *
	 * @return a HashTable containing the different values of the TOD component of this sleep motive
	 */
	public static Hashtable<Integer, Double> getLookupTable() {
		if (!builtLookupTable) {
			buildLookupTable();
			SleepM.builtLookupTable = true;
		}
		return SleepM.times;
	}

}

/**
 * The action of sleeping. Increase the level of the SleepV state variable.
 * @author Nick Malleson
 *
 */
class SleepA extends Action {

	public SleepA(Motive motive) {
		super(motive);
		this.description = "Sleeping";
		this.sleepable = true; // It is ok for the agent to be put to 'sleep' if they are performing this action
	}

	/**
	 * This action increases the size of the agent's Sleep state variable.
	 * @return false because this is not a temporary action (it will take an entire iteration to complete). 
	 */
	@Override
	public boolean performAction() {
		this.getStateVariable().changeValue(GlobalVars.SLEEP_GAIN);
		return false;
	}

}