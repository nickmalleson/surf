package burgdsim.burglars;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import repast.simphony.random.RandomHelper;

import burgdsim.burglary.Burglary;
import burgdsim.burglary.TargetChooser;
import burgdsim.burglary.VictimChooser;
import burgdsim.environment.Cacheable;
import burgdsim.environment.Community;
import burgdsim.environment.Sociotype;
import burgdsim.environment.buildings.Building;
import burgdsim.environment.buildings.DrugDealer;
import burgdsim.environment.buildings.House;
import burgdsim.environment.buildings.Social;
import burgdsim.environment.buildings.Workplace;
import burgdsim.main.ContextCreator;
import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;
import burgdsim.pecs.Action;
import burgdsim.pecs.DoNothingV;
import burgdsim.pecs.DrugsV;
import burgdsim.pecs.Motive;
import burgdsim.pecs.SleepV;
import burgdsim.pecs.SocialV;
import burgdsim.pecs.StateVariable;


/**
 * Class of burglar used in the Base scenario. The burglar has no work and chooses social
 * locations by distance and similarity to their home location.
 * @author Nick Malleson
 *
 */
public class BaseBurglar extends Burglar implements Cacheable {

	// Map of communities with their similarity to agent's home (will be ordered by most similar)
	private LinkedHashMap<Community, Double> socialCommunities;
	
	// Save all communities with at least one social place in them for efficiency
	private static List<Community> allSocialCommunities;
	
	private Building work; // Just need this so null values don't cause problems, these agents cannot work.
	
	public BaseBurglar(List<StateVariable> stateVariables) {
		super(stateVariables);
		GlobalVars.CACHES.add(this);
	}
	
	/** Returns a personalised Burglary action, one which gives a high weight to Distance_W parameter. 
	 * This is a hack used in the gis_env_tests scenario. */
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Action> T getSpecificAction(Class<T> actionClass, Motive motive) {
		T obj = null;
		if (actionClass.isAssignableFrom(Burglary.class)) {
			// See if the relevant varables have been set properly.
			double val = GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.GIS_SENSITIVITY_TEST_DISTW;
			if (val != -1.0) {
				if (GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.GIS_SENSITIVITY_TEST_TYPE == 0) {
					Outputter.errorln("BaseBurglar.getSpecificAction has been told to change the value " +
							"of the Distance_W parameter but the GIS_SENSITIVITY_TEST_TYPE parameter " +
							"hasn't been set so it looks like this isn't a sensitivity test. Should the " +
							"GIS_SENSITIVITY_TEST_DISTW parameter have been set in the first place?");	
				}				
				if (!( val<=1.0 && val>=0.0 )) { // Check value of Dist_W weight is between 1 and 0
					Outputter.errorln("BaseBurglar.getSpecificAction() is expecting a " +
							"GIS_SENSITIVITY_TEST_DISTW parameter value in the range 0-1 but got "+val);
				}
				else { // Set the value of the Dist_W parameter
					// Define a weight map which gives a specific value to the distance weight in 
					// TargetChooser (all other parameters will keep default values).
					Map<GlobalVars.BURGLARY_WEIGHTS.TARGET_CHOOSER, Double> weightMap = 
						new Hashtable<GlobalVars.BURGLARY_WEIGHTS.TARGET_CHOOSER, Double> ();
					weightMap.put(GlobalVars.BURGLARY_WEIGHTS.TARGET_CHOOSER.DIST_W, val);
					TargetChooser tc = new TargetChooser(this, weightMap);
					// Create a new burglary action and set the victim chooser.
					Burglary b = new Burglary(motive);
					b.setTargetChooser(tc);
					obj = (T) b;
					System.out.println("BaseBurglar setting distw weight: "+val);
				}				
			} // if val != -1.0
		} 
		return obj;
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
			Outputter.debugln("Populated all social communities list ("+allSocialCommunities.size()+") communities).",
					Outputter.DEBUG_TYPES.INIT);
		} // if allSocialCommunities == null

		// See if need to fill in the list of all communities for this burglar
		if (this.socialCommunities == null) {
			// Now list all social communities in ascending order, along with their RWS proportions,
			
			// Create a map of all communities along with their similarity
			Map<Community, Double> communitySimilarities = new Hashtable<Community, Double>();
			Sociotype homeSociotype= this.getHome().getCommunity().getSociotype();
			double totalSimilarity = 0; double similarity = 0;	// Required for RWS calculation
			for (Community c:allSocialCommunities) {
				similarity = c.getSociotype().compare(homeSociotype); 
				communitySimilarities.put(c, similarity);
				totalSimilarity+=similarity;
			}
			// Order the communities by their similarity
			Map<Community, Double> orderedCommunitySimilarities = ContextCreator.sortByValue(communitySimilarities);
			// Finally store all the communities and their RWS proportions (this is cached)
			this.socialCommunities = new LinkedHashMap<Community, Double>();
			for (Community c:orderedCommunitySimilarities.keySet()) {
				// Add community and its proportion of the total similarity to the map
				this.socialCommunities.put(c, orderedCommunitySimilarities.get(c)/(double)totalSimilarity);
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
		Outputter.errorln("BaseBurglar.getSocial() error: could't find a social location (or " +
				"possibly a suitable community) for burglar "+this.toString()+". Returning null, this will cause problems!");
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

	public void clearCaches() {
		allSocialCommunities.clear();		
		allSocialCommunities = null;
	}

	/**
	 *  Create the burglar. Normally this is all done in BurglarFactory, but in some cases the BaseBurglar can
	 *  be created by reading from the scenarios file. If this happens then BurglarFactory doesn't know anything
	 *  about the Burglar other than the name (BaseBurglar) so doesn't know which StateVariables to create etc. 
	 */
	public static Burglar createBurglar(House homePlace, Workplace workPlace, Social socialPlace, DrugDealer drugDealerPlace) {
		List<StateVariable> simpleList = new ArrayList<StateVariable>();
		Burglar burglar = new BaseBurglar(simpleList);

		// Agent just drives around.
//		burglar.addToTransportAvailable(GlobalVars.TRANSPORT_PARAMS.CAR);
		
		// Agent has to use public transport
		burglar.addToTransportAvailable(GlobalVars.TRANSPORT_PARAMS.BUS);
		burglar.addToTransportAvailable(GlobalVars.TRANSPORT_PARAMS.TRAIN);
		
		GlobalVars.BURGLAR_ENVIRONMENT.add(burglar);
		burglar.setName("BaseBurglar"+Burglar.burglarNumber++);
		
		// Have to set up their homes etc first because these are required by actions. If these are null then set
		// them randomly.
		House home; Workplace work; Social social; DrugDealer drugDealer;
		home = homePlace == null ? (House)GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(House.class) : homePlace; 
		work = workPlace == null ? (Workplace) GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(Workplace.class) : workPlace;
		social = socialPlace == null ? (Social) GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(Social.class) : socialPlace;
		drugDealer = drugDealerPlace == null ? (DrugDealer) GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(DrugDealer.class) : drugDealerPlace; 
		burglar.setHome(home);
		burglar.setWork(work);
		burglar.setDrugDealer(drugDealer);
		burglar.setSocial(social);

		GlobalVars.BURGLAR_ENVIRONMENT.move(burglar, GlobalVars.BUILDING_ENVIRONMENT.getCoords(home));
		burglar.initMemory();

		StateVariable sleepV = new SleepV(burglar, 0.5);
		simpleList.add(sleepV);
		StateVariable doNothingV = new DoNothingV(burglar);
		simpleList.add(doNothingV);
		StateVariable drugsV = new DrugsV(burglar, 1.0);
		simpleList.add(drugsV);
		StateVariable socialV = new SocialV(burglar, 1.0);
		simpleList.add(socialV);
		
//		System.out.println("BaseBurglar returning a new burglar: "+burglar.getName()+" with following: \n" +
//				"\thome: "+home.toString()+" "+GlobalVars.BUILDING_ENVIRONMENT.getCoords(home).toString()+"\n" +
//				"\twork: "+burglar.getWork().toString()+" "+GlobalVars.BUILDING_ENVIRONMENT.getCoords(burglar.getWork()).toString()+"\n" +
//				"\tsocial: "+burglar.getSocial().toString()+" "+GlobalVars.BUILDING_ENVIRONMENT.getCoords(burglar.getSocial()).toString()+"\n" +
//				"\tdrug: "+burglar.getDrugDealer().toString()+" "+GlobalVars.BUILDING_ENVIRONMENT.getCoords(burglar.getDrugDealer()).toString()+"\n");
		
		return burglar;
	}

	
}