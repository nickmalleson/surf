package burgdsim.burglars;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;

import repast.simphony.random.RandomHelper;

import burgdsim.environment.Community;
import burgdsim.environment.Sociotype;
import burgdsim.environment.buildings.Building;
import burgdsim.environment.buildings.Social;
import burgdsim.environment.buildings.Workplace;
import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;
import burgdsim.pecs.StateVariable;


/**
 * Class of burglar used in the Vancouver SkyTrain case study. The burglar has no work and chooses social
 * locations by distance and similarity to their home location.
 * @author Nick Malleson
 *
 */
public class VancouverSkyTrainBurglar extends Burglar {

	// Map of communities with their similarity to agent's home (ordered by most similar)
	private LinkedHashMap<Community, Double> socialCommunities;
	
	// Save all communities with at least one social place in them for efficiency
	private static List<Community> allSocialCommunities;
	
	private Building work; // Just need this so null values don't cause problems, these agents cannot work.
	
	public VancouverSkyTrainBurglar(List<StateVariable> stateVariables) {
		super(stateVariables);
		GlobalVars.CACHES.add(this);
	}

	/**
	 * Override default action to return a sociotype as follows:
	 * <ol>
	 * <li>Rank all communities in order of similarity to burglar's home location (ignoring any communities which
	 * don't have social locations in them)</li>
	 * <li>Use roulette wheel seletion to choose community (more similar communities have higher probability</li>
	 * <li>Pick a random social location from within the community</li>
	 * </ol>
	 * The class also maintains a static list of all communities with social locations in them (shared by all
	 * VancouverSkyTrainBurglars) and also caches the similarity of all communities for this burglar (for
	 * efficiency).
	 */
	@Override
	public Building getSocial() {
		// See if need to populate list of communities that have a social place in them (only do once for efficiency)
		if (allSocialCommunities == null) {
			allSocialCommunities = new ArrayList<Community>();
			for (Community c:GlobalVars.COMMUNITY_ENVIRONMENT.getAllObjects(Community.class)) {
				// Check the community has a social building in it.
				for (Building b:c.getBuildings()) {
					if (Social.class.isAssignableFrom(b.getClass())) {
						allSocialCommunities.add(c);
						break;
					}
				}
			}
			System.out.println("Populated all social communities list ("+allSocialCommunities.size()+") communities).");
		} // if allSocialCommunities == null

		// See if need to fill in the list of all communities for this burglar
		if (this.socialCommunities == null) {
			// Now list all social communities in ascending order, along with their RWS proportions,
			// going via a temporary TreeMap (guarantees ascending order)
			TreeMap<Double, Community> temp = new TreeMap<Double, Community>();
			Sociotype homeSociotype= this.getHome().getCommunity().getSociotype();
			double totalSimilarity = 0; double similarity = 0;	// Required for RWS calculation
			for (Community c:allSocialCommunities) {
				similarity = c.getSociotype().compare(homeSociotype); 
				temp.put(similarity, c);
				totalSimilarity+=similarity;
			}
			this.socialCommunities = new LinkedHashMap<Community, Double>(allSocialCommunities.size());
			for (Double d:temp.keySet()) {
				// Add community and its proportion of the total similarity to the map
				this.socialCommunities.put(temp.get(d), d/totalSimilarity);
			}			
		} // if socialCommunities == null
		// Do RWS to pick community
		double rand = RandomHelper.nextDouble(); // (Total RWS proportions will equal 1)
		double val = 0;
		for (Community c:this.socialCommunities.keySet()) {
			val += this.socialCommunities.get(c);
			if (val >= rand) {
				Social s = c.getRandomBuilding(Social.class);
//				System.out.println("returning building "+s.toString()+" from community "+c.toString()+
//						" with similarity "+socialCommunities.get(c)+" and rws: "+val+" (rand "+rand+")");
				return s;
			}
		}
		Outputter.errorln("VancouverSkyTrainBurglar.getSocial() error: could't find a social location (or " +
				"possibly a suitable community. Returning null, this will cause problems!");
		return null;
	}
	
	/**
	 *  Agents for this scenario cannot work, ever.
	 *  @return false.
	 */
	@Override
	public boolean canWork() {
		return false;
	}

	/**
	 * As these agents cannot work this doesn't need to return anything. Will just return a random
	 * work building so that nulls don't cause problems later (usually with outputting information).
	 * @return a work building chosen at random. (NOTE: the building is cached so the same building 
	 * will be returned each time this function is called.  
	 */
	@Override
	public Building getWork() {
		if (this.work==null) {
			this.work = GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(Workplace.class);
		}
		return this.work;
	}

	@Override
	public void clearCaches() {
		super.clearCaches();
		allSocialCommunities.clear();		
		allSocialCommunities = null;
	}	
}