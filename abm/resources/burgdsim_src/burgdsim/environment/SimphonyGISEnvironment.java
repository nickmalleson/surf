package burgdsim.environment;

import java.util.ArrayList;
import java.util.List;

import org.geotools.referencing.GeodeticCalculator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;

import repast.simphony.context.Context;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import repast.simphony.space.gis.SimpleAdder;
import repast.simphony.space.projection.Projection;

/**
 * Wrapper for repast spatial projections so that underlying model doens't need to know about the
 * specific environment which is being used.<p> 
 * This class also deals with the contexts required for the projections. Again, the basic
 * model classes do not need to know anything about contexts, they just need to be able to move around
 * and query the their Environment.
 * @author Nick Malleson
 * @param <T> The type of objects this Environment will store
 * @param <S> The type of objects put in this Environment's parentContext (e.g. if T is 'House'
 * then S is 'Building')
 *
 */
public class SimphonyGISEnvironment<S, T extends S> extends AbstractEnvironment<T> implements Environment<T> {
	
	
	private Geography<T> projection; // The projection actually being used (a Geography or a Grid).
	private GeometryFactory geomFac;	// Used to create geometries
	
	// To do with a geotools bug...
	private static boolean printedeotoolsError = false;

	/**
	 * Will create the contexts and projections required to use a Repast Simphony GIS projection.
	 * <p>
	 * The context is created using the EnvironmentFactory.createContext method because Contexts are are
	 * bit annoying and require their own separate class and entry in the model.score file. The projection
	 * is created here and given the same name as the environment but with "Geography" on the end (E.g. 
	 * BurglarEnvironment becomes BurglarEnvironmentGeogrpahy).
	 * 
	 * @param name The name of this environment.
	 * @param parentContext The parent context.
	 */
	public SimphonyGISEnvironment(String name, Context<S> parentContext) {
		super();
		this.geomFac = new GeometryFactory();
		this.name = name;
		if (GlobalVars.ENVIRONMENT_TYPE != GlobalVars.ENVIRONMENT_TYPES.GIS) {
			/* Should never get here because the EnvironmentFactory will only create a SimphonyGridEnvironment
			 * if the ENVIRONMENT_TYPE is GRID */
			Outputter.errorln("SimphonySpatialEnvironment() error: can only create GIS or GRID spatial environments, not "+GlobalVars.ENVIRONMENT_TYPE.toString());
			return;
		}
		// Create the new projection and context here
		this.context = EnvironmentFactory.createContext(this.name);
		parentContext.addSubContext(this.context);
		GeographyParameters<T> geoParams = new GeographyParameters<T>(new SimpleAdder<T>());
		try {
			this.projection = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
					(name+"Geography"), this.context, geoParams);
		} catch (Exception e) {
			// TODO Work out why this "org.opengis.referencing.FactoryException: Failed to connect to the EPSG database."
			// error is sometimes thrown
			if (!SimphonyGISEnvironment.printedeotoolsError) {
				Outputter.errorln("SimphonyGISEnvironment caught an Exception trying to create a projection, this " +
						"is caused by a problem with GeoTools that I don't really understand. Not printing this " +
						"error again. Message and stack trace: "+e.getMessage());
				Outputter.errorln(e.getStackTrace());
				SimphonyGISEnvironment.printedeotoolsError = true;
			}
		}

		Outputter.debugln("SimphGISEnv: created new environment with context/projection names: "+this.context.getId().toString()+"/"+this.projection.getName(),
				Outputter.DEBUG_TYPES.GENERAL);

	}

	@SuppressWarnings("unchecked")
	public String test() {
		String s = "";
		s+="SimGISEnv info: ";
		s+=" projections: ";
		for (Projection p:this.context.getProjections()) {
			s+= p.getName()+", ";
		}
		s+=", Context name: "+this.context.getId().toString()+"\n";
		s+="\tprojection name: "+this.context.getProjection(this.projection.getName()).getName();
		s+="\n\tprojection objects: ";
		for (T o:this.projection.getAllObjects()) {
			s+=o.toString()+"("+o.getClass().getName()+")"+",";
		}
		
		return s;
	}


	public Coord getCoords(T object) {
		return new Coord(this.projection.getGeometry(object).getCoordinate());
	}

	@SuppressWarnings("unchecked")
	public <U extends T> List<U> getObjectsWithin(Coord c, Class<U> clazz, double bufferDist, boolean returnObjects) {
		List<U> list = new ArrayList<U>();
		boolean foundObjects = false; int bufferMultiplier = 1;
		// Loop until some objects have been found or the buffer has been increased 5 times.
		while (!foundObjects && bufferMultiplier < 6) {
			Iterable<T> it = this.projection.getObjectsWithin(
				new GeometryFactory().createPoint(c.toCoordinate()).buffer(bufferDist*bufferMultiplier).getEnvelopeInternal());
			for (T o:it) {
				if (clazz.isAssignableFrom(o.getClass())) {
					list.add((U) o);
					foundObjects = true;
				}
			}
			if (!returnObjects) { // If don't care about returning objects just break out of the while loop
				break;
			}
			else if (!foundObjects) { // Haven't found anything, search a larger area.
				bufferMultiplier++;
			}
		}
		if (returnObjects && list.size() == 0) {
			Outputter.errorln("SimphonyGridEnvironment.getObjectsWithin() warning: can't find any objects " +
					"within the buffer region even after making it "+bufferMultiplier+" times as large.");
		}
		return list;
	}

	public synchronized void move(T object, Coord dest) {
		this.projection.move(object, geomFac.createPoint(dest.toCoordinate()));
	}

	public synchronized void moveByVector(T object, double distance, double angleInRadians) {
//		if (angleInRadians > Math.PI ||angleInRadians < -Math.PI ) {
//			Outputter.errorln("SimphonyGISEnvironent.moveByVector() error, angle must be in range " +
//					"-PI -> PI, but it is: "+angleInRadians);
//		}
//		else 
			this.projection.moveByVector(object, distance, angleInRadians);
	}
	
	/**
	 * Calculate the distance (in meters) between two Coordinates, using the coordinate reference system
	 * that this Environment's projection is using. Similar function available in GISRoute  
	 * @param c1
	 * @param c2
	 * @return The distance between Coords c1 and c2.
	 */
	public double getDistance (Coord c1, Coord c2) {
			
		GeodeticCalculator calculator = new GeodeticCalculator(this.projection.getCRS());
		Coordinate co1 = c1.toCoordinate();
		Coordinate co2 = c2.toCoordinate();
		calculator.setStartingGeographicPoint(co1.x, co1.y);
		calculator.setDestinationGeographicPoint(co2.x, co2.y);
		return calculator.getOrthodromicDistance();
	}
	

	
	/**
	 * Returns the context. This function should only be used when explicit access to the context is
	 * required. This only occurs in {@link EnvironmentFactory} because to create the GIS environments it is
	 * necessary to use ShapefileLoader which requires a pointer to context to load agents into. 
	 * @return the context used by this SimphonyGISEnvironment
	 * @see EnvironmentFactory
	 */
	public Context<T> getGISContext() {
		return this.context;
	}

	/**
	 * Returns the projection. This function should only be used when explicit access to the projection is
	 * required. This only occurs in {@link EnvironmentFactory} because to create the GIS environments it is
	 * necessary to use ShapefileLoader which requires a pointer to the projection to move the agents to their
	 * correct starting position 
	 * @return the Geography projection used by this SimphonyGISEnvironment
	 * @see EnvironmentFactory
	 */
	public Geography<T> getGISProjection() {
		return this.projection;
	}


	/**
	 * Gets the object of the specified class at the specified location or returns null if none was found.
	 * This should be used sparingly because it is a very expensive operation, it involves iterating over
	 * every object in the context and finding one at the given coordinates, definitely not something
	 * that each agent should have to do every turn! At the moment it's only done when a BurglarMemory
	 * object is created (which happens only when new Burglars are created).
	 */
	public T getObjectAt(Class<? extends T> clazz, Coord c) {
		
		// Iterate over all objects in the geography, returning the first one which has a geometry that
		// contains the given coordinate.
		Point p = geomFac.createPoint(c.toCoordinate());
		Geometry g;
		for (T o:this.context.getObjects(clazz)) {
			g = this.projection.getGeometry(o);
			if (g.contains(p))
				return o;
		}
//		Outputter.errorln("SimphonyGISEnvironment: getObjectAt(): no object of type "+clazz.toString()+
//				"found at "+c.toString());
		return null;
	}

//
//	/**
//	 * Return the closest object(s) to the given coordinate.
//	 * @param class The class of object to return
//	 * @param c the coordinate to search around
//	 * @return a list of the nearest coordinates (very unlikely that in a GIS environment there
//	 * will be two objects the same distance from the coordinate so probably only one item in the list).
//	 */
//	public List<? extends T> getNearestObjects(Class<? extends T> clazz, Coord c) {
//		System.err.println("GIS environment: not implemented getObjectAt() yet!");
//		
//		return null;
//	}
	
	


}
