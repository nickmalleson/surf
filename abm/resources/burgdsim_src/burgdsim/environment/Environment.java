package burgdsim.environment;

import java.util.List;

/**
 * Represents the model Environment.
 * <p>
 * This class is used so that the underlying model does not need to know anything specific about the
 * environment which is being used. It has two main functions:<br>
 * 1. It can be subclassed to provide a personalised environment (e.g. NullEnvironment).<br>
 * 2. The SimphonyEnvironment subclass acts as a wrapper for Repast projections so that the model 
 * doesn't need to know about the specific projection which is being used. NOTE: can only be used to
 * wrap Geography, Grid or ContinuousSpace projections, not Networks.<br>
 * 
 * @author Nick Malleson
 */
public interface Environment <T> {
	
	/**
	 * Add an object to the environment
	 * @param object
	 */
	void add(T object);
	/**
	 * Get the objects which surround the given coordinate within the given distance.
	 * @param c The coordinate to search around.
	 * @param clazz The class of objects to search for.
	 * @param buffer The size of the buffer (I *think* that this is in meters).
	 * @param returnObjects If true the function will increase the buffer size to look for more objects if
	 * none are found initially. It will exit after 5 increases though, returning an empty list if still no 
	 * objects were found.
	 * @return
	 * @throws Exception 
	 */
	<U extends T> List<U> getObjectsWithin(Coord c, Class<U> clazz, double buffer, boolean returnObjects) throws Exception;
	
	void move(T object, Coord dest);
	void moveByVector(T object, double distance, double angleInRadians);
	
	/**
	 * Get all objects from the environment. Must specify the type of the object. 
	 * @param clazz The object class, e.g. Burglar.class
	 * @return A List of all object in the environment of the type T
	 */
	<U extends T> List<U> getAllObjects(Class<U> clazz);
	
	/**
	 * Gets a random object from the environment
	 * @param clazz The class type to return
	 * @return 
	 */
	 T getRandomObject(Class<? extends T> clazz);
	
	/**
	 * Get the (x,y) coordinates of the centroid of the object
	 * @return an array of Coord objects
	 */
	Coord getCoords(T object);
	
	/**
	 * Get the distance between two cordinates.
	 * @param c1
	 * @param c2
	 * @return The distance between Coors c1 and c2
	 */
	double getDistance(Coord c1, Coord c2);

	
	/**
	 * Get the object of the specified class at the specified coordinates. Return null if there is
	 * no object at the coordinates.
	 * <p>
	 * If there is more than one object of the specified class at the given coordinate
	 * only one of the objects will be returned. There is no warning if this happens.
	 * <p>
	 * This should be used sparingly because it is a very expensive operation, definitely not something
	 * that each agent should have to do every turn! At the moment it's only done when a Burglar needs
	 * to create a new Burglary action.
	 * 
	 * @param clazz The class of the object to return.
	 * @param c The coordinates to search at.
	 * @return the object at the given coordinates or null if there is nothing there.
	 */
	T getObjectAt(Class<? extends T> clazz, Coord c);
	
	/**
	 * Remove the given agent from the environment
	 * @param obj
	 * @return True if the agent was removed, false if the agent doesn't exist in this environment.
	 */
	boolean remove(T obj);
//	
//	/**
//	 * Get's the nearest object(s) to the coordainte. If multiple objects are the same distance
//	 * from the coordinate they will all be returned. The superclass has to be passed as well 
//	 * because all objects close to the Coord are cached, the only way to do this is pass
//	 * the superclass to the Context.getObjects(clazz) method. (I can't work out how to find out
//	 * what the superclass is within the method, e.g. something like Context.getObjects(?.getSuperclass()) ). 
//	 * @param clazz The class of object to return
//	 * @param superclass The super class of clazz (might be different).
//	 * @param c The coordinate to search from
//	 * @return A list of the nearest object(s)
//	 */
//	List<? extends T> getNearestObjects(Class<? extends T> clazz, Coord c);
}
