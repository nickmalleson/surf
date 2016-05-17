package burgdsim.environment;

import java.util.ArrayList;
import java.util.List;

public class Junction implements FixedGeography{
	
	public static int UniqueID = 0;
	private String identifier = "";
	private int id ;
	private Coord coord;
	private List<Road> roads; // The Roads connected to this Junction, used in GIS road network
	
	public Junction(Coord coord) {
		super();
		this.id = UniqueID++;
		this.coord = coord;
		this.roads = new ArrayList<Road>();
	}
	
	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}
	/**
	 * @return the identifier
	 */
	public String getIdentifier() {
		return identifier;
	}
	/**
	 * @param identifier the identifier to set
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
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
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Junction "+this.id;//+" ("+this.coord.getX()+","+this.coord.getY()+")";
	}
	
	public List<Road> getRoads() {
		return this.roads;
	}
	
	public void addRoad(Road road) {
		this.roads.add(road);
	}
	
	/**
	 * Tests if Junctions are equal by comparing the coorinates.
	 * @param j The junction to be compared with this one
	 * @return True if their coordinates are equal, false otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Junction)) {
			return false;
		}
		Junction j = (Junction) obj;
		return this.getCoords().equals(j.getCoords());
	}
	
}
