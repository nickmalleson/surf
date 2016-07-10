package surf.projects.breezeroutes;

import com.graphhopper.GraphHopper;
import com.graphhopper.matching.*;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.GPXEntry;
import com.graphhopper.util.PointList;

import java.util.List;

/**
 * Do some map matching between GPX files and OSM roads.
 *
 * Adapted from code snippet at: https://github.com/graphhopper/map-matching
 */
public class MapMatcher {

    private static  MapMatching mapMatching;
    private static GraphHopperStorage graph;

    private static void init() {
        // import OpenStreetMap data
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile("./map-data/massachusetts-latest.osm.pbf");
        hopper.setGraphHopperLocation("./out/massachusetts");
        CarFlagEncoder encoder = new CarFlagEncoder();
        hopper.setEncodingManager(new EncodingManager(encoder));
        hopper.getCHFactoryDecorator().setEnabled(false);
        hopper.importOrLoad();

        // create MapMatching object, can and should be shared across threads
        graph = hopper.getGraphHopperStorage();
        LocationIndexMatch locationIndex = new LocationIndexMatch(graph,
                (LocationIndexTree) hopper.getLocationIndex());
        mapMatching = new MapMatching(graph, locationIndex, encoder);
    }

    private static void match() {
        // do the actual matching, get the GPX entries from a file or via stream
        List<GPXEntry> inputGPXEntries = new GPXFile().doImport("./traces/trace1.gpx").getEntries();
        MatchResult mr = mapMatching.doWork(inputGPXEntries);
        double dist = mr.getMatchLength();
        System.out.println("Finished matching. Length: "+dist);

        /* XXXX HERE - At this point I can get:
                - the path
                - the points associated with it
                - the edges
          Next:
                - Find a way to write out or map (with GraphHopper?) the path or edges (for validation)
                - Use GraphHopper Routing in earnest to compare this route with the shortest
         */

        Path path = mapMatching.calcPath(mr);
        System.out.println("PATH:"+path.toDetailsString());

        PointList pl = path.calcPoints();
        System.out.println("PointList: "+pl.toString());

        List<EdgeIteratorState> edges = path.calcEdges();
        for (EdgeIteratorState e:edges) {
            System.out.println("\tEdgeID: "+e.getEdge());
            System.out.println("\t\tEdgeIteratorState:" + e.toString());
            System.out.println("\t\tWayGeometry: "+e.fetchWayGeometry(3).toString());

        }


        // XXXX THEN - need to take the start and end points and use GraphHopper to do a route


        // return GraphHopper edges with all associated GPX entries
        /*List<EdgeMatch> matches = mr.getEdgeMatches();

        for (EdgeMatch m: matches) {
            System.out.println("\tEdgeMatch:" + m.toString());
            EdgeIteratorState e = m.getEdgeState();
            PointList p = e.fetchWayGeometry(0);

            int edgeID = e.getEdge();
            System.out.println("\tEdgeID:"+edgeID+" Way Geom: "+p.toString());

        }*/
    }


    public static void main(String[] args ) {

        init();
        match();

    }
}
