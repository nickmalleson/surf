package burgdsim.environment;

/**
 * Class used to represent stations on a transport route. These objects aren't actually added to the
 * model, instead they are read in from a shapefile and then the existing road network can be
 * adapted to include extra links between existing Junctions. See 
 * EnvironmentFactory.createTransportNetworks().
 * 
 * @author Nick Malleson
 *
 */
public class Station implements FixedGeography {
	
	private int id;			// An id for this Station
	private Coord coords;
	private String type;	// The type of transport this Station is part of, e.g. "bus"
	private int routeNum ; 	// Unique identifier for the route that this Station is part of (e.g. bus route '43')
	private int statNum;	// The number of this Station on the route (e.g. 2 = second station on route)
	
	// ID guaranteed to be unique, used by equals() (there's no guarantee that id is unique although it should be)
	private int uniqueID;
	private static int UniqueRoadID=0;

	
	/**
	 * Only default constructor is provided because all variables will be automatically read 
	 * in from ShapefileReader
	 */
	public Station() {
		this.uniqueID=UniqueRoadID++;
	}
	
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = (int) id;
	}
	
	public Coord getCoords() {
		return coords;
	}

	public void setCoords(Coord coord) {
		this.coords = coord;
	}

	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}

	public int getRouteNum() {
		return routeNum;
	}

	public void setRouteNum(int routeNum) {
		this.routeNum = routeNum;
	}
	
	public int getStatNum() {
		return this.statNum;
	}

	public void setStatNum(int number) {
		this.statNum = number;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Station))
			return false;
		Station s = (Station) obj;
		return this.uniqueID==s.uniqueID;
	}


	@Override
	public int hashCode() {
		return this.uniqueID;
	}

	@Override
	public String toString() {
		return "Station "+this.id+": "+this.type+" route "+this.routeNum+" (station "+this.statNum+")";
	}
	
}