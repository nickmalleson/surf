
package burgdsim.environment;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.distance.DistanceOp;


import repast.simphony.context.Context;
import repast.simphony.context.DefaultContext;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.space.gis.DefaultGeography;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.ShapefileLoader;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import burgdsim.burglars.Burglar;
import burgdsim.burglary.SearchAlg;
import burgdsim.data_access.DataAccess;
import burgdsim.data_access.DataAccessFactory;
import burgdsim.environment.buildings.Building;
import burgdsim.environment.buildings.DrugDealer;
import burgdsim.environment.buildings.House;
import burgdsim.environment.buildings.Social;
import burgdsim.environment.buildings.Workplace;
import burgdsim.environment.contexts.BuildingContext;
import burgdsim.environment.contexts.BurglarContext;
import burgdsim.environment.contexts.CommunityContext;
import burgdsim.environment.contexts.JunctionContext;
import burgdsim.environment.contexts.RoadContext;
import burgdsim.main.ContextCreator;
import burgdsim.main.FlatFileObjectReader;
import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;

public abstract class EnvironmentFactory {

	// This class is also used to store the Network projection which the Route objects used to build
	// routes around the environment.
	private static Network<Junction> roadNetwork = null;
	//	private static Network<Junction> busNetwork;
	//	private static Network<Junction> footNetwork;
	//	private static Network<Junction> trainNetwork;

	private static Context<Junction> junctionContext;// Need to keep a reference to this for the road network

	// These are used by GIS so we can keep a link between Roads (in the RoadGeography) and Edges in the RoadNetwork
	//	private static Map<RepastEdge<?>, String> edgeIDs_KeyEdge;// = new HashMap<RepastEdge<?>, String>(); // Stores the TOIDs of edges (Edge as key)
	//	private static Map<String, RepastEdge<Junction>> edgeIDs_KeyID;// = new HashMap<String, RepastEdge<Junction>>(); // Stores the TOIDs of edges (TOID as key)
	//	private static Map<NetworkEdge<Junction>, Road> edges_roads ;//= new HashMap<String, RepastEdge<Junction>>(); // Stores the Edges and their associated Roads
	//	private static Map<Road, NetworkEdge<Junction>> roads_edges ;
	//	private static Map<String, Road> roadIdCache = null;

	private static boolean createdCommunities = false; // Need to check communities created before roads/buildings
	// This is a bit nasty, have to remember the coordinates of all communities in the Grid environment because
	// I didn't think about how to handle objects which span more than one cell :-( So in createBuildingsAndRoads()
	// this map has to be used (with GIS Environment can use getObjectsWithin(Buffer) function).
	private static Map<Community, List<Coord>> gridCommunityCoords;

	// Keep a record of houses which have been burgled so their security can be returned to base levels over time
	private static Map<House, ?> burgledHouses;

	/**
	 * Create a new environment, type is defined by the GlobalVars.ENVIRONMENT_TYPE variable.
	 * 
	 * @param <S> The type of objects in the parent context
	 * @param <T> The type of objects that this Environment will store
	 * @param name The name of the environment
	 * @param parentContext The parent context
	 * @return An environment which contains objects of type T.
	 */
	public static <S, T extends S> Environment<T> createEnvironment(String name, Context<S> parentContext) {

		Outputter.debugln("EnvironmentFactory: creating new "+GlobalVars.ENVIRONMENT_TYPE.toString()+" environment with name: "+name,
				Outputter.DEBUG_TYPES.INIT);

		switch (GlobalVars.ENVIRONMENT_TYPE) {
		case NULL:	return new NullEnvironment<S, T>(name, parentContext);
		case GIS: 	return new SimphonyGISEnvironment<S, T>(name, parentContext);
		case GRID: 	return new SimphonyGridEnvironment<S, T>(name, parentContext);
		default: 	Outputter.errorln("EnvironmentFactory.getEnvironment(): Error: Environment type not recognised: "+GlobalVars.ENVIRONMENT_TYPE.toString());
		return null; 
		}
	}

	@SuppressWarnings("unchecked")
	public static Route createRoute(Burglar burglar, Coord dest, Building destinationBuilding, SearchAlg.SEARCH_TYPES type) {
		switch (GlobalVars.ENVIRONMENT_TYPE) {
		case NULL:	return new NullRoute(burglar, dest, destinationBuilding, type);
		case GIS:	return new GISRoute(burglar, dest, destinationBuilding, type,
				((SimphonyGISEnvironment<Road, Road>) GlobalVars.ROAD_ENVIRONMENT).getGISProjection());

		case GRID: 	return new GridRoute(burglar, dest, destinationBuilding, type);
		default: 	Outputter.errorln("EnvironmentFactory.createRoute(): Error: Environment type not recognised: "+GlobalVars.ENVIRONMENT_TYPE.toString());
		return null;
		}
	}

	/**
	 * Returns the distance between two coordinates. Uses Route.getDistance() (see default Route constructor
	 * for reasons why Route.getDistance() isn't static so can't be used by other classes directly).
	 * @param b The burglar who is travelling, this will affect the distance in a GIS environment (they
	 * might be able to make use of transportation).
	 * @param c1 Origin coord
	 * @param c2 Destination coord.
	 * @return The distance, relative to amount of time it will take the burglar to travel it.
	 */
	public static double getDistance(Burglar b, Coord c1, Coord c2) {
		switch (GlobalVars.ENVIRONMENT_TYPE) {
		case NULL:	return new NullRoute().getDistance(b, c1, c2);
		case GIS:	return new GISRoute().getDistance(b, c1, c2);
		case GRID: 	return new GridRoute().getDistance(b, c1, c2);
		default: 	Outputter.errorln("EnvironmentFactory.getDistance(): Error: Environment type not recognised: "+GlobalVars.ENVIRONMENT_TYPE.toString());
		return -1;
		}
	}


	/**
	 * Each Environment must have an associated context to store agents. These contexts must also
	 * be added to the model.score or Simphony will complain.
	 * @param <T>
	 * @param name The name of the Environment
	 * @return a context
	 */
	@SuppressWarnings("unchecked")
	protected static <T> Context<T> createContext(String name) {
		// XXXX USE REFLECTION HERE TO CREATE A CONTEXT USING THE APPROOPRIATE CLASS NAME
		// NOTE: keep references to some of these contexts and projections which will be used to
		// build up the road network.
		if (name.equals("BurglarEnvironment")) {
			return new BurglarContext<T>();
		}
		else if (name.equals("BuildingEnvironment")) {
			return new BuildingContext<T>();
		}
		else if (name.equals("RoadEnvironment")) {
			return new RoadContext<T>();
		}
		else if (name.equals("JunctionEnvironment")) {
			junctionContext = new JunctionContext<Junction>();
			return (Context<T>) junctionContext;
		}
		else if (name.equals("CommunityEnvironment")) {
			return new CommunityContext<T>();
		}
		else {
			Outputter.errorln("EnvironmentFactory: createContext() error: unrecognised environment name: "+name);
			return null;
		}

	}

	/**
	 * Have to 'reset' static variables so they don't persist over multiple simulation runs (this will
	 * cause problems with batch runs or if the repast GUI is used to restart a model).
	 * <p>
	 * This should be called in ContextCreator before any EnvironmentFactory.createEnvironment() methods
	 * are called.
	 * <p>
	 * This could be
	 * done by using the Cacheable interface (like GISRoute does) but it isn't possible to register the
	 * EnvironmentFactory as a Cacheable objects in GlobalVars.caches() because it is static (i.e.
	 * GlobalVars.caches.add(this) wont work)
	 */
	public static void init() {
		roadNetwork = null;
		junctionContext = null;
		//		edgeIDs_KeyEdge = new Hashtable<RepastEdge<?>, String>(); // Stores the TOIDs of edges (Edge as key)
		//		edgeIDs_KeyID = new Hashtable<String, RepastEdge<Junction>>(); // Stores the TOIDs of edges (TOID as key)
		//		edges_roads = new Hashtable<NetworkEdge<Junction>, Road>(); // Stores the Edges and their associated Roads
		//		roads_edges = new Hashtable<Road, NetworkEdge<Junction>>(); // Stores the Roads and their associated Edges
		//		roadIdCache = null;
		burgledHouses = null;
	}


	/**
	 * Used to populate the communityEnvironment from a shapefile or grid image.
	 * 
	 * @param communityEnvironment
	 * @throws Exception if something went wrong, will have to examine the exception in more detail to find
	 * out what happened (I can't be bothered to work out and describe all the different exceptions that could be
	 * thrown and why!). 
	 */
	@SuppressWarnings("unchecked")
	public static void createCommunities(Environment<Community> communityEnvironment) throws Exception {
		Outputter.debugln("EnvironmentFactory: creating communities", Outputter.DEBUG_TYPES.INIT);
		EnvironmentFactory.createdCommunities = true;

		if (GlobalVars.ENVIRONMENT_TYPE.equals(GlobalVars.ENVIRONMENT_TYPES.NULL)) {
			// See if need to create a specific environment for sensitivity tests
			if (GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.NULL_ENVIRIONMENT_BUILDING_CONFIGURATION.equals("")) {
				// Null string, no sensitivity tests, manually create a single community here for the null environment
				communityEnvironment.add(EnvironmentFactory.createDefaultCommunity());
			}

			// Configuration when testing house variables: 11 houses with ascending parameter values, 1 community
			else if (GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.NULL_ENVIRIONMENT_BUILDING_CONFIGURATION.
					equals("TESTING_HOUSE_PARAMETER")) {
				// Testing a house parameter so just create a single, default community.
				communityEnvironment.add(EnvironmentFactory.createDefaultCommunity());
			}

			// When testing community parameters: 11 communities with ascending parameter values, 1 house in each
			else if (GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.NULL_ENVIRIONMENT_BUILDING_CONFIGURATION.
					equals("TESTING_COMMUNITY_PARAMETER")) {
				for (double d=0.0; d<=1.0; d+=0.1) {
					Community c = EnvironmentFactory.createDefaultCommunity();
					// Need to check if the parameter is part of the community or it's sociotype. Try the operation,
					// if it fails then assume parameter is part of Sociotype
					if (GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.setObjectValue(c, d, false)) {
						// Operation would have succeeded so parameter is part of community, set the value.
						GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.setObjectValue(c, d, true);						
					}
					else {
						// Parameter is hopefully part of Sociotype
						if (!GlobalVars.SOCIOTYPE_CLASS.equals(OAC.class)) {
							Outputter.errorln("EnvFac.createCommunities() error: when running sensitivity tests " +
							"cannot use Sociotypes other than OAC, I haven't included this functionality.");
						}
						OAC s = (OAC) c.getSociotype();
						GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.setObjectValue(s, d, true);
					}

					communityEnvironment.add(c);
				}
			}
			else {
				String error = "EnvironmentFactory.createBuildingsAndRoads(): Using NULL environment: unrecognised" +
				" type of building configuration: "+GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.NULL_ENVIRIONMENT_BUILDING_CONFIGURATION;
				Outputter.errorln(error);
				throw new Exception(error);
			} // else

		}

		/* Read communities from an image and get their attributes from an associated flat file. Each
		 * community must be represented by a different colour in the image, the actual colours don't
		 * matter but they must be different because they are used to distinguish community boundaries.
		 * IMPORTANT: the blue component is used to represent the ID of the community's Sociotype which
		 * is read in from a separate flat file.
		 * NOTE: it is not possible for a single object (e.g. Community) to exist in more than one grid
		 * cell at the same time. To get round this a copy of the Community is created in every cell it
		 * covers. The copies will all have the same parameters except for their coords and their equals()
		 * methods will return false otherwise repast thinks they're the same!*/
		else if (GlobalVars.ENVIRONMENT_TYPE.equals(GlobalVars.ENVIRONMENT_TYPES.GRID)) {

			// Read the image file to create communities and (based on given coordinates in the flat file)
			File f = null;
			BufferedImage bi = null;

			f = new File(GlobalVars.GRID_PARAMS.COMMUNITIES_FILENAME);
			bi = ImageIO.read(f);

			// Store each community and all the coordinates they contain
			EnvironmentFactory.gridCommunityCoords = new Hashtable<Community, List<Coord>>();
			Map<Color, Community> colours = new Hashtable<Color, Community>();// Used to distinguish communities
			int xDim = GlobalVars.GRID_PARAMS.XDIM;
			int yDim = GlobalVars.GRID_PARAMS.YDIM;
			Raster image = bi.getRaster();
			// Get an array of pixels starting in top-left corner working to the right. Each cell has three
			// consecutive integer entries in the array representing r,g,b values.
			int[] pixels = image.getPixels(0, 0, xDim, yDim, (int[])null); // An RGP pixel array
			int x=0; int y=0;
			for (int i=0; i<pixels.length; i+=3) {
				// Try to create a Color for each pixel
				Color col = new Color(pixels[i], pixels[i+1], pixels[i+2]);
				int communityID = pixels[i+2];
				Coord coord = new Coord(x,yDim-y-1); // Read image from top to bottom so reverse y value
				if (col.equals(GlobalVars.GRID_PARAMS.ROAD_COLOR)) {
					// Ignore roads, don't want to use the road network to create a Community!
				}
				else if (!colours.containsKey(col)) { // Found a new community
					Community com = new Community();
					com.setId(communityID);
					colours.put(col, com);
					List<Coord> coords = new ArrayList<Coord>();
					coords.add(coord);
					gridCommunityCoords.put(com, coords);
//					Outputter.debugln("Found new community: "+com.toString()+" ("+(new Coord(x,y)).toString(),//+") map: "+gridCommunityCoords.keySet().toString(),
//							Outputter.DEBUG_TYPES.INIT);
				}
				else { // Already found some pixels belonging to this community
					Community com = colours.get(col);
					// Add this coord to the existing community
					gridCommunityCoords.get(com).add(coord);
//					Outputter.debugln("Adding coords to community "+com.getId()+" ("+(new Coord(x,y)).toString()+")"+gridCommunityCoords.get(com).toString(),
//							Outputter.DEBUG_TYPES.INIT);
				}				
				x++;
				if (x>=xDim) {
					x=0;
					y++;
				}
			} // for

			Outputter.debugln("EnvironmentFactory.createCommunities(grid env) found following communities: ", Outputter.DEBUG_TYPES.INIT);
			for (Community c:gridCommunityCoords.keySet()) {
				Outputter.debugln("\t"+c.toString(),Outputter.DEBUG_TYPES.INIT);
				for (Coord co:gridCommunityCoords.get(c)) {
					Outputter.debug(","+co.toString(), Outputter.DEBUG_TYPES.INIT);
				}
				Outputter.debugln("", Outputter.DEBUG_TYPES.INIT);
			}

			// Read the flat file and create Sociotype objects
			List<? extends Sociotype> sociotypes = FlatFileObjectReader.readSociotypeObjects(
					GlobalVars.SOCIOTYPE_CLASS, GlobalVars.GRID_PARAMS.COMMUNITIES_DATA_LOCATION, null);
			// Initialise the Sociotypes
			for (Sociotype s:sociotypes) s.init();

			//Assign each community the correct Sociotype object using the Coords
			for (Community c:gridCommunityCoords.keySet()) {
				boolean foundSociotype = false;
				for (Sociotype s:sociotypes) {
					if (c.getId()==s.getId()) {
						c.setSociotype(s);
						foundSociotype = true;
						break;
					}
				}
				if (!foundSociotype) 
					Outputter.errorln("EnvironmentFactory.createCommunities() error: couldn't find a Sociotype" +
							"for the community: "+c.toString());
			}

			// Set some other community parameters. e.g. traffic volume and collective efficacy should be part
			// of Community but in Grid environment they're read in with sociotype data for simplicitiy (the
			// Grid Community file only has an identifier to a Sociotype object in it, no other data).
			// See Sociotype.getCE() or getTV() for more info.
			for (Community c:gridCommunityCoords.keySet()) {
				c.setArea(gridCommunityCoords.get(c).size()); // The geographic area
				//				c.setTrafficVolume(c.getSociotype().getTV());		// The traffic volume
				c.setCollectiveEfficacy(c.getSociotype().getCE());	// The collective efficacy
			}

			//			System.out.println("Communities: "+gridCommunityCoords.keySet().toString());
			// Finally add the communities to the environment.
			for (Community c:gridCommunityCoords.keySet()) {
//				System.out.println("Coords for this community: "+c.toString()+": "+gridCommunityCoords.get(c).toString());
				for (Coord coord:gridCommunityCoords.get(c)) {
//					 Create a copy of the community because same object cannot exist in > 1 grid cell
					Community copy = new Community(c, coord);
//					System.out.println("Created community: "+copy.toString());
					communityEnvironment.add(copy);
					communityEnvironment.move(copy, coord);
				}
			}
//			Outputter.debugln("EnvironmentFactory.createCommunities(GRID) created communities: ", Outputter.DEBUG_TYPES.INIT);
//			for (Community c:GlobalVars.COMMUNITY_ENVIRONMENT.getAllObjects(Community.class)) {
//				Outputter.debugln(c.toString(), Outputter.DEBUG_TYPES.INIT);
//			}
		}

		/* Read communities from a shapefile. This is slightly confusing because the 'communities' shapefile
		 * also contains all the variables which will make up the particular Sociotype of the community (e.g.
		 * using the OAC classification the communities shapefile contains the 41 variables which distinguish
		 * a community). So, the shapefile is read twice, the first time to create Sociotypes and the second
		 * to create the Communities, afterwards the Communities can be assigned the relevant Sociotype object
		 * using the 'ID' field to link them. */
		else if (GlobalVars.ENVIRONMENT_TYPE.equals(GlobalVars.ENVIRONMENT_TYPES.GIS)) {

			// Read in the Sociotypes (have included possibility of using different types of community classification)
			List<? extends Sociotype> sociotypes = null;
			sociotypes = EnvironmentFactory.readShapefile(
					GlobalVars.SOCIOTYPE_CLASS, GlobalVars.GIS_PARAMS.COMMUNITIES_FILENAME);
			// Initialise the sociotypes, some things can't be done until the objects have been created
			for (Sociotype s:sociotypes) s.init();

			// Read in the Communities
			SimphonyGISEnvironment<Community, Community> communityEnv = 
				(SimphonyGISEnvironment<Community, Community>) communityEnvironment;
			EnvironmentFactory.readShapefile(Community.class, GlobalVars.GIS_PARAMS.COMMUNITIES_FILENAME,
					communityEnv.getGISProjection(), communityEnv.getGISContext());

			// Assign each Community a Sociotype using the id's to link (remember that sociotypes and
			// communities are actually read from the same data set.
			for (Community c:GlobalVars.COMMUNITY_ENVIRONMENT.getAllObjects(Community.class)) {
				boolean foundSociotype = false;
				for (Sociotype s:sociotypes) {
					if (s.getId()==c.getId()) {
						c.setSociotype(s);
						foundSociotype = true;
					}
				}
				if (!foundSociotype) {
					Outputter.errorln("EnvironmentFactor.createCommunities() error: could not find a Sociotype" +
							"for the Community with id "+c.getId());
				}
			}		
		}
		else {
			Outputter.errorln("EnvironmentFactory.createCommunitird(): unrecognised enviroment type: "+
					GlobalVars.ENVIRONMENT_TYPE.toString());
		}


	}

	/**
	 * Creates some buildings (Houses, Workplaces etc), Junctions and Roads by reading a file(s).
	 * Behaves differently depending on the environment being used.<br>
	 * 
	 * For the null environment one of each type of building is created at null coordinates.<br> 
	 * 
	 * For the grid environment, roads and building are read in from a picture file, with different
	 * colours denoting different types of object (road, house, workplace etc).<br>
	 * 
	 * For the gis environment different shapefiles are read for the different types of object
	 * (Road, Building etc).
	 * 
	 * @param buildingEnvironment
	 * @param roadEnvironment
	 * @param junctionEnvironment If an input file couldn't be read
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	public static void createBuildingsAndRoads(Environment<Building> buildingEnvironment,
			Environment<Road> roadEnvironment, Environment<Junction> junctionEnvironment,
			Environment<Community> communityEnvironment) throws Exception {

		if (!EnvironmentFactory.createdCommunities) {
			Outputter.errorln("EnvironmentFactory.createRoadsAndBuildings: error, createCommunities should " +
			"be called first, this will probably cause errors.");
		}

		Outputter.debugln("EnvironmentFactory: creating buildings and roads", Outputter.DEBUG_TYPES.INIT);

		if (GlobalVars.ENVIRONMENT_TYPE.equals(GlobalVars.ENVIRONMENT_TYPES.NULL)) {
			// See if a particular building/community configuration has been specified, this is used in the
			// Null environment sensitivity tests
			if (GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.NULL_ENVIRIONMENT_BUILDING_CONFIGURATION.equals("")) {
				// Empty string: default configuration (one house, one community).
				Outputter.debugln("EnvironmentFactory.createBuildingsAndRoads() using default building configuration.",
						Outputter.DEBUG_TYPES.INIT);
				// Manually create objects here for the null environment
				Coord coord = new Coord(1,1); 
				Community c = GlobalVars.COMMUNITY_ENVIRONMENT.getRandomObject(Community.class);  // (Only one community in the null environment)
				Building j = new House(coord); buildingEnvironment.add(j); j.setCommunity(c);
				// Give the house default parameter values
				//				j.setAccessibility(0.5); j.setSecurity(0.5); j.setTrafficVolume(0.5); j.setVisibility(0.5);
				// Now some drug dealers, social locations and workplaces			
				Building k = new DrugDealer(coord); buildingEnvironment.add(k); k.setCommunity(c);
				Building l = new Social(coord); buildingEnvironment.add(l); l.setCommunity(c);
				Building o = new Workplace(coord); buildingEnvironment.add(o); o.setCommunity(c);			
				// Tell the community about the buildings
				c.addBuilding(j);c.addBuilding(k);c.addBuilding(l);c.addBuilding(o);
			}
			// Configuration when testing house variables: 11 houses with ascending parameter values, 1 community
			else if (GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.NULL_ENVIRIONMENT_BUILDING_CONFIGURATION.
					equals("TESTING_HOUSE_PARAMETER")) {
				Community com = GlobalVars.COMMUNITY_ENVIRONMENT.getRandomObject(Community.class);  // (Only one community in the null environment)
				Coord c = new Coord(1,1);
				for (double d=0.0; d<=1.0; d+=0.1) {
					Building b = new Building(c); // Has to be a Building because that's where all set* methods declared
					// Set the value of the house's parameter which is being tested
					GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.setObjectValue(b, d, true);
					// Now can create a house from the building, will inherit all parameter values
					House h = new House(b);
					buildingEnvironment.add(h); com.addBuilding(h); h.setCommunity(com);
				}
				// Finally create drug dealer, social and work places.
				Building k = new DrugDealer(c); buildingEnvironment.add(k); k.setCommunity(com);
				Building l = new Social(c); buildingEnvironment.add(l); l.setCommunity(com);
				Building o = new Workplace(c); buildingEnvironment.add(o); o.setCommunity(com);			
				com.addBuilding(k);com.addBuilding(l);com.addBuilding(o);

				// Some debugging info
				String debug = "";
				debug = "EnvFac.createBuildingsAndRoads() created following buildings:\n";
				for (Building b:GlobalVars.BUILDING_ENVIRONMENT.getAllObjects(Building.class)) {
					debug += "\t"+b.getParameterValuesString()+" in community "+b.getCommunity().toString()+"\n";
				}
				Outputter.debugln(debug, Outputter.DEBUG_TYPES.INIT);
			}
			// When testing community parameters: 11 communities with ascending parameter values, 1 house in each
			else if (GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.NULL_ENVIRIONMENT_BUILDING_CONFIGURATION.
					equals("TESTING_COMMUNITY_PARAMETER")) {
				Coord coord = new Coord(1,1);
				// Communities will have already been created, just go through each one and create a house in each
				for (Community c:GlobalVars.COMMUNITY_ENVIRONMENT.getAllObjects(Community.class)) {
					Building b = new House(coord);
					c.addBuilding(b); b.setCommunity(c); buildingEnvironment.add(b);
				}
				// Also create a single drug dealer, social and workplace
				Building b1 = new Workplace(coord);
				Community c1 = GlobalVars.COMMUNITY_ENVIRONMENT.getRandomObject(Community.class);
				c1.addBuilding(b1); b1.setCommunity(c1); buildingEnvironment.add(b1);

				Building b2 = new Social(coord);
				Community c2 = GlobalVars.COMMUNITY_ENVIRONMENT.getRandomObject(Community.class);
				c2.addBuilding(b2); b2.setCommunity(c2); buildingEnvironment.add(b2);

				Building b3 = new DrugDealer(coord);
				Community c3 = GlobalVars.COMMUNITY_ENVIRONMENT.getRandomObject(Community.class);
				c3.addBuilding(b3); b3.setCommunity(c3); buildingEnvironment.add(b3);

				// Some debugging info
				String debug = "";
				debug = "EnvFac.createBuildingsAndRoads() have following communities with buildings in them:\n";
				for (Community c:GlobalVars.COMMUNITY_ENVIRONMENT.getAllObjects(Community.class)) {
					debug += "\t"+c.getParameterValuesString()+" :: ";
					for (Building b:c.getBuildings()) {
						debug+=b.toString()+", ";
					}
					debug += "\n";
				}
				Outputter.debugln(debug, Outputter.DEBUG_TYPES.INIT);
			}
			else {
				String error = "EnvironmentFactory.createBuildingsAndRoads(): Using NULL environment: unrecognised" +
				" type of building configuration: "+GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.NULL_ENVIRIONMENT_BUILDING_CONFIGURATION;
				Outputter.errorln(error);
				throw new Exception(error);
			} // else
		}

		/*
		 * In a GRID environment, roads and buildings are read in from an image file where Road, House,
		 * DrugDealer or Social place are all coded with different colours. The file is read by scrolling
		 * horizontally from top left to bottom right .
		 * Houses have some additional values which must be set (accessibility, security and visibility).
		 * These are read in from an external source using the building id to link (this is an auto-increment
		 * number which is incremented for every new building so the upper-left-most building has value 0
		 * and lower-right building has value numBuildings-1.  
		 */
		else if (GlobalVars.ENVIRONMENT_TYPE.equals(GlobalVars.ENVIRONMENT_TYPES.GRID)) {
			// Data store to get the extra house information.
			DataAccess da = DataAccessFactory.getDataAccessObject(GlobalVars.GRID_PARAMS.GRID_DATA_LOC);
			// Read the grid image file
			File f = null;
			BufferedImage bi = null;
			f = new File(GlobalVars.GRID_PARAMS.BUILDINGS_FILENAME);
			bi = ImageIO.read(f);			

			int xDim = GlobalVars.GRID_PARAMS.XDIM;
			int yDim = GlobalVars.GRID_PARAMS.YDIM;
			Raster image = bi.getRaster();
			// Get an array of pixels starting in top-left corner working to the right. Each cell has three
			// consecutive integer entries in the array representing r,g,b values.
			int[] pixels = image.getPixels(0, 0, xDim, yDim, (int[])null); // An RGP pixel array
			int x=0; int y=0;
			for (int i=0; i<pixels.length; i+=3) {
				// Try to create a Color for each pixel
				Color col = new Color(pixels[i], pixels[i+1], pixels[i+2]);
				Coord coord = new Coord(x,yDim-y-1); // Read image from top to bottom so reverse y value
				if (col.equals(GlobalVars.GRID_PARAMS.ROAD_COLOR)) {
					Road r = new Road(coord);
					roadEnvironment.add(r);roadEnvironment.move(r, coord);
				}
				else if (col.equals(GlobalVars.GRID_PARAMS.HOUSE_COLOR)) {
					Building h = new House(coord);
					h.setType(GlobalVars.GIS_PARAMS.HOUSE_TYPE); // Not necessary but useful for results analysis
					// Get the house's extra parameters
					h.setAccessibility(EnvironmentFactory.getGridHouseValues(h.getId(), da, "acc"));
					h.setSecurity(EnvironmentFactory.getGridHouseValues(h.getId(), da, "sec"));
					h.setVisibility(EnvironmentFactory.getGridHouseValues(h.getId(), da, "vis"));
					buildingEnvironment.add(h);buildingEnvironment.move(h, coord);
				}
				else if (col.equals(GlobalVars.GRID_PARAMS.DRUG_DEALER_COLOR)) {
					Building d = new DrugDealer(coord);
					buildingEnvironment.add(d);buildingEnvironment.move(d, coord);
					d.setType(GlobalVars.GIS_PARAMS.DRUG_DEALER_TYPE); // Not necessary but useful for results analysis
				}
				else if (col.equals(GlobalVars.GRID_PARAMS.SOCIAL_COLOR)) {
					Building s = new Social(coord);
					buildingEnvironment.add(s);buildingEnvironment.move(s, coord);
					s.setType(GlobalVars.GIS_PARAMS.SOCIAL_TYPE); // Not necessary but useful for results analysis
				}
				else if (col.equals(GlobalVars.GRID_PARAMS.WORK_COLOR)) {
					Building w = new Workplace(coord);
					buildingEnvironment.add(w);buildingEnvironment.move(w, coord);
					w.setType(GlobalVars.GIS_PARAMS.WORKPLACE_TYPE); // Not necessary but useful for results analysis
				}
				else {
					Outputter.errorln("EnvironmentFactory.createBuildingsandRoads: unrecognised colour in pixel (" +
							x+","+y+"): "+col.toString());
				}
				x++;
				if (x>=xDim) {
					x=0;
					y++;
				}

			} // for

			// Now tell the buildings and communities about each other.
			for (Building b:buildingEnvironment.getAllObjects(Building.class)) {
				//				// Unfortunately can't use getObjectAt() because I haven't made it possible for objects in Grid
				//				// Environment to exist over more than one cell :-( Use the map created in createCommunities()
				//				// instead
				//				boolean foundCommunity = false;
				//				for (Community com:communityEnvironment.getAllObjects(Community.class)) {
				//					if (gridCommunityCoords.get(com).contains(b.getCoords())) { 
				//						// The building's coords are within this communities coords
				//						b.setCommunity(com);
				//						com.addBuilding(b);
				//						foundCommunity=true;
				//						break;
				//					}
				// 				}
				//				if (!foundCommunity) {
				//				Outputter.errorln("EnvironmentFactory.createBuildingsAndRoads(GRID) error: could not find " +
				//						"a community which building "+b.toString()+"("+b.getCoords().toString()+") is part of.");
				//			}
				Community com = communityEnvironment.getObjectAt(Community.class, b.getCoords());
				if (com==null)
					Outputter.errorln("EnvironmentFactory.createBuildingsAndRoads(GRID) error: could not find " +
							"a community which building "+b.toString()+"("+b.getCoords().toString()+") is part of.");
				b.setCommunity(com);
				com.addBuilding(b);	
			}

		} // if GRID

		else if (GlobalVars.ENVIRONMENT_TYPE.equals(GlobalVars.ENVIRONMENT_TYPES.GIS)) {
			// To use the shapefileLoad we need explicit access to each Environment's context and projection
			// so here cast each environment to a SimphonyGISEnvironment.
			SimphonyGISEnvironment<Road, Road> roadEnv =
				(SimphonyGISEnvironment<Road, Road>) roadEnvironment;
			SimphonyGISEnvironment<? extends Building, Building> buildingEnv = 
				(SimphonyGISEnvironment<? extends Building, Building>) buildingEnvironment;
			// Also have to case the community environment for access to getObjectsWithin(Geometry) method
			SimphonyGISEnvironment<Community, Community> communityEnv = 
				(SimphonyGISEnvironment<Community, Community>) communityEnvironment;

			// Read in the roads
			EnvironmentFactory.readShapefile(Road.class, GlobalVars.GIS_PARAMS.ROAD_FILENAME,
					roadEnv.getGISProjection(), roadEnv.getGISContext());
			// Initialise the roads, there are some things that can't be done until the objects have been created
			for (Road r:roadEnvironment.getAllObjects(Road.class))
				r.initialise();

			// Read in the buildings 
			EnvironmentFactory.readShapefile(Building.class, GlobalVars.GIS_PARAMS.BUILDING_FILENAME,
					buildingEnv.getGISProjection(), buildingEnv.getGISContext());

			// Now cast the buildings appropriately, depending on their 'type'
			EnvironmentFactory.castBuildings(buildingEnv.getGISContext(), buildingEnv.getGISProjection());

			// Finally tell the buildings and the communities about each other
			Map<Building, Community> foundBuildings = new Hashtable<Building, Community>(); // Remember buildings which have been found
			Geometry communityGeom;
			for (Building b:buildingEnv.getAllObjects(Building.class)) {
				// Loop through every community to find one which contains the building. Can't use the getObjectsWithin(Envelope) because this doesn't give the correct results (probably something to do with the shape of the Envelope).
				for (Community c:communityEnv.getAllObjects(Community.class)) {
					communityGeom = communityEnv.getGISProjection().getGeometry(c);
					if (communityGeom.contains(buildingEnv.getGISProjection().getGeometry(b))) {
						// Have found a community which the building 'b' is within
						if (foundBuildings.containsKey(b)){
							Outputter.errorln("EnvironmentFactory.createBuildingsAndRoads(GIS) error: have already "+
									"found that building '"+b.toString()+"' is within community '"+
									foundBuildings.get(b).toString()+"', "+
									"but apparantly it should also be within community '"+c.toString()+"'!");
						}
						else {
							foundBuildings.put(b, c);
							b.setCommunity(c);
							c.addBuilding(b);
						}
						break;
					} // if communityGeom contains buildingGeom
				} // for communities
			} // for buildings
			// This commented code uses the Envelope but it doesn't work, misses some houses
			//			for (Community c:communityEnv.getAllObjects(Community.class)) {
			//			communityGeom = communityEnv.getGISProjection().getGeometry(c);
			//			for (Building b:buildingEnv.getGISProjection().getObjectsWithin(communityGeom.getEnvelopeInternal())) {
			//				if (foundBuildings.containsKey(b)){
			//					Outputter.errorln("EnvironmentFactory.createBuildingsAndRoads(GIS) errro: have already "+
			//							"found that building '"+b.toString()+"' is within community '"+
			//							foundBuildings.get(b).toString()+"', "+
			//							"but apparantly it should also be within community '"+c.toString()+"'!");
			//				}
			//				else {
			//					foundBuildings.put(b, c);
			//					b.setCommunity(c);
			//					c.addBuilding(b);
			//				}
			//			}
			//		}

			// Check that all buildings have been assigned a community
			for (Building b:buildingEnv.getAllObjects(Building.class)){
				if (!foundBuildings.containsKey(b)) {
					Outputter.errorln("EnvironmentFactory.createBuildingsAndRoads(GIS) error: couldn't find a "+
							"community for building '"+b+"'.");
				}
			}

		} // if GIS


		else {
			Outputter.errorln("EnvironmentFactory.createBuildingsAndRoads(): unrecognised enviroment type: "+
					GlobalVars.ENVIRONMENT_TYPE.toString());
		}

		// Finally run some post-initisation for the buildings.
		for (Building b:GlobalVars.BUILDING_ENVIRONMENT.getAllObjects(Building.class)) {
			b.postInit();
		}

	}


	/**
	 * Read data from an external source to get the values for Houses when using a grid environment
	 * @param uniqueID The House's unique id. ID's are assigned incrementally from top left (0) to
	 * bottom right (numBuildings-1). 
	 * @param da The DataAccess object used to get the building data
	 * @param paramName The name of the Building paramater to get (e.g. "acc", "sec" or "vis").
	 * @return
	 * @throws SQLException 
	 */
	private static double getGridHouseValues(int uniqueID, DataAccess da, String paramName) throws SQLException {
		return da.getValue(
				GlobalVars.GRID_PARAMS.BUILDING_DATA,	// The name of file or database table
				paramName,						// The variable name (as a string), e.g. "v31"
				Double.class, 					// The expected return type
				"id",		 					// The column which will uniquely identify this building
				Integer.class, 					// The column type
				uniqueID);				 		// The unique identifier for this OAC's sub-group.
	}

	/**
	 * Used to create a road network. Takes the Environment which will contain the junctions (this is the
	 * environment which will also have the Network projection) and the Environment which will contain the
	 * road objects. Depending on the type of Environment being used (GIS, GRID etc) the road network
	 * will be created appropriately.<br>
	 * 
	 * For the grid environment it is assumed that all roads are connected horizontally or vertically 
	 * (no diagonal roads). A Junction is created at every road cell and these are connected to junctions 
	 * in adjacent cells.<br>
	 * 
	 * For the GIS environment...
	 * 
	 * @param junctionEnvironment Contains the junctions and the Network projection.
	 * @param roadEnvironment Contains the road objects.
	 */
	@SuppressWarnings("unchecked")
	public static void createRoadNetwork(Environment<Junction> junctionEnvironment, Environment<Road> roadEnvironment) {

		if (GlobalVars.ENVIRONMENT_TYPE.equals(GlobalVars.ENVIRONMENT_TYPES.NULL)) {
			Outputter.debugln("EnvironmentFactory.createRoadNetwork(): no need to create a road network for a" +
					" NULL environment.", Outputter.DEBUG_TYPES.INIT);
			return;
		}

		Outputter.debugln("EnvironmentFactory: creating road network", Outputter.DEBUG_TYPES.INIT);		
		// Create the network projection required for Grids and GIS environments
		//		EnvironmentFactory.roadNetwork = (Network<Junction>) NetworkFactoryFinder.createNetworkFactory(new HashMap<String, Object>()).
		//			createNetwork("RoadNetwork", junctionContext, false);
		NetworkBuilder<Junction> builder = new NetworkBuilder<Junction>("RoadNetwork", junctionContext, false);
		builder.setEdgeCreator(new NetworkEdgeCreator<Junction>());
		EnvironmentFactory.roadNetwork = builder.buildNetwork();


		/* For GIS environments have to create network of junctions from end points of each road
		 * and link them with repast edges. */
		if (GlobalVars.ENVIRONMENT_TYPE.equals(GlobalVars.ENVIRONMENT_TYPES.GIS)) {
			Outputter.debugln("EnvironmentFactory.createRoadNetwork(): creating a road network for a GIS environment",
					Outputter.DEBUG_TYPES.INIT);
			// As with createBuildingsAndRoads() need to cast the junction and road envionments to
			// SimphonyGISEnvrionment objects to have access to their contexts and projections.  - YUK!!
			SimphonyGISEnvironment<Road, Road> roadEnv =
				(SimphonyGISEnvironment<Road, Road>) GlobalVars.ROAD_ENVIRONMENT;
			SimphonyGISEnvironment<Junction, Junction> junctionEnv = 
				(SimphonyGISEnvironment<Junction, Junction>) GlobalVars.JUNCTION_ENVIRONMENT;
			buildGISRoadNetwork(roadEnv, junctionEnv);
		}


		/* For Grid environments each road cell can be a Junction and edges can be created between
		 * each adjacent road */
		else if (GlobalVars.ENVIRONMENT_TYPE.equals(GlobalVars.ENVIRONMENT_TYPES.GRID)) {
			Outputter.debugln("EnvironmentFactory.createRoadNetwork(): creating a road network for a GRID environment",
					Outputter.DEBUG_TYPES.INIT);
			// Create a junction at at the same points as each building in the environment so far.
			for (Road r:roadEnvironment.getAllObjects(Road.class)) {
				Junction j = new Junction(r.getCoords());
				junctionEnvironment.add(j); junctionEnvironment.move(j, r.getCoords());
			}

			// Now go through all junctions, linking adjacent ones
			for (Junction j:junctionEnvironment.getAllObjects(Junction.class)){
				// Assign North, East, South, West junctions, checking not go outside grid limits
				Junction northJunction = (j.getCoords().getY() == GlobalVars.GRID_PARAMS.YDIM-1) ? null :
					getJunctionAt(junctionEnvironment, new Coord(j.getCoords().getX(),(int)(j.getCoords().getY()+1)));
				Junction eastJunction = (j.getCoords().getX() ==  GlobalVars.GRID_PARAMS.XDIM-1) ? null :
					getJunctionAt(junctionEnvironment, new Coord(j.getCoords().getX()+1,(int)(j.getCoords().getY())));
				Junction southJunction = (j.getCoords().getY()==0) ? null :
					getJunctionAt(junctionEnvironment, new Coord(j.getCoords().getX(),(int)(j.getCoords().getY()-1)));
				Junction westJunction = (j.getCoords().getX()==0) ? null :
					getJunctionAt(junctionEnvironment, new Coord(j.getCoords().getX()-1,(int)(j.getCoords().getY())));
				addEdge(new NetworkEdge<Junction>(j, northJunction, false, 1, null));
				addEdge(new NetworkEdge<Junction>(j, eastJunction, false, 1, null));
				addEdge(new NetworkEdge<Junction>(j, southJunction, false, 1, null)); 
				addEdge(new NetworkEdge<Junction>(j, westJunction, false, 1, null));
			}// for Junctions


		}
		else {
			Outputter.errorln("EnvironmentFactory.createRoadNetwork(): unrecognised enviroment type: "+
					GlobalVars.ENVIRONMENT_TYPE.toString());
		}
	}

	/**
	 * Read in transport route information.
	 * @throws MalformedURLException For some reason.. 
	 */
	public static void createTransportRoutes(Environment<Junction> junctionEnvironment) throws MalformedURLException {
		if (GlobalVars.ENVIRONMENT_TYPE.equals(GlobalVars.ENVIRONMENT_TYPES.NULL)) {
			Outputter.debugln("EnvironmentFactory.createTransportRoutes(): no need to create transport " +
					"network for a NULL environment.", Outputter.DEBUG_TYPES.INIT);
			return;
		}

		Outputter.debugln("EnvironmentFactory: creating transport routes.", Outputter.DEBUG_TYPES.INIT);

		/* For the GIS environment, transport networks are used as follows:
		 * <p><ol>
		 * <li>Read in stations information. Each station has a route number (unique identifier of the route
		 * that the station is part of), a type (e.g. "bus" - the type is used to find the speed multiplier) 
		 * and a station number (the number of the station on the route - used to link stations together, 
		 * must be in ascending order but not necessarily increment by 1, can leave space to add stations
		 * easily later).</li>
		 * <li>Find the nearest Junction to each station</li>
		 * <li>Create NetworkEdge objects between the relevant Junctions using the station numbers. Give each
		 * NetworkEdge the appropriate type, this way the 'getWeight()' function knows what to return (e.g.
		 * if this is a bus route edge and the agent can use busses return a certain weight (e.g. distance/2),
		 * but if the agent can't use busses return infinity).</li></ol>
		 * <p>In this manner the same roadNetwork can be used which will include transport networks which
		 * certain agents will use. 
		 */
		if (GlobalVars.ENVIRONMENT_TYPE.equals(GlobalVars.ENVIRONMENT_TYPES.GIS)) {
			Outputter.debugln("EnvironmentFactory.createTransportRoutes(): creating a transport network " +
					"for a GIS environment", Outputter.DEBUG_TYPES.INIT);

			List<Station> allStations = readShapefile(Station.class, GlobalVars.GIS_PARAMS.STATIONS_FILENAME);

			// Find the nearest Junctions to each station 
			Hashtable<Station, Junction> stationsMap = new Hashtable<Station, Junction>();
			GeometryFactory geomFac = new GeometryFactory();
			for (Station s:allStations) {
				// As this is iterating through the stations anyway do a few checks
				if (!(GlobalVars.TRANSPORT_PARAMS.ALL_PARAMS).contains(s.getType())){
					Outputter.errorln("EnvFac.createTransportRoutes error: unrecognised type of transport " +
							"route: '"+s.getType()+"' for station '"+s.toString()+"'");
				}

				Point statGeom = geomFac.createPoint(s.getCoords().toCoordinate());
				double minDist = Double.MAX_VALUE;
				Junction closestJunction = null;
				double dist;  
				for (Junction j:junctionEnvironment.getAllObjects(Junction.class)){
					dist = (new DistanceOp(statGeom, geomFac.createPoint(j.getCoords().toCoordinate()))).distance();
					if (dist < minDist ) {
						minDist = dist;
						closestJunction = j;
					}
				} // for junctions
				if (closestJunction==null) 
					Outputter.errorln("EnvironmentFactory.createTransportRoutes(GIS): couldn't find a junction for station: "+s.toString());
				stationsMap.put(s, closestJunction);
			} // for stations

			// Creating a list of TransportRoute objects which hold all stations in order for each route.
			class TransportRoute {
				List<Station> stations;	int number; String type;
				public TransportRoute(int number, String type) { 
					this.number = number; this.type=type; this.stations=new ArrayList<Station>();
				}
				public void addStation (Station station) {
					if (stations.size()==0) {stations.add(station); return;}
					else if (station.getStatNum() < stations.get(0).getStatNum()) {
						stations.add(0, station); // station is lower number, add to start of list
					}
					for (int i=0; i<stations.size(); i++) { // station num is larger, add to appropriate place
						if (station.getStatNum() > stations.get(i).getStatNum()) { 
							stations.add(i+1, station);					
							return; 
						}
					}
				}
			}
			List<TransportRoute> routes = new ArrayList<TransportRoute>();
			for (Station s:stationsMap.keySet()) {
				boolean newRoute = true; // See if this station's route has already been created
				for (TransportRoute r:routes) if (r.number==s.getRouteNum()) newRoute = false;
				if (newRoute) {
					TransportRoute tr = new TransportRoute(s.getRouteNum(), s.getType());
					routes.add(tr);
					tr.addStation(s);
				}
				else {
					for (TransportRoute r:routes) {
						if (r.number==s.getRouteNum()) {
							r.addStation(s);
						} // if
					} // for
				} // else (newRoute)

			} // For stations

			// Now go through each station in each route and link the appropriate junctions			
			for (TransportRoute r:routes) {
				for (int i=0; i<r.stations.size()-1; i++) {

					Junction source = stationsMap.get(r.stations.get(i));
					Junction dest = stationsMap.get(r.stations.get(i+1));
					NetworkEdge<Junction> edge = new NetworkEdge<Junction>(source, dest, false, 
							new DistanceOp( geomFac.createPoint(source.getCoords().toCoordinate()),	geomFac.createPoint(dest.getCoords().toCoordinate())).distance(),
							Arrays.asList(new String[]{r.type}));
					addEdge(edge);
				}
			}
		}


		else if (GlobalVars.ENVIRONMENT_TYPE.equals(GlobalVars.ENVIRONMENT_TYPES.GRID)) {
			Outputter.debugln("EnvironmentFactory.createTransportRoutes(): no need to create transport " +
					"network for a GRID environment.", Outputter.DEBUG_TYPES.INIT);

		}
		else {
			Outputter.errorln("EnvironmentFactory.createTransportRoutes(): unrecognised enviroment type: "+
					GlobalVars.ENVIRONMENT_TYPE.toString());
		}

	}


	// Adds an edge to the network, checking the source and target aren't null.
	// If the edge already exists the new edge probably represents another way to access the edge (e.g.
	// a bus route) so just add the new type to the existing edge. Used by both GIS and GRID environments.
	private static void addEdge(NetworkEdge<Junction> edge) {
		// Don't create an edge if source or target is null (can happen with GRID environment)
		if (!(edge.getSource()==null || edge.getTarget()==null)) { 
			if (! (networkContainsEdge(edge) ))  {
				roadNetwork.addEdge(edge); 
			}
			else { // Network already contains an edge between the two junction, add the new accessibility type 
				((NetworkEdge<Junction>)roadNetwork.getEdge(edge.getSource(), edge.getTarget())).
				addType(edge.getTypes().get(0));
			}
		}
	}

	private static boolean networkContainsEdge(NetworkEdge<Junction> edge) {
		for (RepastEdge<Junction> e:roadNetwork.getEdges()) {
			if (e.getSource().equals(edge.getSource()) && e.getTarget().equals(edge.getTarget())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return the road network
	 */
	public static Network<Junction> getRoadNetwork() {
		if (roadNetwork==null) {
			Outputter.errorln("EnvironmentFactory.getRoadNetwork(): road network hasn't been initialised " +
			"(need to run EnvironmentFactory.buildRoadNetwork() first.");
		}
		return roadNetwork;
	}

	//	/**
	//	 * Simply pythag, get the distance between two points
	//	 * @return the distance
	//	 */
	//	private static double distance(Coord p1, Coord p2) {
	//		return Math.sqrt(
	//				Math.pow((p1.getX()-p2.getX()), 2) +
	//				Math.pow((p1.getY()-p2.getY()), 2));
	//	}
	/**
	 * Get the object at the specified coordinates. Used when building a road network in a Grid environment.
	 * Return null if there is no object at the coordinates.
	 * @param c
	 * @return
	 */
	private static Junction getJunctionAt(Environment<Junction> junctionEnvironment, Coord c) {
		for (Junction j:junctionEnvironment.getAllObjects(Junction.class)) {
			if (j.getCoords().equals(c))
				return j;
		}
		return null;
	}

	/**
	 * Nice generic function :-) reads in objects from shapefiles.
	 * <p>
	 * The objects (agents) created must extend FixedGeography to guarantee that they will
	 * have a setCoords() method. This is necessary because, for simplicity, geographical
	 * objects which don't move store their coordinates alongside the projection which stores
	 * them as well. So the coordinates must be set manually by this function once the shapefile
	 * has been read and the objects have been given coordinates in their projection.
	 * 
	 * @param <T> The type of object to be read (e.g. PecsHouse). Must exted
	 * @param cl The class of the building being read (e.g. PecsHouse.class).
	 * @param location The location of the shapefile containing the objects.
	 * @param geog A geography to add the objects to.
	 * @param context A context to add the objects to.
	 * @throws MalformedURLException If the location of the shapefile cannot be converted into a URL
	 */
	private static <T extends FixedGeography> void readShapefile(Class<T> cl, String location, Geography<T> geog, Context<T> context) throws MalformedURLException {
		File shapefile = null;
		ShapefileLoader<T> loader = null;
		shapefile = new File(location);
		loader = new ShapefileLoader<T>(cl, shapefile.toURL(), geog, context);
		while (loader.hasNext()) {
			loader.next();
		}

		for (T obj:context.getObjects(cl)){
			obj.setCoords(new Coord(geog.getGeometry(obj).getCentroid().getCoordinate()));
		}
	}

	/**
	 * Sames as readShapeFile() but returns the objects in a list. This can be used if the spatial information
	 * isn'r required. A temporary geography and context are used to extract the objects from the shapefile.  
	 * 
	 * @param <T> The type of object to be read (e.g. PecsHouse).
	 * @param cl The class of the building being read (e.g. PecsHouse.class).
	 * @param location The location of the shapefile containing the objects.
	 * @return A list of all objects which were read in (obviously they will have no geographical information
	 * associated with them).
	 * @throws MalformedURLException If the shapefile location cannot be converted to a URL.
	 */
	@SuppressWarnings("unchecked")
	private static <T, E extends FixedGeography> List<T> readShapefile(Class<T> cl, String location) throws MalformedURLException {
		File shapefile = null;
		ShapefileLoader<T> loader = null;
		Context<T> context = new DefaultContext<T>();
		Geography<T> geog = new DefaultGeography<T>("TempGeog");
		//		Geography<T> geog = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
		//				"TempGeog", context, new GeographyParameters<T>(new SimpleAdder<T>()));

		shapefile = new File(location);
		loader = new ShapefileLoader<T>(cl, shapefile.toURL(), geog, context);
		while (loader.hasNext()) {
			loader.next();
		}

		// Possibly set the coords for this object, have to check it implements FixedGeography
		for (Class<?> c:cl.getInterfaces()) {
			if (c.equals(FixedGeography.class)) { // One of this classes interfaces is FixedGeography
				for (T obj:context.getObjects(cl)){
					// Cast all objects and set the coords
					E o = (E) obj;
					o.setCoords(new Coord(geog.getGeometry(obj).getCentroid().getCoordinate()));
					obj = (T)o;
				}
				break;

			} // if
		} // for

		// Now return all objects in a list
		List<T> list = new ArrayList<T>();		
		for (T obj:context.getObjects(cl)) {
			list.add(obj);
		}
		return list;
	}

	/**
	 * 'Cast' Buildings to the correct subclass by creating a new object of the correct sub-type with
	 * the same parameters as the original building, adding it to the context and then removing
	 * the original Building from the context.
	 * @param <T>
	 * @param context
	 * @param geog
	 */
	private static <T extends Building> void castBuildings(Context<Building> context, Geography<Building> geog){
		List<Building> toRemove = new ArrayList<Building>(); // Store list of Builings to remove afterwards
		for (Building b:context.getObjects(Building.class)) {
			int type = b.getType();
			Building newB = null;
			if (type==GlobalVars.GIS_PARAMS.HOUSE_TYPE) {
				newB = new House(b);//.getCoords(), b.getId(), b.getType());
			}
			else if (type==GlobalVars.GIS_PARAMS.SOCIAL_TYPE) {
				newB = new Social(b);//.getCoords(), b.getId(), b.getType());
			}
			else if (type==GlobalVars.GIS_PARAMS.WORKPLACE_TYPE) {
				newB = new Workplace(b);//.getCoords(), b.getId(), b.getType());
			}
			else if (type==GlobalVars.GIS_PARAMS.DRUG_DEALER_TYPE) {
				newB = new DrugDealer(b);//.getCoords(), b.getId(), b.getType());
			}
			else if (type==10) {
				Outputter.errorln("EnvironmentFactory.castBuildingds() GOT BUILDING TYPE 10, TEMPORARILY SETTING THIS TO DRUG DEALER");
				newB = new DrugDealer(b);//.getCoords(), b.getId(), b.getType());
			}
			else {
				Outputter.errorln("EnvironmentFactory.castBuildings(): error, unrecognised type of building: '"+type+
						"' for building "+b.toString()+". Using buildings file:"+GlobalVars.GIS_PARAMS.BUILDING_FILENAME+
				".\n Ignoring the building, it wont exist in the simulation.");
			}
			if (newB!=null) {
				context.add(newB);
				geog.move(newB, geog.getGeometry(b).getCentroid());
			}
			toRemove.add(b);

		}
		for (Building b:toRemove) {
			context.remove(b);
		}

	}


	/** 
	 * Actually creates the road network.  Runs through the RoadGeography and generate nodes from the
	 * end points of each line. These nodes are added to the RoadNetwork (part of the JunctionContext)
	 * as well as to edges.
	 */
	private static void buildGISRoadNetwork(SimphonyGISEnvironment<Road, Road> roadEnv,
			SimphonyGISEnvironment<Junction, Junction> junctionEnv) {
		Outputter.debugln("EnvironmentFactory. buildGISRoadNetwork() building road network",
				Outputter.DEBUG_TYPES.INIT);
		// Get the required geographies and the contexts
		Geography<Road> roadGeography = roadEnv.getGISProjection();
		Geography<Junction> junctionGeography= junctionEnv.getGISProjection();
		Context<Junction> junctionContext = junctionEnv.getGISContext();
		// Create a GeometryFactory so we can create points/lines from the junctions and roads (this is so they can be displayed on the same display to check if the network has been created successfully)
		GeometryFactory geomFac = new GeometryFactory();
		// Create a cache of all Junctions and coordinates so we know if a junction has already been created at a particular coordinate
		Map<Coord, Junction> coordMap = new HashMap<Coord, Junction>();
		// Iterate through all roads
		Iterable<Road> roadIt = roadGeography.getAllObjects();
		for (Road road:roadIt) {
			// Create a LineString from the road so we can extract coordinates
			Geometry roadGeom = roadGeography.getGeometry(road);
			Coord c1 = new Coord(roadGeom.getCoordinates()[0]);	// First coord (XXXX - check coorinates are in this order)
			Coord c2 = new Coord(roadGeom.getCoordinates()[roadGeom.getNumPoints()-1]); // Last coord
			// Create Junctions from these coordinates and add them to the JunctionGeography (if they haven't been created already)
			Junction junc1;// = new Junction(c1); 
			Junction junc2;// = new Junction(c2);
			// Check if Junctions have already been created at those coordinates, creating them if not
			if (coordMap.containsKey(c1)) {
				// A Junction with those coordinates (c1) has been created, get it so we can add an edge to it
				junc1 = coordMap.get(c1);
			}
			else { // Junction does not exit
				junc1 = new Junction(c1);
				junctionContext.add(junc1);
				coordMap.put(c1, junc1);
				Point p1 = geomFac.createPoint(c1.toCoordinate());
				junctionGeography.move(junc1, p1);
			}
			if (coordMap.containsKey(c2)) {
				junc2 = coordMap.get(c2);
			}
			else { // Junction does not exit
				junc2 = new Junction(c2);
				junctionContext.add(junc2);
				coordMap.put(c2, junc2);
				Point p2 = geomFac.createPoint(c2.toCoordinate());
				junctionGeography.move(junc2, p2);
			}
			// Tell the road object who it's junctions are
			road.addJunction(junc1);
			road.addJunction(junc2);
			// Tell the junctions about this road
			junc1.addRoad(road);
			junc2.addRoad(road);

			// Create an edge between the two junctions, assigning a weight equal to it's length
			NetworkEdge<Junction> edge = new NetworkEdge<Junction>(junc1, junc2, false, roadGeom.getLength(), road.getAccessibility());
			// Set whether or not the edge represents a major road (gives extra benefit to car drivers).
			if (road.isMajorRoad()) edge.setMajorRoad(true);
			//			// Store the road's TOID in a dictionary (one with edges as keys, one with id's as keys)
			//			try {
			////				edgeIDs_KeyEdge.put(edge, (String) road.getIdentifier());
			////				edgeIDs_KeyID.put((String) road.getIdentifier(), edge);
			//				edges_roads.put(edge, road);
			//				roads_edges.put(road, edge);
			//			} catch (Exception e) {
			//				Outputter.errorln("EnvironmentFactory: buildGISRoadNetwork error, here's the message:\n"+e.getMessage());
			//			}
			// Tell the Road and the Edge about each other
			road.setEdge(edge);
			edge.setRoad(road);
			if (!roadNetwork.containsEdge(edge)) {
				roadNetwork.addEdge(edge);
			}
			else {
				System.err.println("CityContext: buildRoadNetwork: for some reason this edge that has just been created already exists in the RoadNetwork!");
			}

		} // for road:


	}


	/**
	 * Returns the road which is crosses the given coordinates
	 * (Actually it just returns the *nearest* road to the coords). Used by GISRoute.setRoute().
	 * 
	 * @param coord The coordinate which from which we want to find the nearest road
	 * @param roadGeography The roadGeography
	 * @return
	 * @throws Exception 
	 */

	public static Road findRoadAtCoordinates (Coord coord, Geography<Road> roadGeography) throws Exception {
		if (coord == null) {
			throw new NullPointerException("EnvironmentFactory.findRoadAtCoordinates(): ERROR: the input coordinate is null"); 
		}
		GeometryFactory geomFac = new GeometryFactory();
		Point point = geomFac.createPoint(coord.toCoordinate());
		//		Geometry buffer = point.buffer(GlobalVars.GIS_PARAMS.XXXX_BUFFER);
		double minDist = Double.MAX_VALUE;
		Road nearestRoad = null;
		//		for (Road road:roadGeography.getObjectsWithin(buffer.getEnvelopeInternal(), Road.class)) {
		for (Road road:GlobalVars.ROAD_ENVIRONMENT.getObjectsWithin(coord, Road.class, GlobalVars.GIS_PARAMS.XXXX_BUFFER, true)) {
			DistanceOp distOp = new DistanceOp(point, roadGeography.getGeometry(road));
			double thisDist = distOp.distance();
			if (thisDist < minDist) {
				minDist = thisDist;
				nearestRoad = road;
			} // if thisDist < minDist
		} // for nearRoads
		if (nearestRoad == null) {
			System.err.println("EnvironmentFactory.findRoadAtCoordinates(): ERROR: couldn't find a road at these coordinates:\n\t"+
					coord.toString());
		}
		return nearestRoad;
	}

	//	/* Gets the ID String associated with a given edge. Useful for linking RepastEdges with spatial objects (i.e. Roads). */
	//	public static String getIDFromEdge(RepastEdge<Junction> edge) {
	//		String id = "";
	//		try {
	//			id = (String) edgeIDs_KeyEdge.get(edge);
	//		} catch (Exception e) {
	//			Outputter.errorln("EnvironmentFactory: getIDDromEdge: Error, probably no id found for edge "+edge.toString());
	//		}
	//		return id;
	//	}
	//
	//	/* Get the edge with the given ID */
	//	public static RepastEdge<Junction> getEdgeFromID(String id) {
	//		RepastEdge<Junction> edge = null;
	//		try {
	//			edge = edgeIDs_KeyID.get(id);
	//		} catch (Exception e) {
	//			Outputter.errorln("EnvironmentFactory: getEdgeDromID: Error, probably no edge found for id "+id);
	//		}
	//		return edge;
	//
	//	}

	//	public static Road getRoadFromEdge(RepastEdge<Junction> edge) {
	//		Road road = null;
	//		try {
	//			road = edges_roads.get(edge);
	//		} catch (Exception e) {
	//			Outputter.errorln("EnvironmentFactory: getRoadFromEdge: Error, probably no road found for edge: "+
	//					"'"+edge.toString()+"'");
	//		}
	////		if (road==null) {
	////			Outputter.errorln("EnvironmentFactory error: couldn't find a road matching edge: "+edge.toString());
	////		}
	//		return road;
	//		
	//	}

	//	public static NetworkEdge<Junction> getEdgeFromRoad(Road road) {
	//		NetworkEdge<Junction> edge = null;
	//		try {
	//			edge = roads_edges.get(road);
	//		} catch (Exception e) {
	//			Outputter.errorln("EnvironmentFactory: getEdgeFromRoad: Error, probably no edge found for road: "+
	//					"'"+road.toString()+"'");
	//		}
	//		return edge;
	//		
	//	}


	//	/**
	//	 * Returns a Road given by an id. Will create a cache of all roads and ids for efficiency.
	//	 * @param id The of the road to search for
	//	 * @return The road with the given id or null if none could be found (will print an error
	//	 * if this happens).
	//	 */
	//	public static Road findRoadWithID(String id) {
	//		if (roadIdCache==null) {
	////			Outputter.debug("EnvironmentFactory.findRoadWithID(): creating cache of roads and identifiers",
	////					Outputter.DEBUG_TYPES.ROUTING);
	//			roadIdCache = new HashMap<String, Road>(GlobalVars.ROAD_ENVIRONMENT.getAllObjects(Road.class).size());
	//			for (Road road:GlobalVars.ROAD_ENVIRONMENT.getAllObjects(Road.class)) {
	//				roadIdCache.put(road.getIdentifier(), road);
	//			}
	//		}
	//		if (roadIdCache.get(id)==null) {
	//			Outputter.errorln("EnvironmentFactory.findRoadWithId, error: could not find a road with " +
	//					"identifier "+id);
	//			return null;
	//		}
	////		Outputter.debugln("...finished creating roads/identifiers cache", Outputter.DEBUG_TYPES.ROUTING);
	//		return roadIdCache.get(id);
	//	}

	//	/**
	//	 * A version of readShapeFile used to read in Buildings. This has to be different because, unlike
	//	 * other shapefiles, different types of Building subclass are created depending on the value
	//	 * of the 'type' parameter.
	//	 * 
	//	 * @param location The location of the shapefile containing the objects.
	//	 * @param geog A geography to add the objects to.
	//	 * @param context A context to add the objects to.
	//	 */
	//	private static void readBuildingsShapefile(String location, Geography<Building> geog, Context<Building> context) {
	//		File buildingFile = null;
	//		ShapefileLoader<Building> loader = null;
	//		try {
	//			buildingFile = new File(location);
	//			loader = new ShapefileLoader<Building>(Building.class, buildingFile.toURL(), geog, context);
	//			//ShapefileLoader<Building> tempLoader = new ShapefileLoader<Building>(Building.class, buildingFile.toURL(), geog, this);
	//		}
	//		catch (java.net.MalformedURLException e) {
	//			System.out.println("ContextCreator: malformed URL exception when reading junction shapefile. Check the 'junctionLoc' parameter is correct");
	//			e.printStackTrace();
	//		}
	//		while (loader.hasNext()) {
	//			loader.next();
	//		}
	//
	//		
	//	}

	/**
	 * Add a burgled house to the list of burgled houses so it's security can be reduced over time back to it's
	 * base level. If the house is already in the list of burgled houses nothing is done here.
	 */
	public static void addBurgledHouse(House house) {
		if (burgledHouses == null) {
			burgledHouses = new HashMap<House, Object>();
		}
		if (!burgledHouses.containsKey(house)) { // House not in the map, add it
			burgledHouses.put(house, null);
		}		
	}

	/**
	 * Iterate over all the burgled houses and reduce their level of security. This is called once per day
	 * by the ContextCreator.doHousekeeping(). 
	 */
	public static void deteriorateSecurtyValues() {
		if (burgledHouses == null)
			return;
		//		RunEnvironment.getInstance().pauseRun();
		//		System.out.print("Reducing security of houses (and pausing sim): \n\t");
		Iterator<House> i = burgledHouses.keySet().iterator();
		while (i.hasNext()) {
			House h = i.next();
			//			System.out.print(h.toString()+"("+h.getSecurity()+","+h.getBaseSecurity()+") : ");
			double newSec;
			if (ContextCreator.isTrue(GlobalVars.BURGLARY_BUILDING_PARAMS.HALVE_SECURITY_OVER_WEEK)) {
				// This variable was introduced in Base 7 scenario to stop security decreasing indefinitely
				// Security is reduced by half each week (so multiplied by 0.5^(1/7) each day).
				newSec = h.getSecurity() * Math.pow(0.5, 1.0/7.0);
			}
			else {
				newSec= h.getSecurity() - GlobalVars.BURGLARY_BUILDING_PARAMS.SECURITY_DETERIORATE;
			}
			if (newSec < h.getBaseSecurity()) { // House has reached base security, remove it from list.
				//				System.out.print("->base \n\t");
				h.setSecurity(h.getBaseSecurity());
				i.remove();
			}
			else { // Reduce security of the house
				//				System.out.print("->"+newSec+"\n\t");
				h.setSecurity(newSec);
			}
		}
	} // deteriorateSecurityValues()

	/**
	 * Convenience method to create a default community and return it. Used by the Null environment
	 */
	private static Community createDefaultCommunity() throws IllegalAccessException, InstantiationException {
		Community c = new Community(new Coord(1,1));
		c.setArea(1); // Need to set the area that the community covers
		Sociotype s = null;
		try {
			s = GlobalVars.SOCIOTYPE_CLASS.newInstance();
			s.initDefaultSociotype();
		} catch (IllegalAccessException e) {
			Outputter.errorln("EnvironmentFactory.createCommunities(NULL): "+e.getClass().getName()+"error "
					+"creating a new instance of the current Sociotype class: "+GlobalVars.SOCIOTYPE_CLASS.toString());
			throw e;
		} catch (InstantiationException e) {
			Outputter.errorln("EnvironmentFactory.createCommunities(NULL): "+e.getClass().getName()+"error "
					+"creating a new instance of the current Sociotype class: "+GlobalVars.SOCIOTYPE_CLASS.toString());
			throw e;
		}
		c.setSociotype(s);
		return c;
	}

}



