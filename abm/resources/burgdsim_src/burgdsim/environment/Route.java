package burgdsim.environment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.event.EventListenerList;

import repast.simphony.space.gis.Geography;

import burgdsim.burglars.Burglar;
import burgdsim.burglary.SearchAlg;
import burgdsim.environment.buildings.Building;
import burgdsim.main.Outputter;

/**
 * Abstract class for Route objects. Used so that Burglars can easily travel around different
 * types of environment, e.g. GIS or Grid projections. Subclasses must override setRoute()
 * (which builds the list of coordinates the agent must travel along) and travel() which moves
 * the agents along the route 1 unit of travel.
 * <p>
 * A "unit of travel" is the distance that an agent can cover in one iteration (one square on a grid
 * environment or the distance covered at walking speed in an iteration on a GIS environment). This
 * will change depending on the type of transport the agent is using. E.g. if they are in a car they
 * will be able to travel faster, similarly if they are travelling along a transort route they will
 * cover more ground.
 * 
 * @author Nick Malleson
 */
public abstract class Route  {

	protected Burglar burglar;
	protected Coord destination;   // Can be used to check that this route is correct (agent's destination might change)
	protected Building destinationBuilding;

	// The route consists of a list of coordinates which describe how to get to the destination. Each coordinate
	// might have an attached 'speed' which acts as a multiplier and is used to indicate whether or not the agent is
	// travelling along a transport route (i.e. if a coordinate has an attached speed of '2' the agent will be
	// able to get to the next coordinate twice as fast as they would do if they were walking).	
	private List<Coord> route;
	protected LinkedHashMap<Coord, Double> routeSpeeds = new LinkedHashMap<Coord, Double>();
	// The Coord most recently removed from the route, sometimes useful to remember where the agent is coming from 
	private Coord previousRouteCoord;
	// When agents are on a transport route they wont add anything to their cognitive environment and won't
	// be able to change action until they get to the next station
	protected boolean onTransportRoute = false; 
	// The agent wants to change action, stop when they get to the next station (usually would just continue onwards)
	protected boolean agentAwaitingUnlock = false;

	protected static Geography<Road> roadGeography; // XXXX only using temporarily, required for GISRoute. Problem is that GisRoute.setRoute() requrires roadGeography pointed, which cannot be initialised in GISRoute constructor before Route calls GISRoute.setRoute()!

	// Can be used to create specific types of route, e.g. burglar search routines.
	protected SearchAlg.SEARCH_TYPES type;

	// Listeners - sometimes ErrorEvents might be thrown, used for debugging
	protected EventListenerList listeners = new EventListenerList();

	/**
	 * Default constructor creates a Route with no parameters. This should only be used to gain access to
	 * the getDistance(Burglar, Coord, Coord) method which is basically static but cann't be defined as such
	 * because it must be overridden by subclasses. EnvironmentFactory.getDistance() function uses this
	 * constructor, it means that high-level functions (e.g. used in Burglary) can find distances.
	 */
	public Route() {

	}

	/**
	 * Creates a new Route object.
	 * @param burglar The burglar which this Route will control.
	 * @param destination The agent's destination.
	 * @param destinationBuilding The (optional) building they're heading to.
	 * @param type The (optional) type of route, used by burglars who want to search.
	 */
	public Route (Burglar burglar, Coord destination, Building destinationBuilding, SearchAlg.SEARCH_TYPES type) {
		this.destination = destination;
		this.burglar = burglar;
		this.destinationBuilding = destinationBuilding;
		this.type = type;
	}

	// XXXX only using this temporarily, required by GISRoute
	public Route (Burglar burglar, Coord destination, Building destinationBuilding, SearchAlg.SEARCH_TYPES type, 
			Geography<Road> roadGeography) {
		this.destination = destination;
		this.burglar = burglar;
		this.destinationBuilding = destinationBuilding;
		this.type = type;
		if (Route.roadGeography==null || Route.roadGeography!=roadGeography) {
			Route.roadGeography = roadGeography; // XXXX only temporary
		}
	}

	/**
	 * Determine whether or not the Person associated with this Route is at their destination
	 * @return true if the agent is at their destination
	 */
	public abstract boolean atDestination();

	/**
	 * Get the distance (on a network) between the origin and destination. Take into account the Burglar
	 * because they might be able to speed up the route by using different transport methods. Actually
	 * calculates the distance between the nearest Junctions between the source and destination. Note that
	 * the GRID environment doesn't have any transport routes in it so all distances will always be the same
	 * regardless of the burglar. 
	 * @param burglar
	 * @param destination
	 * @return
	 */
	public abstract double getDistance(Burglar burglar, Coord origin, Coord destination);

	/**
	 * Find out where this Route leads to.
	 * @return the coordinate of this Route's destination.
	 */
	public Coord getDestination() {
		return this.destination;
	}

	protected abstract List<Coord> setRoute() throws Exception ;


	/**
	 * Set a specific type of route. This can be used when agents want to burgle (i.e. they don't
	 * just want to get to a destination as quickly as possible, setRoute() is used for that).
	 * This class must be overridden by subclasses.
	 * 
	 * @param type The type of route, must be one of Route.ROUTE_TYPES and must be supported
	 * by subclasses.
	 * @return A list of coords describing the route.
	 * @throws Exception 
	 */
	protected List<Coord> setRoute(SearchAlg.SEARCH_TYPES type) throws Exception {
		Outputter.errorln("Route.setRoute(type) errpor: this function shouldn't have been called, which "+
				"ever class is subclassing Route should override this to implement a specific type of route: " +
				type.toString()) ;
		return null;
	}

	/**
	 * Move the agent towards their destination. This must be overriden by subclasses, but subclasses should
	 * be sure to call super.travel() to make sure the route is populated first.
	 * <p>
	 * For safety, subclasses don't have direct write access to the route, they use the setRoute() method to 
	 * create a new list of coordinates. I've don't this because a few times I've accessed this.route directly
	 * in subclasses.setRoute() method where I shouldn't have been. 
	 * @throws Exception If the agent couldn't travel for some reason.
	 */
	public void travel() throws Exception {
		if (this.route==null) {
			if (type==null)
				this.route = this.setRoute();
			else
				this.route = this.setRoute(type);
		}
	}


	/**
	 * Will add the given buildings to the awareness space of the Burglar who is being controlled
	 * by this Route. Also tells the burglar which buildings have been passed if appropriate, this
	 * is needed for agents who are currently looking for a burglary target.
	 * @param buildings A list of buildings
	 */
	@SuppressWarnings("unchecked")
	protected <T> void passedObjects(List<T> objects, Class<T> clazz) {
		this.burglar.addToMemory(objects, clazz);
		if (clazz.isAssignableFrom(Building.class)) {
			//			System.out.println("Route.passedObjects(): "+objects.toString());
			this.burglar.buildingsPassed((List<Building>) objects);	
		}
	}
	/**
	 * Will add the given buildings to the awareness space of the Burglar who is being controlled
	 * by this Route. 
	 * @param buildings A list of buildings
	 */
	protected <T> void passedObject(T object, Class<T> clazz) {
		List<T> list = new ArrayList<T>(1);
		list.add(object);
		this.burglar.addToMemory(list, clazz);
	}

	/**
	 * Returns the angle of the vector from p0 to p1 relative to the x axis 
	 * <p>The angle will be between
	 * -Pi and Pi. I got this directly from the JUMP program source.
	 * @return the angle (in radians) that p0p1 makes with the positive x-axis.
	 */
	protected static double angle(Coord p0, Coord p1) {
		double dx = p1.getX() - p0.getX();
		double dy = p1.getY() - p0.getY();

		return Math.atan2(dy, dx);
	}

	//	/**
	//	 * Calculates the angle between a vector and another point. The vector is the straight line
	//	 * between 'origin' and 'destination'. 
	//	 * <p>
	//	 * Adapted code from here: http://www.devx.com/tips/Tip/33124
	//	 * 
	//	 * @param origin
	//	 * @param destination
	//	 * @param other
	//	 * @return The angle between 0 to PI (0 if vector is pointing directly at 'other' Coord and PI
	//	 * if vector is pointing directly away).
	//	 */
	//	protected static double angle(Coord origin, Coord destination, Coord other) {
	//		/* NOTE: can also think of this as comparing the angle between two lines, where line 1 is
	//		 * origin->destination and line 2 is origin-other). */
	//		
	//		double differenceX1 = origin.getX() - destination.getX();
	//		double differenceY1 = origin.getY() - destination.getY();
	//		double differenceX2 = origin.getX() - other.getX();
	//		double differenceY2 = origin.getY() - other.getY();
	//
	//		double angle = Math.acos(((differenceX1 * differenceX2) + (differenceY1 * differenceY2)) /
	//				(Math.sqrt((Math.pow(differenceX1,2) + Math.pow(differenceY1,2)) * 
	//						Math.sqrt(Math.pow(differenceX2,2) + Math.pow(differenceY2,2)))));
	//
	//		return angle;
	//	}

	/**
	 * Returns the angle between two vectors. Will be between 0 and Pi.
	 * <p>
	 * This is copied almost exactly from the JUMP project (http://www.vividsolutions.com/JUMP/)
	 * source from class com.vividsolutions.jump.geom.Angle.
	 * 
	 * @param tail the tail of each vector
	 * @param tip1 the tip of one vector
	 * @param tip2 the tip of the other vector
	 * @return the angle between tail-tip1 and tail-tip2
	 */

	public static double angle(Coord tail, Coord tip1, Coord tip2) {

		double a1 = angle(tail, tip1);
		double a2 = angle(tail, tip2);

		double da;
		if (a1 < a2) {
			da = a2 - a1;
		} else {
			da = a1 - a2;
		}
		if (da > Math.PI) {
			da = (2 * Math.PI) - da;
		}

		return da;

	}

	//	private double acos (double X) {
	//		if (X > 1 || X < -1 ) {
	//		 // if the ratio supplies is outside the acceptable range.
	//			System.err.println("ERROR IN ROUTE.acos");
	//			return -1;
	//		}
	//		else if (X == 1) {
	//		    return 0;
	//		}
	//		else if (X==-1) {
	//		    return Math.PI;
	//		}
	//		else {
	//			return (Math.atan(-X / Math.sqrt(-X * X + 1)) + 2 * Math.atan(1));
	//		}
	//			
	//	}


	//	/**
	//	 * Simply pythag, get the distance between two points
	//	 * @return the distance
	//	 */
	//	public static double distance(Coord p1, Coord p2) {
	//		return Math.sqrt(
	//				Math.pow((p1.getX()-p2.getX()), 2) +
	//				Math.pow((p1.getY()-p2.getY()), 2));
	//	}

	protected Coord getRoute(int index) {
		Coord c = null;
		try {
			c =  this.route.get(index);
		}
		catch (java.lang.IndexOutOfBoundsException e) {
			Outputter.errorln("Route.getRoute() error: no more coords left on route to move to.\n" +
					"Burglar "+this.burglar.toString()+" is trying to get to "+this.destinationBuilding.toString());
			throw e;
		}
		return c;
	}

	public int getRouteSize() {
		return this.route.size();
	}
	/**
	 * Removes the first coordinate from the current route (Coord at index 0).
	 * @return true if the route is now empty (i.e. that was the last coord in the route), false otherwise.
	 */
	protected  boolean removeFirstCoordFromRoute() {
		this.previousRouteCoord = this.route.remove(0);
		if (this.route.size()==0)
			return true;
		return false;
	}
	public String getRouteString() {
		return this.route.toString();
	}
	/**
	 * Get the Route Coord that the agent is moving away from (i.e. the coordinate that was most recently
	 * removed from the route list). This is useful for working out which buildings they've passed.
	 */
	protected Coord previousRouteCoord() {
		if (this.previousRouteCoord==null)
			this.previousRouteCoord = this.getRoute(0);
		return this.previousRouteCoord;
	}

	/**
	 * Function used by the BULLS_EYE burglary search algorithm. Returns the 'attractiveness' of a direction
	 * taking into account the number of times that an agent has travelled the path already. Will be used by
	 * both GISRoute and GridRoute in setRoute(BULLS_EYE) functions.
	 * 
	 * @param angle
	 * @param numVisits
	 * @return
	 */
	protected static double BE_calcAngleAttractiveness(double angle, double numVisits) {
		return (Math.pow(0.71*(angle-3),3) + Math.pow(angle-3.1,2) + 0.1 ) / (1000*(numVisits+1)); // (nuVisits+1 so no divide by 0)
	}

	public boolean isOnTransportRoute() {
		return this.onTransportRoute;
	}

	/**
	 * Find out if the agent wants to change their action, if they are then the route will stop when the
	 * agent gets to the next station, this is set by Actions. 
	 * @return true if the agent wants to change their action and needs to be unlocked.
	 */
	public boolean isAwaitingUnlock() {
		return this.agentAwaitingUnlock;
	}
	/**
	 * Actions use this to tell this Route) that it should unlock the agent asap.   
	 * @param awaiting
	 */
	public void setAwaitingUnlock(boolean awaiting) {
		this.agentAwaitingUnlock = awaiting;
	}

	public void errorEventOccurred(ErrorEvent e) {

	}

	public void addErrorEventListener(ErrorEventListener l) {
		this.listeners.add(ErrorEventListener.class, l);
	}

	/** 
	 * Method can be used to fire ChangeAction events
	 */
	protected void fireErrorEvent(ErrorEvent evt) {
		Object[] listeners = this.listeners.getListenerList();
		// Each listener occupies two elements - the first is the listener class and the second is the listener instance
		for (int i=0; i<listeners.length; i+=2) {
			if (listeners[i]==ErrorEventListener.class) {
				((ErrorEventListener)listeners[i+1]).errorEventOccurred(evt);
			}
		}
	}
}
