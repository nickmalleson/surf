package burgdsim.environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.geotools.referencing.GeodeticCalculator;

import cern.colt.Arrays;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.distance.DistanceOp;


import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.graph.ShortestPath;

import burgdsim.burglars.Burglar;
import burgdsim.burglary.SearchAlg;
import burgdsim.environment.buildings.Building;
import burgdsim.environment.buildings.House;
import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;

/**
 * Simple prototype moves agent directly towards their destination one unit at a time
 * 
 * @author Nick Malleson
 *
 */

public class GISRoute extends Route implements Cacheable {

	// Cache every coordinate which forms a road so that Route.onRoad() is quicker. Also save the Road(s)
	// they are part of, useful for the agent's awareness space (see getRoadFromCoordCache()).
	private static Map<Coord, List<Road>> coordCache;
	// Cache the nearest road Coordinate to every building for efficiency (agents usually/always need to get
	// from the centroids of houses to/from the nearest road).
	private static NearestRoadCoordCache nearestRoadCoordCache;
	/** Store which road every building is closest to. This is used to efficiently add buildings to the
	 * agent's awareness space */
	private static BuildingsOnRoadCache buildingsOnRoadCache;
	private static Object buildingsOnRoadCacheLock = new Object(); // To stop threads competing for the cache
	/** Store a route once it has been created, might be used later (note that the same object acts as key and
	 * value. */
	private static Map<CachedRoute, CachedRoute> routeCache ;
	/** Store a route distance once it has been created */
	private static Map<CachedRouteDistance, Double> routeDistanceCache ;

	// Keep a record of the last community and road passed so that the same buildings/communities aren't added
	// to the cognitive map multiple times (the agent could spend a number of iterations on the same road or
	// community).
	private Road previousRoad;
	private Community previousCommunity;
	// This map route coordinates to their containing Road, used so that when travelling we
	// know which road/community the agent is on
	private Map<Coord, Road> roads = new Hashtable<Coord, Road>();

	// Record which function has added each coord, useful for debugging
	private List<String> routeDescription;

	// Some parts of the route can be traversed faster (due to available transportation)
	private double speed = 1;



	//	private GeometryFactory geomFac = new GeometryFactory();

	/**
	 * See Route() for reason that this constructor is necessary.
	 */
	public GISRoute() {}

	/**
	 * Create a GISRoute which will head to the given coordinates (which correspond to the, optional,
	 * destination building.
	 * 
	 * @param burglar The burglar who this Route is being created for
	 * @param destination The coordinates they will move to
	 * @param destinationBuilding The (optional) building the agent is travelling to, this is useful
	 * for debugging (can find the building looking at original GIS data).
	 * @param roadGeography The GIS projection associated with the ROAD_ENVIRONMENT. This is required
	 * to get the geometries of the Roads. XXXX - would be nice not to have to use this, doesn't
	 * fit with the idea of using Environments as wrappers for contexts and projections. Could store
	 * the Road geometries in the Road objects themselves?
	 */
	public GISRoute(Burglar burglar, Coord destination, Building destinationBuilding, SearchAlg.SEARCH_TYPES type,
			Geography<Road> roadGeography	) {
		super(burglar, destination, destinationBuilding, type, roadGeography);
		// NOTE: super() will call setRoute() now, before all these other assignments have been made!
		//		this.geomFac = new GeometryFactory();
		GlobalVars.CACHES.add(this); // Required so the caches will be cleared if the sim is restarted
		Outputter.debugln("GISRoute(): new GISRoute created for '"+burglar.toString()+"' of type '"
				+(type==null ? "null" : type.toString())+"'. Destination: "+
				(this.destination==null ? "null" : this.destination.toString()) +"("+	// Sometimes null for BULLS_EYE routes
				(this.destinationBuilding==null ? "null" : this.destinationBuilding.toString())+
				") using geography: "+roadGeography.getName(),Outputter.DEBUG_TYPES.ROUTING);
	}

	/** A route is a list of Coordinates which describe the route to a destination restricted to a road 
	 *  network. The algorithm consists of three major parts:
	 *  <ol> 
	 *  <li>Find out if the agent is on a road already, if not then move to the nearest road segment</li>
	 *  <li>Get from the current location (probably mid-point on a road) to the nearest junction</li>
	 *  <li>Travel to the junction which is closest to our destination (using Dijkstra's shortest path)</li> 
	 *  <li>Get from the final junction to the road which is nearest to the destination<li>
	 *  <li>Move from the road to the destination</li>
	 *  </ol> 
	 * @throws Exception 
	 */
	protected List<Coord> setRoute() throws Exception {
		long time = System.nanoTime();
		List<Coord> theRoute = new ArrayList<Coord>();
		this.routeDescription = new ArrayList<String>();
		List<Coord> tempList = new ArrayList<Coord>(); // Used to temporarily store coords before adding to list (debugging)
		Outputter.debugln("GISRoute is Planning route for: "+this.burglar.toString()+" to: "+this.destinationBuilding.toString()+
				"using transport: "+this.burglar.getTransportAvailable().toString(), Outputter.DEBUG_TYPES.ROUTING);
		if (atDestination()) {
			Outputter.debugln("Already at destination, cannot create a route for "+this.burglar.toString(),
					Outputter.DEBUG_TYPES.ROUTING);
			return null;
		}		
		Coord currentCoord = GlobalVars.BURGLAR_ENVIRONMENT.getCoords(burglar);
		Coord destCoord = this.destination;

		// See if a route has already been cached.
		if (GISRoute.routeCache == null ){ 
			GISRoute.routeCache = new Hashtable<CachedRoute, CachedRoute>();
		}
		CachedRoute cachedRoute = new CachedRoute(currentCoord, destCoord, this.burglar.getTransportAvailable());
		synchronized (GISRoute.routeCache) {
			if (GISRoute.routeCache.containsKey(cachedRoute)) {
				Outputter.debugln("GISRoute.setRoute, found a cached route from "+currentCoord+" to "+destCoord+
						" using available transport "+this.burglar.getTransportAvailable()+", returning it.",
						Outputter.DEBUG_TYPES.ROUTING);
				// Return a clone of the route that is stored in the cache
				List<Coord> routeClone = Cloning.copy(GISRoute.routeCache.get(cachedRoute).getRoute());
				// Also need to set the routeSpeeds, these are needed by travel()
				LinkedHashMap<Coord, Double> routeSpeedsClone = Cloning.copy(GISRoute.routeCache.get(cachedRoute).getRouteSpeeds());
				this.routeSpeeds = routeSpeedsClone;
				return routeClone;
			}
		} // synchronized

		// No route cached, have to create a new one (and cache it at the end).	
		try{
			/* ** See if the current position and the destination are on road segments. ** */
			// If the destination is not on a road segment we have to move to the closest road segment, then onto
			// the destination.
			boolean destinationOnRoad = true;
			Coord finalDestination = null;
			if (!coordOnRoad(currentCoord)) {
				// Not on a road so the first coordinate to add to the route is the point on the closest road segment.
				//				Coord nearestRoadCoord = getNearestRoadCoord(currentCoord);
				currentCoord = getNearestRoadCoord(currentCoord);
				theRoute.add(currentCoord);
				this.routeDescription.add("setRoute() initial");
			}
			if (!coordOnRoad(destCoord)) {
				// Not on a road, so need to set the destination to be the closest point on a road, and set the
				// destinationOnRoad boolean to false so we know to add the final dest coord at the end of the route
				//				Coord nearestRoadCoord = getNearestRoadCoord(destCoord);
				destinationOnRoad = false;
				finalDestination = destCoord; // this will be added to the route at the end of the alg.
				destCoord = getNearestRoadCoord(destCoord);

			}

			/* ** Find the nearest junctions to our current position (road endpoints)** */ 
			// Find the road that this coordinate is on
			//TODO EFFICIENCY: often the agent will be creating a new route from a building so will always find the same road, could use a cache 
			Road currentRoad = EnvironmentFactory.findRoadAtCoordinates(currentCoord, roadGeography);
			// Find which Junction is closest to us on the road.
			List<Junction> currentJunctions = currentRoad.getJunctions();

			/* ** Find the nearest Junctions to our destination (road endpoints) ** */
			// Find the road that this coordinate is on
			Road destRoad = EnvironmentFactory.findRoadAtCoordinates(destCoord, roadGeography);
			// Find which Junction connected to the edge is closest to the coordinate.
			List<Junction> destJunctions = destRoad.getJunctions();
			/* Now have four possible routes (2 origin junctions, 2 destination junctions)
			 * need to pick which junctions form shortest route */
			Junction[] routeEndpoints = new Junction[2];
			List<RepastEdge<Junction>> shortestPath = getShortestRoute(currentJunctions, destJunctions, routeEndpoints);
			//			NetworkEdge<Junction> temp = (NetworkEdge<Junction>) shortestPath.get(0);
			Junction currentJunction = routeEndpoints[0];
			Junction destJunction = routeEndpoints[1];

			/* ** Add the coordinates describing how to get to the nearest junction** */
			tempList = this.getCoordsAlongRoad(
					currentCoord, 
					GlobalVars.JUNCTION_ENVIRONMENT.getCoords(currentJunction), 
					currentRoad, true);
			theRoute.addAll(tempList);
			for (int i=0; i<tempList.size(); i++) this.routeDescription.add("getCoordsAlongRoad (toJunction)");


			/* ** Add the coordinates and speeds which describe how to move along the chosen path ** */
			this.routeSpeeds = getRouteBetweenJunctions(shortestPath, currentJunction);
			for (Coord c:this.routeSpeeds.keySet()) { // Now add the coords to the route list
				theRoute.add(c);
			}			

			for (int i=0; i<tempList.size(); i++) this.routeDescription.add("getRouteBetweenJunctions()");

			/* ** Add the coordinates describing how to get from the final junction to the destination ** */
			tempList = this.getCoordsAlongRoad(
					GlobalVars.JUNCTION_ENVIRONMENT.getCoords(destJunction), destCoord, destRoad, false);
			theRoute.addAll(tempList);
			for (int i=0; i<tempList.size(); i++)
				this.routeDescription.add("getCoordsAlongRoad (fromJunction)");

			if (!destinationOnRoad) {
				theRoute.add(finalDestination);
				this.routeDescription.add("setRoute final");
			}


			theRoute = removePairs(theRoute); // If the algorithm was better no coordinates would have been duplicated
			//			Outputter.debugln("GISRoute.setRoute() created route ("+(0.000001*(System.nanoTime()-time))+" ms): ",
			//					Outputter.DEBUG_TYPES.ROUTING);
			//			for (Coord c:theRoute)
			//				Outputter.debugln("\t"+c.toString(), Outputter.DEBUG_TYPES.ROUTING);			
		}catch (Exception e) {
			Outputter.errorln("GISRoute.setRoute(): Problem creating route for "+this.burglar.toString()+" going from "+
					currentCoord.toString()+" to "+this.destination.toString()+"("+
					(this.destinationBuilding == null ? "" : this.destinationBuilding.toString())+").\n"+
					"See earlier messages error messages for more info. Error type, message and stack " +
					"trace: "+e.getClass().getName()+", "+e.getMessage());
			Outputter.errorln(e.getStackTrace());
			// If testing the environment then fire an error event, these can be picked up by TestEnvironment class
			if (GlobalVars.TEST_ENVIRONMENT) {
				this.fireErrorEvent( (destinationBuilding==null) ?
						new ErrorEvent(this, "GISRoute couldn't create a route to the destination." +
								"Destination building not known, here's the message: "+e.getMessage()) :
									new ErrorEvent(this, "GISRoute couldn't create a route to the destination", destinationBuilding) 
				);
			}
			throw e;
		}
		// Cache the route and route speeds (have to clone them because the coordinates are removed from 
		// the original route as the agent travel)
		List<Coord> routeClone = Cloning.copy(theRoute);
		LinkedHashMap<Coord, Double> routeSpeedsClone = Cloning.copy(this.routeSpeeds);
		cachedRoute.setRoute(routeClone);
		cachedRoute.setRouteSpeeds(routeSpeedsClone);
		synchronized (GISRoute.routeCache) {
			GISRoute.routeCache.put(cachedRoute, cachedRoute); // Same cached route is both value and key
		} 
		Outputter.debugln("...GISRoute cacheing new route with unique id "+cachedRoute.hashCode(), 
				Outputter.DEBUG_TYPES.ROUTING);

		Outputter.debugln("...GISRoute Finished planning route with "+theRoute.size()+" coords in "+
				(0.000001*(System.nanoTime()-time))+"ms. Route: "+theRoute.toString(), Outputter.DEBUG_TYPES.ROUTING);

		return theRoute;
	}

	/**
	 * Travel towards our destination, as far as we can go this turn.
	 * <p> Also adds houses to the agent's cognitive environment. This is done by saving each coordinate
	 * the person passes, creating a polygon with a radius given by the "cognitive_map_search_radius" and
	 * adding all houses which touch the polygon.
	 * <p> Note: the agent might move their position many times depending on how far they are allowed to move
	 * each turn, this requires many calls to geometry.move(). This function could be improved (quite easily)
	 * by working out where the agent's final destination will be, then calling move() just once. 
	 * 
	 * @param housesPassed If not null then the buildings which the agent passed during their travels this iteration
	 * will be calculated and stored in this array. This can be useful if a burglar needs to know which houses it has just
	 * passed and, therefore, which are possible victims. This isn't done by default because it's quite an
	 * expensive operation (lots of geographic tests which must be carried out in each iteration). If the array is
	 * null then the houses passed are not calculated.
	 * @return null or the buildings passed during this iteration if housesPassed boolean is true
	 * @throws Exception 
	 */
	public void travel() throws Exception {
		super.travel();
		try {
			//		travel2(); return;
			if (atDestination()) {
				Outputter.debugln("GISRoute.travel(): Person "+this.burglar.toString()+" is at destination, not moving",
						Outputter.DEBUG_TYPES.ROUTING);
				return;
			}
			//			synchronized (GlobalVars.TRANSPORT_PARAMS.currentBurglarLock) { // Only one Burglar can be current burglar
			//				GlobalVars.TRANSPORT_PARAMS.currentBurglar = this.burglar;
			double time = System.nanoTime();
			Outputter.debugln("GISRoute: Travelling (person "+this.burglar.toString()+")...",
					Outputter.DEBUG_TYPES.ROUTING);
			// Store the roads the agent walks along (used to populate the awareness space)
			List<Road> roadsPassed = new ArrayList<Road>();
			double distTravelled = 0; 	// The distance travelled so far
			Coord currentCoord = null;	// Current location
			Coord target = null; 		// Target coordinate we're heading for (in route list)
			boolean travelledMaxDist = false;	// True when travelled maximum distance this iteration 
			currentCoord = GlobalVars.BURGLAR_ENVIRONMENT.getCoords(this.burglar); // Current location
			//		Outputter.debugln("GISRoute.travel(). ENTERING TRAVEL LOOP. Will travel max: "+
			//				GlobalVars.GIS_PARAMS.TRAVEL_PER_TURN, Outputter.DEBUG_TYPES.ROUTING);

			while (!travelledMaxDist && !atDestination() ) {
				//			Outputter.debugln("\n\tGISRoute.travel(). CurrentCoord: "+currentCoord.toString(),Outputter.DEBUG_TYPES.ROUTING);
				target = getRoute(0);			
				// Remember which roads have been passed, used to work out what should be added to cognitive map
				// Only add roads once the agent has moved all the way down them, i.e. when their endpoint has been
				// removed from the route list (otherwise will be adding roads *before* agent has reached them).
				roadsPassed.add(
						this.roads.get(
								this.previousRouteCoord()));  			
				// Work out the distance and angle to the next coordinate
				double[] distAndAngle = new double[2];
				GISRoute.distance(currentCoord.toCoordinate(), target.toCoordinate(), distAndAngle);
				double distToTarget =  distAndAngle[0] / this.speed; // divide by speed because distance might effectively be shorter
				//			Outputter.debugln("\tDist to target is: "+distToTarget, Outputter.DEBUG_TYPES.ROUTING);

				// If we can get all the way to the next coords on the route then just go there
				if (distTravelled+distToTarget < GlobalVars.GIS_PARAMS.TRAVEL_PER_TURN) {
					//				Outputter.debugln("\tMoving all they way to next coord on route", Outputter.DEBUG_TYPES.ROUTING);
					distTravelled += distToTarget;
					// GlobalVars.BURGLAR_ENVIRONMENT.move(this.burglar, target);
					currentCoord = target;
					// Set a new speed value from this coord to the next one.
					Double tempSpeed = this.routeSpeeds.get(currentCoord); // If null then agent has to walk (speed = 1, i.e. no change)
					this.speed = ( tempSpeed == null ) ? 1 : tempSpeed ;
					// Need to check if the agent is on a transport (not walking or driving). This will meant they are
					// locked until they reach the next junction and that buildings aren't added to the awareness space.
					// NOTE: if in a car and on a major road they will also be locked, this is ok though.
					if (! (this.speed == GlobalVars.TRANSPORT_PARAMS.getSpeed(GlobalVars.TRANSPORT_PARAMS.WALK)
							|| this.speed == GlobalVars.TRANSPORT_PARAMS.getSpeed(GlobalVars.TRANSPORT_PARAMS.CAR)) ) {
						this.onTransportRoute = true;
						//					Outputter.debugln("\t\tOn a transport route", Outputter.DEBUG_TYPES.ROUTING);
						// Possible another action might want to take control of the agent (will have set
						// Route.agentAwaitingUnlock. Don't move onto the next station
						if (this.agentAwaitingUnlock) {
							//						Outputter.debugln("\t\tAgent is awaiting unlock, stopping", Outputter.DEBUG_TYPES.ROUTING);
							GlobalVars.BURGLAR_ENVIRONMENT.move(burglar, currentCoord);
							this.onTransportRoute = false;
							break; // Break out of while loop
						}
					} // if speed > 1
					else {
						this.onTransportRoute = false;
						//					Outputter.debugln("\t\tNot on a transport route", Outputter.DEBUG_TYPES.ROUTING);
					}				
					//				Outputter.debugln("\tMoving to next coord on list"+target.toString()+
					//						", now dist travelled="+distTravelled, Outputter.DEBUG_TYPES.ROUTING);
					// See if agent has reached the end of the route.
					if (this.removeFirstCoordFromRoute()) {
						//					Outputter.debugln("\t**Travel(): \t\tTravel(): Have reached end of route.",
						//							Outputter.DEBUG_TYPES.ROUTING);
						GlobalVars.BURGLAR_ENVIRONMENT.move(burglar, currentCoord);
						break; // Break out of while loop, have reached end of route.
					}
				} // if can get all way to next coord

				// Check if dist to next coordinate is exactly same as maximum distance allowed to travel (unlikely but possible)
				else if (distTravelled+distToTarget==GlobalVars.GIS_PARAMS.TRAVEL_PER_TURN) {
					travelledMaxDist = true;
					GlobalVars.BURGLAR_ENVIRONMENT.move(burglar, target);
					Outputter.errorln("\tTravel(): UNUSUAL CONDITION HAS OCCURED!");
					//						RunEnvironment.getInstance().pauseRun();
				}
				else { // Otherwise move as far as we can towards the target along the road we're on
					// Move along the vector the maximum distance we're allowed this turn (take into account relative speed)
					double distToTravel = (GlobalVars.GIS_PARAMS.TRAVEL_PER_TURN-distTravelled)*speed;
					//				Outputter.debugln("\t*Travel(): Last move, moving "+distToTravel+
					//						" at angle "+distAndAngle[1]+", next Coord is: "+this.getRoute(0).toString(), 
					//						Outputter.DEBUG_TYPES.ROUTING);
					// Move the agent, first move them to the current coord (the first part of the while loop
					// doesn't do this for efficiency)
					GlobalVars.BURGLAR_ENVIRONMENT.move(burglar, currentCoord);
					//  Now move by vector towards target (calculated angle earlier).
					GlobalVars.BURGLAR_ENVIRONMENT.moveByVector(this.burglar, distToTravel, distAndAngle[1]);
					travelledMaxDist = true;
				} // else			
			} // while
			//		Outputter.debugln("GISRoute.travel(). LEAVING TRAVEL LOOP. Agent moved to: "+
			//				GlobalVars.BURGLAR_ENVIRONMENT.getCoords(burglar)+"\n",Outputter.DEBUG_TYPES.ROUTING);

			// Agent has finished moving, now just add all the buildings and communities passed to their awareness
			// space (unless they're on a transport route). 
			// Note also that if on a transport route without an associated road no roads are added to the 
			// 'roads' map so even if the check wasn't made here no buildings would be added anyway.
			Community c = null;
			if (!this.onTransportRoute) {
				String outputString = "GISRoute.travel() adding following to awareness space for '"+
				this.burglar.toString()+"':";
				Road current = roadsPassed.get(0); // roadsPassed will have duplicates, this is used to ignore them
				// TODO The next stuff is a mess when it comes to adding communities to the memory. Need to go
				// through and make sure communities aren't added too many times (i.e. more than once for each journey)
				// and that they are always added when they should be.
				for (Road r:roadsPassed) { // last road in list is the one the agent finishes iteration on
					if (r!=null && roadsPassed.get(0)!=null && !current.equals(r)) {
						// Check road isn't null () and that buildings on road haven't already been added
						// (road can be null when coords that aren't part of a road are added to the route)
						current = r;
						if (r.equals(this.previousRoad)) {
							// The agent has just passed over this road, don't add the buildings or communities again
						}else {
							outputString += "\n\t"+r.toString()+": ";
							List<Building> passedBuildings = getBuildingsOnRoad(r);
							List<Community> passedCommunities = new ArrayList<Community>();
							if (passedBuildings!=null) { // There might not be any buildings close to the road (unlikely)
								outputString += passedBuildings.toString();
								this.passedObjects(passedBuildings, Building.class);
								// For efficiency just find one of the building's communities and hope no other
								// communities were passed through - NO! I'VE CHANGED THIS BELOW!
								c = passedBuildings.get(0).getCommunity();
								// Check all buildings to make sure that if the agent has passed more than one community
								// then they are all added.
								for (Building b:passedBuildings) {
									if (!passedCommunities.contains(b.getCommunity())) {
										passedCommunities.add(b.getCommunity());
									}
								}
								for (Community com:passedCommunities) {
									if (com!=null) {
										this.passedObject(com, Community.class);	
									}
								}

							}
							else { // Community won't have been added because no buildings passed, use slow method
								c = GlobalVars.COMMUNITY_ENVIRONMENT.getObjectAt(Community.class, currentCoord);
								if (c!=null) {
									this.passedObject(c, Community.class);	
								}
								//TODO I think the following line is wrong, if the agent has made
								// a long move they might have passed right through a community that doesn't
								// have any buildings, perhaps this should check *all* the communities that touch
								// the road, not just the community the agent finished the move in (i.e. currentCoord)
								passedCommunities.add(GlobalVars.COMMUNITY_ENVIRONMENT.getObjectAt(Community.class, currentCoord));
							}
//							if (c!=null && !c.equals(this.previousCommunity)) {
//								this.passedObject(c, Community.class);
//							}
						}
					}
				} // for roadsPassed
				Outputter.debugln(outputString+"\n",  Outputter.DEBUG_TYPES.AWARENESS_SPACE);
			} // if !onTransportRoute
			else {
				Outputter.debug("GISRoute.travel() not adding to burglar '"+this.burglar.toString()+
						"' awareness space beecause on transport route: ", Outputter.DEBUG_TYPES.AWARENESS_SPACE);
			}

			// Finally set the previousRoad and previousCommunity so that if these haven't changed in the next
			// iteration they're not added to the cognitive map again.
			this.previousRoad = roadsPassed.get(roadsPassed.size()-1); // The most recent road passed, i.e. the one the agent finishes on (might pass many in one iteration)
//			this.previousCommunity = c; // This was the most recent community passed over 

			Outputter.debugln("...Finished Travelling("+(0.000001*(System.nanoTime()-time))+"ms)",
					Outputter.DEBUG_TYPES.ROUTING);
			//			} // synchronized GlobalVars.TRANSPORT_PARAMS.currentBurglar
		} catch (Exception e) {
			Outputter.errorln("GISRoute.trave(): Caught error travelling for "+this.burglar.toString()+" going to " +
					"destination "+
					(this.destinationBuilding == null ? "" : this.destinationBuilding.toString())+").\n"+
					"Error type, message and stack "+"trace: "+e.getClass().getName()+", "+e.getMessage());
			Outputter.errorln(e.getStackTrace());
			// If testing the environment then fire an error event, these can be picked up by TestEnvironment class
			if (GlobalVars.TEST_ENVIRONMENT) {
				this.fireErrorEvent( (destinationBuilding==null) ?
						new ErrorEvent(this, "GISRoute get to the destination while travelling." +
								"Destination building not known, here's the message: "+e.getMessage()) :
									new ErrorEvent(this, "GISRoute get to the destination while travelling", destinationBuilding) 
				);
			} // if testing environment
			throw e;
		} // catch exception
	}

	@Override
	public double getDistance(Burglar theBurglar, Coord origin, Coord destination) {
		Outputter.debug("GISRoute.getDistance(): ", Outputter.DEBUG_TYPES.ROUTING);

		// See if this distance has already been calculated
		if (GISRoute.routeDistanceCache == null) {
			GISRoute.routeDistanceCache = new Hashtable<CachedRouteDistance, Double>(); 
		}
		CachedRouteDistance crd = new CachedRouteDistance(origin, destination, theBurglar.getTransportAvailable());

		synchronized (GISRoute.routeDistanceCache) {
			Double dist = GISRoute.routeDistanceCache.get(crd);
			if (dist != null) {
				Outputter.debugln("GISRoute.ggetDistance, found a cached route distance from "+origin+" to "+destination
						+" using available transport "+theBurglar.getTransportAvailable()+", returning it.",
						Outputter.DEBUG_TYPES.ROUTING);
				return dist;
			}
		}
		// No distance in the cache, calculate it
		synchronized (GlobalVars.TRANSPORT_PARAMS.currentBurglarLock) {
			GlobalVars.TRANSPORT_PARAMS.currentBurglar = theBurglar;
			// Find the closest Junctions to the origin and destination		
			double minOriginDist = Double.MAX_VALUE; double minDestDist = Double.MAX_VALUE; double dist; 
			Junction closestOriginJunc = null; Junction closestDestJunc = null;
			DistanceOp distOp = null; GeometryFactory geomFac = new GeometryFactory();
			// TODO EFFICIENCY: here could iterate over near junctions instead of all?
			for (Junction j:GlobalVars.JUNCTION_ENVIRONMENT.getAllObjects(Junction.class)) {
				// Check that the agent can actually get to the junction (if might be part of a transport route
				// that the agent doesn't have access to)
				boolean accessibleJunction = false;
				accessibleJunc:
					for (RepastEdge<Junction> e:EnvironmentFactory.getRoadNetwork().getEdges(j)) {
						NetworkEdge<Junction> edge = (NetworkEdge<Junction>) e;
						for (String s:edge.getTypes()) {
							if (theBurglar.getTransportAvailable().contains(s)) {
								accessibleJunction = true;
								break accessibleJunc;
							}
						} // for types
					}// for edges
				if (!accessibleJunction) { // Agent can't get to the junction, ignore it
					continue;
				}
				Point juncPoint = geomFac.createPoint(j.getCoords().toCoordinate());
				// Origin
				distOp = new DistanceOp(juncPoint, geomFac.createPoint(origin.toCoordinate()));
				dist = distOp.distance();
				if (dist<minOriginDist) {
					minOriginDist = dist;
					closestOriginJunc = j;
				}
				// Destination
				distOp = new DistanceOp(juncPoint, geomFac.createPoint(destination.toCoordinate()));
				dist = distOp.distance();
				if (dist<minDestDist) {
					minDestDist = dist;
					closestDestJunc = j;
				}
			} // for Junctions
			Outputter.debugln("found origin and dest juncs: "+closestOriginJunc.toString()+", "+
					closestDestJunc, Outputter.DEBUG_TYPES.ROUTING);
			// Return the shortest path plus the distance from the origin/destination to their junctions
			// NOTE: Bug in ShortestPath so have to make finalize is called, otherwise following lines are neater
			//		return (new ShortestPath<Junction>(
			//		EnvironmentFactory.getRoadNetwork(), closestOriginJunc)) .getPathLength(closestDestJunc)
			//		+ minOriginDist + minDestDist ;
			// TODO : using non-deprecated methods don't work on NGS, probably need to update repast libraries
			ShortestPath<Junction> p = new ShortestPath<Junction>(EnvironmentFactory.getRoadNetwork(), closestOriginJunc);
			double theDist = p.getPathLength(closestDestJunc);
			//			ShortestPath<Junction> p = new ShortestPath<Junction>(EnvironmentFactory.getRoadNetwork());
			//			double theDist = p.getPathLength(closestOriginJunc, closestDestJunc);
			p.finalize(); p=null;
			double finalDist = theDist + minOriginDist + minDestDist ;
			// Cache this distance
			synchronized (GISRoute.routeDistanceCache) {
				GISRoute.routeDistanceCache.put(crd, finalDist);
			}
			return finalDist; 
		} // synchronized

	}

	/**
	 * Find the nearest coordinate which is part of a Road. Returns the coordinate which is actually
	 * the closest to the given coord, not just the corner of the segment which is closest. Uses the
	 * DistanceOp class which finds the closest points between two geometries.
	 * <p>
	 * When first called, the function will populate the 'nearestRoadCoordCache' which calculates
	 * where the closest road coordinate is to each building. The agents will commonly start journeys
	 * from within buildings so this will improve efficiency.
	 * @param inCoord The coordinate from which to find the nearest road coordinate
	 * @return the nearest road coordinate
	 * @throws Exception 
	 */
	private Coord getNearestRoadCoord(Coord inCoord) throws Exception {
		//		double time = System.nanoTime(); 

		synchronized (buildingsOnRoadCacheLock) {
			if (nearestRoadCoordCache==null) {
				Outputter.debugln("GISRoute.getNearestRoadCoord called for first time, " +
						"creating cache of all roads and the buildings which are on them ...",
						Outputter.DEBUG_TYPES.GENERAL);
				// Create a new cache object, this will be read from disk if possible (which is why the
				// getInstance() method is used instead of the constructor.
				File buildingsFile = new File(GlobalVars.GIS_PARAMS.BUILDING_FILENAME);
				File roadsFile = new File(GlobalVars.GIS_PARAMS.ROAD_FILENAME);
				nearestRoadCoordCache = NearestRoadCoordCache.getInstance(
						GlobalVars.BUILDING_ENVIRONMENT, buildingsFile, 
						GlobalVars.ROAD_ENVIRONMENT, roadsFile,
						new File(GlobalVars.GIS_PARAMS.BUILDINGS_ROADS_COORDS_CACHE_LOCATION),
						new GeometryFactory());
			} // if not cached
		} // synchronized
		return nearestRoadCoordCache.get(inCoord);
	}


	/**
	 * Finds the shortest route between multiple origin and destination junctions. Will return the shortest
	 * path and also, via two parameters, can return the origin and destination junctions which make up the
	 * shortest route.
	 * @param currentJunctions An array of origin junctions
	 * @param destJunctions An array of destination junctions
	 * @param routeEndpoints An array of size 2 which can be used to store the origin (index 0) and 
	 * destination (index 1) Junctions which form the endpoints of the shortest route.
	 * @return the shortest route between the origin and destination junctions
	 * @throws Exception 
	 */
	private List<RepastEdge<Junction>> getShortestRoute(List<Junction> currentJunctions, List<Junction> destJunctions,
			Junction[] routeEndpoints) throws Exception {
		double time = System.nanoTime();
		synchronized (GlobalVars.TRANSPORT_PARAMS.currentBurglarLock) {
			// This must be set so that NetworkEdge.getWeight() can adjust the weight depending on how this 
			// particular agent is getting around the city
			GlobalVars.TRANSPORT_PARAMS.currentBurglar = this.burglar; 
			double shortestPathLength = Double.MAX_VALUE;
			double pathLength = 0;
			List<RepastEdge<Junction>> shortestPath = null;
			for (Junction o:currentJunctions) {
				for (Junction d:destJunctions) {
					if (o == null || d == null) {
						Outputter.errorln("GISRoute.getShortestRoute() error: either the destination or origin junction " +
								"is null. This can be caused by disconnected roads. It's probably OK to ignore this as a " +
						"route should still be created anyway.");
					}
					else {
						// NOTE: bug in repast so need to create two ShortestPath objects to get distance and to get path
						ShortestPath<Junction> p1 = new ShortestPath<Junction>(EnvironmentFactory.getRoadNetwork(), o);
						pathLength = p1.getPathLength(d);
						p1.finalize(); p1 = null;
						// TODO : using non-deprecated methods don't work on grid, probably need to update repast libraries
						//						ShortestPath<Junction> p1 = new ShortestPath<Junction>(EnvironmentFactory.getRoadNetwork());
						//						pathLength = p1.getPathLength(o, d);
						if (pathLength< shortestPathLength) {
							shortestPathLength = pathLength;
							ShortestPath<Junction> p2 = new ShortestPath<Junction>(EnvironmentFactory.getRoadNetwork());
							shortestPath = p2.getPath(o, d);
							p2.finalize(); p2 = null;
							//							shortestPath = p1.getPath(o, d);
							//							p1.finalize(); p1 = null;
							routeEndpoints[0] = o;
							routeEndpoints[1] = d;
						}
					} // if junc null
				} // for dest junctions
			} // for origin junctions
			if (shortestPath == null) {
				String debugString = 
					"GISRoute.getShortestRoute() could not find a route. Looking for the shortest route between :\n";
				for (Junction j:currentJunctions)
					debugString+="\t"+j.toString()+", roads: "+j.getRoads().toString()+"\n";
				for (Junction j:destJunctions)
					debugString+="\t"+j.toString()+", roads: "+j.getRoads().toString()+"\n";
				Outputter.errorln(debugString);
				throw new Exception(debugString);
			}
			Outputter.debugln("Route.getShortestRoute ("+(0.000001*(System.nanoTime()-time))+"ms) found shortest path " +
					"(length: "+shortestPathLength+") from "+ routeEndpoints[0].toString()+" to "+
					routeEndpoints[1].toString(), Outputter.DEBUG_TYPES.ROUTING);
			return shortestPath;
		} // synchronized
	}

	/**
	 * Returns the coordinates required to move an agent from their current position to the destination
	 * along a given road. The algorithm to do this is as follows:
	 * <ol><li>Starting from the destination coordinate, record each vertex and check inside the booundary of
	 * each line segment until the destination point is found.</li>
	 * <li>Return all but the last vertex, this is the route to the destination.</li></ol>
	 * A boolean allows for two cases: heading towards a junction (the endpoint of the line) or heading away from the
	 * endpoint of the line (this function can't be used to go to two midpoints on a line) 
	 * 
	 * @param currentCoord
	 * @param destinationCoord
	 * @param road
	 * @param toJunction whether or not we're travelling towards or away from a Junction
	 * @return
	 * @throws Exception 
	 */
	private List<Coord> getCoordsAlongRoad(Coord currentCoord, Coord destinationCoord,
			Road road, boolean toJunction) throws Exception {
		double time = System.nanoTime();
		// NOTE: it is a bit confusing here because I use both Coord and Coordinate objects interchangeably
		Coordinate[] roadCoords = Route.roadGeography.getGeometry(road).getCoordinates();
		ArrayList<Coordinate> routeCoords = new ArrayList<Coordinate>(roadCoords.length); // The list of coordinates to return

		// Check that the either the destination or current coordinate are actually part of the road
		boolean currentCorrect = false, destinationCorrect= false;;
		for (int i=0; i<roadCoords.length; i++) {

			if (toJunction && destinationCoord.equals(roadCoords[i])) { // (It is OK to compare Coords and Coordinates, Coord.equals() checks if a Coordinate is passed) 
				destinationCorrect = true;
				break;
			}
			else if (!toJunction  && currentCoord.equals(roadCoords[i])) {
				currentCorrect = true;
				break;				
			}
		} // for
		if (!(destinationCorrect || currentCorrect)) {
			String roadCoordsString = "";
			for (Coordinate c:roadCoords) roadCoordsString+=c.toString()+" - ";
			throw new Exception("GISRoute: getCoordsAlongRoad: Error, neigher the origin or destination nor the current" +
					"coordinate are part of the road '"+road.toString()+"' (person '"+this.burglar.toString()+"').\n" +
					"Road coords: "+roadCoordsString+"\n"+
					"\tOrigin: "+currentCoord.toString()+"\n"+
					"\tDestination: "+destinationCoord.toString()+" ( "+this.destinationBuilding.toString()+" )\n "+
					"Heading "+(toJunction?"to":"away from")+" a junction, so "+(toJunction?"destination":"origin")+
			" should be part of a road segment");
		}

		// Might need to reverse the order of the road coordinates
		if (toJunction && !destinationCoord.equals(roadCoords[roadCoords.length-1])) {
			ArrayUtils.reverse(roadCoords); // If heading towards a junction, destination coordinate must be at end of road segment
		}
		else if (!toJunction && !currentCoord.equals(roadCoords[0])) {
			ArrayUtils.reverse(roadCoords); // If heading away form junction, current coord must be at beginning of road segment
		}
		GeometryFactory geomFac = new GeometryFactory();
		Point destinationPointGeom = geomFac.createPoint(destinationCoord.toCoordinate());
		Point currentPointGeom = geomFac.createPoint(currentCoord.toCoordinate());
		boolean foundAllCoords = false ; // If still false at end then algorithm hasn't worked
		search: for (int i=0; i<roadCoords.length-1; i++ ) {
			Coordinate[] segmentCoords = new Coordinate[]{roadCoords[i], roadCoords[i+1]};
			LineString segment = geomFac.createLineString(segmentCoords);
			// Draw a small buffer around the line segment and look for the coordinate within the buffer
			Geometry buffer = segment.buffer(GlobalVars.GIS_PARAMS.XXXX_little_buffer);
			if (!toJunction) {
				// If heading away from a junction, keep adding road coords until we find the destination
				routeCoords.add(roadCoords[i]);
				//				this.addToRoute(routeCoords, roadCoords[i]);
				if (destinationPointGeom.within(buffer)) {
					routeCoords.add(destinationCoord.toCoordinate());
					//					this.addToRoute(routeCoords, destinationCoord.toCoordinate());
					foundAllCoords = true;
					break search;
				}
			}
			else if (toJunction) {
				// If heading towards a junction: find the curent coord, add it to the route, then add all
				// the remaining coords which make up the road segment
				if (currentPointGeom.within(buffer)) {
					//					this.addToRoute(routeCoords, destinationCoord.toCoordinate());
					for (int j=i+1; j<roadCoords.length; j++) {
						routeCoords.add(roadCoords[j]);
					}
					routeCoords.add(destinationCoord.toCoordinate());
					// Need to add the currentCoord otherwise this road isn't included in list of passedRoad s
					this.roads.put(currentCoord, road);
					foundAllCoords = true;
					break search;
				}
			}
		} // for
		if (foundAllCoords) {
			List<Coord> returnCoords = Coord.buildCoordArray(routeCoords);
			for (Coord c:returnCoords) this.roads.put(c,road); // Remember the roads each coord is part of
			Outputter.debugln("getCoordsAlongRoad ("+(0.000001*(System.nanoTime()-time))+"ms)",
					Outputter.DEBUG_TYPES.ROUTING);
			return returnCoords;
		}
		else {	// If we get here then the route hasn't been created
			if (GlobalVars.TEST_ENVIRONMENT) { // Error must be thrown when testing the environment
				throw new Exception("GISRoute: getCoordsAlongRoad: could not find destination coordinates " +
						"along the road for this person. Heading "+(toJunction? "towards" : "away from")+" a junction. " +
						"(Person: "+ this.burglar.toString()+")\n"+
						"Destination: "+destinationBuilding.toString()+
						"\nRoad vertex coordinates: "+Arrays.toString(roadCoords));
			}
			// Hack: ignore the error, printing a message and just returning the origin destination and
			// coordinates. This means agent will jump to/from the junction but I can't figure out why the
			// fuck it occasionally doesn't work!! It's so rare that hopefully this isn't a problem.
			Outputter.errorln("GISRoute: getCoordsAlongRoad: error... (not debugging).");
			// A load of debugging info
			//			Outputter.errorln("GISRoute: getCoordsAlongRoad: could not find destination coordinates " +
			//					"along the road, ignoring this error and just returning origin/destintation coords for route\n\t"+
			//					"Heading *"+(toJunction? "towards" : "away from")+"* a junction.\n\t" +
			//					"Person: "+ this.burglar.toString()+")\n\t"+
			//					"Destination building: "+destinationBuilding.toString()+"\n\t"+
			//					"Road causing problems: "+road.toString()+"\n\t"+
			//					"Road vertex coordinates: "+Arrays.toString(roadCoords));
			List<Coord> coords = new ArrayList<Coord>();
			coords.add(currentCoord); coords.add(destinationCoord);
			for (Coord c:coords) this.roads.put(c,road); // Remember the roads each coord is part of
			return coords;

		}
	}

	/**
	 * Returns all the coordinates that describe how to travel along a path, restricted to road coordinates.
	 * In some cases the route wont have an associated road, this occurs if the route is part of a transport
	 * network. In this case just the origin and destination coordinates are added to the route. 
	 *   
	 * @param shortestPath
	 * @param startingJunction The junction the path starts from, this is required so that the
	 * algorithm knows which road coordinate to add first (could be first or last depending on the
	 * order that the road coordinates are stored internally).
	 * @return the coordinates as a mapping between the coord and its associated speed (i.e. how fast the 
	 * agent can travel to the next coord) which is dependent on the type of edge and the agent 
	 * (e.g. driving/walking/bus). LinkedHashMap is used to guarantee the insertion order of the
	 * coords is maintained.
	 */
	private LinkedHashMap<Coord, Double> getRouteBetweenJunctions (List<RepastEdge<Junction>> shortestPath, Junction startingJunction) {
		double time = System.nanoTime();
		if (shortestPath.size()<1) {
			// This could happen if the agent's destination is on the same road as the origin
			return new LinkedHashMap<Coord, Double>();			
		}
		synchronized (GlobalVars.TRANSPORT_PARAMS.currentBurglarLock) {
			GlobalVars.TRANSPORT_PARAMS.currentBurglar = this.burglar;

			// Maintain a list of coords and another list of speeds (with associated indices).
			List<Coord> coords  = new ArrayList<Coord>();
			List<Double> speeds = new ArrayList<Double>();

			// Iterate over all edges in the route adding coords and weights as appropriate
			NetworkEdge<Junction> e; Road r;
			// Use sourceFirst to represent whether or not the edge's source does actually represent the start
			// of the edge (agent could be going 'forwards' or 'backwards' over edge
			boolean sourceFirst;
			for (int i=0; i<shortestPath.size(); i++) {
				e = (NetworkEdge<Junction>) shortestPath.get(i);
				if (i==0) { // No coords in route yet, compare the source to the starting junction
					sourceFirst = (e.getSource().equals(startingJunction)) ? true : false ;
				}			
				else { // Otherwise compare the source to the last coord added to the list
					sourceFirst = (e.getSource().getCoords().equals(coords.get(coords.size()-1))) ? true : false ;
				}
				/* Now add the coordinates describing how to move along the road. If there is no road
				 * associated with the edge (i.e. it is a transport route) then just add the source/dest coords.
				 * Note that the shared coordinates between two edges will be added twice, these must be
				 * removed later*/
				//			r = EnvironmentFactory.getRoadFromEdge(e);
				r = e.getRoad();
				// Get the speed that the agent will be able to travel along this edge (depends on the transport
				// available to the agent and the edge). Some speeds will be < 1 if the agent shouldn't be using
				// this edge but doesn't have any other way of getting to the destination. in these cases set speed
				// to 1 (equivalent to walking).
				double speed = e.getSpeed();
				if (speed < 1) speed = 1;
				if (r==null) { // No road associated with this edge (it is a transport link) so just add source
					if (sourceFirst) {
						coords.add(e.getSource().getCoords());
						coords.add(e.getTarget().getCoords());
					}
					else {
						coords.add(e.getTarget().getCoords());
						coords.add(e.getSource().getCoords());
					}
					speeds.add(speed); 
					speeds.add(-1.0); // Don't know weight between this coord and the next
				}
				else { // This edge is a road, add all the coords which make up its geometry
					Coordinate[] roadCoords = Route.roadGeography.getGeometry(r).getCoordinates();			
					if (roadCoords.length<2 ) 
						Outputter.errorln("GISRoute.getRouteBetweenJunctions: for some reason road " +
								"'"+r.toString()+"' doesn't have at least two coords as part of its geometry ("+
								roadCoords.length+")");
					// Make sure the coordinates of the road are added in the correct order
					if (!sourceFirst) {
						ArrayUtils.reverse(roadCoords);
					}
					// Add all the road geometry's coords
					for (int j=0; j<roadCoords.length; j++ ) {
						Coord c = new Coord(roadCoords[j]);
						coords.add(c);
						//					if (j==roadCoords.length-1) { // The last coord in list
						//						speeds.add(-1.0);
						//					}
						//					else {
						//						speeds.add(e.getSpeed());
						//					}
						speeds.add(speed); // Note that last coord will have wrong weight
						// Remember the road associated with each coordinate (note that if on a transport route
						// nothing will be added to the cognitive map).
						this.roads.put(c,r); 
					} // for roadCoords.length
				} // if road!=null
			}
			if (coords.size()!=speeds.size()) Outputter.errorln("GISRoute. getRouteBetweenJunctions error: the " +
			"size of the coords array doesn't match the size of the weights for some reason!");
			// Will have added sources and destinations twice, now remove them (the will have null weight values)
			for (int i=0; i<speeds.size(); i++) {
				if (speeds.get(i)==-1.0) {
					speeds.remove(i);
					coords.remove(i);
				}
			}
			// Finally add the coords and weights to a map and return
			LinkedHashMap<Coord, Double> map = new LinkedHashMap<Coord, Double>();
			for (int i=0; i<coords.size(); i++) {
				map.put(coords.get(i), speeds.get(i));
			}
			Outputter.debug("getRouteBetweenJunctions ("+(0.000001*(System.nanoTime()-time))+"ms) added " +
					"following (with weights): ", Outputter.DEBUG_TYPES.ROUTING);
			for (Coord c:map.keySet())
				Outputter.debug(c+"("+map.get(c)+"), ", Outputter.DEBUG_TYPES.ROUTING);
			Outputter.debugln("", Outputter.DEBUG_TYPES.ROUTING);
			return map;
		} // synchronized
	} // getRouteBetweenJunctions

	/**
	 * Determine whether or not the person associated with this Route is at their destination. Compares their
	 * current coordinates to the destination coordinates (must be an exact match). 
	 * @return True if the person is at their destination
	 */
	public boolean atDestination() {
		return GlobalVars.BURGLAR_ENVIRONMENT.getCoords(this.burglar).equals(this.destination);
	}

	/**
	 * Removes any duplicate coordinates from the given route (coordinates which are the same
	 * *and* next to each other in the list).
	 * <p>
	 * If my route-generating algorithm was better this would't be necessary.
	 * @param coordList the list of coordinates which will have it's duplicates removed
	 */
	private static List<Coord> removePairs(List<Coord> theRoute) {
		if (theRoute.size()<1){// No coords to iterate over, probably something has gone wrong
			Outputter.errorln("GISRoute.removeDuplicateCoordinates(): WARNING an empty list has been " +
			"passed to this function, something has probably gone wrong");
			return null;
		}
		// Iterate over the list, only adding a coord to the temp list if it isn't the same as the one
		// next to it
		List<Coord> tempList = new ArrayList<Coord>();
		Coord c1, c2; int size = theRoute.size();
		for (int i=0; i<size-1;i++) {
			c1 = theRoute.get(i);
			c2 = theRoute.get(i+1);
			if (!c1.equals(c2)) {
				tempList.add(c1);
			}
			else {
			}
		}
		// Need to check that the last coord is not the same as the one before it, otherwise it won't be added
		c1 = theRoute.get(size-2); c2 = theRoute.get(size-1); 
		if (!c1.equals(c2)) {
			tempList.add(c2);
		}
		return tempList;	
	}

	/**
	 * Returns the angle of the vector from p0 to p1. The angle will be between
	 * -Pi and Pi. I got this directly from the JUMP program source.
	 * @return the angle (in radians) that p0p1 makes with the positive x-axis.
	 */
	public static double angle(Coordinate p0, Coordinate p1) {
		double dx = p1.x - p0.x;
		double dy = p1.y - p0.y;

		return Math.atan2(dy, dx);
	}

	/**
	 * The building which this Route is targeting
	 * @return the destinationHouse
	 */
	public Building getDestinationBuilding() {
		if (this.destinationBuilding==null) {
			Outputter.errorln("GISRoute: getDestinationBuilding(), warning, no destination building has " +
					"been set. This might be ok, the agent might be supposed to be heading to a coordinate " +
			"not a particular building(?)");
			return null;
		}
		return destinationBuilding;
	}

	/**
	 * The coordinate the route is targeting
	 * @return the destination
	 */
	public Coord getDestination() {
		return this.destination;
	}

	/**
	 * Maintain a cache of all coordinates which are part of a road segment. Store the coords and
	 * all the road(s) they are part of.
	 * @param coord The coordinate which should be part of a road geometry
	 * @return The road(s) which the coordinate is part of or null if the coordinate is not part of any
	 * road
	 */
	private List<Road> getRoadFromCoordCache(Coord coord) {

		populateCoordCache(); // Check the cache has been populated
		return coordCache.get(coord);
	}
	/**
	 * Test if a coordinate is part of a road segment.
	 * @param coord The coordinate which we want to test
	 * @return True if the coordinate is part of a road segment
	 */
	private boolean coordOnRoad(Coord coord) {
		populateCoordCache(); // check the cache has been populated
		return coordCache.containsKey(coord);
	}

	private synchronized static void populateCoordCache() {

		double time = System.nanoTime();
		if (coordCache==null) { // Fist check cache has been created
			coordCache = new HashMap<Coord, List<Road>>();
			Outputter.debugln("GISRoute.populateCoordCache called for first time, " +
					"creating new cache of all Road coordinates...", Outputter.DEBUG_TYPES.ROUTING);
		}
		if (coordCache.size()==0) { // Now popualte it if it hasn't already been populated
			Outputter.debugln("GISRoute.populateCoordCache: is empty, " +
					"creating new cache of all Road coordinates...", Outputter.DEBUG_TYPES.ROUTING);			

			for (Road r:GlobalVars.ROAD_ENVIRONMENT.getAllObjects(Road.class)) {
				for (Coordinate c:Route.roadGeography.getGeometry(r).getCoordinates()) {
					if (coordCache.containsKey(c)){
						coordCache.get(c).add(r);
					}
					else {
						List<Road> l = new ArrayList<Road>();
						l.add(r);
						coordCache.put(new Coord(c), l);
					}
				}
			}

			Outputter.debugln("... finished caching all road coordinates (in "+
					0.000001*(System.nanoTime()-time)+"ms)", Outputter.DEBUG_TYPES.ROUTING);			
		}
	}

	/**
	 * Find the buildings which can be accessed from the given road (the given road is the closest to
	 * the buildings). Uses a separate cache object which can be serialised so that the cache doesn't
	 * need to be rebuilt every time.
	 * @param road
	 * @return
	 * @throws Exception 
	 */
	private List<Building> getBuildingsOnRoad(Road road) throws Exception {
		if (buildingsOnRoadCache==null) {
			Outputter.debugln("GISRoute.getBuildingsOnRoad called for first time, " +
					"creating cache of all roads and the buildings which are on them ...",
					Outputter.DEBUG_TYPES.ROUTING);
			// Create a new cache object, this will be read from disk if possible (which is why the
			// getInstance() method is used instead of the constructor.
			File buildingsFile = new File(GlobalVars.GIS_PARAMS.BUILDING_FILENAME);
			File roadsFile = new File(GlobalVars.GIS_PARAMS.ROAD_FILENAME);
			buildingsOnRoadCache = BuildingsOnRoadCache.getInstance(
					GlobalVars.BUILDING_ENVIRONMENT, buildingsFile, 
					GlobalVars.ROAD_ENVIRONMENT, roadsFile,
					new File(GlobalVars.GIS_PARAMS.BUILDINGS_ROADS_CACHE_LOCATION),
					new GeometryFactory());
		} // if not cached
		return buildingsOnRoadCache.get(road);
	}
	//	private List<Building> getBuildingsOnRoad(Road road) {
	//		if (buildingsOnRoadCache==null || buildingsOnRoadCache.size()==0) {
	//			Outputter.debugln("GISRoute.getBuildingsOnRoad called for first time, " +
	//					"creating cache of all roads and the buildings which are on them ...",
	//					Outputter.DEBUG_TYPES.ROUTING);
	//			if (buildingsOnRoadCache==null) 
	//				buildingsOnRoadCache = new Hashtable<Road,List<Building>>();
	//			GeometryFactory geomFac = new GeometryFactory();
	//			for (Building b:GlobalVars.BUILDING_ENVIRONMENT.getAllObjects(Building.class)) {
	//				// Find the closest road to this building
	//				Geometry buildingPoint = geomFac.createPoint(b.getCoords().toCoordinate());
	//				double minDistance = Double.MAX_VALUE;
	//				Road closestRoad = null;
	//				double distance;
	////				for (Road r:GlobalVars.ROAD_ENVIRONMENT.getAllObjects(Road.class)) {
	//				for (Road r:GlobalVars.ROAD_ENVIRONMENT.getObjectsWithin(b.getCoords(), Road.class, GlobalVars.GIS_PARAMS.XXXX_BUFFER, true)) {	
	//					distance = DistanceOp.distance(buildingPoint, Route.roadGeography.getGeometry(r));
	//					if (distance<minDistance) {
	//						minDistance = distance;
	//						closestRoad = r;
	//					}
	//				} // for roads
	//				// Found the closest road, add the information to the buildingsOnRoadCache
	//
	//				if (buildingsOnRoadCache.containsKey(closestRoad)) {
	//					buildingsOnRoadCache.get(closestRoad).add(b);
	//				}
	//				else {
	//					List<Building> l = new ArrayList<Building>();
	//					l.add(b);
	//					buildingsOnRoadCache.put(closestRoad, l);
	//				}
	//			} // for buildings
	//			int numRoads = buildingsOnRoadCache.keySet().size();
	//			int numBuildings = 0; for (List<Building>l:buildingsOnRoadCache.values()) numBuildings+= l.size();
	//			Outputter.debugln("... finished caching roads and buildings. Cached "+numRoads+" roads and "+
	//					numBuildings+" buildings.", Outputter.DEBUG_TYPES.ROUTING);
	//		} // if not cached
	//		return buildingsOnRoadCache.get(road);
	//	}



	/**
	 * Calculate the distance (in meters) between two Coordinates, using the coordinate reference system
	 * that the roadGeography is using. For efficiency it can return the angle as well (in the range -0 to 2PI)
	 * if returnVals passed in as a double[2] (the distance is stored in index 0 and angle stored in index 1).  
	 * @param c1
	 * @param c2
	 * @param returnVals Used to return both the distance and the angle between the two Coordinates. If null
	 * then the distance is just returned, otherwise this array is populated with the distance at index 0
	 * and the angle at index 1. 
	 * @return The distance between Coordinates c1 and c2.
	 */
	public static double distance (Coordinate c1, Coordinate c2, double[] returnVals) {

		GeodeticCalculator calculator = new GeodeticCalculator(Route.roadGeography.getCRS());
		calculator.setStartingGeographicPoint(c1.x, c1.y);
		calculator.setDestinationGeographicPoint(c2.x, c2.y);
		double distance = calculator.getOrthodromicDistance();
		if (returnVals!=null && returnVals.length==2) {
			returnVals[0] = distance;
			double angle = Math.toRadians(calculator.getAzimuth()); // Angle in range -PI to PI
			// Need to transform azimuth (in range -180 -> 180 and where 0 points north) 
			// to standard mathematical (range 0 -> 360 and 90 points north)
			if (angle > 0 && angle < 0.5*Math.PI) { // NE Quadrant 
				angle=0.5*Math.PI-angle;
			}
			else if (angle >= 0.5*Math.PI) { // SE Quadrant
				angle=(-angle)+2.5*Math.PI;
			}
			else if (angle < 0 && angle > -0.5*Math.PI) { // NW Quadrant
				angle=(-1*angle)+0.5*Math.PI;
			}
			else { // SW Quadrant
				angle=-angle+0.5*Math.PI;
			}
			returnVals[1] = angle; 
		}
		return distance;
		// These are some conversions from Simphopny code, don't think they're relevant here.
		//		Unit unit = SI.METER;
		//		double distance;
		//		if (!calculator.getEllipsoid().getAxisUnit().equals(NonSI.DEGREE_ANGLE)) {
		//			if (unit == SI.METER) {
		//				distance = calculator.getOrthodromicDistance();
		//			} else {
		//				Converter converter = SI.METER.getConverterTo(unit);
		//				distance = converter.convert(calculator.getOrthodromicDistance());
		//			}
		//		} 
		//		else if (!unit.equals(calculator.getEllipsoid().getAxisUnit())) {
		//			Converter converter = calculator.getEllipsoid().getAxisUnit().getConverterTo(unit);
		//			distance = converter.convert(calculator.getOrthodromicDistance());
		//		} 
		//		else {
		//			distance = calculator.getOrthodromicDistance();
		//		}
		//		return distance;

	}

	/**
	 * Converts a distance returned by DistanceOp to meters (isn't very accurate for some reason but gives
	 * a rough idea. The value shouldn't be used in any calculations which is why it's returned as a String.
	 * @param dist The distance (as returned by DistanceOp) to convert to meters
	 * @return The approximate distance in meters as a String (to discourage using this approximate value in
	 * calculations).
	 * @throws Exception 
	 * @see com.vividsolutions.jts.operation.distance.DistanceOp
	 */
	public static String distanceToMeters (double dist) throws Exception {
		// Works by creating two coords (close to a randomly chosen object) which are a certain distance apart
		// then using similar method as other distance() function
		try {
			GeodeticCalculator calculator = new GeodeticCalculator(Route.roadGeography.getCRS());
			Coordinate c1 = GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(Building.class).getCoords().toCoordinate();
			calculator.setStartingGeographicPoint(c1.x, c1.y);
			calculator.setDestinationGeographicPoint(c1.x, c1.y+dist);
			return String.valueOf(calculator.getOrthodromicDistance());
		}
		catch (Exception e) {
			Outputter.errorln("GISRoute.distanceToMeters() caught error calculating distance: "+dist);
			Outputter.errorln(e.getStackTrace());
			throw e;
		}
	}

	//	/**
	//	 * The 'roads' keeps track of which road each Coord on the route is part of, this is so that when travelling
	//	 * Buildings can easily be added to the cognitive map (just add all the buildings on the agent's current road,
	//	 * don't need to do complex geographical calculations to find buildings within a buffer).
	//	 * @param c The current coordinate
	//	 * @param r It's associated road
	//	 */
	//	private void addToRoads(Coord c, Road r) {
	//		
	//	}

	public void clearCaches() {
		if (coordCache!=null)
			coordCache.clear();
		if (nearestRoadCoordCache != null) {
			nearestRoadCoordCache.clear();
			nearestRoadCoordCache= null;
		}
		if (buildingsOnRoadCache!=null) {
			buildingsOnRoadCache.clear();
			buildingsOnRoadCache = null;
		}
		if (routeCache!=null) {
			routeCache.clear();
			routeCache = null;
		}
		if (routeDistanceCache!=null) {
			routeDistanceCache.clear();
			routeDistanceCache = null;
		}
	}

	/**
	 * Create different types of route
	 * @throws Exception 
	 */
	protected List<Coord> setRoute(SearchAlg.SEARCH_TYPES type) throws Exception {

		try {

			if (type.equals(SearchAlg.SEARCH_TYPES.BULLS_EYE)) {
				/*
				 * Creates a route for a 'bulls eye' search routine. Assumes that the agent starts at a 'target'
				 * building (getting to the target is not part of the search routine). Works as follows:
				 * 1. choose a random direction along the current road in which to travel. Keep a record
				 * of roads which have been travelled and number of times.
				 * 2. when reaching a junction choose a new road to travel down based on the direction (want
				 * angle of 180 (0.5 pi) to target (to orbiting target)) and number of times visited
				 * (prefer roads not travelled down).
				 * 3. repeat 2 until search time has finished.
				 */

				// XXXX - need to make sure the agent only walks on this search routine?

				Outputter.debugln("GISRoute is planning a "+type.toString()+" route for: "+
						this.burglar.toString(), Outputter.DEBUG_TYPES.ROUTING);
				// (Required otherwise get nullPointer although not used by searching alg).
				Building b = GlobalVars.BUILDING_ENVIRONMENT.getRandomObject(House.class);
				this.destination = GlobalVars.BUILDING_ENVIRONMENT.getCoords(b);
				this.destinationBuilding = b;

				double time = System.nanoTime();
				Map<Road, Integer> visitedRoads = new Hashtable<Road, Integer>(); // The number of times a road has been passed 
				List<Coord> theRoute = new ArrayList<Coord>();
				/* Get the current road the agent is on (or nearest to). */
				Road currentRoad = null;
				Coord currentCoord = GlobalVars.BURGLAR_ENVIRONMENT.getCoords(this.burglar);
				Coord target = currentCoord; // The target coordinate the agent will orbit
				List<Road> roads = this.getRoadFromCoordCache(currentCoord);
				currentCoord = this.getNearestRoadCoord(currentCoord);
				theRoute.add(currentCoord);
				currentRoad = EnvironmentFactory.findRoadAtCoordinates(currentCoord, Route.roadGeography);

				/* Choose a random direction of travel (head towards either of the road's Junctions) */
				Junction destJunction = currentRoad.getJunctions().get(
						RandomHelper.nextIntFromTo(0, currentRoad.getJunctions().size()-1));
				theRoute.addAll(this.getCoordsAlongRoad(currentCoord, destJunction.getCoords(), currentRoad, true));
				visitedRoads.put(currentRoad, 1); // Remember agent has been down this road

				/* Now start main algorithm. Choose a new direction of travel from this junction, add the
				 * coords to the next Junction, then repeat until max route length is reached. */ 
				double distanceTravelled = 0; // Measure distance of search
				Junction currentJunction = destJunction;	// Current Junction agent has just reached
				currentCoord = currentJunction.getCoords();

				// Plan a route that is at least long enough that the agent will travel until they give up
				// searching. This distance is calculated using:
				// searchTime(hours) * iterPerHour * travelPerIter
				double maxPossibleDistance = 
					GlobalVars.BULLS_EYE_SEARCH_TIME * (GlobalVars.ITER_PER_DAY/24.0) * GlobalVars.GIS_PARAMS.TRAVEL_PER_TURN;
				Road previousRoad = null; // Remember the previous road to prevent backtracking
				while (distanceTravelled <= maxPossibleDistance*1.1) { // Make distance 10% longer
					// Get the roads attached to this junction
					roads = currentJunction.getRoads();
					// Decide which road to travel down:
					double highestAttract = 0; // Most attractive road is chosen
					Road mostAttractiveRoad = null;
					Coordinate[] mostAttractiveRoadCoords = null;	// The road's vertices (to calculate direction)
					for (Road r:roads) {

						Coordinate[] roadCoords = Route.roadGeography.getGeometry(r).getCoordinates();
						if (!currentCoord.equals(roadCoords[0])) // Might need to reverse order of vertices
							ArrayUtils.reverse(roadCoords);
						// Find out direction road is heading (assuming first two coords give overall direction)
						double angle = Route.angle(new Coord(roadCoords[0]), new Coord(roadCoords[1]), target);
						Integer numVisits = visitedRoads.get(r);
						if (numVisits == null) numVisits = 0;
						// Road attractiveness based on number of previous visits and the angle it makes
						// with the target (roads which will orbit target are more attractive - 0.5PI).
						//							double attract = (angle < Math.PI ? Math.sin(angle+0.4) : -Math.sin(angle-0.4) ) / (2*(numVisits+1)); // (nuVisits+1 so no divide by 0)
						double attract = Route.BE_calcAngleAttractiveness(angle, numVisits);
						if (r.equals(previousRoad))
							attract = 2*Double.MIN_VALUE; // Previously visited road is the least attractive (*2 so that if previous road is the only one available it will still be chosen)
						if (attract>highestAttract) {
							mostAttractiveRoad = r;
							highestAttract = attract;
							mostAttractiveRoadCoords = roadCoords;
						}
					} // for roads

					// Have found most attractive road, so add coordinates to next Junction along the road
					previousRoad = mostAttractiveRoad; // Remember this road to stop backtracking
					List<Coord> coordsToAdd = Coord.buildCoordArray(mostAttractiveRoadCoords);
					theRoute.addAll(coordsToAdd);
					double segmentLength;
					for (int i=0; i<coordsToAdd.size()-1;i++) { // Calculate length of the road
						segmentLength=GISRoute.distance(
								coordsToAdd.get(i).toCoordinate(), coordsToAdd.get(i+1).toCoordinate(), null);
						distanceTravelled += segmentLength;
						//							GlobalVars.MINS_PER_ITER*(segmentLength/GlobalVars.GIS_PARAMS.TRAVEL_PER_TURN);
						// Also add to the 'roads' map, this keeps track of the road associated with each coord
						this.roads.put(coordsToAdd.get(i), mostAttractiveRoad);
					}
					// Update the currentJunction
					currentJunction = !mostAttractiveRoad.getJunctions().get(0).equals(currentJunction) ?
							mostAttractiveRoad.getJunctions().get(0) : mostAttractiveRoad.getJunctions().get(1) ;
							currentCoord = currentJunction.getCoords();
							if (visitedRoads.containsKey(mostAttractiveRoad))
								visitedRoads.put(mostAttractiveRoad, visitedRoads.get(mostAttractiveRoad)+1);
							else
								visitedRoads.put(mostAttractiveRoad, 1);
				} // while distTravelled

				Outputter.debugln("GISRoute.setRoute(BULLSEYE) finished creating route in "+
						0.000001*(System.nanoTime()-time)+"ms", Outputter.DEBUG_TYPES.ROUTING);
				return theRoute;
			}
			else {
				Outputter.errorln("GISRoute.setRoute(type) error, haven't written the code for this type of " +
						"route: "+type.toString());
			}
		} catch (Exception e) {
			Outputter.errorln("GISRoute.setRoute(type): Problem creating a burglary route for '"+this.burglar.toString()+
					"'. Type of route: "+type.toString());
			Outputter.errorln("Error type, message and stack trace: "+e.getClass().getName()+", "+e.getMessage());
			Outputter.errorln(e.getStackTrace());
			throw e;
		}
		return null;
	}


	/**
	 * Wrapper for EnvironmentFactory.findRoadAtCoord(), used only for debugging / testing the environment.
	 * @param c
	 * @return
	 * @throws Exception 
	 */
	public static Road findNearestRoad(Coord c) throws Exception {
		return EnvironmentFactory.findRoadAtCoordinates(c, roadGeography);
	}
}

/* ************************************************************************ */

/**
 * Class can be used to store a cache of all roads and the buildings which can be accessed by them (a map of
 * Road<->List<Building>. Buildings are 'accessed' by travelling to the road which is nearest to them.
 * <p>
 * This class can be serialised so that if the GIS data doesn't change it doesn't have to be re-calculated
 * each time. However, the Roads and Buildings themselves cannot be serialised because if they are there will
 * be two sets of Roads and BUildings, the serialised ones and those that were created when the model was
 * initialised. To get round this, an array which contains the road and building ids is serialised and the
 * cache is re-built using these caches ids after reading the serialised cache. This means that the id's given
 * to Buildings and Roads must not change (i.e. auto-increment numbers are no good because if a simulation is
 * restarted the static auto-increment variables will not be reset to 0).
 * @author Nick Malleson
 */
class BuildingsOnRoadCache implements Serializable {

	private static final long serialVersionUID = 1L;
	// The actual cache, this isn't serialised
	transient private static Hashtable<Road, ArrayList<Building>> theCache ;
	// The 'reference' cache, stores the building and road ids and can be serialised
	private Hashtable<Integer, ArrayList<Integer>> referenceCache;

	// Check that the road/building data hasn't been changed since the cache was last created
	private File buildingsFile;
	private File roadsFile;
	// The location that the serialised object might be found.
	private File serialisedLoc;
	// The time that this cache was created, can be used to check data hasn't changed since
	private long createdTime;

	// Private constructor because getInstance() should be used
	private BuildingsOnRoadCache(
			Environment<Building> buildingEnvironment, File buildingsFile, 
			Environment<Road> roadEnvironment, File roadsFile,
			File serialisedLoc, GeometryFactory geomFac) throws Exception {
		//		this.buildingEnvironment = buildingEnvironment;		
		//		this.roadEnvironment = roadEnvironment;
		this.buildingsFile = buildingsFile;
		this.roadsFile = roadsFile;
		this.serialisedLoc = serialisedLoc;
		theCache = new Hashtable<Road,ArrayList<Building>>();
		this.referenceCache = new Hashtable<Integer, ArrayList<Integer>>();

		Outputter.debugln("BuildingsOnRoadCache() creating new cache with data (and modification date):\n\t"+
				this.buildingsFile.getAbsolutePath()+" ("+new Date(this.buildingsFile.lastModified())+")\n\t"+
				this.roadsFile.getAbsolutePath()+" ("+new Date(this.roadsFile.lastModified())+")\n\t"+
				this.serialisedLoc.getAbsolutePath(),
				Outputter.DEBUG_TYPES.CACHES);

		populateCache(buildingEnvironment, roadEnvironment, geomFac);
		this.createdTime = new Date().getTime();
		serialise();
	}

	public void clear() {
		theCache.clear();
		this.referenceCache.clear();

	}

	private void populateCache(Environment<Building> buildingEnvironment, Environment<Road> roadEnvironment, GeometryFactory geomFac) throws Exception {
		double time = System.nanoTime();
		Outputter.debugln("BuildingsONRoadCache is populating cache...", Outputter.DEBUG_TYPES.CACHES);
		for (Building b:buildingEnvironment.getAllObjects(Building.class)) {
			// Find the closest road to this building
			Geometry buildingPoint = geomFac.createPoint(b.getCoords().toCoordinate());
			double minDistance = Double.MAX_VALUE;
			Road closestRoad = null;
			double distance;
			for (Road r:roadEnvironment.getObjectsWithin(b.getCoords(), Road.class, GlobalVars.GIS_PARAMS.XXXX_BUFFER, true)) {	
				distance = DistanceOp.distance(buildingPoint, Route.roadGeography.getGeometry(r));
				if (distance<minDistance) {
					minDistance = distance;
					closestRoad = r;
				}
			} // for roads
			// Found the closest road, add the information to the cache
			if (theCache.containsKey(closestRoad)) {
				theCache.get(closestRoad).add(b);
				this.referenceCache.get(closestRoad.getId()).add(b.getId());
			}
			else {
				ArrayList<Building> l = new ArrayList<Building>();
				l.add(b);
				theCache.put(closestRoad, l);
				ArrayList<Integer> l2 = new ArrayList<Integer>();
				l2.add(b.getId());
				this.referenceCache.put(closestRoad.getId(), l2);
			}
		} // for buildings
		int numRoads = theCache.keySet().size();
		int numBuildings = 0; for (List<Building>l:theCache.values()) numBuildings+= l.size();
		Outputter.debugln("... finished caching roads and buildings. Cached "+numRoads+" roads and "+
				numBuildings+" buildings in "+ 0.000001*(System.nanoTime()-time)+"ms", Outputter.DEBUG_TYPES.CACHES);
	}

	public List<Building> get(Road r) {
		return theCache.get(r);
	}

	private void serialise() throws IOException {
		double time = System.nanoTime();
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try
		{
			if (!this.serialisedLoc.exists()) this.serialisedLoc.createNewFile();
			fos = new FileOutputStream(this.serialisedLoc);
			out = new ObjectOutputStream(fos);
			out.writeObject(this);
			out.close();
		}
		catch(IOException ex){
			Outputter.errorln("BuildingsOnRoadCache caught error trying to serialised cache: "+ex.getMessage());
			if (serialisedLoc.exists()) serialisedLoc.delete();  // delete to stop problems loading incomplete file next time
			Outputter.errorln(ex.getStackTrace());
			throw ex;
		}
		Outputter.debugln("... serialised BuildingsOnRoadCache to "+this.serialisedLoc.getAbsolutePath() +
				" in ("+0.000001*(System.nanoTime()-time)+"ms)", Outputter.DEBUG_TYPES.CACHES);
	}

	/**
	 * Used to create a new BuildingsOnRoadCache object. This function is used instead of the constructor directly so that the class can check if there is a serialised version on disk already. If not then a new one is created and returned.
	 * @param buildingEnv
	 * @param buildingsFile
	 * @param roadEnv
	 * @param roadsFile
	 * @param serialisedLoc
	 * @param geomFac
	 * @return
	 * @throws Exception 
	 */
	public synchronized static BuildingsOnRoadCache getInstance(
			Environment<Building> buildingEnv, File buildingsFile, 
			Environment<Road> roadEnv, File roadsFile,
			File serialisedLoc,
			GeometryFactory geomFac) throws Exception {
		double time = System.nanoTime();
		// See if there is a cache object on disk.
		if (serialisedLoc.exists()) {
			FileInputStream fis = null;
			ObjectInputStream in = null;
			BuildingsOnRoadCache bc = null;
			try
			{
				fis = new FileInputStream(serialisedLoc);
				in = new ObjectInputStream(fis);
				bc = (BuildingsOnRoadCache) in.readObject();
				in.close();

				// Check that the cache is representing the correct data and the modification dates are ok
				// (WARNING, if this class is re-compiled the serialised object will still be read in).
				if (	!buildingsFile.getAbsolutePath().equals(bc.buildingsFile.getAbsolutePath()) ||
						!roadsFile.getAbsolutePath().equals(bc.roadsFile.getAbsolutePath()) || 
						buildingsFile.lastModified() > bc.createdTime || 
						roadsFile.lastModified() > bc.createdTime ) {
					Outputter.debugln("BuildingsOnRoadCache, found serialised object but it doesn't match the " +
							"data (or could have different modification dates), will create a new cache.", Outputter.DEBUG_TYPES.CACHES);
				}
				else {
					// Have found a useable serialised cache. Now use the cached list of id's to construct a
					// new cache of buildings and roads.
					// First need to buld list of existing roads and buildings
					Hashtable<Integer, Road> allRoads = new Hashtable<Integer, Road>();
					for (Road r:roadEnv.getAllObjects(Road.class)) allRoads.put(r.getId(), r);
					Hashtable<Integer, Building> allBuildings = new Hashtable<Integer, Building>();
					for (Building b:buildingEnv.getAllObjects(Building.class)) allBuildings.put(b.getId(), b);

					// Now create the new cache					
					theCache = new Hashtable<Road, ArrayList<Building>>();

					for (int roadId:bc.referenceCache.keySet()) {
						ArrayList<Building> buildings = new ArrayList<Building>();
						for (Integer buildingId:bc.referenceCache.get(roadId)) {
							buildings.add(allBuildings.get(buildingId));
						}
						theCache.put(
								allRoads.get(roadId), 
								buildings);
					}
					Outputter.debugln("BuildingsOnRoadCache, found serialised cache, returning it (in "+
							0.000001*(System.nanoTime()-time)+"ms)", Outputter.DEBUG_TYPES.CACHES);
					return bc;
				}
			}
			catch(IOException ex) {
				if (serialisedLoc.exists())  serialisedLoc.delete(); // delete to stop problems loading incomplete file next tinme
				Outputter.errorln("BuildingsOnRoadCache caught error looking for serialised cache: "+ex.getMessage());
				Outputter.errorln(ex.getStackTrace());
				throw ex;
			}
			catch(ClassNotFoundException ex) {
				if (serialisedLoc.exists())  serialisedLoc.delete();
				Outputter.errorln("BuildingsOnRoadCache caught error looking for serialised cache: "+ex.getMessage());
				Outputter.errorln(ex.getStackTrace());
				throw ex;
			}

		}

		// No serialised object, or got an error when opening it, just create a new one
		return new BuildingsOnRoadCache(
				buildingEnv, buildingsFile, 
				roadEnv, roadsFile,
				serialisedLoc,
				geomFac);
	}


}

/* ************************************************************************ */

/**
 * Caches the nearest road Coordinate to every building for efficiency (agents usually/always need to get
 * from the centroids of houses to/from the nearest road).
 * <p>
 * This class can be serialised so that if the GIS data doesn't change it doesn't have to be re-calculated each
 * time. 
 * @author Nick Malleson
 */
class NearestRoadCoordCache implements Serializable {

	private static final long serialVersionUID = 1L;
	private Hashtable<Coord, Coord> theCache ; // The actual cache
	// Check that the road/building data hasn't been changed since the cache was last created
	private File buildingsFile;
	private File roadsFile;
	// The location that the serialised object might be found.
	private File serialisedLoc;
	// The time that this cache was created, can be used to check data hasn't changed since
	private long createdTime;

	private NearestRoadCoordCache(
			Environment<Building> buildingEnvironment, File buildingsFile, 
			Environment<Road> roadEnvironment, File roadsFile,
			File serialisedLoc, GeometryFactory geomFac) throws Exception {
		this.buildingsFile = buildingsFile;
		this.roadsFile = roadsFile;
		this.serialisedLoc = serialisedLoc;
		theCache = new Hashtable<Coord, Coord>();

		Outputter.debugln("NearestRoadCoordCache() creating new cache with data (and modification date):\n\t"+
				this.buildingsFile.getAbsolutePath()+" ("+new Date(this.buildingsFile.lastModified())+") \n\t"+
				this.roadsFile.getAbsolutePath()+" ("+new Date(this.roadsFile.lastModified())+"):\n\t"+
				this.serialisedLoc.getAbsolutePath(),
				Outputter.DEBUG_TYPES.CACHES);

		populateCache(buildingEnvironment, roadEnvironment, geomFac);
		this.createdTime = new Date().getTime();
		serialise();
	}

	public void clear() {
		this.theCache.clear();		
	}

	private void populateCache(Environment<Building> buildingEnvironment, Environment<Road> roadEnvironment, GeometryFactory geomFac) throws Exception {
		double time = System.nanoTime();
		Outputter.debugln("NearestRoadCoordCache is populating cache...", Outputter.DEBUG_TYPES.CACHES);
		theCache = new Hashtable<Coord, Coord>();
		// Iterate over every building and find the nearest road point
		for (Building b:buildingEnvironment.getAllObjects(Building.class)) {
			double minDist = Double.MAX_VALUE;
			Coordinate houseCoord = b.getCoords().toCoordinate();
			Coordinate nearestPoint = null;
			Point houseCoordGeom = geomFac.createPoint(houseCoord);
			for (Road r:roadEnvironment.getObjectsWithin(b.getCoords(), Road.class, GlobalVars.GIS_PARAMS.XXXX_BUFFER, true)) {
				Geometry roadGeom = Route.roadGeography.getGeometry(r);
				DistanceOp distOp = new DistanceOp(houseCoordGeom, roadGeom);
				double thisDist = distOp.distance();
				if (thisDist < minDist) {
					minDist = thisDist;
					// Two coordinates returned by closestPoints(), need to find the one which isn''t the coord parameter
					for (Coordinate c:distOp.closestPoints()) {
						if (!c.equals(houseCoord)) {
							nearestPoint = c;
							break;
						}
					}
				} // if thisDist < minDist
			} // for allRoads
			if (nearestPoint==null) {
				Outputter.errorln("GISRoute.getNearestRoadCoord() error: couldn't find a road coordinate which " +
						"is close to building "+b.toString());
			}
			Coord hc = new Coord(houseCoord);
			Coord np =new Coord(nearestPoint); 
			theCache.put(hc, np);
		}// for Houses
		Outputter.debugln("...finished caching nearest roads ("+
				(0.000001*(System.nanoTime()-time))+"ms)", Outputter.DEBUG_TYPES.CACHES);
	} // if nearestRoadCoordCache = null;


	public Coord get(Coord c) throws Exception {
		if (c==null) {
			Outputter.errorln("GISRoute.NearestRoadCoordCache.get() error: the given coordinate is null.");
		}
		double time = System.nanoTime();
		Coord nearestCoord = this.theCache.get(c);
		if (nearestCoord!=null) {
			Outputter.debugln("NearestRoadCoordCache.get() (using cache) - ("+
					(0.000001*(System.nanoTime()-time))+"ms)", Outputter.DEBUG_TYPES.ROUTING);
			return nearestCoord;
		}
		// If get here then the coord is not in the cache, agent not starting their journey from a
		// house, search for it manually
		// Search all roads in the vicinity, looking for the point which is nearest the person
		double minDist = Double.MAX_VALUE;
		Coordinate nearestPoint = null;
		GeometryFactory geomFac = new GeometryFactory();
		Point coordGeom = geomFac.createPoint(c.toCoordinate());
		for (Road road:GlobalVars.ROAD_ENVIRONMENT.getObjectsWithin(c, Road.class, GlobalVars.GIS_PARAMS.XXXX_BUFFER, true)) {
			// XXXX: BUG: if an agent is on a really long road, the long road will not be found by getObjectsWithin because it is not within the buffer 
			DistanceOp distOp = new DistanceOp(coordGeom, Route.roadGeography.getGeometry(road));
			double thisDist = distOp.distance();
			if (thisDist < minDist) {
				minDist = thisDist;
				//nearestRoad = road;
				// Two coordinates returned by closestPoints(), need to find the one which isn''t the coord parameter
				Coordinate[] closestPoints = distOp.closestPoints();
				nearestPoint = (c.equals(closestPoints[0])) ? closestPoints[1] : closestPoints[0] ;
				//				for (Coordinate coord:distOp.closestPoints()) {
				//					if (!c.equals(coord)) {
				//						nearestPoint = coord;
				//					}
				//
				//				}
			} // if thisDist < minDist
		} // for nearRoads
		if (nearestPoint!=null) {
			Outputter.debugln("NearestRoadCoordCache.get() (not using cache) - ("+
					(0.000001*(System.nanoTime()-time))+"ms)", Outputter.DEBUG_TYPES.ROUTING);
			return new Coord(nearestPoint);		
		}		
		/* IF HERE THEN ERROR, PRINT DEBUGGING INFO */
		String debugString = "";
		debugString+="GISRoute.NearestRoadCoordCache.get() error: couldn't find a coordinate to return.\n";
		List<Road> roads = GlobalVars.ROAD_ENVIRONMENT.getObjectsWithin(c, Road.class, GlobalVars.GIS_PARAMS.XXXX_BUFFER, true);
		debugString+="Looking for nearest road coordinate around "+c.toString()+".\n";
		debugString+="RoadEnvironment.getObjectsWithin() returned "+roads.size()+" roads, printing debugging info:\n";
		minDist = Double.MAX_VALUE; nearestPoint = null; //coordGeom = geomFac.createPoint(c.toCoordinate());
		for (Road r:roads) {
			DistanceOp distOp = new DistanceOp(coordGeom, Route.roadGeography.getGeometry(r));
			double thisDist = distOp.distance();
			debugString+="\troad "+r.toString()+" is "+thisDist+" distance away (at closest point). ";
			if (thisDist < minDist) {
				debugString+="Closest road so far. ";
				minDist = thisDist;
				//nearestRoad = road;
				// Two coordinates returned by closestPoints(), need to find the one which isn''t the coord parameter
				Coordinate[] closestPoints = distOp.closestPoints();
				debugString+="Closest points ("+closestPoints.length+") are: "+Arrays.toString(closestPoints);
				nearestPoint = (c.equals(closestPoints[0])) ? closestPoints[1] : closestPoints[0] ;
				debugString+="Nearest point is "+nearestPoint.toString();
				//				for (Coordinate coord:closestPoints) {
				//					if (!c.equals(coord)) {
				//						debugString+="Nearest point is "+coord.toString();
				//						nearestPoint = coord;
				//					}
				//				}
			} // if thisDist < minDistDist
			debugString+="\n";
		} // for roads
		Outputter.errorln(debugString);
		throw new Exception(debugString);

	}

	private void serialise() throws IOException {
		double time = System.nanoTime();
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try
		{
			if (!this.serialisedLoc.exists()) this.serialisedLoc.createNewFile();
			fos = new FileOutputStream(this.serialisedLoc);
			out = new ObjectOutputStream(fos);
			out.writeObject(this);
			out.close();
		}
		catch(IOException ex){
			Outputter.errorln("NearestRoadCoordCache caught error looking for serialised cache: "+ex.getMessage());
			if (serialisedLoc.exists()) serialisedLoc.delete();  // delete to stop problems loading incomplete file next time
			Outputter.errorln(ex.getStackTrace());
			throw ex;
		}
		Outputter.debugln("... serialised NearestRoadCoordCache to "+this.serialisedLoc.getAbsolutePath() +
				" in ("+0.000001*(System.nanoTime()-time)+"ms)", Outputter.DEBUG_TYPES.CACHES);
	}

	/**
	 * Used to create a new BuildingsOnRoadCache object. This function is used instead of the constructor directly so that the class can check if there is a serialised version on disk already. If not then a new one is created and returned.
	 * @param buildingEnv
	 * @param buildingsFile
	 * @param roadEnv
	 * @param roadsFile
	 * @param serialisedLoc
	 * @param geomFac
	 * @return
	 * @throws Exception 
	 */
	public synchronized static NearestRoadCoordCache getInstance(
			Environment<Building> buildingEnv, File buildingsFile, 
			Environment<Road> roadEnv, File roadsFile,
			File serialisedLoc,
			GeometryFactory geomFac) throws Exception {
		double time = System.nanoTime();
		// See if there is a cache object on disk.
		if (serialisedLoc.exists()) {
			FileInputStream fis = null;
			ObjectInputStream in = null;
			NearestRoadCoordCache ncc = null;
			try
			{

				fis = new FileInputStream(serialisedLoc);
				in = new ObjectInputStream(fis);
				ncc = (NearestRoadCoordCache) in.readObject();
				in.close();

				// Check that the cache is representing the correct data and the modification dates are ok
				if (	!buildingsFile.getAbsolutePath().equals(ncc.buildingsFile.getAbsolutePath()) ||
						!roadsFile.getAbsolutePath().equals(ncc.roadsFile.getAbsolutePath()) || 
						buildingsFile.lastModified() > ncc.createdTime || 
						roadsFile.lastModified() > ncc.createdTime) {
					Outputter.debugln("BuildingsOnRoadCache, found serialised object but it doesn't match the " +
							"data (or could have different modification dates), will create a new cache.", Outputter.DEBUG_TYPES.CACHES);
				}
				else {
					Outputter.debugln("NearestRoadCoordCache, found serialised cache, returning it (in "+
							0.000001*(System.nanoTime()-time)+"ms)", Outputter.DEBUG_TYPES.CACHES);
					return ncc;
				}
			}
			catch(IOException ex) {
				if (serialisedLoc.exists())  serialisedLoc.delete(); // delete to stop problems loading incomplete file next tinme
				Outputter.errorln("NearestRoadCoordCache caught error looking for serialised cache: "+ex.getMessage());
				Outputter.errorln(ex.getStackTrace());
				throw ex;
			}
			catch(ClassNotFoundException ex) {
				if (serialisedLoc.exists())  serialisedLoc.delete();
				Outputter.errorln("NearestRoadCoordCache caught error looking for serialised cache: "+ex.getMessage());
				Outputter.errorln(ex.getStackTrace());
				throw ex;
			}

		}

		// No serialised object, or got an error when opening it, just create a new one
		return new NearestRoadCoordCache(
				buildingEnv, buildingsFile, 
				roadEnv, roadsFile,
				serialisedLoc,
				geomFac);
	}


}

/** Used to cache routes. Saves the origin and destination coords and the transport available to the agent (if
 * transport changes then the agent might have to create a new route. 
 * @author Nick Malleson
 */
class CachedRoute {
	private List<Coord> theRoute; // The list of coords representing the route
	private LinkedHashMap<Coord, Double> routeSpeeds; // This has to be stored along with the actual coords
	private Coord origin;
	private Coord destination;
	private List<String> transportAvailable;
	private static int uniqueRouteCacheID; // Used to generate hash codes (each route must have unique ID)
	private int uniqueID;
	//	private List<Coord> theRoute; // The actual route (a list of coords)

	public CachedRoute(Coord origin, Coord destination, List<String> transportAvailable) {
		super();
		this.origin = origin;
		this.destination = destination;
		this.transportAvailable = transportAvailable;
		this.uniqueID = CachedRoute.uniqueRouteCacheID++;
	}

	//	/**
	//	 * Get the list of Coords that make up this CachedRoute
	//	 */
	//	public List<Coord> getTheRoute() {
	//		return theRoute;
	//	}
	//
	//	/**
	//	 * Set the coordinates that make up this CachedRoute
	//	 * @param theRoute the list of Coords
	//	 */
	//	public void setTheRoute(List<Coord> theRoute) {
	//		this.theRoute = theRoute;
	//	}

	public void setRoute(List<Coord> theRoute) {
		this.theRoute = theRoute;
	}

	public List<Coord> getRoute() {
		return this.theRoute;
	}

	public void setRouteSpeeds(LinkedHashMap<Coord, Double> routeSpeeds) {
		this.routeSpeeds = routeSpeeds;
	}

	public LinkedHashMap<Coord, Double> getRouteSpeeds() {
		return this.routeSpeeds;
	}

	@Override
	public String toString() {
		return "CachedRoute "+this.uniqueID;
	}

	/**
	 * Returns true if input object is a CachedRoute and the the origin, destination and
	 * transport available are the same as this CachedRoute
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CachedRoute) {
			CachedRoute r = (CachedRoute) obj;
			return (r.origin.equals(this.origin)) && 
			(r.destination.equals(this.destination)) && 
			(r.transportAvailable.equals(this.transportAvailable));
		}
		else {
			Outputter.errorln("GISRoute.RouteCache.equals(): warning, trying to compare this RouteCache with an" +
					"object which isn't a RouteCache: " +obj.toString());
			return false;
		}
	}

	/**
	 * Returns: <code>Float.floatToIntBits((float)(this.origin.getX()+this.origin.getY()))</code>
	 */
	@Override
	public int hashCode() {
		return Float.floatToIntBits((float)(this.origin.getX()+this.origin.getY()));
	}
}


/** Used to cache route distances. Saves the origin and destination coords and the transport available to the agent (if
 * transport changes then the agent might have to create a new route). 
 * @author Nick Malleson
 */
class CachedRouteDistance {
	private Coord origin;
	private Coord destination;
	private List<String> transportAvailable;
	private static int uniqueRouteCacheID; // Used to generate hash codes (each route must have unique ID)
	private int uniqueID;
	//	private List<Coord> theRoute; // The actual route (a list of coords)

	public CachedRouteDistance (Coord origin, Coord destination, List<String> transportAvailable) {
		this.origin = origin;
		this.destination = destination;
		this.transportAvailable = transportAvailable;
		this.uniqueID = CachedRouteDistance.uniqueRouteCacheID++;
	}

	@Override
	public String toString() {
		return "CachedRouteDistance "+this.uniqueID;
	}

	/**
	 * Returns true if input object is a CachedRoute and the the origin, destination and
	 * transport available are the same as this CachedRoute. Because routes are non-directional
	 * the origins and destinations are interchangeable.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CachedRouteDistance) {
			CachedRouteDistance r = (CachedRouteDistance) obj;
			return 
			(
					(r.origin.equals(this.origin) && r.destination.equals(this.destination)) 
					||
					(r.origin.equals(this.destination) && r.destination.equals(this.origin)) 
			)
			&& r.transportAvailable.equals(this.transportAvailable);
		}
		else {
			Outputter.errorln("GISRoute.RouteCacheDistance.equals(): warning, trying to compare this " +
					"CachedRouteDistance with an object which is a " +obj.getClass().toString());
			return false;
		}
	}

	/**
	 * Returns: <code>Float.floatToIntBits((float)(this.origin.getX()+this.origin.getY()))</code>
	 */
	@Override
	public int hashCode() {
		return Float.floatToIntBits((float)(this.origin.getX()+this.origin.getY()));
	}
}

/** Convenience class for creating deep copies of lists/maps (copies the values stored as well). Haven't
 * made this generic because need access to constructors to create new objects (e.g. new Coord(c)) */
class Cloning {

	public static List<Coord> copy (List<Coord> in) {

		List<Coord> out = new ArrayList<Coord>(in.size());
		for (Coord c:in) {
			out.add(new Coord(c));
		}
		return out;
	}

	public static LinkedHashMap<Coord, Double> copy (LinkedHashMap<Coord, Double> in) {

		LinkedHashMap<Coord, Double> out = new LinkedHashMap<Coord, Double>(in.size()); 
		for (Coord c:in.keySet()) {
			out.put(c, in.get(c));
		}
		return out;
	}


}
