
package simburglar;

/**
 * Place to record any String values that the model relies on, e.g. data column
 * names. Storing them here makes it easier to keep track and change later.
 *
 * @author Nick Malleson
 */
public enum FIELDS {

    // Fields for GIS data files
    BUILDINGS_ID("ID"),
    BUILDINGS_NAME("NAME"),
    BUILDING_FLOORS("FLOORS"),
    
    ;
    
    
    
    private String repr; // The string representation of this field
    FIELDS(String str) {
        this.repr = str;
    }

    @Override
    public String toString() {
        return this.repr;
    }
    
    
    
    
    
}
