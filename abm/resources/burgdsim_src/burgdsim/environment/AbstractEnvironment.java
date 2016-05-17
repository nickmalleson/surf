package burgdsim.environment;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.context.Context;

/**
 * Abstract class which implements functions common to SimphonySpatialEnvironment and SimphonyGridEnvionment
 * so they don't have to be implemented twice
 * @author Nick Malleson
 * 
 * @param <T> The type of object the Environment will store.
 */
public abstract class AbstractEnvironment<T> implements Environment<T> {

	protected String name;
	protected Context<T> context; 	// The context
	
	public void add(T object) {
		this.context.add(object);
	}

	@SuppressWarnings("unchecked")
	public <U extends T> List<U> getAllObjects(Class<U> clazzU) {
		List<U> list = new ArrayList<U>();
		for (T oT:context.getObjects(clazzU)) {
			if (clazzU.isAssignableFrom(oT.getClass())) {
//			if (clazzU.isAssignableFrom(oT.getClass())) { // This will not return anything if Building.class passed
				list.add((U)oT); // This should never fail because of isAssignableFrom line
			}
		}
		return list;	
	}

	public T getRandomObject(Class<? extends T> c) {
		
//		Iterator<T> i= this.context.getObjects(c).iterator();
//		ArrayList<T> l = new ArrayList<T>();
//		while (i.hasNext()) {
//			T next = i.next();
//			System.out.println(next.toString());
//			l.add(next);
//		}
//		System.out.println("Abstract ENV: "+l.size());
		
		T object = null;
		object = this.context.getRandomObjects(c, 1).iterator().next(); 
		
		return object;
	}
	
	public boolean remove(T obj) {
		return this.context.remove(obj);
	}


}
