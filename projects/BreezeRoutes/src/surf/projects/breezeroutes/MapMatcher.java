package surf.projects.breezeroutes;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.PathWrapper;
import com.graphhopper.matching.*;
import com.graphhopper.routing.*;
import com.graphhopper.routing.util.*;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.ui.MiniGraphUI;
import com.graphhopper.util.*;


import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

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
    private static boolean WRITE_MATCHED_PATH = true;
    /** Whether or not to write out the GPX shortest path after matching. */
    private static boolean WRITE_SHORTEST_PATH = true;


    private static GraphHopper hopper;
    private static MapMatching mapMatching;
    private static GraphHopperStorage graph;
    private static FlagEncoder encoder;
    //private static MiniGraphUI ui; // For visualising the graph(s)

    /**
     * Do the matching.
     * @param osmDataFile The file that contains the OSM data.
     * @param cacheDir The directory to use for cacheing the graph (generated from the OSM data)
     * @param tracesDir The directory to search for traces in (the input gpx files).
     * @param writeMatchedPath Whether to write GPX files for each matched path
     */
    public MapMatcher(String osmDataFile, String cacheDir, String tracesDir, boolean writeMatchedPath, boolean writeShortestPath ) throws Exception {
        init(osmDataFile, cacheDir);
        run(tracesDir, writeMatchedPath, writeShortestPath);
    }

    /**
     * Initialise the map matcher. Load the OSM data, cache the graph, and prepare the required objects.
     */
    private static void init(String osmDataFile, String cacheDir) {
        // import OpenStreetMap data
        hopper = new GraphHopper();
        hopper.setOSMFile(osmDataFile);
        hopper.setGraphHopperLocation(cacheDir);
        // This Encoder specifies how the network should be navigated. If it is changes (i.e. from foot to car) then the
        // cache needs to be deleted to force importOrLoad() to recalculate the graph.
        //CarFlagEncoder Encoder = new CarFlagEncoder();
        encoder = new FootFlagEncoder();
        hopper.setEncodingManager(new EncodingManager(encoder));
        hopper.getCHFactoryDecorator().setEnabled(false);
        hopper.importOrLoad();

        //ui = new MiniGraphUI(hopper, true);


        // create MapMatching object, can and should be shared across threads
        graph = hopper.getGraphHopperStorage();
        LocationIndexMatch locationIndexMatch = new LocationIndexMatch(graph, (LocationIndexTree) hopper.getLocationIndex());
        mapMatching = new MapMatching(graph, locationIndexMatch, encoder);
    }

    /**
     * Run the map matcher.
     * @param tracesDir The directory from which to read (and optionally write) the traces
     * @param writeMatchedPath Whether or not to write each mathched path (as GPX).
     * @throws IOException
     */
    private static void run(String tracesDir, boolean writeMatchedPath, boolean writeShortestPath) throws IOException, Exception {

        File tracesDirectory = new File(tracesDir);
        if (!tracesDirectory.isDirectory()) {
            throw new IOException("Traces directory ("+tracesDir+") is not a directory");
        }

        // Read all the gpx files in the traces directory
        File[] allFiles = tracesDirectory.listFiles();
        for (int i = 0; i < allFiles.length; i++) {
            File file = allFiles[i];
            if (file.isFile() && file.getName().endsWith(".gpx") &&
                    !file.getName().endsWith("-matched.gpx") && // Ignore 'matched' and 'shortest' files - these
                    !file.getName().endsWith("-shortest.gpx")   // are created by this program!
                    ) {

                System.out.println("Reading file ("+i+"): "+file);

                // get the GPX entries from a file
                List<GPXEntry> inputGPXEntries = new GPXFile().doImport(file.getAbsolutePath()).getEntries();

                // Do the matching
                Path matchedPath;
                try {
                    matchedPath = match(inputGPXEntries);
                }
                catch (java.lang.RuntimeException ex) {
                    System.err.println("Could not match file "+file.getName() + ". Message: "+ex.getMessage());
                    // TODO do something about these errors - maybe move the files to make it easier to analyse them
                    continue;
                }

                if (writeMatchedPath) {
                    String gpxout = file.getAbsolutePath().substring(0,file.getAbsolutePath().length()-4) + "-matched.gpx";
                    System.out.println("\tWriting matched path to GPX: "+gpxout);
                    GPXFile.write(matchedPath, gpxout, hopper.getTranslationMap().get("en_us"));
                }

                // Now find the shortest path.

                // Find the shortest path
                Path shortestPath = shortest(inputGPXEntries);

                if (writeShortestPath) {
                    String gpxout = file.getAbsolutePath().substring(0,file.getAbsolutePath().length()-4) + "-shortest.gpx";
                    System.out.println("\tWriting shortest path to GPX: "+gpxout);
                    GPXFile.write(shortestPath, gpxout, hopper.getTranslationMap().get("en_us"));

                }

            } // if isfile
        } // For all files

    }


    /**
     * Take the input gpx entries (e.g. from <code>new GPXFile().doImport("a_file.gpx").getEntries()</code>) and match
     * the route to an OSM path.
     * @param inputGPXEntries
     * @return The matched path.
     */
    private static Path match(List<GPXEntry> inputGPXEntries) {
        MatchResult mr = mapMatching.doWork(inputGPXEntries);
        double dist = mr.getMatchLength();
        System.out.println("Finished matching. Length: " + dist);
        Path path = mapMatching.calcPath(mr);
        return path;
    }

    /**
     * Take the input gpx entries (e.g. from <code>new GPXFile().doImport("a_file.gpx").getEntries()</code>) and
     * use GraphHopper to find a shortest path. See https://github.com/graphhopper/graphhopper/blob/master/docs/core/routing.md
     */
    private static Path shortest(List<GPXEntry> inputGPXEntries) throws Exception{

        double fromLat = inputGPXEntries.get(0).getLat(); // latFrom
        double fromLon = inputGPXEntries.get(0).getLon(); // lonFrom
        double toLat = inputGPXEntries.get(inputGPXEntries.size()-1).getLat(); //latTo
        double toLon = inputGPXEntries.get(inputGPXEntries.size()-1).getLon(); //latFrom


        //This looks like a nicer way of doing the route, but returns a PathWrapper, not a Path, and I can't
        // work out how to make a Path from a PathWrapper
        /*
        GHRequest req = new GHRequest(fromLat, fromLon, toLat, toLon)
        req.setWeighting("fastest");
        req.setVehicle("foot");
        req.setLocale(Locale.US);

        GHResponse rsp = hopper.route(req);
        // first check for errors
        if(rsp.hasErrors()) {
            throw new Exception("Errors creating a shortest path "+rsp.getErrors().toString());
        }

        // use the best path, see the GHResponse class for more possibilities.
        PathWrapper bestPath = rsp.getBest();
        return bestPath;
        */

        QueryGraph qg = new QueryGraph(graph);
        QueryResult fromQR = hopper.getLocationIndex().findClosest(fromLat, fromLon, EdgeFilter.ALL_EDGES);
        QueryResult toQR = hopper.getLocationIndex().findClosest(toLat, toLon, EdgeFilter.ALL_EDGES);
        qg.lookup(fromQR, toQR);

        Path path = new Dijkstra(qg, encoder, new FastestWeighting(encoder), hopper.getTraversalMode()).
                calcPath(fromQR.getClosestNode(), toQR.getClosestNode());

        return path;
    }



    public static void main(String[] args ) throws Exception {

        new MapMatcher(OSM_DATA_FILE, CACHE_DIR, TRACES_DIR, WRITE_MATCHED_PATH, WRITE_SHORTEST_PATH);

    }
}
