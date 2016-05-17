package burgdsim.environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.graph.ShortestPath;

import burgdsim.burglars.Burglar;
import burgdsim.burglary.SearchAlg;
import burgdsim.environment.buildings.Building;
import burgdsim.environment.buildings.House;
import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;

public class GridRoute extends Route implements Cacheable{
	
	private static HashMap<Coord, List<Junction>> closestObjectsCache = new HashMap<Coord, List<Junction>>();

	/**
	 * See Route() for reason that this constructor is necessary.
	 */
	public GridRoute() {}
	
	public GridRoute(Burglar burglar, Coord destination, Building destinationBuilding, SearchAlg.SEARCH_TYPES type) {
		super(burglar, destination, destinationBuilding, type);
		GlobalVars.CACHES.add(this);  // Required so the caches will be cleared if the sim is restarted
	}

	/**
	 * Returns true if the person's current position is the same as their destination
	 */
	@Override
	public boolean atDestination() {
		if (GlobalVars.BURGLAR_ENVIRONMENT.getCoords(this.burglar).equals(destination)) {
			return true;
		}
		return false;
	}

	/**
	 * Move the Burglar to the next coordinate (the first one in the list of coordinates) and
	 * remove it.
	 * @throws Exception If the agent wasn't able to travel.
	 */
	@Override
	public void travel() throws Exception {
		super.travel();
		if (this.getRouteSize()<1) {
			Outputter.errorln("Error, GridRoute: travel(): No more coordinates left in route, this shouldn't" +
					" have been called by TravelA");
		}
		else {
			GlobalVars.BURGLAR_ENVIRONMENT.move(burglar, this.getRoute(0));
			// Add passed buildings to the Burglar's memory, using the Route.passedObjects() function
			List<Building> passingBuildings = this.getAdjacentObjects(
					Building.class, this.getRoute(0), GlobalVars.BUILDING_ENVIRONMENT);
			this.passedObjects(passingBuildings, Building.class);
			// Do same for communities, note that in grid environment roads are not part of a community, even
			// if they run right throught the middle of one!) so have to get communities in adjacent squares
			List<Community> passedCommunities = this.getAdjacentObjects(
					Community.class, this.getRoute(0), GlobalVars.COMMUNITY_ENVIRONMENT);
			this.passedObjects(passedCommunities, Community.class);
			
			Outputter.debugln("GridRoute.travel(). Moving to: "+this.getRoute(0).toString()+
					". Passing buildings: "+ passingBuildings.toString()+" and commnities: "+
					passedCommunities.toString(), Outputter.DEBUG_TYPES.ROUTING);
			this.removeFirstCoordFromRoute();
		}

	}

	/**
	 * Moves agent along road network to get to destination. EnvironmentFactory.createBuildingsAndRoads()
	 * will create a Junction on every square which is a Road so this method doesn't need to worry about
	 * how to get from one Junction to another, it can just add the coordinates of every Junction
	 * along the route.
	 * <br>
	 * Works as follows:
	 * <ol>
	 * <li>Find the junction(s) nearest the origin and the destination.</li>
	 * <li>Calculate the shortest route between the origin(s) and destination(s) (there might be a few
	 * different possible routes because the origin/destination might be surrounded by a few junctions).</li>
	 * <li>For the shortest route, add all the coordinates of the Junctions and the final destination.</li>
	 * </ol>
	 * <b>SMALL BUG</b>: This algorithm is slightly different to that used by the GISRoute because Roads
	 * don't play any part in the route. For the GIS algorithm the shortest path is used to find a list
	 * of roads which must be traversed and coordinates are calculated from the Roads, whereas here the
	 * coordinates are calculated directly from the source and target Junctions of each RepastEdge. This
	 * means that in some cases the source and target Junctions will be in the wrong order so the agent
	 * would jump backwards and forwards on their way to the destination. This problem can be solved
	 * by either adding all the source Junctions *or* all the target junctions but not both. Only problem
	 * is that agent will make miss a cell either at the beginning or the end of the route.
	 *  
	 * 
	 * @return a list of Coords to get to the destination
	 * @throws Exception If something went wrong.
	 */
	@Override
	protected List<Coord> setRoute() throws Exception {
		try {
			Outputter.debugln("GridRoute.setRoute() called, making route from "+
					GlobalVars.BURGLAR_ENVIRONMENT.getCoords(burglar).toString()+" to "+this.destination.toString(),
					Outputter.DEBUG_TYPES.ROUTING);
			// Check not already at destination (this can happen because in TravelA.performAction() a route object
			// must be created first, before route.atDestiation() can be tested
			Coord currentCoord = GlobalVars.BURGLAR_ENVIRONMENT.getCoords(burglar);
			List<Coord> theRoute = new Vector<Coord>(); // Use vector so can efficiently remove items from front of list
			if (this.destination.equals(currentCoord)) {
				theRoute.add(currentCoord);
				return theRoute;
			}

			List<? extends Junction> originJuncs = this.getNearestJunctions(currentCoord);
			List<? extends Junction> destJuncs = this.getNearestJunctions(this.destination);

//			Outputter.debugln("NEAREST JUNCTIONS TO ORIGIN: "+originJuncs.toString(), Outputter.DEBUG_TYPES.ROUTING);
//			Outputter.debugln("NEAREST JUNCTIONS TO DESTINATION: "+destJuncs.toString(), Outputter.DEBUG_TYPES.ROUTING);

			// Find the distances for each path (storing in a TreeMap so paths will be sorted on ascending distance - 
			Map<Double,List<RepastEdge<Junction>>> pathsList = new TreeMap<Double, List<RepastEdge<Junction>>>();
			boolean foundPath = false;
			for (Junction o:originJuncs) {
				for (Junction d:destJuncs) {
					if (!o.equals(d)) {
//						System.out.println("LKJ: trying to find a path between "+o.toString()+" and "+d.toString());
						ShortestPath<Junction> path1 = new ShortestPath<Junction>(
								EnvironmentFactory.getRoadNetwork(), o);
						double distance = path1.getPathLength(d);
						ShortestPath<Junction> path2 = new ShortestPath<Junction>(EnvironmentFactory.getRoadNetwork());
						List<RepastEdge<Junction>> shortPath = path2.getPath(o, d);
						pathsList.put(distance, shortPath);
						foundPath = true;
						path1.finalize(); path1 = null;
						path2.finalize(); path2 = null;
					}
				}
			}
			List<RepastEdge<Junction>> shortestPath = new ArrayList<RepastEdge<Junction>>();
			if (!foundPath) {
				// RARE: This will happen if the agent needs to move only one square (to do with problem with
				// jumping diagonally at beginning/end of route). Entire route consists of only one junction
				// so can't create a path, just add the junction to the route instead
				
				theRoute.add(originJuncs.get(0).getCoords());
			}
			else {
				// The keys are in ascending order, so shortest path is indexed by first key in map
				shortestPath = pathsList.get(pathsList.keySet().iterator().next());
			}

			//		System.out.println("SHORTEST PATH FROM: "+currentCoord.toString()+" TO :"+destination.toString()+": ");
			//		for (RepastEdge<Junction> e:shortestPath) {
			//			System.out.println("\t"+e.getSource().getCoord().toString()+"->"+e.getTarget().getCoord().toString());
			//		}
			//		System.out.println("\t\t"+shortestPath.toString());

			// Now have a list of junctions so build up a list of Coords
			theRoute.add(currentCoord);
			for (RepastEdge<Junction> e:shortestPath) {
				//			theRoute.add(e.getSource().getCoord());
				theRoute.add(e.getTarget().getCoords());
			}
			theRoute.add(destination);
			Outputter.debugln("GRIDROUTE.setRoute(): CREATED ROUTE: "+theRoute.toString(),
					Outputter.DEBUG_TYPES.ROUTING);
			return theRoute;
		} catch (Exception e) {
			Outputter.errorln("GridRoute.setRoute(): exception caught");
			Outputter.errorln(e.getStackTrace());
			throw e;
		}		
//		return null;
	}
	
	@Override
	public  double getDistance(Burglar burglar, Coord origin, Coord destination) {
		// Find the closest Junctions to the origin and destination
		List<? extends Junction> originJuncs = this.getNearestJunctions(origin);
		List<? extends Junction> destJuncs = this.getNearestJunctions(destination);
		
		// Find shortest path and return length (might be a few possible origins and destinations because
		// they could be adjacent to a number of different junctions. Note that Burglar isn't used
		// here because there are no transport routes in the GRID environment.
		double shortestDist = Double.MAX_VALUE; double dist;
		for (Junction o:originJuncs) {
			for (Junction d:destJuncs) {
				if (!o.equals(d)) {
					ShortestPath<Junction> p = new ShortestPath<Junction>(EnvironmentFactory.getRoadNetwork(), o); 
					dist = p.getPathLength(d);
					p.finalize(); p = null;
					if (dist<shortestDist)
						shortestDist = dist;
					}
			}
		}
		return shortestDist;
	}
	
/*	// Not used any more, creates a route that moves the agent directly towards their destination
	// in a straight line
	protected List<Coord> setRouteNoRoads() {
		// Check not already at destination (this can happen because in TravelA.performAction() a route object
		// must be created first, before route.atDestiation() can be tested
		Coord currentCoord = GlobalVars.BURGLAR_ENVIRONMENT.getCoords(burglar);
		List<Coord> theRoute = new Vector<Coord>(); // Use vector so can efficiently remove items from front of list
		if (this.destination.equals(currentCoord)) {
			theRoute.add(currentCoord);
			return theRoute;
		}		
//		System.out.println("Planning route: curr: "+currentCoord+" dest: "+destination.toString());
		double angle; // The angle between the current coord and the destination
		int counter = 0;
		while (!currentCoord.equals(destination)) {
			angle = Route.angle(currentCoord, destination);
			if (angle>-0.25*Math.PI && angle<=0.25*Math.PI) // Angle points to the East
				currentCoord = new Coord(currentCoord.getX()+1, currentCoord.getY());
			else if (angle>0.25*Math.PI && angle<=0.75*Math.PI)// Angle points to the North
				currentCoord = new Coord(currentCoord.getX(), currentCoord.getY()+1);
			else if (angle>0.75*Math.PI || angle<=-0.75*Math.PI) // Angle points to the West (note 'OR' operator)
				currentCoord = new Coord(currentCoord.getX()-1, currentCoord.getY());
			else if (angle>-0.75*Math.PI || angle<=-0.25*Math.PI) // Angle points to the South
				currentCoord = new Coord(currentCoord.getX(), currentCoord.getY()-1);
			else
				Outputter.errorln("Error. GridRoute: setRoute(): unrecognised angle: "+angle);

			theRoute.add(currentCoord);
			if (counter++>10000) { // In an infinite loop
				Outputter.errorln("Error. GridRoute: setRoute(): in infinite loop trying to build route!");
				break;
			}
		}
		return theRoute;
	}*/

	@Override
	public String toString() {
		return "GridRoute from "+this.getRoute(0).toString()+" to: "+this.destination.toString();
	}
	
	private List<Junction> getNearestJunctions(Coord c) {
		// Check if the nearest objects for this coordinate have already been cached
		if (closestObjectsCache.containsKey(c)) {
//			Outputter.debugln("COORD IN CACHE", Outputter.DEBUG_TYPES.ROUTING);
//			Outputter.debugln("CONTENTS OF CACHE: "+closestObjectsCache.keySet().toString(), Outputter.DEBUG_TYPES.ROUTING);
			return closestObjectsCache.get(c);
		}
//		Outputter.debugln(c.toString()+"NOT IN CACHE", Outputter.DEBUG_TYPES.ROUTING);
			
		// Nothing has been cached so get every near Junction for this coordinate
		List<Junction> junctions = new ArrayList<Junction>();
		List<Double> distances = new ArrayList<Double>();
		junctions= GlobalVars.JUNCTION_ENVIRONMENT.getAllObjects(Junction.class);
		for (Junction j:junctions) {
			Coord juncCoords = j.getCoords();
			distances.add(GlobalVars.JUNCTION_ENVIRONMENT.getDistance(c, juncCoords));
		}
		// Find the shortest distance:
		double minDist = Double.MAX_VALUE;
		for (Double d:distances) if (d<minDist) minDist = d;
		// Now run through lists again adding all the closest points
		List<Junction> closestObjects = new ArrayList<Junction>();
		for (int i=0;i<junctions.size();i++) {
			if (distances.get(i)==minDist) {
				closestObjects.add(junctions.get(i));
			}
		}
//		Outputter.debugln("FOUND CLOSEST OBJECTS TO "+c.toString()+": ", Outputter.DEBUG_TYPES.ROUTING);
//		Outputter.debugln("ADDING OBJECTS TO NEW CACHE ENTRY FOR COORD "+c.toString(), Outputter.DEBUG_TYPES.ROUTING);
//		closestObjectsCache.put(c, closestObjects);
//		Outputter.debugln("CONTENTS OF CACHE: "+closestObjectsCache.keySet().toString(), Outputter.DEBUG_TYPES.ROUTING);

		return closestObjects;
	}

	/**
	 * Get all the objects which burglar being controlled by this Route is adjacent to.
	 * <p>NOTE: only looks for first type of object in adjacent cells, if there is more than one
	 * object of given class at an adjacent cell only the first is returned. 
	 * @return Buildings at north, east, south and west positions relative to the burglar's
	 * current position.
	 */
	private <T> List<T> getAdjacentObjects(Class<T> clazz, Coord c, Environment<T> env) {
//		Coord c = GlobalVars.BURGLAR_ENVIRONMENT.getCoords(this.burglar);
		List<T> l = new ArrayList<T>(4); // Will never get more than four adjacent buildings
		T b;
		// Will check that not looking for building outside grid build
		if (c.getY()<GlobalVars.GRID_PARAMS.YDIM-1) {
			b = env.getObjectAt(clazz, new Coord(c.getX(), c.getY()+1));
			if (b!=null) l.add(b);
		}
		if (c.getY()>0) {
			b = env.getObjectAt(clazz, new Coord(c.getX(), c.getY()-1));
			if (b!=null) l.add(b);		
		}
		if (c.getX()<GlobalVars.GRID_PARAMS.XDIM-1) {
			b=env.getObjectAt(clazz, new Coord(c.getX()+1, c.getY()));
			if (b!=null) l.add(b);
		}
		if (c.getX()>0) {
			b=env.getObjectAt(clazz, new Coord(c.getX()-1, c.getY()));
			if (b!=null) l.add(b);
		}
		return l;
	}
	
	public void clearCaches() {
		if (closestObjectsCache!=null) 
			closestObjectsCache.clear();		
	}
	
	/**
	 * Create different types of route
	 * @throws Exception 
	 */
	protected List<Coord> setRoute(SearchAlg.SEARCH_TYPES type) {
		/* Creates a route for a 'bulls eye' search routine. Assumes that the agent starts at a 'target'
		 * point (getting to the target is not part of the search routine). Works as follows:
		 * 1. choose a random direction along the current road(s) in which to travel. Keep a record
		 * of roads which have been travelled and number of times.
		 * 2. when reaching a junction choose a new road to travel down based on the direction (want
		 * angle of 180 (0.5 pi) to target (to orbiting target)) and number of times visited
		 * (prefer roads not travelled down).
		 * 3. repeat 2 until search time has finished.
		 */
		if (type.equals(SearchAlg.SEARCH_TYPES.BULLS_EYE)) {
			// Required otherwise get nullPointer although not used by searching alg:
			Building b = GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(House.class);
			this.destination = GlobalVars.BUILDING_ENVIRONMENT.getCoords(b);
			this.destinationBuilding = b;	
			Map<Junction, Integer> passedJunctions = new Hashtable<Junction, Integer>();
			List<Coord> theRoute = new ArrayList<Coord>();
			Coord target = GlobalVars.BURGLAR_ENVIRONMENT.getCoords(burglar);
			
			// If not on a road, move to the nearest road (junction) coordiante
			Coord currentCoord = GlobalVars.BURGLAR_ENVIRONMENT.getCoords(this.burglar);
			List<Junction> closestJuncs = getNearestJunctions(currentCoord);
			Junction currentJunction = closestJuncs.get(RandomHelper.nextIntFromTo(0, closestJuncs.size()-1));
			theRoute.add(currentJunction.getCoords());
			
			Junction previousJunction = null; // Remember which junction has just been visited
			
			/* Main algorithm. Analyse all possible directions and decide which is the most
			 * attractive (but don't allow moving back over the most recently visited road unless no
			 * alternatives are available to stop agents backtracking) */
			
			double maxSearchTime = //Number of iterations to spend searching ( searchTime(hours) * iterPerHour )  
				GlobalVars.BULLS_EYE_SEARCH_TIME * (GlobalVars.ITER_PER_DAY/24.0);
			double timeSearching = 0; // Will always spend same number of iterations searching 
			while (timeSearching <= maxSearchTime ) {
				// NOTE: will need to change iterPerDay for this search time to be correct
				// Find the most attractive adjacent junction
				Junction mostAttractiveJ = null;
				double highestAttract = Double.MIN_VALUE;
//				previousJunction = currentJunction;
//				System.out.println("\t\tExamining junctions around "+currentJunction.toString()+":");
				for (Junction j:this.getAdjacentObjects(Junction.class, currentJunction.getCoords(), GlobalVars.JUNCTION_ENVIRONMENT)) {
					double angle = Route.angle(currentJunction.getCoords(), j.getCoords(), target);
					Integer numVisits = passedJunctions.get(j);
					if (numVisits == null) numVisits = 0;
					double attract = Route.BE_calcAngleAttractiveness(angle, numVisits);
					if (j.equals(previousJunction))
						attract = 2*Double.MIN_VALUE; // Previously visited road is the least attractive (*2 so that if previous road is the only one available it will still be chosen)
//					System.out.println("\t\t\t"+j.toString()+"\n\t\t\t Angle: "+angle+"\n\t\t\t numVisits: "+numVisits+"\n\t\t\t attract: "+attract);
					if (attract>highestAttract) {
						mostAttractiveJ = j;
						highestAttract = attract;
					}
				} // for junctions
				// Have found most attractive junction
				theRoute.add(mostAttractiveJ.getCoords());
				previousJunction = currentJunction; // Remember the junction last visited
				currentJunction = mostAttractiveJ;
				if (passedJunctions.containsKey(currentJunction))
					passedJunctions.put(currentJunction, passedJunctions.get(currentJunction)+1);
				else
					passedJunctions.put(currentJunction, 1);
				
				timeSearching++;
			} // while time searching

			return theRoute;
//			return this.setRoute();
			
		}
		else {
			Outputter.errorln("GISRoute.setRoute(type) error, haven't written the code for this type of " +
					"route: "+type.toString());
		}
		return null;
	}

	
}

/* OLD GENERIC FUNCTIONS USED BEFORE I MOVED THEM FROM SIMPHONYGRIDENVIRONMENT TO HERE */

///**
//* Get the object at the specified coordinates. Return null and print an error if there is
//* no object at the coordinates.
//* @param c
//* @return
//*/
//public T getObjectAt(Coord c) {
//	T object = null;
//	object = this.projection.getObjectAt((int)c.getX(), (int)c.getY());
//	if (object == null) {
//		//Outputter.errorln("SimphonyGridEnvironment: getObjectAt: no object found at: "+c.toString());
//		return null;
//	}
//	return object;
//}
//
///**
// * Get's the nearest object(s) to the coordainte. If multiple objects are the same distance
// * from the coordinate they will all be returned. The superclass has to be passed as well 
// * because all objects close to the Coord are cached, the only way to do this is pass
// * the superclass to the Context.getObjects(clazz) method. (I can't work out how to find out
// * what the superclass is within the method, e.g. something like Context.getObjects(?.getSuperclass()) ). 
// * @param clazz The class of object to return
// * @param superclass The super class of clazz (might be different).
// * @param c The coordinate to search from
// * @return A list of the nearest object(s)
// */
//public List<? extends T> getNearestObjects(Class<? extends T> clazz, Coord c) {
//	// Check if the nearest objects for this coordinate have already been cached
////	System.out.println("CHECKING CACHE FOR "+c.toString()+": ");
////	System.out.println("CONTAINS KEY? "+this.closestObjectsCache.containsKey(c));
//	if (this.closestObjectsCache.containsKey(c) && this.closestObjectsCache.get(c).objectTypes.contains(clazz)) {
////		System.out.println("COORD IN CACHE");
////		System.out.println("CONTENTS OF CACHE: "+this.closestObjectsCache.keySet().toString());
//		return getObjectsOfType(clazz, this.closestObjectsCache.get(c).objects);
//	}
////	System.out.println(c.toString()+"NOT IN CACHE");
//		
//	// Nothing has been cached so get every near object of the correct type
//	List<? extends T> objects = new ArrayList<T>();
//	List<Double> distances = new ArrayList<Double>();
//	objects = this.getAllObjects(clazz);
//	for (T object:objects) {
//		Coord objCoord = this.getCoords(object);
//		distances.add(this.projection.getDistance(
//				new GridPoint((int)c.getX(), (int)c.getY()), new GridPoint((int)objCoord.getX(), (int)objCoord.getY())));
//	}
//	// Find the shortest distance:
//	double minDist = Double.MAX_VALUE;
//	for (Double d:distances) if (d<minDist) minDist = d;
//	// Now run through lists again adding all the closest points
//	List<T> closestObjects = new ArrayList<T>();
//	for (int i=0;i<objects.size();i++) {
//		if (distances.get(i)==minDist) {
//			closestObjects.add(objects.get(i));
//		}
//	}
////	System.out.print("FOUND CLOSEST OBJECTS TO "+c.toString()+": ");
////	for (T ob:closestObjects) {
////		System.out.print(((Junction)ob).getCoord().toString()+" ");
////	}
//	// Add the closest objects to the cache then return them
//	if (this.closestObjectsCache.containsKey(c)) {
//		// Some objects of a different type have already been added for this coord, so add the new ones just found
////		System.out.println("ADDING OBJECTS TO ESISTING COORD IN CACHE");
//		this.closestObjectsCache.get(c).objectTypes.add(clazz);
//		this.closestObjectsCache.get(c).objects.addAll(closestObjects);
//	}
//	else {
////		System.out.println("ADDING OBJECTS TO NEW CACHE ENTRY");
//		this.closestObjectsCache.put(c, new CoordCache<T>(clazz, closestObjects));
//	}
//	
//	return closestObjects;
//}
//
//// Convenience function, only returns objects of a given type from the array
//private List<? extends T> getObjectsOfType(Class<? extends T> clazz, List<? extends T> allObjects) {
//	List<T> objects = new ArrayList<T>();
//	for (T o:allObjects) {
//		if (clazz.isAssignableFrom(o.getClass())) {
//			objects.add(o);
//		}
//	}
//	return objects;
//}
//
///* For efficiency if getNearestObjects() is called, cache the results. Need to check what types of objects
// * have been put into the cache for each coord (e.g. Houses might have been found surrounding a coordinate
// * but we might now want to find Workplaces */
//class CoordCache <U> {
//	public List<Class<? extends U>> objectTypes; // Remember the types of objects which have been cached
//	public List<U> objects; // A list of all the cached objects
//	public CoordCache(Class<? extends U> clazz, List<U> objects) {
//		this.objectTypes=new ArrayList<Class<? extends U>>();
//		objectTypes.add(clazz);
//		this.objects = objects;
//	}
//}	
//
//// Convenience function, only returns objects of a given type from the array
//private List<? extends T> getObjectsOfType(Class<? extends T> clazz, List<? extends T> allObjects) {
//	List<T> objects = new ArrayList<T>();
//	for (T o:allObjects) {
//		if (clazz.isAssignableFrom(o.getClass())) {
//			objects.add(o);
//		}
//	}
//	return objects;
//}


