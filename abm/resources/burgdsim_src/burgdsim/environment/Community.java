package burgdsim.environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import burgdsim.environment.buildings.Building;
import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;

import repast.simphony.random.RandomHelper;


public class Community implements FixedGeography, Cacheable {

	private static final long serialVersionUID = 1L;

	private int id;
	private int uniqueID;
	private static int uniqueIDs = 0;
	// All community id's can be used to check they're unique
	private static Map<Integer, ?> comunityIDs;
	private double collectiveEfficacy = 0.5;
	private Sociotype sociotype;
	private Coord coord; // The centre-point of the community, BEWARE: this is not same as the Coord returned by COMMUNITY_ENVIRONMENT.getCoords()
	private List<Building> buildings;	// The buildings contained within this community.
	private double area = -1; // The total area of the community
	private double averageDist = -1; // The average distance from the centre of area to every point in the community

	public Community() {
		if (comunityIDs == null) {
			comunityIDs = new HashMap<Integer, Object>();
		}
		GlobalVars.CACHES.add(this);
		this.buildings = new ArrayList<Building>();
		this.uniqueID = uniqueIDs++;
	}
	
	public Community(Coord c) {
		if (comunityIDs == null) {
			comunityIDs = new HashMap<Integer, Object>();
		}
		GlobalVars.CACHES.add(this);
		this.buildings = new ArrayList<Building>();
		this.uniqueID = uniqueIDs++;
		this.setId(this.uniqueID);
		this.coord = c;
	}

	/** 
	 * Create a new community with the exactly the same values as the given community except the uniqueID
	 * and it's Coordinates. This is required for Grid environment where a single community cannot exist in
	 * more than one cell, so need to create copies with same values but different coordinates.
	 * <p>
	 * Note that Community.equals() compares id numbers so this Community and the given one will
	 * be considered the same.
	 * @param c
	 */
	public Community(Community c, Coord coord) {
		this.uniqueID = uniqueIDs++;
		this.setId(c.id);
		this.collectiveEfficacy = c.collectiveEfficacy;
		this.sociotype = c.sociotype;
		this.coord = coord;
		this.buildings = c.buildings;
		this.area = c.area;
	}

	public int getId(){
		return this.id;
	}

	public void setId(int id) {
		// Check this id hasn't been used by another community (doesn't apply in Grid environment where lots of
		// Community objects have to be created to cover a contiguous community (one object for each grid cell)
		if (!GlobalVars.ENVIRONMENT_TYPE.equals(GlobalVars.ENVIRONMENT_TYPES.GRID) &&  Community.comunityIDs.containsKey(id)) {
			Outputter.errorln("Community.setID() warning: the id '"+id+"' has already been used by another Community");
		}
		else {
			Community.comunityIDs.put(id, null);
		}
		this.id = id;
		//		this.uniqueID = id;
	}

	public Coord getCoords() {
		return this.coord;
	}
	public void setCoords(Coord c) {
		this.coord = c;
	}

	public double getCollectiveEfficacy() {
		return this.collectiveEfficacy;
	}
	public void setCollectiveEfficacy(double collectiveEfficacy) {
		this.collectiveEfficacy = collectiveEfficacy;
	}

	/* These are required so that collectiveEfficacy can be abbreviated in the shapefile) */

	public void setCE(double collectiveEfficacy) {
		this.collectiveEfficacy=collectiveEfficacy;
	}

	public Sociotype getSociotype() {
		return sociotype;
	}
	public void setSociotype(Sociotype sociotype) {
		this.sociotype = sociotype;
	}

	public void addBuilding(Building b) {
		this.buildings.add(b);
	}

	public List<Building> getBuildings() {
		return this.buildings;
	}

	/**
	 * Get a random Building from this community. This function is very expensive so definitely shouldn't
	 * be called regularly, only during model initialisation.
	 * @return A Building chosen at random (using RandomHelper) or null if this community doesn't have
	 * any buildings in it.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Building> T getRandomBuilding(Class<T> clazz) {
		if (this.buildings==null || this.buildings.size()<1) {
			Outputter.errorln("Community.getRandomBuilding() error: the list of buildings for this community " +
					"("+this.id+") is null or contains no buildings. Returning null, this will cause problems!");
		}
		else {
			List<T> list = new ArrayList<T>();
			for (Building b:this.buildings) {
				if (clazz.isAssignableFrom(b.getClass())) {
					list.add((T)b);
				}
			}
			if (list.size()==0) {
				Outputter.debugln("Community.getRandomBuilding() error: couldn't find any buildings of type "+
						clazz.getName()+" to return. Returning null, this will cause problems!", 
						Outputter.DEBUG_TYPES.GENERAL);
			}
			else {
				return list.get(RandomHelper.nextIntFromTo(0, list.size()-1));
			}
		}
		return null; // if get here an error occurred.
	}


	public String toString() {
		return "Community "+this.id;//+ (this.sociotype==null? "" : " (sociotype: "+this.sociotype.toString()+")");
	}

	/**
	 * Set the total area that (in km^2 or number of grid cells) that this community covers. 
	 * @param area
	 */
	public void setArea(double area) {
		this.area = area;
	}
	public double getArea() {
		return this.area;
	}

	/**
	 * Get the average distance from the centre of this community to every point within the community assuming
	 * that it is a circle. This is used when Burglars calculate the distance from their current location to a 
	 * community, if they are situated in the community that they are examining then the distance is set to the
	 * average distance.
	 * <p>
	 * Actually just returns half the radius of the circle which would be made from the equivalent area of
	 * this Community. Although this isn't entirely accurate (there are more points on the outside half the
	 * radius that indisde which will drive the average distance above the radius/2) it's close enough!
	 * @return the average distance
	 */
	public double getAverageDistance() {
		if (this.area==-1)
			Outputter.errorln("Community.getAverageDistance() error: the area of this Community ("+toString()+
					") hasn't been set, this should have been done in EnvironmentFactory when communities were " +
			"read in.");
		else if (this.averageDist==-1) { // Calculate the average distance
			this.averageDist = Math.sqrt((this.area/Math.PI))/2; // return half the radius
			if (Double.isInfinite(this.averageDist)){
				Outputter.errorln("Community.getAverageDistance() for some reason the averageDistance parameter " +
						"for this community ("+this.toString()+") is calculated to be infinity!");
			}
		}
		return this.averageDist;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return this.uniqueID;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Community))
			return false;
		Community c = (Community) obj;
		return this.id==c.id;
		//		return this.uniqueID == c.uniqueID;
	}

	public void clearCaches() {
		Community.comunityIDs.clear();
		Community.uniqueIDs=0;
	}

	public String getParameterValuesString() {
		return "Community "+this.id+" CE: "+this.collectiveEfficacy+", "+this.sociotype.getParameterValuesString();
	}

}
