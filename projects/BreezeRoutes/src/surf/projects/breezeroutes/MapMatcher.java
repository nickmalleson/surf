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
import org.apache.commons.io.FileUtils;


import java.io.File;
import java.io.IOException;
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
    /** The directory that contains the input trace files */
    private static String TRACES_DIR = "./traces";
    /** Whether or not to write out the GPX traces after matching. These files contain the original gps points and path, as well as the matched route.*/
    private static boolean WRITE_GPX = true;


    private static GraphHopper hopper;
    private static  MapMatching mapMatching;
    private static GraphHopperStorage graph;
    private static MiniGraphUI ui; // For visualising the graph(s)

    /**
     * Do the matching.
     * @param osmDataFile The file that contains the OSM data.
     * @param cacheDir The directory to use for cacheing the graph (generated from the OSM data)
     * @param tracesDir The directory to search for traces in (the input gpx files).
     */
    public MapMatcher(String osmDataFile, String cacheDir, String tracesDir, boolean writeGPX ) throws Exception {
        init(osmDataFile, cacheDir);
        run(tracesDir, writeGPX);
    }

    /**
     * Initialise the map matcher. Load the OSM data, cache the graph, and prepare the required objects.
     */
    private static void init(String osmDataFile, String cacheDir) {
        // import OpenStreetMap data
        hopper = new GraphHopper();
        hopper.setOSMFile(osmDataFile);
        hopper.setGraphHopperLocation(cacheDir);
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


    private static void run(String tracesDir, boolean writeGPX) throws IOException {

        File tracesDirectory = new File(tracesDir);
        if (!tracesDirectory.isDirectory()) {
            throw new IOException("Traces directory ("+tracesDir+") is not a directory");
        }

        // Read all the gpx files in the traces directory
        File[] allFiles = tracesDirectory.listFiles();
        for (int i = 0; i < allFiles.length; i++) {
            File file = allFiles[i];
            if (file.isFile() && file.getName().endsWith(".gpx")) {

                System.out.println("Reading file ("+i+"): "+file);
                Path p = match(file.getAbsolutePath());


                if (writeGPX) {
                    String gpxout = file.getAbsolutePath().substring(0,file.getAbsolutePath().length()-4) + "-matched.gpx";
                    System.out.println("Writing matched path to GPX: "+gpxout);
                    GPXFile.write(p, gpxout, hopper.getTranslationMap().get("en_us"));
                }


            } // if isfile
        } // For all files

    }

    private static Path match(String inputGPXFile) {
        // do the actual matching, get the GPX entries from a file or via stream
        List<GPXEntry> inputGPXEntries = new GPXFile().doImport(inputGPXFile).getEntries();

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
/*        System.out.println("PATH:"+path.toDetailsString());

        PointList pl = path.calcPoints();
        System.out.println("PointList: "+pl.toString());

        List<EdgeIteratorState> edges = path.calcEdges();
        for (EdgeIteratorState e:edges) {
            System.out.println("\tEdgeID: "+e.getEdge());
            System.out.println("\t\tEdgeIteratorState:" + e.toString());
            System.out.println("\t\tWayGeometry: "+e.fetchWayGeometry(3).toString());

        }
*/
       return path;
    }


    public static void main(String[] args ) throws Exception {

        new MapMatcher(OSM_DATA_FILE, CACHE_DIR, TRACES_DIR, WRITE_GPX);

    }
}
