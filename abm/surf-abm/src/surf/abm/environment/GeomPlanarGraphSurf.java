package surf.abm.environment;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateArrays;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.planargraph.DirectedEdge;
import com.vividsolutions.jts.planargraph.Edge;
import com.vividsolutions.jts.planargraph.Node;
import com.vividsolutions.jts.planargraph.PlanarGraph;
import java.util.Iterator;
import sim.field.geo.GeomVectorField;
import sim.field.network.Network;
import sim.util.Bag;
import sim.util.geo.GeomPlanarGraphDirectedEdge;
import sim.util.geo.MasonGeometry;
import surf.abm.exceptions.RoutingException;
import surf.abm.main.SurfABM;
import surf.abm.main.SurfGeometry;

/** A JTS PlanarGraph
 *
 * Planar graph useful for exploiting network topology.
 *
 * Important: This is a copy of the Mason GeomPlanarGraph class. All I have done is extend it slightly
 * so that the links in the graph (GeomPlanarGraphEdgeSurf objects) maintain a link to the underlying
 * road from which they were created.
 *
 * The orignal authors, who deserve all other credit, are: Mark Coletti, Keith Sullivan, Sean Luke, and
 * George Mason University Mason University Licensed under the Academic Free License version 3.0
 *
 * @author Nick Malleson
 */
public class GeomPlanarGraphSurf extends PlanarGraph
{

    public GeomPlanarGraphSurf()
    {
        super();
    }

    /** populate network with lines from a GeomVectorField
     *
     * @param field containing line segments
     *
     * Assumes that 'field' contains co-planar linear objects
     *
     */
    public void createFromGeomField(GeomVectorField field) throws RoutingException
    {

        Bag geometries = field.getGeometries();

        for (int i = 0; i < geometries.numObjs; i++)
        {
            if (((SurfGeometry) geometries.get(i)).geometry instanceof LineString)
            {
                addLineString((SurfGeometry)geometries.get(i));
            }
            else {
                throw new RoutingException("Error, this geometry is not a LineString");
            }
        }
    }

    /** Add the given line to the graph
     *
     * @param wrappedLine is MasonGeometry wrapping a JTS line
     *
     * @note Some code copied from JTS PolygonizeGraph.addEdge() and hacked
     * to fit
     */
    private void addLineString(SurfGeometry wrappedLine)
    {
        LineString line = (LineString) wrappedLine.geometry;

        if (line.isEmpty())
        {
            return;
        }

        Coordinate[] linePts = CoordinateArrays.removeRepeatedPoints(line.getCoordinates());

        if (linePts.length < 2)
        {
            return;
        }

        Coordinate startPt = linePts[0];
        Coordinate endPt = linePts[linePts.length - 1];

        Node nStart = getNode(startPt); // nodes added as necessary side-effect
        Node nEnd = getNode(endPt);

        GeomPlanarGraphEdgeSurf edge = new GeomPlanarGraphEdgeSurf(line, wrappedLine);

        GeomPlanarGraphDirectedEdge de0 = new GeomPlanarGraphDirectedEdge(nStart, nEnd, linePts[1], true);
        GeomPlanarGraphDirectedEdge de1 = new GeomPlanarGraphDirectedEdge(nEnd, nStart, linePts[linePts.length - 2], false);

        edge.setDirectedEdges(de0, de1);

        // edge.setAttributes(wrappedLine.getAttributes());

        add(edge);
    }

    /** get the node corresponding to the coordinate
     *
     * @param startPt
     * @return graph node associated with point
     *
     * Will create a new Node if one does not exist.
     *
     * @note Some code copied from JTS PolygonizeGraph.getNode() and hacked to fit
     */
    private Node getNode(Coordinate pt)
    {
        Node node = findNode(pt);
        if (node == null)
        {
            node = new Node(pt);
            // ensure node is only added once to graph
            add(node);
        }
        return node;
    }

    /** Create a MASON Network from this planar graph
     *
     * XXX Unfortunately we need this since JTS planar graphs do not support
     * shortest distance and other common graph traversals.
     */
    public Network getNetwork()
    {
        Network network = new Network(false); // false == not directed

        for (Iterator it = dirEdges.iterator(); it.hasNext();)
        {
            Object object = it.next();
            GeomPlanarGraphDirectedEdge edge = (GeomPlanarGraphDirectedEdge) object;
            network.addEdge(edge.getFromNode(), edge.getToNode(), edge);
        }

        return network;
    }

}
