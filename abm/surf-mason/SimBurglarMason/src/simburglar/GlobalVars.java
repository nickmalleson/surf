package simburglar;

import java.io.Serializable;
import java.util.Properties;
import sim.field.geo.GeomVectorField;

/**
 * Convenience class to hold global variables.
 * 
 * <P>This is primarily a way of simplifying the main <code>SimBurglar</code>
 * class. It holds variables that aren't directly related to the model itself
 * (e.g. locations of data files, constants, strings, etc.</P>
 *
 * <P>Follows singleton design pattern - use <code>GlobalVars.getInstance()</code>
 * to get hold of an object.</P>
 *
 * @author Nick Malleson
 */
public class GlobalVars implements Serializable{

    private static GlobalVars instance = null;
    
    /**
     * Set, on initialisation, to be the main SimBurglarMason directory,
     * regardless of the working directory when the model is started.
     */
    public String ROOT_DIR = null;
    /**
     * Model-specific propeties (set on initialisation)
     */
    public Properties properties;
   
    
    
    
    /**
     * Get a property. Convenience for <code>GlobalVars.getInstance().properties.get(name);</code>
     */
    public static String getProperty(String name) {
        return GlobalVars.getInstance().properties.getProperty(name);
    }
    
    
    protected GlobalVars() {
        // Exists only to defeat instantiation.
    }

    public static synchronized GlobalVars getInstance() {
        if (instance == null) {
            instance = new GlobalVars();
        }
        return instance;
    }

}
