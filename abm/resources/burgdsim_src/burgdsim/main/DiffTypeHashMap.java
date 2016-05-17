package burgdsim.main;
import java.util.HashMap;
import java.util.List;

/**
 * An extension of HashMap which allows for explicitly declaring which type of object
 * should be returned. Used in BurgdSim model so that an agent's cognitive map can store
 * Buildings of different types but only return those the agent might be interested in
 * (such as House objects for example).
 * @author nick
 *
 * @param <K> The key
 * @param <V> The value
 */
public class DiffTypeHashMap<K, V> extends HashMap<Object, Object> {

	private static final long serialVersionUID = 1L; // ? (Eclipse wants this).


	/**
	 * Get all objects in the map, allowing a the type of objects to
	 * be returned to be specified.
	 * 
	 * @param type The type of object which should be returned
	 * @return all List of all objects of type <? extends V> 
	 */
	public List<V> getAll(Class<? extends V> type) {

		return null;
		
	}
	
	

}
