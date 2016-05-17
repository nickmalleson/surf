package burgdsim.environment;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;

import burgdsim.burglars.Burglar;
import burgdsim.burglars.BurglarFactory;
import burgdsim.environment.buildings.Building;
import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;

/**
 * Can be called to test that the environment is OK. Does the following:
 * <ol>
 * <li>From a random starting location (building), check that it is possible to create a route
 * to every other building.</li>
 * <li>Check that every building is within a community</li>
 * <li>Check that every road coordinate is within a community</li>
 * <li></li>
 * <li></li>
 * </ol>
 */
public class TestEnvironment implements ErrorEventListener {

	// Lists used to store objects which have problems with them
	private List<Building> inacBuildings = new ArrayList<Building>();
	private List<Road> inacRoads = new ArrayList<Road>();
	private List<Building> buildingsWithoutCommunities = new ArrayList<Building>();
	private List<Road> roadsWithoutCommunities = new ArrayList<Road>();
	private List<Community> communitiesWithoutBuildings = new ArrayList<Community>();

	@SuppressWarnings("unchecked")
	public TestEnvironment() {


		if (GlobalVars.TEST_ENVIRONMENT) {
			if (!GlobalVars.ENVIRONMENT_TYPE.equals(GlobalVars.ENVIRONMENT_TYPES.GIS)) {
				Outputter.describe("Sorry haven't implemented a TestEnvironment to test any environments other " +
				"the GIS environment");
				return;
			}
			Outputter.describeln("TESTING ENVIRONMENT");
			Outputter.errorOn(false);
			double startTime = System.currentTimeMillis();

			//	double buildingID = 30734; // small-centre
			//	double buildingID = 1; // toy-city
			// double buildingID = 81470; // vancouver_downtown
			// double buildingID = 14754; // vancouver_city
			double buildingID = 202455; // EASEL
			Building building = null;
			for (Building b:GlobalVars.BUILDING_ENVIRONMENT.getAllObjects(Building.class))
				if (b.getId()==buildingID)
					building = b;
			Outputter.describeln("WILL USE BUILDING "+building.toString()+" to create routes. " +
					"Make sure this building is definitely connected to the main city road network (it will be " +
			"used to look for disconnected roads and buildings).");
			Burglar burglar = BurglarFactory.createBurglar(building); // Create a specific burglar for environment testing
			Coord burglarCoords = GlobalVars.BURGLAR_ENVIRONMENT.getCoords(burglar); // REmember burglar starting position so he can be put back
			burglar.step();

			int numBuildings = GlobalVars.BUILDING_ENVIRONMENT.getAllObjects(Building.class).size();
			Outputter.describeln("CREATING A ROUTE TO ALL BUILDINGS ("+numBuildings+
					") FROM "+building.toString()+" "+building.getCoords());

			int counter = 0;
			for (Building b:GlobalVars.BUILDING_ENVIRONMENT.getAllObjects(Building.class)) {
				if (!b.equals(building)){
					// Try to create routes, if there are any errors an ErrorEvent will be caught which will
					// populate the global arrays
					try {
						GlobalVars.BURGLAR_ENVIRONMENT.move(burglar, burglarCoords); // Move agent back to start before creating route
						Route r = EnvironmentFactory.createRoute(burglar, b.getCoords(), b, null);
						r.addErrorEventListener(this);
						r.travel();
						r=null;
					} catch (OutOfMemoryError e) {
						System.err.println("CAUGHT OUT OF MEMORY ERROR");
						System.err.println("Tring to get to "+b.toString());
						System.err.println("EXITING PROGRAM");
						System.exit(0);
					}
					// System.out.println("created route to "+b.toString()+". Mem(tot, free, max): "+
					// Runtime.getRuntime().totalMemory()+", "+ 
					// Runtime.getRuntime().freeMemory()+", "+ 
					// Runtime.getRuntime().maxMemory());
					catch (Exception e) {
						System.err.println("CAUGHT AN ERROR TRAVELLING>");
						e.printStackTrace();
					}
				}
				counter++;
				if (counter % 1000 == 0) {
					Outputter.describeln("Analysed "+counter+" of "+numBuildings+" buildings in "+
							((System.currentTimeMillis()-startTime)/(60000))+" mins");					
				}
			}// for buildings
			// If any errorEvent's thrown the buildings that caused problems will have been added to global lists
			Outputter.describeln("COULD NOT CREATE ROUTES TO THE FOLLOWING BUILDINGS: "+this.inacBuildings.toString());

			// The nearest roads to these buildings might also be bad
			for (Building b:this.inacBuildings) {
				Road r = null;
				try {
					r = GISRoute.findNearestRoad(b.getCoords());
				} catch (Exception e) {
					System.err.println("TestEnvironment() caught an exception calling GISRoute.findNearestRoad()");
					e.printStackTrace();
				}
				if (!this.inacRoads.contains(r)) { // Don't want to add roads more than once
					this.inacRoads.add(r);
				}
			}

			Outputter.describeln("THESE ROADS ARE THE CLOSEST TO THE INACCESSIBLE BUILDINGS SO THEY ARE PROBABLY " +
					"DISCONNECTED FROM THE NETWORK: "+this.inacRoads.toString());

			Outputter.describeln("NOW LOOKING FOR ROADS WHICH AREN'T COVERED BY A COMMUNITY");
			// Now look for road segments which aren't covered by a community
			SimphonyGISEnvironment<Road, Road> env = (SimphonyGISEnvironment<Road, Road>) GlobalVars.ROAD_ENVIRONMENT; 
			for (Road r:GlobalVars.ROAD_ENVIRONMENT.getAllObjects(Road.class)) {
				for (Coordinate c:env.getGISProjection().getGeometry(r).getCoordinates()) {
					Community com = GlobalVars.COMMUNITY_ENVIRONMENT.getObjectAt(Community.class, new Coord(c));
					if (com==null) {
						this.roadsWithoutCommunities.add(r);
						break; // Don't need to keep searching this Road's coordinates
					}
				} // for Coordinates	
			} // for roads
			Outputter.describeln("FOUND FOLLOWING ROADS WITHOUT COMMUNITIES: "+this.roadsWithoutCommunities.toString());

			Outputter.describeln("NOW LOOKING FOR BUILDINGS WITHOUT A COMMUNITY");
			for (Building b:GlobalVars.BUILDING_ENVIRONMENT.getAllObjects(Building.class)) {
				Community com = GlobalVars.COMMUNITY_ENVIRONMENT.getObjectAt(Community.class, b.getCoords());
				if (com==null) {
					this.buildingsWithoutCommunities.add(b);
				}
			}
			Outputter.describeln("FOUND FOLLOWING BUILDINGS WITHOUT COMMUNITIES: "+this.buildingsWithoutCommunities.toString());

			Outputter.describeln("NOW LOOKING FOR COMMUNITIES WITHOUT A SINGLE BUILDING");
			for (Community c:GlobalVars.COMMUNITY_ENVIRONMENT.getAllObjects(Community.class)) {
				if (c.getBuildings()==null || c.getBuildings().size() == 0) {
					this.communitiesWithoutBuildings.add(c);
				}
			}
			Outputter.describeln("FOUND FOLLOWING COMMUNITIES WITHOUT A BUILDING: "+this.communitiesWithoutBuildings.toString());
			Outputter.describeln("**IMPORTANT!** Communities without a building aren't necessaryily a problem, " +
			"there is a check for this in the model");

			Outputter.describeln("\n FINALLY WILL PRINT THE PROBLEMATIC ROADS AND BUILDINGS IN CSV FORMAT.\n");
			Outputter.describeln("Inaccessible_buildings");
			if (this.inacBuildings.size()==0) Outputter.describeln("\t(none)");
			else
				for (Building b:this.inacBuildings)
					Outputter.describeln(""+b.getId());
			Outputter.describeln("Inaccessible_roads");
			for (Road r:this.inacRoads) {
				Outputter.describeln(r.getId()+"");
			}
			Outputter.describeln("buildings_no_comm");
			if (this.inacRoads.size()==0) Outputter.describeln("\t(none)");
			else 
				for (Building b:this.buildingsWithoutCommunities)
					Outputter.describeln(""+b.getId());
			Outputter.describeln("roads_no_comm");
			if (this.roadsWithoutCommunities.size()==0) Outputter.describeln("\t(none)");
			else 
				for (Road r:this.roadsWithoutCommunities)
					Outputter.describeln(""+r.getId());
			Outputter.describeln("comms_no_buildings");
			if (this.communitiesWithoutBuildings .size()==0) Outputter.describeln("\t(none)");
			else 
				for (Community c:this.communitiesWithoutBuildings)
					Outputter.describeln(""+c.getId());

			//			if (GlobalVars.EXIT_AFTER_TESTING) {
			Outputter.describeln("\nFINISHED TESTING, EXITING");
			System.exit(0);				
			//			}
			//			else {
			//				Outputter.describeln("\nFINISHED TESTING, DELETING BAD OBJECTS FROM CONTEXT AND CONTINUING");
			//				for (Building b:this.inacBuildings) {
			//					GlobalVars.BUILDING_ENVIRONMENT.remove(b);
			//				}
			//				for (Road r:this.inacRoads) {
			//					GlobalVars.ROAD_ENVIRONMENT.remove(r);
			//				}
			//				GlobalVars.BURGLAR_ENVIRONMENT.remove(burglar);
			//			}

		}
	}

	/**
	 * When creating routes an ErrorEvent will have been fired if there was a problem. This function
	 * listens for those events and handles them.
	 */
	public void errorEventOccurred(ErrorEvent e) {
		// See if the error was caused by a building:
		Building b = null;
		b = e.getBuilding();
		if (b!=null) {
			//			Outputter.describeln("Caught ErrorEvent creating a route to building "+b.toString());			
			this.inacBuildings.add(b);
		}
		//		Outputter.describeln("Caught ErrorEvent but not sure what to do with it.\n" +
		//				"\tMessage is: "+e.getMessage()+"\n"+
		//				"\tFired by "+e.getSource().toString());		
	}
}

interface ErrorEventListener extends EventListener {
	void errorEventOccurred(ErrorEvent e);
}
