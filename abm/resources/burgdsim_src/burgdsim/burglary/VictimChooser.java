package burgdsim.burglary;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import repast.simphony.random.RandomHelper;


import burgdsim.burglars.Burglar;
import burgdsim.environment.Community;
import burgdsim.environment.Sociotype;
import burgdsim.environment.buildings.Building;
import burgdsim.environment.buildings.House;
import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;

public class VictimChooser implements IVictimChooser {

	protected Burglar burglar;
	// Maps weight names to their values, used in equation to calculate whether or not a victim is suitable.
	protected Map<GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER, Double> weightMap;

	// Remember the buildings last passed to save on computation if the houses haven't changed (agent on long road)
	private List<Building> previousBuildings;

	/**
	 * Default implementation, just takes the relevant burglar as an argument. The weightMap
	 * is empty so that when a the buildings and agent has just passed have been searched 
	 * none of the relevant weights will be found and they will be given 'default' values of 0.5.
	 * @param burglar
	 */
	public VictimChooser(Burglar burglar) {
		this.burglar = burglar;
		//		this.weightMap = new HashMap<GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER, Double>();
	}


	public VictimChooser(Burglar burglar, Map<GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER, Double> weightMap) {
		super();
		this.burglar = burglar;
		this.weightMap = weightMap;
	}

	/**
	 * From the Collection of given houses see if any are suitable burglary victims.  
	 * @param buildings The Buildings that the agent is passing during this iteration
	 * @param calculationValues An optional List of doubles. If this is not null, all 
	 * the values of the variables used in the calculation will be put in here. 
	 * <br>Variables are put into the array in the following order:
	 * <ol>
	 * <li>ce - collective efficacy of the community</li>
	 * <li>occ - occupancy levels of the community</li>
	 * <li>acc - accessibility of the house</li>
	 * <li>vis - visibility of the house</li>
	 * <li>sec - security of the house</li>
	 * <li>tv - traffic volume on the road next to the house (a house variable)</li>
	 * <li>ce_w - the weight, specific to the burglar applied to the collective efficacy variable</li>
	 * <li>tv_w</li>
	 * <li>occ_w</li>
	 * <li>acc_w</li>
	 * <li>vis_w</li>
	 * <li>sec_w</li>
	 * <li>suitability - the suitability value, derived from above variables</li>
	 * <li>motive_intensity - the intensity of the motive driving the burglar</li>
	 * <li>difference - the difference between suitability and motive intensity</li>
	 * <li>probability - the probability that a burglary will actually occur (derived from difference).</li>
	 * </ol>	
	 * 
	 * @return the house which is suitable or null if none are.
	 * @throws Exception 
	 */
	public House chooseVictim(List<Building> passedBuildings, List<Double> calculationValues) throws Exception {		
		// If haven't passed any buildings cannot look for suitable target
		if (passedBuildings==null || passedBuildings.size()<1) {
			Outputter.debugln("VictimChooser: passed no buildings", Outputter.DEBUG_TYPES.BURGLARY);
			return null;
		}
		// Check that the passed buildings aren't exactly the same as last iteration. If they are then
		// assume that they are still unsuitable.

		// TODO XXXX *** VictimChooser not ignoring previously passed buildings. ***
		// Outputter.describeln("*** VictimChooser not ignoring previously passed buildings. ***");

		//		else if (passedBuildings.equals(this.previousBuildings)) { 
		//			Outputter.debugln("VictimChooser: passed same buildings as last iteration, not checking" +
		//					"suitability again", Outputter.DEBUG_TYPES.BURGLARY);
		//			return null;
		//
		//		}

		// Only interested in Houses, remove all other buildings from the list
		List<House> houses = new ArrayList<House>();
		Iterator<Building> buildingIt = passedBuildings.iterator(); Building b;
		while (buildingIt.hasNext()) {
			b = buildingIt.next(); 
			if (b instanceof House) {
				House h = (House) b;
				houses.add(h);
			}
		}
		if (houses==null || houses.size()<1) {
			Outputter.debugln("VictimChooser: passed no houses", Outputter.DEBUG_TYPES.BURGLARY);
			return null;
		}		
		//		System.out.println("VictimChooser: passed houses: "+(houses == null ? "null" : houses.toString()));

		//		int numHouses = houses.size();
		//		double[] suitabilities = new double[numHouses];
		double motiveIntensity = this.burglar.getActionGuidingMotive().getIntensity();
		House h; Community c; Sociotype s;
		for (int i=0; i<houses.size(); i++) {
			h = houses.get(i); c = h.getCommunity(); s= c.getSociotype();
			if (!h.equals(this.burglar.getHome())) {
				double ce = c.getCollectiveEfficacy();
				//			double tv = c.getTrafficVolume(GlobalVars.time);
				double occ = s.getOccupancy(GlobalVars.time);
				// TODO XXXX should this be -accessibility, because high accessibility is good for burglar?
				double acc = h.getAccessibility(); 
				double vis = h.getVisibility();
				double sec = h.getSecurity();
				double tv = h.getTrafficVolume(GlobalVars.time);
				double ce_w = getWeight(GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER.CE_W);
				double tv_w = getWeight(GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER.TV_W);
				double occ_w = getWeight(GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER.OCC_W);
				double acc_w = getWeight(GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER.ACC_W);
				double vis_w = getWeight(GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER.VIS_W);
				double sec_w = getWeight(GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER.SEC_W);
				// See BurglarAgents.docx for definition of suitability. Low suitability value here means house is
				// very suitable for burglary.
				double suitability =
					( ce*ce_w + tv*tv_w + occ*occ_w + acc*acc_w + vis*vis_w + sec*sec_w ) /
					( ce_w + tv_w + occ_w + acc_w + vis_w + sec_w );

				// Do *not* burgle if suitability > motive intensity
				if (motiveIntensity > suitability ) { 
					// House is suitable, now see if a burglary is going to occur (include random component, greater
					// probability the greater the difference between suitability and motive intensity)
					double difference = motiveIntensity - suitability;
					double prob = Math.pow(difference, 3); // exponential probability
					if (prob > RandomHelper.nextDouble()) {
						Outputter.debugln(
								"VictimChooser: found suitable house: "+h.toString()+
								". Probability: "+prob+". Individual components of suitability:"+
								"\n\tce: "+ce+" w: "+getWeight(GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER.CE_W)+
								"\n\ttv: "+tv+" w: "+getWeight(GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER.TV_W)+
								"\n\tocc: "+occ+" w: "+getWeight(GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER.OCC_W)+
								"\n\tacc: "+acc+" w: "+getWeight(GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER.ACC_W)+
								"\n\tvis: "+vis+" w: "+getWeight(GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER.VIS_W)+
								"\n\tsec: "+sec+" w: "+ getWeight(GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER.SEC_W)+
								"\n\tsuitability: "+suitability+
								"\n\tmotive intensity: "+motiveIntensity+
								"\n\tprobability difference: "+difference, Outputter.DEBUG_TYPES.BURGLARY);
						if (calculationValues != null) {
							Outputter.debugln("VictimChooser.chooseVictim() saving values used in the burglary" +
									"calculation to the given array.", Outputter.DEBUG_TYPES.BURGLARY); 
							this.addValuesToArray(calculationValues, ce, occ, acc, vis, sec, tv, ce_w, tv_w,
									occ_w, acc_w, vis_w, sec_w, suitability, motiveIntensity, difference, prob);
							//						System.out.println("VICTIM CHOOSER CalcVals: "+calculationValues.toString());
						}
						return h;
					} // if prob > random
					else {
						Outputter.debugln("House "+h.toString()+" not suitable (probability "+prob+").", 
								Outputter.DEBUG_TYPES.BURGLARY);
					}
				} // if motive intensity > suitability
			} // if house != agent's home
			else {
				Outputter.debugln("VictimChooser.chooseVictim() not choosing burglar's ("+this.burglar.toString()+
						") house ("+h.toString()+")to burgle in.", Outputter.DEBUG_TYPES.BURGLARY);
			}
		} // for
		// No suitable building found
		this.previousBuildings = passedBuildings; // Remember these buildings in case they don't change on the next iteratio
		return null;
	}

	/** Convenience function to put the given values into a double array */
	private void addValuesToArray(List<Double> calculationValues, double ce,
			double occ, double acc, double vis, double sec, double tv,
			double ce_w, double tv_w, double occ_w, double acc_w, double vis_w,
			double sec_w, double suitability, double motiveIntensity,
			double difference, double prob) {
		calculationValues.add(ce); calculationValues.add(occ); calculationValues.add(acc); calculationValues.add(vis); calculationValues.add(sec); calculationValues.add(tv);
		calculationValues.add(ce_w); calculationValues.add(tv_w); calculationValues.add(occ_w); calculationValues.add(acc_w); calculationValues.add(vis_w);
		calculationValues.add(sec_w); calculationValues.add(suitability); calculationValues.add(motiveIntensity); calculationValues.add(difference); 
		calculationValues.add(prob);
	}


	/**
	 * Get the value of the given weight. If no value can be found (as will be the case when a default
	 * VictimChooser is used which doesn't take any individual weights) the function returns 1.0.
	 * @param weight The weight to return, all possible weights are defined in GlobalVars.BURGLARY_WEIGHTS
	 * @return The value of the weight or 1 if this TargetChooser hasn't been given a value for the weight
	 */
	private double getWeight(GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER weight) {
		if (this.weightMap==null) // No weight map has been defined, return 1.0
			return 1.0;
		Double val = null;
		val = this.weightMap.get(weight);
		if (val==null)
			return 1.0;
		return val;
	}

	/** Prints the weight map for this VictimChooser */
	public String toString() {
		String out = "VictimChooser weights: ";
		if (this.weightMap==null || this.weightMap.size()==0) {
			return "VictimChooser with no weights";
		}
		else {
			for (GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER w:weightMap.keySet()) {
				out+="["+w.toString()+" : "+this.weightMap.get(w)+"] ";
			}
			return out;
		}
	}


}
