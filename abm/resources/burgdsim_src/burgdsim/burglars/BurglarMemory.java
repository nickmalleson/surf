package burgdsim.burglars;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import burgdsim.environment.Community;
import burgdsim.environment.buildings.Building;
import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;


/**
 * Implementation of agent's memory and cognitive map. Will store communities and buildings passed,
 * along with the number of times they have been visited and any successful burglaries. Will use both
 * Map and List implementations to  store the memory, lists are better when the cognitive map is small,
 * but when it grows (approx over 50 buildings) map becomes more efficient. Have options to use list
 * or map exclusively depending on which is better (see GlobalVars.MEMORY_IMPLEMENTATION).
 * <p>
 * This class could be made neater using some generic methods (e.g. a single addToMemory(T ob, Class<T> c)) but
 * then it is less efficient because lots of 'if' statements to work out what the type of object is and how
 * it should be stored. Instead hard-code objects directly, e.g. addToBuildingMemory() and addToCommunityMemory().
 * 
 * @author Nick Malleson
 */
public class BurglarMemory {
	
	private Burglar burglar;
	
	private List<Building> knownBuildings;
	private List<Integer> buildingVisits;
	private List<Integer> buildingBurglaries;
	private List<Community> knownCommunities;
	private List<Integer> communityVisits;
	private List<Integer> communityBurglaries;
	
	// Used so calling objects can declare if they want to find out about the number of visits to a
	// building/community or the number of successful burglaries committed there.
	public static final int VISITS_INDEX = 0, BURGLARIES_INDEX = 1;	
	
	// Can use Maps as a replacement to Lists, they are more efficient when the number of objects stored
	// approaches 40 (see graph in model_dev chapter). The Info class stores all the required information
	// (number of visits, number of crimes etc).
	private Map<Building, List<Integer>> buildingMap;
	private Map<Community, List<Integer>> communitiesMap;
	
	private boolean useList; 	// If true use a list, if false us a map
	
	public BurglarMemory(Burglar b) {
		this.burglar = b;
		if (GlobalVars.MEMORY_IMPLEMENTATIONS.getImplementation().equals(GlobalVars.MEMORY_IMPLEMENTATIONS.LIST)) {
			this.useList = true;
			initialiseLists();
		}
		else if (GlobalVars.MEMORY_IMPLEMENTATIONS.getImplementation().equals(GlobalVars.MEMORY_IMPLEMENTATIONS.MAP)) {
			this.useList = false;
			this.initialiseMaps(); // Initialise maps, but don't transfer memory from lists
		}
		else {
			Outputter.errorln("BurglarMemory() error: uncrecognised type of memory implementation: "+
					GlobalVars.MEMORY_IMPLEMENTATIONS.getImplementation().toString());
		}
		this.addFirstToMemory();
		Outputter.debugln("BurglaryMemory() for burglar "+this.burglar.toString()+" using "+(useList ? "lists" : "maps")+".",
				Outputter.DEBUG_TYPES.AWARENESS_SPACE);
	}

	/**
	 * Add all the given objects to the memory, assuming that the memory knows what to do with
	 * the type of the objects.
	 * @param <T>
	 * @param objects
	 * @param objectClass
	 */
	@SuppressWarnings("unchecked")
	public <T> void addToMemory(Collection<T> objects, Class<T> objectClass) {
		if(objectClass.isAssignableFrom(Building.class)) { // getting buildings from memory
			this.addToBuildingMemory((Collection<Building>)objects);
		}		
		else if (objectClass.isAssignableFrom(Community.class)) { // getting communities from memory
			this.addToCommunityMemory((Collection<Community>) objects);
		}		
		else {
			Outputter.errorln("BurglarMemory.addToMemory() error (for agent '"+this.burglar+"') not storing objects "+
					"of type "+objectClass.toString()+" in the agent's memory");
		}
	}
	
	/**
	 * Get all the objects of the given class from this BurglarMemory. Will return the objects stored as
	 * keys in a Map. The associated values are List objects which hold, for each object, the number of
	 * visits (times the agent has passed it) and the number of burglaries committed there (the list indexes
	 * are defined by the static final ints VISITS and BURGLARIES).
	 * @param <T> The type of objects to return
	 * @param clazz The Class of the objects to return
	 * @return a list of objects of the given type which are stored in the memory. Return null if there are no
	 * objects of that type in the memory and return null with an error if this BurglaryMemory doesn't know
	 * what to do with objects of the passed type.
	 */
	@SuppressWarnings("unchecked")
	public <T> Map<T, List<Integer>> getFromMemory(Class<T> clazz) {
		if(clazz.isAssignableFrom(Building.class)) { // getting buildings from memory
			if (useList) {
				Map<T, List<Integer>> map = new Hashtable<T, List<Integer>>(knownBuildings.size());
				for (int i=0; i<knownBuildings.size(); i++) {
					List<Integer> list = new ArrayList<Integer>();
					list.add(buildingVisits.get(i));
					list.add(buildingBurglaries.get(i));
					map.put((T) knownBuildings.get(i), list);
				}
				return map;			
			}
			else {
				return (Map<T, List<Integer>>) this.buildingMap;
			}
		}
		else if (clazz.isAssignableFrom(Community.class)) { // getting communities from memor
			if (useList) {				
				Map<T, List<Integer>> map = new Hashtable<T, List<Integer>>(knownCommunities.size());
				for (int i=0; i<knownCommunities.size(); i++) {
					List<Integer> list = new ArrayList<Integer>();
					list.add(communityVisits.get(i));
					list.add(communityBurglaries.get(i));
					map.put((T) knownCommunities.get(i), list);
				}
				return map;
			}
			else {
				return (Map<T, List<Integer>>) this.communitiesMap;
			}
		}
		
		else {
			Outputter.errorln("BurglarMemory.getFromMemory() error (for agent '"+this.burglar+"') not storing objects "+
					"of type "+clazz.toString()+" in the agent's memory");
		}		
		return null;
	}

	
	/**
	 * Used to add a burglary event to this BurglarMemory.
	 * @param building The Building which the agent has burgled
	 * @param community The Community which the building is part of.
	 * @throws Exception 
	 */
	public void committedBurglary(Building building, Community community) throws Exception {
		if (building==null || community==null) {
			String error = "BurglarMemory.committedBurglary() error. Either the given building or community " +
			"is null. Building: "+(building==null ? "null" : building.toString())+
			" Community: "+(community==null ? "null" : community.toString())+"\n Ending Run";
			Outputter.errorln(error);
			throw new Exception(error);
		}
		if (useList) {
			int index = knownBuildings.indexOf(building); // Find the given building from the known buildings 
			if (index == -1) {
				Outputter.errorln("BurglarMemory.committedBurglary() error. Burglar '"+this.burglar.toString()+"' " +
						"doesn't have the given building ('"+building.toString()+"') in its awareness space");
			}
			else {
				buildingBurglaries.set(index, buildingBurglaries.get(index)+1);
			}
			// Do same for the community
			index = knownCommunities.indexOf(community); // Find the given building from the known buildings 
			if (index == -1) {
				Outputter.errorln("BurglarMemory.committedBurglary() error. Burglar '"+this.burglar.toString()+"' " +
						"doesn't have the given community ('"+community.toString()+"') in its awareness space");
			}
			else {
				communityBurglaries.set(index, communityBurglaries.get(index)+1);
			}
		} // if useList
		else {
			// Get the current number of burglaries from the maps and increment
			List<Integer> burglaries;
			burglaries = this.buildingMap.get(building); // Buildings
			if (burglaries == null)
				this.fixBuilding(building); // Horrible hack, see fixBuilding()
			else
				burglaries.set(BURGLARIES_INDEX, burglaries.get(BURGLARIES_INDEX)+1);
			
			burglaries = this.communitiesMap.get(community); // Communities
			if (burglaries == null)
				this.fixCommunity(community);
			else
				burglaries.set(BURGLARIES_INDEX, burglaries.get(BURGLARIES_INDEX)+1);	
			
		}
	}
	
	/* Horrible hack, for some reason sometimes objects aren't added to the memory properly which can cause 
	 * null pointer after a burglar, this stops that happening because I can't be bothered to track the 
	 * problem properly. */
	private void fixBuilding(Building b) {
		Outputter.errorln("BurglarMemory WARNING: The building just burgled ("+b.toString()+") wasn't in the " +
				"burglar's memory but it's been manually added now so this shouldn't cause any errors (but it's " +
				"a problem I should fix!).");
		List<Integer> l = new ArrayList<Integer>();
		l.add(0); l.add(0); l.set(VISITS_INDEX, 1); l.set(BURGLARIES_INDEX, 1);
		this.buildingMap.put(b, l);
	}	
	private void fixCommunity(Community c) {
		Outputter.errorln("BurglarMemory WARNING: The community just burgled ("+c.toString()+") wasn't in the " +
				"burglar's memory but it's been manually added now so this shouldn't cause any errors (but it's" +
				"a problem I should fix!).");
		List<Integer> l = new ArrayList<Integer>();
		l.add(0); l.add(0); l.set(VISITS_INDEX, 1); l.set(BURGLARIES_INDEX, 1);
		this.communitiesMap.put(c, l);
	}
	
	
	private void addToBuildingMemory(Collection<Building> buildings) {
		if (useList) {
			int index;
			for (Building b:buildings) {
				index = knownBuildings.indexOf(b); 
				if (index == -1) {
					knownBuildings.add(b);
					buildingVisits.add(1);
					buildingBurglaries.add(0);
				}
				else {
					buildingVisits.set(index, buildingVisits.get(index)+1);
				}
			} // for Buildings
		} // if useList
		else {
			for (Building b:buildings) {
				List<Integer> bm = this.buildingMap.get(b); // Look for information about this building
				if (bm==null) { // None found, create a new list of burglaries and visits and store
					List<Integer> newL = new ArrayList<Integer>();
					newL.add(0); newL.add(0); 
					newL.set(VISITS_INDEX, 1);
					this.buildingMap.put(b, newL);
				}
				else { // Already have information about this building, increment the number of visits
					bm.set(VISITS_INDEX, bm.get(VISITS_INDEX)+1);
				}
			}
		}
	}
	
	private void addToCommunityMemory(Collection<Community> communities) {
		if (useList) {
			int index;
			for (Community c:communities) {
				index = knownCommunities.indexOf(c); 
				if (index == -1) {
					knownCommunities.add(c);
					communityVisits.add(1);
					communityBurglaries.add(0);
				}
				else {
					communityVisits.set(index, communityVisits.get(index)+1);
				}
			} // for Buildings
		} // if useLIst
		else {
			for (Community c:communities) {
				List<Integer> cm = this.communitiesMap.get(c); // Look for information about this community
				if (cm==null) { // None found, create a new list of burglaries and visits and store
					List<Integer> newL = new ArrayList<Integer>();
					newL.add(0); newL.add(0);
					newL.set(VISITS_INDEX, 1);
					this.communitiesMap.put(c, newL);
				}
				else { // Already have information about this community, increment the number of visits
					cm.set(VISITS_INDEX, cm.get(VISITS_INDEX)+1);
				}
			} // for communities
		} // else useList
	}

	/**
	 * Initialise all the lists that will be used.
	 */
	private void initialiseLists() {
		this.knownBuildings = new ArrayList<Building>();
		this.buildingVisits = new ArrayList<Integer>();
		this.buildingBurglaries = new ArrayList<Integer>();
		this.knownCommunities = new ArrayList<Community>();
		this.communityVisits = new ArrayList<Integer>();
		this.communityBurglaries = new ArrayList<Integer>();
	}
	
	/**
	 * Initialise the maps required for this BurglarMemory.
	 */
	private void initialiseMaps() {

		this.buildingMap = new Hashtable<Building, List<Integer>>();
		this.communitiesMap = new Hashtable<Community, List<Integer>>();
	}
	
	/**
	 * Add the first elements to the memory (the building and community that the agent is currently
	 * located at) because otherwise a newly created burglar will have an empty memory which can lead
	 * to NullPointerExceptions.  
	 */
	private void addFirstToMemory() {
		Collection<Building> c1 = new ArrayList<Building>();
		c1.add(GlobalVars.BUILDING_ENVIRONMENT.getObjectAt(
				Building.class,	GlobalVars.BURGLAR_ENVIRONMENT.getCoords(this.burglar)));
		this.addToBuildingMemory(c1);
		
		Collection<Community> c2 = new ArrayList<Community>();
		c2.add(GlobalVars.COMMUNITY_ENVIRONMENT.getObjectAt(
				Community.class, GlobalVars.BURGLAR_ENVIRONMENT.getCoords(this.burglar)));
		this.addToCommunityMemory(c2);		
	}
} // BurglarMemory class

///**
// * Class used to store building/community info (number of visits, number of burglaries, last time visitted etc).
// * @author Nick Malleson
// *
// */
//class Info{
//	
////	public T obj ; // The object that this Info is storing data for
//	/** The object's information. Different List indices store different information, as specified by
//	 * the static variables BurglarMemory.VISITS and BurglarMemory.BURGLARIES.*/
//	public List<Integer> information;
//	
//	public Info() {
//		this.information = new ArrayList<Integer>(2); // Currently only store two pieces of info: visits or burglaries
//	}
//	
//	/**
//	 * Add information about the object (i.e. a new burglary or a new visit).
//	 * @param index The types of info to store (BurglarMemory.VISIT or BurglarMemory.BURGLARY).
//	 */
//	public void addInfo(int index) {
//		int val = this.information.get(index);
//		this.information.set(index, val++);
//	}
//	
//	public List<Integer> getInfo() {
//		return this.information;
//	}
//	
//}
