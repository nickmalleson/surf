package burgdsim.environment.buildings;

import java.util.Hashtable;
import java.util.Map;

import burgdsim.environment.Cacheable;
import burgdsim.environment.Community;
import burgdsim.environment.Coord;
import burgdsim.environment.FixedGeography;
import burgdsim.main.ContextCreator;
import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;

/**
 * Abstract class to represent all buildings.
 * <p>
 * Using Repast Simphony it isn't necessary for objects/agents to store their spatial coordinates
 * (this is the purpose of projections) but I'm letting static objects store their coordinates as
 * a convenience so that I don't have to ask the projection for them. It is important, therefore,
 * to make sure that the Building's coordinates and those in the projection are the same when the
 * object is created (otherwise behaviour will be strange and displays wont work!).
 * <p>
 * NOTE: this class should be abstract, but this causes problems with the ShapefileLoader. 
 * 
 * @author Nick Malleson
 */
public class Building implements FixedGeography, Comparable<Building>, Cacheable {
	
	private static final long serialVersionUID = 1L;
	protected Coord coord;
	protected Community community;	// The community area that this building is part of
	protected int id;
	protected int type; // Used when creating buildings, different types mean create different subclasses
	private int uniqueID; // Used to that each building has a unique identifier, necessary for hashCode() and equals()
	private static int buildingIDs = 0;
	
	/* Variables unique to burglary: describe different aspects of the environment. These are only relevant
	 * to Houses but because Buildings are read in from shapefile these need to be here.*/
	protected double accessibility = 0.5;
	protected double visibility = 0.5;
	protected double security = 0.5;
	protected double baseSecurity = 0.5; // Security returns to this level over time.
	private double trafficVolume = 0.5; // Trafic volume will vary depending on the time of day
	private static Map<Integer, Double> trafficVolumeWeights; // Store weights depending on TOD for quick lookup
	
	private int numBurglaries = 0; // The number of burglaries in this building.
	
	// Used in sensitivity tests, return a constant value for traffic volume depending on TOD
	protected double const_tv = 0.5;
	
	/**
	 * Default constructor required by repast shapefile loader
	 */
	public Building() {
		init();
	}
	
	public Building(Coord coord) {
		this.coord = coord;
		init();
	}
	

//	public Building(Coord coord, int id, int type) {
//		super();
//		this.coord = coord;
//		this.id = id;
//		this.type = type;
//		init();
//	}
	
	public Building (Building b) {
		init();
		this.coord = b.coord;
		this.id = b.id;
		this.type = b.type;
		this.trafficVolume = b.trafficVolume;
	}
	
	/** 
	 * Basic initialisation which must be done in each constructor. Sometimes this Building's id will be
	 * set by the constructor or by the setID() method (with GIS environment), but it's initialised here in
	 * case it isn't done in the constructor (which happens in the Grid environment).
	 */
	private void init() {
		this.uniqueID=Building.buildingIDs++;
		this.id = this.uniqueID;
		GlobalVars.CACHES.add(this); // This is to reset the unique building ID when simulation is restarted;
	}
	
	/**
	 * Some initialisation must be done once this Building object has been created.
	 */
	public void postInit() {
		this.baseSecurity = this.security; // Set the base level of security.		
	}

	/**
	 * @return the coord
	 */
	public Coord getCoords() {
		return coord;
	}

	/**
	 * @param coord the coord to set
	 */
	public void setCoords(Coord coord) {
		this.coord = coord;
	}
	

	/**
	 * Get the building type, this is used when creating Buildings so we know which type
	 * of subclass (e.g. house, workplace etc) to use.
	 * @return the type
	 */
	public int getType() {
		return this.type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(int type) {
		this.type = type;
	}
	
	public int getId(){
		return this.id;
	}
	
	public void setId(int id) {
		this.id = id;
//		this.uniqueID = id;
	}
	
//	/**
//	 * Get the unique identifier for this Building. This is an auto-increment number which starts at
//	 * 0 and is incremented each time a building is created. It is different to the 'id' which is not
//	 * guaranteed to be unique.
//	 * @return
//	 */
//	public int getUniqueID() {
//		return this.id;
//	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Builing "+this.id;
	}
	
	/** Return a description of the accessibility, security and visibility values of this building. */
	public String getParameterValuesString() {
		return this.getClass().getCanonicalName()+" "+this.id+" acc: "+this.getAccessibility()+"" +
				", sec: "+this.getSecurity()+", vis: "+this.getVisibility()+
				", tv: "+this.getTrafficVolume(GlobalVars.time);
	}

	/**
	 * Checks to see if passed object is a Building and if the unique id's are equal
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Building))
			return false;
		Building b = (Building) obj;
		return this.uniqueID==b.uniqueID;
//		return this.id==b.id;
	}

	/**
	 * Return this buildings unique id number.
	 */
	@Override
	public int hashCode() {
		return this.uniqueID;
//		return this.id;
	}

	public int compareTo(Building o) {
//		if (this.id<o.id)
//			return -1;
//		else if (this.id>o.id)
//			return 1;
//		return 0;
		if (this.uniqueID<o.uniqueID)
			return -1;
		else if (this.uniqueID>o.uniqueID)
			return 1;
		return 0;
	}

	/**
	 * @return the accessibility
	 */
	public double getAccessibility() {
		return accessibility;
	}

	/**
	 * Set this Building's accessibility.
	 * @param accessibility the accessibility to set
	 */
	public void setAccessibility(double accessibility) {
		this.accessibility = accessibility;
	}
	
	/**
	 * Set this Building's accessibility. Used by ShapefileReader (shapefile's have a limit on the 
	 * length of field sizes so accessibility is abbreviated to 'acc'). Only relevant
	 * to Houses but because Buildings are read in from shapefile these need to be here
	 * @param accessibility the accessibility to set
	 */
	public void setAcc(double accessibility) {
		this.accessibility = accessibility;
	}

	/**
	 * Get this Building's visibility
	 * @return the visibility
	 */
	public double getVisibility() {
		return visibility;
	}

	/**
	 * Set this Building's visibility
	 * @param visibility the visibility to set
	 */
	public void setVisibility(double visibility) {
		this.visibility = visibility;
	}
	/**
	 * Set this Building's visibility. Used by ShapefileReader (shapefile's have a limit on the 
	 * length of field sizes so visibility is abbreviated to 'vis'). Only relevant
	 * to Houses but because Buildings are read in from shapefile these need to be here
	 * @param visibility the visibility to set
	 */
	public void setVis(double visibility) {
		this.visibility = visibility;
	}

	/**
	 * @return the security
	 */
	public double getSecurity() {
		return security;
	}

	/**
	 * Set this Building's security.
	 * @param security the security to set
	 */
	public void setSecurity(double security) {
		this.security = security;
	}
	
	/**
	 * Set this Building's security. Used by ShapefileReader (shapefile's have a limit on the 
	 * length of field sizes so security is abbreviated to 'sec'). Only relevant
	 * to Houses but because Buildings are read in from shapefile these need to be here
	 * @param security the security to set
	 */
	public void setSec(double security) {
		this.security = security;
	}
	
	public void setCommunity(Community c) {
		this.community = c;
	}
	public Community getCommunity() {
		return this.community;
	}
	
	/**
	 * Tell this Building that it has been burgled: increment the numBurglaries variable.
	 */
	public void burglaryOccurred() {
		this.numBurglaries++;
	}
	
	/**
	 * Get the number of burglaries in this building. Doesn't affect logic of model, used mainly by Simphony
	 * so that the number of burglaries can be displayed in a GUI.
	 * @return The number of burglaries in this building.
	 */
	public int getNumBurglaries() {
		return this.numBurglaries;
	}

	/**
	 * Return this Building's base security level. This is the security level that is set when the building
	 * is created. After a burglary the security of this building will increase but it will gradually
	 * degrade back to this level over time.
	 * @return the baseSecurity
	 */
	public double getBaseSecurity() {
		return baseSecurity;
	}

	/* To get the traffic volume levels for this building */
	
	public double getTrafficVolume(double time){
		// If doing a sensitivity test just return the constant value for TV, not changing with TOD
		if (ContextCreator.isTrue(GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.CONST_TV)) {
//			Outputter.describeln("Building.getTV() returning constant traffic volume: "+this.const_tv );
			return this.const_tv;
		}
		else { 
			return this.trafficVolume*getTVWeight(time);
		}
	}
	
	public void setTV(double trafficVolume) {
		this.trafficVolume = trafficVolume;
	}

	public void setTrafficVolume(double trafficVolume) {
		this.trafficVolume = trafficVolume;
	}
	
	/** Used in sensitivity tests to set a constant traffic volume that this building will always return,
	 * rather than calculating based on time of day etc. */
	public void setConstTV(double d) {
		if (!ContextCreator.isTrue(GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.CONST_TV)) {
			Outputter.errorln("Building.setConstTV() has been called but the global parameter " +
					"GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.CONST_TV is 0 so shouldn't have been.");
		}
		this.const_tv = d;
	}

	/**
	 * Return the weight to multiply this community's traffic volume by, depending on the time of day.
	 * @param time
	 * @return
	 */
	private static double getTVWeight(double time) {
		if (Building.trafficVolumeWeights==null) {
			// TODO read these in from the database rather than hard coding them, make it easier to adjust them (same as OAC.occupancyWeights).
			// Need to build the lookup table, see thesis/environment/risk_profile/figures/traffic_volume_graph for values
			trafficVolumeWeights = new Hashtable<Integer, Double>(25);
			trafficVolumeWeights.put(0,  0.25); // Midnight (00:00)
			trafficVolumeWeights.put(1,  0.00); 
			trafficVolumeWeights.put(2,  0.00); 
			trafficVolumeWeights.put(3,  0.00);
			trafficVolumeWeights.put(4,  0.00);
			trafficVolumeWeights.put(5,  0.00);
			trafficVolumeWeights.put(6,  0.25);
			trafficVolumeWeights.put(7,  0.25);
			trafficVolumeWeights.put(8,  0.75);
			trafficVolumeWeights.put(9,  0.1);
			trafficVolumeWeights.put(10, 0.50);
			trafficVolumeWeights.put(11, 0.50);
			trafficVolumeWeights.put(12, 0.50); // Midday (12:00)
			trafficVolumeWeights.put(13, 0.50);
			trafficVolumeWeights.put(14, 0.50);
			trafficVolumeWeights.put(15, 0.75);
			trafficVolumeWeights.put(16, 0.75);
			trafficVolumeWeights.put(17, 1.00);
			trafficVolumeWeights.put(18, 1.00);
			trafficVolumeWeights.put(19, 0.75);
			trafficVolumeWeights.put(20, 0.50);
			trafficVolumeWeights.put(21, 0.50);
			trafficVolumeWeights.put(22, 0.50);
			trafficVolumeWeights.put(23, 0.25);
			trafficVolumeWeights.put(24, 0.25); // Midnight (00:00) - included because 23:00 needs higher number to interpolate between.
		}
		// Interpolate between two values (upper and lower) in the map (copied from SleepM)
		double lower = trafficVolumeWeights.get((int) time); // When casting to int will round down
		double upper = trafficVolumeWeights.get((int) time + 1);
		double interpolateAmount = time - (int) time; // The amount to interpolate between
//		System.out.println("COMMUNITY.getTODWeight: (for traffic volume) lower:"+lower+". upper: "+upper+". interpolate: "+interpolateAmount+" . return: "+(lower + interpolateAmount*(upper-lower)));
		return lower + interpolateAmount*(upper-lower);
	}


	/**
	 * There aren't actually any caches to clear, but the auto-increment number which is used to generate
	 * unique building id's when setId() isn't used (this happens when using a grid environment) must be reset.
	 */
	public void clearCaches() {
		Building.buildingIDs = 0;
	}
	

}
