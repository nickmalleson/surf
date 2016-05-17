package burgdsim.environment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import repast.simphony.random.RandomHelper;

import burgdsim.main.Outputter;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * A simple class to represent Coordinates in the BurgdSim model. (I haven't called this class
 * "Coordinate" because that name will conflict with classes in other packages). This is used
 * rather than the Coordinate class in the Java Topology Suite because the same Coord obejects
 * can be referred to by different Environments (e.g. a Grid environment can still uses Coord
 * objects to represent agent's positions).
 * @author nick
 *
 */
public class Coord implements Serializable{
	
	private static final long serialVersionUID = 1L;
	private double x;
	private double y;
	private int hashCode = -1; // A cache of the hash code so it doesn't need to be calculated every time it's called
	public Coord(double x, double y) {
		this.x = x;
		this.y = y;
	}
	/***
	 * Create a coordinate from an array doubles.
	 * @param coords The x,y coordinates where coord[0] = x and coord[1] = y
	 */
	public Coord(double[] coords){
		this.x=coords[0];
		this.y=coords[1];
	}
	public Coord(Coordinate coords) {
		this.x = coords.x;
		this.y = coords.y;
	}
	
	public Coord(Coord c) {
		this.x = c.x;
		this.y = c.y;
	}

	public Coordinate toCoordinate() {
		return new Coordinate(this.x, this.y);
	}
	
	/**
	 * Get a random coordinate within a set range.
	 * @param min
	 * @param max
	 * @return a random coordinate with min<x<max and min<y<max
	 */
	public static Coord randomCoord (double min, double max) {
		return new Coord(RandomHelper.nextDoubleFromTo(min, max), RandomHelper.nextDoubleFromTo(min, max));
	}
	
	/**
	 * Build an array of Coord objects from a list of Coordinate objects.
	 * @return A list of Coordinate objects
	 */
	public static List<Coord> buildCoordArray(List<Coordinate> coordinates) {
		List<Coord> coords = new ArrayList<Coord>(coordinates.size());
		for (Coordinate c:coordinates)
			coords.add(new Coord(c));
		return coords;
	}
	/**
	 * Build an array of Coord objects from a list of Coordinate objects.
	 * @return An array of Coordinate objects
	 */
	public static List<Coord> buildCoordArray(Coordinate[] coordinates) {
		List<Coord> coords = new ArrayList<Coord>(coordinates.length);
		for (Coordinate c:coordinates)
			coords.add(new Coord(c));
		return coords;
	}
	
	@Override
	public String toString() {
		return "Coord ("+this.x+","+this.y+")";
	}
	/**
	 * Returns true if the passed object is a Coord and has the same x, y values as this Coord.
	 * Will also return true if a com.vividsolutions.jts.geom.Coordinate is passed which has the same
	 * x,y values.
	 * @return true if the coordinates represent the same point.
	 */
	@Override
	public boolean equals(Object obj) {
//		System.out.print("Coord.equals() "+this.getX()+","+this.getY()+" : ");
		if (obj instanceof Coordinate) {
			Coordinate c = (Coordinate) obj;
//			System.out.println("comparing to a Coordinate with "+c.x+","+this.y);
			return (c.x==this.getX()) && (c.y==this.getY());			
		}
		else if (! (obj instanceof Coord) ){
			Outputter.errorln("Coord.equals(): warning, comparing this Coord with an object which isn't a Coord," +
					" perhaps wront argument used? Being passed a: '"+obj.toString()+"'");
			return false;
		}
		// Object must be a Coord
		Coord c = (Coord) obj;
		return (c.getX()==this.getX()) && (c.getY()==this.getY()); 

	}
	
	
	/**
	 * Returns a hashCode for this Coordinate built from the string concatenation of the x and
	 * y values. Only the last (n/2)-1 digits from each of the coordinates can be used in the
	 * hash code where n is the number of digits contained in the maximum possible representable
	 * integer (otherwise the integer would be too big to represent and a number format exception
	 * is thrown). Also account for the case when Coords are made up of integers (e.g. in a GRID
	 * environment), here concatinate the entire integers rather than a substring.
	 * <p>
	 * In summary: <ul><li>(-1.828357667423,49.902647588746) -> 74238746</li>
	 * <li>(5,30) -> 530</li></ul> 
	 */
	@Override
	public int hashCode() {
		if (this.hashCode==-1){
			int n=String.valueOf(Integer.MAX_VALUE).length(); // The max number of numbers in an integer
			String xCoord = String.valueOf(this.x).replaceAll("\\.", "");
			String yCoord = String.valueOf(this.y).replaceAll("\\.", "");
			if (xCoord.length() < (n/2) || yCoord.length() < (n/2)) {
				// Coords are probably integers, just concatinate the two
				this.hashCode = Integer.parseInt(xCoord+yCoord);
			}
			else { // Coords are decimals with many decimal places, concatinate the ends (last n/2 digits)
				this.hashCode = Integer.parseInt(
						xCoord.substring(xCoord.length()-(n/2)+1) + 
						yCoord.substring(yCoord.length()-(n/2)+1));
			}

		}
		return this.hashCode;
	}
	/**
	 * @return the x
	 */
	public double getX() {
		return x;
	}
	/**
	 * @return the y
	 */
	public double getY() {
		return y;
	}
	/**
	 * @param x the x to set
	 */
	public void setX(double x) {
		this.x = x;
	}
	/**
	 * @param y the y to set
	 */
	public void setY(double y) {
		this.y = y;
	}
	

}
