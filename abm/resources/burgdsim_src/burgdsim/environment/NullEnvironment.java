package burgdsim.environment;

import java.util.List;

import burgdsim.main.Outputter;

import repast.simphony.context.Context;

/**
 * Represents the an environment in which every aspect can be controlled manually. Makes it easy to 
 * properly test agent rules (by, for example, varying the amount of time it takes to travel to somewhere
 * without actually moving the agent).
 * @author nick
 *
 * @param <S> The parent class of objects which this Environment will hold.
 * @param <T> The type of objects which this environment will store.
 */

public class NullEnvironment <S, T extends S> extends AbstractEnvironment <T> implements Environment<T> {
	
	//private List<T> objects;	// A list of objects in the environment
	private Coord coord;		// A coord object which can be used to represent every object in the environment

	
	/**
	 * Creates a null environment. Although this doesn't use a Simphony projection, it is still necessary
	 * to create a Context so that Simphony knows about the agents in the model so it can graph them and
	 * schedule their methods. 
	 * @param name the name of the environment
	 * @param parentContext The parent context
	 */
	public NullEnvironment(String name, Context<S> parentContext) {
		
		this.context = EnvironmentFactory.<T>createContext(name);
		
		
//		this.context = new NullContext<T>(name);
//		this.context = new DefaultContext<T>(name);
		parentContext.addSubContext(this.context);
//		System.out.println("Added subcontext: *"+this.context.toString()+"* to parent context: *"+parentContext.toString()+
//				"* with name: "+name);
//		this.objects = new ArrayList<T>();
		this.coord = new Coord(-1,-1);
	}



	/**
	 * Check object exists in environment but don't do anything.
	 */
	public void move(T object, Coord dest) {
		if (!this.context.contains(object))
			Outputter.errorln("NullEnvironment: move(): error. Object ("+object.getClass().getName()+")not found in environment: "+object.toString());
	}
	
	/**
	 * Check object exists in environment but don't do anything.
	 */
	public void moveByVector(T object, double distance, double angleInRadians) {
		if (!this.context.contains(object))
			Outputter.errorln("NullEnvironment: moveByVector(): error. Object ("+object.getClass().getName()+")not found in environment: "+object.toString());
		
	}

	/**
	 * Will return same coordinates (-1-1) for every object
	 */
	public Coord getCoords(T object) {
		return this.coord;
	}



	/**
	 * Just return all objects
	 */
	public List<? extends T> getNearestObjects(Class<? extends T> clazz, Coord c) {
		return this.getAllObjects(clazz);
	}


	/**
	 * Just return all objects
	 */
	public <U extends T> List<U> getObjectsWithin(Coord c, Class<U> clazz, double buffer, boolean returnObjects) {
		return this.getAllObjects(clazz);
	}



	public double getDistance(Coord c1, Coord c2) {
		return 1;
	}


	/**
	 * Return a random object of the given class
	 */
	public T getObjectAt(Class<? extends T> clazz, Coord c) {
//		List<T> list = new ArrayList<T>();
//		list.add();
		return this.context.getRandomObject();
	}

}
