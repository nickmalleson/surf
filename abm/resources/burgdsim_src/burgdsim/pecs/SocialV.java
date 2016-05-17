package burgdsim.pecs;

import java.util.Hashtable;

import burgdsim.burglars.Burglar;
import burgdsim.burglary.Burglary;
import burgdsim.main.GlobalVars;

public class SocialV extends StateVariable {

	public SocialV(Burglar burglar, double value) {
		super(burglar, value);
	}

	@Override
	protected void setMotive() {
		this.motive = new SocialM(this);
		
	}
	/** Convenience function provided to allow easy graph creation. See main.Grapher.java for example usage.
	 *
	 * @return a HashTable containing the different values of the TOD component of this sleep motive
	 */
	public static Hashtable<Integer, Double> getMotiveLookupTable() {
		return SocialM.getLookupTable();
	}
}

class SocialM extends Motive {

	// The intensities of this motive at different times of day, stored for quick lookup
	private static Hashtable<Integer, Double> times = null; // static because shared over all SleepMotives
	private static boolean builtLookupTable = false;
	
	public SocialM(StateVariable s) {
		super(s);
		if (!SocialM.builtLookupTable) {
			SocialM.buildLookupTable();
			SocialM.builtLookupTable = true;
		}
	}

	@Override
	public void buildActionList() {
		this.actionList.add(new SocialiseA(this));
		this.actionList.add(new TravelA(this, 
				this.getBurglar().getSocial().getCoords(), this.getBurglar().getSocial(), 
				"Travelling to a Social Place"));
		if (this.stateVariable.getBurglar().getWealth()<GlobalVars.INITIAL_COST_SOCIALISE) { // Can't afford to socialise
			if (this.stateVariable.getBurglar().canWork()) {
				this.actionList.add(new WorkA(this, "Working for money to socialise", GlobalVars.INITIAL_COST_SOCIALISE));
				this.actionList.add(new TravelA(this, 
						this.getBurglar().getWork().getCoords(), this.getBurglar().getWork(), "Travelling to Work"));
			}
			else {
				// Unlike all other actions burgling is a unique action which will handle travelling to a victim etc.
				Burglary b = this.getBurglar().getSpecificAction(Burglary.class, this);
				if (b!=null) {
					this.actionList.add(b);
				}
				else {
					this.actionList.add(new Burglary(this));
				}
				
			}
		}
	}

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
		return lower + interpolateAmount*(upper-lower);
		
	}
	
	/** Build a hashtable for quick look-up of different intensities depending on time of day. Find intensity by
	 * interpolating between values at different hours. E.g. to find intensity at 3:30 (decimal value 3.50)
	 * find half way between values of keys '3' and '4'
	 */
	private static void buildLookupTable() {
		SocialM.times = new Hashtable<Integer, Double>(25);
		times.put(0,  0.25);
		times.put(1,  0.20);
		times.put(2,  0.175);
		times.put(3,  0.15);
		times.put(4,  0.125 );
		times.put(5,  0.09);
		times.put(6,  0.075 );
		times.put(7,  0.09);
		times.put(8,  0.125);
		times.put(9,  0.15);
		times.put(10, 0.175);
		times.put(11, 0.20);
		times.put(12, 0.25);
		times.put(13, 0.50);
		times.put(14, 0.80);
		times.put(15, 0.90);
		times.put(16, 0.95);
		times.put(17, 1.00);
		times.put(18, 1.00);
		times.put(19, 0.95);
		times.put(20, 0.90);
		times.put(21, 0.80);
		times.put(22, 0.50);
		times.put(23, 0.30);
		times.put(24, 0.25);
	}
	
	/** Convenience function provided to allow easy graph creation. See main.Grapher.java for example usage.
	 *
	 * @return a HashTable containing the different values of the TOD component of this sleep motive
	 */
	public static Hashtable<Integer, Double> getLookupTable() {
		if (!builtLookupTable) {
			buildLookupTable();
			SocialM.builtLookupTable = true;
		}
		return SocialM.times;
	}

}

/**
 * Socialising is slightly different to other actions. It requires a certain amount of wealth to begin
 * and then will incrementally reduce wealth *and* increase the socialLevel over a number of iterations.
 * <p>
 * The SocialM actionList is built up so that the agent will not start to socialise unless it has enough
 * wealth in the first place so we don't need a check here that socialising can start. Do need to check
 * that the agent hasn't run out of wealth though (they might want to keep socialising even when their wealth
 * has gone). This doesn't happen with other actions (e.g. drug taking) because they only occur over
 * one iteration and then are complete so there isn't this risk that wealth might run out whilst they
 * are still being completed.
 * 
 * @author Nick Malleson
 */

class SocialiseA extends Action {
	
	public SocialiseA(Motive motive) {
		super(motive);
		this.description = "Socialising";
	}

	@Override
	public boolean performAction() {
		// Check that agent hasn't run out of wealth, don't socialise if they have
//		System.out.println("Social stuff: wealthLevel: "+this.getBurglar().getWealth()+", initial cost: "+GlobalVars.INITIAL_COST_SOCIALISE+", cost: "+GlobalVars.COST_SOCIALISE+", gain: "+GlobalVars.SOCIALISE);
		if ( (this.getBurglar().getWealth() - GlobalVars.COST_SOCIALISE ) <= 0 ) {
			// The agent has run out of money so the action needs to be changed, but not the motive (i.e. the
			// agent still wants to socialise but now it needs to work to generate wealth).
//			Outputter.describeln("SocialiseA: Sending a change action event");
			this.fireChangeActionEvent(new ChangeActionEvent(this, "Have run out of money to socialise with"));
			return false;
			// NOTE: Strictly speaking the agent hasn't done anything so we should return true here (because
			// the action is only temporary). But, if we return true the agent gets stuck in an infinite loop
			// because the new actionList wont be read until burglar.step() is called again at the next iteration.
		}
		// Otherwise keep socialising.
		else {
			this.getBurglar().changeWealth(-GlobalVars.COST_SOCIALISE);
			this.getStateVariable().changeValue(GlobalVars.SOCIAL_GAIN);
			return false;
		}
	}
	
}