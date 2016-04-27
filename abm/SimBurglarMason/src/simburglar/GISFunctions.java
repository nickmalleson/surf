/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simburglar;

import org.apache.log4j.Logger;
import sim.field.geo.GeomVectorField;
import sim.util.Bag;
import sim.util.geo.MasonGeometry;
import simburglar.exceptions.RoutingException;

/**
 *
 * @author Nick Malleson
 */
public class GISFunctions {

    static Logger LOG = Logger.getLogger(GISFunctions.class);
    // Used to define radius to search around when looking for objects
    private static volatile double MIN_SEARCH_RADIUS_DENOMINATOR = 1000000.0;

    /**
     * Find the nearest object to the given input coordinate.
     *
     */
    public static MasonGeometry findNearestObject(MasonGeometry centre,
            GeomVectorField geom, SimBurglar state) throws RoutingException {
        
        double radius = state.MBR.getArea() / GISFunctions.MIN_SEARCH_RADIUS_DENOMINATOR;
        Bag closeObjects;
        while (radius < state.MBR.getArea()) {
            closeObjects = geom.getObjectsWithinDistance(centre, radius);
            if (closeObjects.isEmpty()) {
                GISFunctions.MIN_SEARCH_RADIUS_DENOMINATOR *= 0.1;
                radius = state.MBR.getArea() / GISFunctions.MIN_SEARCH_RADIUS_DENOMINATOR;
                LOG.warn("Increasing search radius to " + radius + ". This is very inefficient if it happens regularly.");
            }
            else {
                double minDist = Double.MAX_VALUE;
                double dist ;
                MasonGeometry mg;
                MasonGeometry closest = null;
                for (Object o : closeObjects) {
                    mg = (MasonGeometry) o;
                    if (mg.equals(centre)) {
                        continue; // Ignore the actual object that we're looking around.
                    }
                    dist = centre.geometry.distance(mg.geometry);
                    if (dist < minDist) {
                        closest = mg;
                        minDist = dist;
                    }
                }
                assert closest != null;
                return closest;
            }
        }
        throw new RoutingException("Could not find any objects near to " + centre.toString());
    }

}
