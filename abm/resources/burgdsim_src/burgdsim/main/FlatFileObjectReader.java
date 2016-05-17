package burgdsim.main;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Provides methods to read flat files.
 * 
 * @author Nick Malleson
 *
 */

// TODO Incorporate this class into the DataAccess framework. THen could store in database instead.
public class FlatFileObjectReader {

	/**
	 * Reads a flat file and creates objects from the given values. Assumes that the first line
	 * contains the names of the different object variables and will ignore lines starting '#'
	 * (comments). 
	 * <p>
	 * This was adapted from the Repast Simphony class ShapefileLoader.java
	 * (repast.simphony.space.gis.ShapefileLoader), 
	 * 
	 * @param <T> The type of objects to create from the file.
	 * @param clazz The class of the objects to create.
	 * @param fileLocation The location of the file
	 * @param delimiter The optional delimiter, if null it is assumed to be a comma (',').
	 * @return A list of objects created with the values in the input file  
	 * @throws IOException 
	 * @throws IntrospectionException 
	 * @throws InstantiationException 
	 * @throws IllegalAccessException 
	 * @throws InvocationTargetException 
	 */
	public static <T> List<T> readSociotypeObjects(Class<T> clazz, String fileLocation, Character delim) throws IOException, IntrospectionException, InstantiationException, IllegalAccessException, InvocationTargetException {

		if (delim==null) 
			delim = ',';

		List<T> objectList = new ArrayList<T>();
		File file = new File(fileLocation);
		BufferedReader br = null;;
		try {
			br = new BufferedReader(new FileReader(file));

			// Use a map to store object methods with their names
			Map<String, Method> attributeMethodMap = new HashMap<String, Method>();
			// Must also remember the order that the method appear in the flat file so that, as iterating
			// through values in a line, we know which method to call
			List<String> methodFilePosition = new ArrayList<String>();

			// Work out which methods the input class has, these will be matched to variables in the file
			BeanInfo info = Introspector.getBeanInfo(clazz, Object.class);
			Map<String, Method> methodMap = new HashMap<String, Method>();
			PropertyDescriptor[] pds = info.getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				if (pd.getWriteMethod() != null) {
					methodMap.put(pd.getName().toLowerCase(), pd.getWriteMethod());
				}
			}

			// Read first line and try to match object methods with headers in the flat file
			String line = br.readLine();
			String[] tokens = line.split(delim.toString());
			for (String name:tokens) {
				name = name.replaceAll("\"","").trim(); // Remove quotes from around variables (common in csv files)
				Method method = methodMap.get(name.toLowerCase());
				if (method == null) method = methodMap.get(name.replace("_", "").toLowerCase());
				if (method != null) {
					attributeMethodMap.put(name, method);
					methodFilePosition.add(name);
				}
				else {
					// If no method was found to match the column name store null in the method list
					methodFilePosition.add(null); 
				}
			}

			// Read the remaining lines creating an object from values stored on each line
			line = br.readLine();
			Outputter.debugln("FlatFileObjectReader.readSociotypeObjects): creating new objects.", Outputter.DEBUG_TYPES.DATA_ACCESS);
			while (line!=null) {
				if (line.charAt(0)!='#') { // ignore comments
					// Store the vales in this line
					String[] values = line.split(delim.toString());
					// Create a new object
					T obj = (T) clazz.newInstance();
					// Fill the agent with values from the line
					Outputter.debug("\t new '"+obj.getClass().getName()+"' object: ", Outputter.DEBUG_TYPES.DATA_ACCESS);
					String debugString=""; // For debugging
					for (int i=0; i<values.length; i++) {
						Method write = attributeMethodMap.get(methodFilePosition.get(i));
						// Find the method which can be used to write the value in this position in the file
						// If this is null then there was no associated method in the object which can be
						// used to write values in this collumn
						if (write != null) { 
							// Use the agent's write method to write the value (if the value and method are compatable)
							writeValue(obj, write, values[i]);
							debugString+="("+write.getName().toString()+": "+values[i].toString()+"), ";
						}
						
					} // for values in line
					Outputter.debugln(debugString, Outputter.DEBUG_TYPES.DATA_ACCESS);
					objectList.add(obj);
				} // if not coment line
				line = br.readLine();				
			} // while has lines
			Outputter.debugln("\nFlatFileObjectReader finished reading file", Outputter.DEBUG_TYPES.DATA_ACCESS);
			br.close();
		} catch (FileNotFoundException e) {
			Outputter.errorln(FlatFileObjectReader.class.toString()+".readObjects() error, couldn't find file: "+
					file.getAbsoluteFile());
			Outputter.errorln(e.getStackTrace());
			throw e;
		} catch (IOException e) {
			Outputter.errorln(FlatFileObjectReader.class.toString()+".readObjects() error reading file: "+
					file.getAbsoluteFile());
			Outputter.errorln(e.getStackTrace());
			throw e;
		} catch (IntrospectionException e) {
			Outputter.errorln(FlatFileObjectReader.class.toString()+".readObjects() threw an IntrospectionException");
			Outputter.errorln(e.getStackTrace());
			throw e;
		} catch (InstantiationException e) {
			Outputter.errorln(FlatFileObjectReader.class.toString()+".readObjects() had an error trying to instantiate " +
					"an agent of type: "+clazz.getName().toString());
			Outputter.errorln(e.getStackTrace());
			throw e;
		} catch (IllegalAccessException e) {
			Outputter.errorln(FlatFileObjectReader.class.toString()+".readObjects() threw an illegal access exception" +
					"for agent of type: "+clazz.getName().toString());
			Outputter.errorln(e.getStackTrace());
			throw e;
		} catch (IllegalArgumentException e) {
			Outputter.errorln(FlatFileObjectReader.class.toString()+".readObjects() threw an IllegalArgumentException" +
					"for agent of type: "+clazz.getName().toString());
			Outputter.errorln(e.getStackTrace());
			throw e;
		} catch (InvocationTargetException e) {
			Outputter.errorln(FlatFileObjectReader.class.toString()+".readObjects() threw an InvocationTargetException" +
					"for agent of type: "+clazz.getName().toString());
			Outputter.errorln(e.getStackTrace());
			throw e;
		} 
		return objectList;
	}


	/**
	 * Use the given method to write the given value to the object. Will find out whether the value is an
	 * Integer, Double or String and check that the type matches that of the write method. Doesn't distinguish
	 * between different types of number so long and short are both treated as Integers and floats are treated
	 * as Doubles.
	 * @param <T>
	 * @param obj
	 * @param write
	 * @param value
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private static <T> void writeValue(T obj, Method write, String value) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		
		value = value.trim(); // Get rid of whitespace
		// Now try to write the value. It is read in as a String from the flat file so need to
		// work out what it's actual type is and then try to use the supplied method to write it
		// to the supplied object.

		if (value.charAt(0)=='"' && value.charAt(value.length()-1)=='"') {
			// It's surrounded by quotes so probably a string.
			value = value.replaceAll("\"", "");
			if (isCompatible(write, String.class)) {
				write.invoke(obj, value);
				return;
			}
		} // if surrounded by quotes
		else if (value.contains(".")) { // Probably a Double because there is a decimal point in it
			double val = Double.parseDouble(value);
			if (isCompatible(write, Double.class)) {
				write.invoke(obj, val);
				return;
			}
		}
		else { // Otherwise it must be an int
			int val = Integer.parseInt(value);
			if (isCompatible(write, Integer.class)) {
				write.invoke(obj, val);
				return;
			}

		}
	}
	
	@SuppressWarnings("unchecked")
	private static boolean isCompatible(Method method, Class attributeType) {

		if (method.getParameterTypes()[0].equals(attributeType)) {
			return true;
		}
		Class clazz = primToObject.get(method.getParameterTypes()[0]);
		if (clazz != null) {
			return true; 
			// XXXX Returning true here means that if the argument is a long but the function wants an
			// int (for example) it will still work, probably unstable though. I don't really understand
			// how this works, most is copied from ShapefileLoaders
//			return clazz.equals(attributeType);
		}
		return false;
	}

	// Map primitive types to their class equivalents, used to see if an input primitive is compatible with
	// a write method
	@SuppressWarnings("unchecked")
	private static Map<Class, Class> primToObject = new HashMap<Class, Class>();

	static {
		primToObject.put(int.class, Integer.class);
		primToObject.put(long.class, Long.class);
		primToObject.put(double.class, Double.class);
		primToObject.put(float.class, Float.class);
		primToObject.put(boolean.class, Boolean.class);
		primToObject.put(byte.class, Byte.class);
		primToObject.put(char.class, Character.class);
	}

}
