package surf.projects.breezeroutes;

import com.graphhopper.GraphHopper;
import com.graphhopper.matching.*;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.ui.MiniGraphUI;
import com.graphhopper.util.*;


import java.util.List;

/**
 * Do some map matching between GPX files and OSM roads.
 *
 * Adapted from code snippet at: https://github.com/graphhopper/map-matching
 */
public class MapMatcher {

    /** The location of the input OSM pbf file */
    private static String OSM_DATA_FILE = "./map-data/massachusetts-latest.osm.pbf";
    /** The location to cache the graph after reading the OSM data (Graphhopper creates this on first run) */
    private static String CACHE_DIR = "./cache/massachusetts";
    /** The directory that contains the trace files */
    private static String TRACES_DIR = "./traces";


    private static GraphHopper hopper;
    private static  MapMatching mapMatching;
    private static GraphHopperStorage graph;
    private static MiniGraphUI ui; // For visualising the graph(s)

    private static void init() {
        // import OpenStreetMap data
        hopper = new GraphHopper();
        hopper.setOSMFile(OSM_DATA_FILE);
        hopper.setGraphHopperLocation(CACHE_DIR);
        CarFlagEncoder encoder = new CarFlagEncoder();
        hopper.setEncodingManager(new EncodingManager(encoder));
        hopper.getCHFactoryDecorator().setEnabled(false);
        hopper.importOrLoad();

        ui = new MiniGraphUI(hopper, true);


        // create MapMatching object, can and should be shared across threads
        graph = hopper.getGraphHopperStorage();
        LocationIndexMatch locationIndex = new LocationIndexMatch(graph,
                (LocationIndexTree) hopper.getLocationIndex());
        mapMatching = new MapMatching(graph, locationIndex, encoder);
    }

    private static void match() {
        // do the actual matching, get the GPX entries from a file or via stream
        List<GPXEntry> inputGPXEntries = new GPXFile().doImport(TRACES_DIR+"/trace1.gpx").getEntries();
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

        GPXFile.write(path, "test.gpx", hopper.getTranslationMap().get("en_us"));

        //ui.visualize();

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
