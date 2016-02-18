/* 
 * Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
 * George Mason University Mason University Licensed under the Academic
 * Free License version 3.0
 *
 * See the file "LICENSE" for more information
 *
 * $Id: GeomPlanarGraphDirectedEdge.java 675 2012-06-24 20:16:14Z mcoletti $
 */

package sim.util.geo;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.planargraph.DirectedEdge;
import com.vividsolutions.jts.planargraph.Node;


/**
 *
 */
public class GeomPlanarGraphDirectedEdge extends DirectedEdge
{

    public GeomPlanarGraphDirectedEdge(Node from, Node to,
            Coordinate directionPt,
            boolean edgeDirection)
    {
        super(from, to, directionPt, edgeDirection);
    }


}
