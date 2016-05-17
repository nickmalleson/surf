package burgdsim.burglary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;


import repast.simphony.random.RandomHelper;

import burgdsim.burglars.Burglar;
import burgdsim.burglars.BurglarMemory;
import burgdsim.environment.Community;
import burgdsim.environment.EnvironmentFactory;
import burgdsim.main.Functions;
import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;
import burgdsim.main.GlobalVars.BURGLARY_WEIGHTS.TARGET_CHOOSER;

/**
 * Part of the burglary template. Will choose a target for the burglar to begin their search from.
 * The choice of target depends on the type of burglar (some burglars will prefer to search in
 * different areas), this is reflected through a variable which maps weight names to their values
 * (information about weights in BurglarAgents.docx).
 * @author Nick Malleson
 */
public class TargetChooser implements ITargetChooser {

	protected Burglar burglar;
	// Maps weight names to their values, used in equation to calculate target
	protected Map<GlobalVars.BURGLARY_WEIGHTS.TARGET_CHOOSER, Double> weightMap ;

	/**
	 * Default implementation, just takes the relevant burglar as an argument. The weightMap
	 * is empty so that when a the agent's cognitive map is searched for a target none of the
	 * relevant weights will be found and they will be given 'default' values of 0.5.
	 * @param burglar
	 */
	public TargetChooser(Burglar burglar) {
		this.burglar = burglar;
		this.weightMap = new HashMap<GlobalVars.BURGLARY_WEIGHTS.TARGET_CHOOSER, Double>();

	}

	public TargetChooser(Burglar burglar, Map<GlobalVars.BURGLARY_WEIGHTS.TARGET_CHOOSER, Double> weightMap) {
		this.burglar = burglar;
		this.weightMap = weightMap;
	}

	public Community chooseTarget() {
			/* Get all the communities from the burglar's memory. Returned as a map of communities with a list
			 * that contains the number of visits and the number of burglaries. These are required for the overall
			 * attractiveness calculation.
			 * Need to run through each community and collect all the parameters which will make up its overall
			 * attractiveness, remembering the min/max values of each for normalisation.
			 * Then run through the communities again, calculate overall attractiveness and store, sorted, in a
			 * list. To do this I maintain a list of communities and a 2D list of doubles which stores the community
			 * index and the parameters which make up the overall attractiveness. Then it is easy to run through the
			 * doubles list calculating overall attractiveness (now that min/max parameter values are known) and then
			 * sort it on the attractiveness. Once the doubles list has been sorted (very quick because it only 
			 * contains primitive types) can use the element which stores the original community index to identify
			 * the associated community in the communities list. Sounds complicated but just want a list of communities
			 * sorted by overall attractiveness!*/

			Map<Community, List<Integer>> memory = this.burglar.getMemory().getFromMemory(Community.class);
			
			
			int numCommunities = memory.keySet().size();
			Community[] communities = memory.keySet().toArray(new Community[numCommunities]); 		// Store the communities
			// Community params: { { overallAttract, index, dist, attract, socialDiff, prevSucc }, { .. } ... { .. } }
			double[][] communityParams = new double[numCommunities][6];
			double totalAttract = 0;	// Needed to pick most attractive community later

			// Need to store max/min values for normalisation (index 0 is min value, 1 is max)
			double[] distMM = new double[2]; distMM[0] = Double.MAX_VALUE; distMM[1] = 0;
			double[] attractMM = new double[2]; attractMM[0] = Double.MAX_VALUE; attractMM[1] = 0;
			double[] socialDiffMM = new double[2]; socialDiffMM[0] = Double.MAX_VALUE; socialDiffMM[1] = 0;
			double[] prevSuccMM = new double[2]; prevSuccMM[0] = Double.MAX_VALUE; prevSuccMM[1] = 0;

			// Find the community the agent is currently in, this will affect the distance calculation.
			Community currentCommunity = GlobalVars.COMMUNITY_ENVIRONMENT.getObjectAt(Community.class, GlobalVars.BURGLAR_ENVIRONMENT.getCoords(burglar));

			// Loop over every Community in the memory to calucate min/max values and remember each parameter
			// value for each community so they don't need to be recalculated when calculating overall attractivenss 
			for (int i=0; i<communities.length; i++) { 
				Community c = communities[i];
				communityParams[i][1] = i; // Second element stores the original index, used to link to communities list.
				double dist; // The distance to c, need to check if the agent is in c at the moment or not
				if (c.equals(currentCommunity)) {
					dist = 1 / c.getAverageDistance(); // The average distance to every point in the area
				}
				else {
					// Note, for efficiency use distance from communities, not actual burglar coords (if possible)
					dist = 1 / EnvironmentFactory.getDistance(
							this.burglar, 
							(currentCommunity == null ? 
								GlobalVars.BURGLAR_ENVIRONMENT.getCoords(this.burglar) :
								currentCommunity.getCoords() ),
							c.getCoords());
				}
				checkMinMax(dist, distMM);  // Remember the min/max values for distance
				communityParams[i][2] = dist; // Remember this community's distance value (so not recalculated again)
			
				double attract = c.getSociotype().getAttractiveness();
				checkMinMax(attract, attractMM);
				communityParams[i][3] = attract;

				double socialDiff = c.getSociotype().compare(this.burglar.getHome().getCommunity().getSociotype());
				checkMinMax(socialDiff, socialDiffMM);
				communityParams[i][4] = socialDiff;

				double prevSucc = memory.get(c).get(BurglarMemory.BURGLARIES_INDEX); // Num. successful burglaries
				checkMinMax(prevSucc, prevSuccMM);
				communityParams[i][5] = prevSucc;

			}

			// Now loop again and calculate overall attractiveness as the individual parameters can be normalised
			for (int i=0; i<communities.length; i++) {
				double distN = Functions.normalise(communityParams[i][2], distMM);
				double attractN = Functions.normalise(communityParams[i][3], attractMM);
				// Have to reverse socialDiff because 1 = very different (should lower attractiveness).
				double socialDiffN = Functions.normalise(communityParams[i][4], socialDiffMM);
				double prevSuccN = Functions.normalise(communityParams[i][5], prevSuccMM);
				double overallAttract =  
					( getWeight(TARGET_CHOOSER.DIST_W) * distN ) +
					( getWeight(TARGET_CHOOSER.ATTRACT_W) * attractN ) +
					( getWeight(TARGET_CHOOSER.SOCIALDIFF_W) * socialDiffN ) +
					( getWeight(TARGET_CHOOSER.PREVSUCC_W) * prevSuccN );
				communityParams[i][0] = overallAttract;
				totalAttract += overallAttract; // Used for roulette wheel selection
//				System.out.println(communities[i].toString()+"\n"+
//						"\tdistN: "+distN+", "+distMM[0]+ " -> "+ distMM[1]+
//						"\t(distance: "+EnvironmentFactory.getDistance(this.burglar, (currentCommunity == null ? GlobalVars.BURGLAR_ENVIRONMENT.getCoords(this.burglar) :currentCommunity.getCoords() ),communities[i].getCoords())+")"+
//						"\n\tattractN: "+attractN+", "+attractMM[0]+ " -> "+ attractMM[1]+
//						"\n\tsocialDiffN: "+socialDiffN+", "+socialDiffMM[0]+ " -> "+ socialDiffMM[1]+
//						"\n\tprevSuccN: " + prevSuccN+", "+prevSuccMM[0]+ " -> "+ prevSuccMM[1]+
//						"\n\tOverall attract: "+overallAttract);
			}

			// Sort the list of community parameters on the overall attractiveness from low to high
			double[][] sortedCommunityParams = Functions.sort(communityParams, false);			
//			System.out.println("Communities in order of attractiveness: ");
//			for (int i=0; i<sortedCommunityParams.length; i++) {
//				System.out.println("\t"+communities[(int)(sortedCommunityParams[i][1])].toString()+":"+sortedCommunityParams[i][0]);
//			}

			// Now use RWS to find a community
			double roulette = RandomHelper.nextDoubleFromTo(0, totalAttract);
			//		System.out.println("roulette val: "+roulette);
			double currentAttract = 0;
			for (int i=0; i<numCommunities; i++) {
				currentAttract += sortedCommunityParams[i][0]; // index 0 stores the overall attractivenss
				if (roulette < currentAttract) {
					Community c = communities[(int)(sortedCommunityParams[i][1])]; // Remember this element stores original index 
					Outputter.debugln("TargetChooser.chooseTarget() for burglar '"+this.burglar.toString()+"' found "+
							"most attractive community to travel to: "+c, Outputter.DEBUG_TYPES.BURGLARY);
//					System.out.println("Chosen communiy is: "+c.toString()+" ("+i+"/"+sortedCommunityParams.length+") with overall attractiveness: "+sortedCommunityParams[i][0]);
					return c;
				}
			}
			Outputter.errorln("TargetChooser.chooseTarget() error. Shouldn't have got here, for some reason no " +
					"community was chosen as the most attractive for burglar "+this.burglar.toString()+"\n" +
			"Here's some information about the communities which were examined:");
			for (int i=0; i<sortedCommunityParams.length; i++) {
				Outputter.errorln("Community: "+communities[(int)sortedCommunityParams[i][1]]+
						"\t overallAttract: "+sortedCommunityParams[i][0]+
						"\n\t dist: "+sortedCommunityParams[i][2]+", "+distMM[0]+"->"+distMM[1]+
						"\n\t attract: "+sortedCommunityParams[i][3]+ ", "+attractMM[0]+"->"+attractMM[1]+
						"\n\t socialDiff: "+sortedCommunityParams[i][4]+ ", "+socialDiffMM[0]+"->"+socialDiffMM[1]+
						"\n\t prevSucc: "+sortedCommunityParams[i][5]+ ", "+prevSuccMM[0]+"->"+prevSuccMM[1]);
			}
			return null;
	}


	/**
	 * Get the value of the given weight. If no value can be found (as will be the case when a default
	 * TargetChooser is used which doesn't take any individual weights) the function returns 1.0.
	 * @param weight The weight to return, all possible weights are defined in GlobalVars.BURGLARY_WEIGHTS
	 * @return The value of the weight or 1 if this TargetChooser hasn't been given a value for the weight
	 */
	private double getWeight(GlobalVars.BURGLARY_WEIGHTS.TARGET_CHOOSER weight) {
		Double val = null;
		val = this.weightMap.get(weight);
		if (val==null)
			return 1.0;
		return val;
	}

	/**
	 * Checks if the given value is smaller than the element at index 0 in the given array or larger than
	 * element 1 and, if so, replace the old values in the array with value. Can be called repeatedly to
	 * store min/max values. 
	 * @param value
	 * @param minMaxArray
	 */
	private void checkMinMax(double value, double[] minMaxArray) {
		if (value < minMaxArray[0])
			minMaxArray[0] = value;
		if (value > minMaxArray[1])
			minMaxArray[1] = value;
	}
}
