package burgdsim.environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;

import repast.simphony.context.Context;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.query.space.grid.VNQuery;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.StrictBorders;
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
public class SimphonyGridEnvironment<S, T extends S> extends AbstractEnvironment<T> implements Environment<T> {

	private Grid<T> projection;

	/**
	 * Will create the contexts and projections required to use a Repast Simphony Grid projection
	 * <p>
	 * The context is created using the EnvironmentFactory.createContext method becuase Contexts are are
	 * bit annoying and require their own separate class and entry in the model.score file. The projection
	 * is created here and given the same name as the environment but with Grid attached to the end (e.g.
	 * BurglarEnvironment becaomes BurglarEnvironmentGrid).
	 * 
	 * @param name The name of this environment.
	 * @param parentContext The parent context.
	 * @param type The type of Simphony projection to use, e.g. GRID or GIS.
	 */
	public SimphonyGridEnvironment(String name, Context<S> parentContext) {
		super();
		if (GlobalVars.ENVIRONMENT_TYPE != GlobalVars.ENVIRONMENT_TYPES.GRID) {
			/* Should never get here because the EnvironmentFactory will only create a SimphonyGridEnvironment
			 * if the ENVIRONMENT_TYPE is GRID */
			Outputter.errorln("SimphonyGridEnvironment() error: can only create GRID environments, not "+GlobalVars.ENVIRONMENT_TYPE.toString());
			return;
		}
		this.name = name;
		this.context = EnvironmentFactory.createContext(this.name);
		parentContext.addSubContext(this.context);
		GridFactory factory = GridFactoryFinder.createGridFactory(new HashMap<String, Object>());
		this.projection = factory.createGrid((this.name+"Grid"), this.context, 
				GridBuilderParameters.multiOccupancy2D(
						new SimpleGridAdder<T>(),
						new StrictBorders(),
						GlobalVars.GRID_PARAMS.XDIM,
						GlobalVars.GRID_PARAMS.YDIM));
		//		this.context.addProjection(this.projection); // XXXX
		Outputter.debugln("SimphGridEnv: created new environment with context/projection names: "+this.context.getId().toString()+"/"+this.projection.getName(),
				Outputter.DEBUG_TYPES.GENERAL);
	}
	@SuppressWarnings("unchecked")
	public String test() {
		String s = "";
		s+="SimGridEnv info: ";
		s+=" projections: ";
		for (Projection p:this.context.getProjections()) {
			s+= p.getName()+", ";
		}
		s+=", Context name: "+this.context.getId().toString()+"\n";
		s+="\tprojection name: "+this.context.getProjection(this.projection.getName()).getName();
		s+="\n\tprojection objects: ";
		for (T o:this.projection.getObjects()) {
			s+=o.toString()+"("+o.getClass().getName()+")"+",";
		}

		return s;
	}



	public Coord getCoords(T object) {
		return new Coord(this.projection.getLocation(object).getX(),this.projection.getLocation(object).getY()); 
	}

	/**
	 * Performs a Von Neuman query. Creats a temporary object which has the given coordinates, adds it to the
	 * context, searches around the object and then removes the temporary object from the context.
	 * 
	 * @return A list of objects that are within a specified Von Neumann distance
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	public <U extends T> List<U> getObjectsWithin(Coord coord, Class<U> clazz, double buffer, boolean returnObjects) throws Exception {
		List<U> list = new ArrayList<U>();
		U newObject = null;
		try {
			newObject = clazz.newInstance();
		} catch (InstantiationException e) {
			Outputter.errorln("SimphonyGridEnvironment.getObjectsWithin(): threw an error trying to instantiate " +
					"a temporary object: "+e.getClass()+". Printing stack trace.");
			Outputter.errorln(e.getStackTrace());
			throw e;
		} catch (IllegalAccessException e) {
			Outputter.errorln("SimphonyGridEnvironment.getObjectsWithin(): threw an error trying to instantiate " +
					"a temporary object: "+e.getClass()+". Printing stack trace.");
			Outputter.errorln(e.getStackTrace());
			throw e;
		}
		System.out.println("Created new object: "+newObject.toString());
		this.context.add(newObject);
		this.projection.moveTo(newObject, (int)coord.getX(), (int)coord.getY());
		System.out.println("Moved object "+newObject.toString()+" to "+this.projection.getLocation(newObject).toString());
		boolean foundObjects = false; int bufferMultiplier = 1;
		while (!foundObjects && bufferMultiplier < 6) {
			Iterable<T> it = new VNQuery<T>(
					this.projection, newObject, (int)buffer*bufferMultiplier, (int)buffer*bufferMultiplier).query();
			System.out.println("Found objects around "+coord.toString()+" :");
			for (T o:it) {
				if (clazz.isAssignableFrom(o.getClass())) {
					list.add((U) o);
					System.out.println(o.toString());
					foundObjects = true;
				}
			}
			if (!returnObjects) {
				break;
			}
			else if (!foundObjects) {
				bufferMultiplier++;
			}
		}
		if (returnObjects && list.size() == 0) {
			Outputter.errorln("SimphonyGridEnvironment.getObjectsWithin() warning: can't find any objects " +
					"within the buffer region even after making it "+bufferMultiplier+" times as large.");
		}

		System.out.println("Read to remove object. In context? "+this.context.contains(newObject));
		System.out.println("Location: "+this.projection.getLocation(newObject).toString());
		System.out.println("Removing object "+newObject.toString()+" from context: "+this.context.contains(newObject));

		this.context.remove(newObject);

		return list;
	}



	public synchronized void move(T object, Coord dest) {
		this.projection.moveTo(object, (int)dest.getX(), (int)dest.getY());

	}

	public synchronized void moveByVector(T object, double distance, double angleInRadians) {
		this.projection.moveByVector(object, distance, angleInRadians);		
	}
	public double getDistance(Coord c1, Coord c2) {
		return this.projection.getDistance(
				new GridPoint((int)c1.getX(), (int)c1.getY()),
				new GridPoint((int)c2.getX(), (int)c2.getY()));
	}

	public T getObjectAt(Class<? extends T> clazz, Coord c) {
		//		List<T> list = new ArrayList<T>();
		for (T obj:this.projection.getObjectsAt(new Double(c.getX()).intValue(), new Double(c.getY()).intValue())) {
			if (clazz.isAssignableFrom(obj.getClass())) {
				return obj;
				//				list.add(obj);
			}
		}
		//		if (list.size()>0)
		//			return list;
		return null; // Return null if no objects were found at the Coord

	}




}