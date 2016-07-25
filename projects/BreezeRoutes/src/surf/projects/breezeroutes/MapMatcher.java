package surf.projects.breezeroutes;

import com.graphhopper.GraphHopper;
import com.graphhopper.matching.*;
import com.graphhopper.routing.*;
import com.graphhopper.routing.util.*;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.*;
import scala.actors.threadpool.Arrays;


import java.io.File;
import java.io.IOException;
import java.util.List;


/**
 * Do some map matching between GPX files and OSM roads.
 *
 * Adapted from code snippet at: https://github.com/graphhopper/map-matching
 */
public class MapMatcher {

    /** Debug mode. Stop afer 20 files */
    private static boolean DEBUG = false;

    /** Whether to overide matched- or shortest-paths that have already been created */
    private static boolean OVERWRITE = false;

    /** The location of the input OSM pbf file */
    private static String OSM_DATA_FILE = "./map-data/massachusetts-latest.osm.pbf";

    /** The location to cache the graph after reading the OSM data (Graphhopper creates this on first run) */
    private static String CACHE_DIR = "./cache/massachusetts";

    /** A class to store the names of the directories where data files can be read and written */
    private static final class Directories {
        /** The root for all files */
        private static final String ROOT = "/Users/nick/mapping/projects/runkeeper/mitmount/runkeeper/mapmatching-traces/";
        //String ROOT = "./traces"
        /** Subdirectory contains the original gpx files (to be read) */
        static String ORIG_GPX = ROOT + "gpx/";
        /** Subdirectory contains the matched (output) gpx files */
        static String GPX_MATCHED = ROOT + "gpx-matched/";
        /** Subdirectory contains the shortest path (output) gpx files */
        static String GPX_SHORTEST = ROOT + "gpx-shortest/";
    }

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
     * @param writeMatchedPath Whether to write GPX files for each matched path
     */
    public MapMatcher(String osmDataFile, String cacheDir, boolean writeMatchedPath, boolean writeShortestPath ) throws Exception {
        init(osmDataFile, cacheDir);
        run(writeMatchedPath, writeShortestPath);
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

        // Configure some parameters to try to stop the algorithm breaking.

        // The following attempts to fix errors like:
        // Could not match file 58866174745e88db19b3eced744141fc.gpx. Message: Cannot find matching path! Wrong vehicle foot or missing OpenStreetMap data? Try to increase max_visited_nodes (500). Current gpx sublist:2, start list:[6547-300786  42.35620562568512,...
        // mapMatching.setMaxVisitedNodes(1000); // Didn't work

        // The following attempts to fix errors like:
        // Could not match file 5e6c6e4d6cc9efe9e314aa77a229f88e.gpx. Message:  Result contains illegal edges. Try to decrease the separated_search_distance (300.0) or use force_repair=true. Errors:[duplicate edge::457954->304150
        //mapMatching.setSeparatedSearchDistance(200);
        mapMatching.setForceRepair(true);
    }

    /**
     * Run the map matcher.
     * @param writeMatchedPath Whether or not to write each mathched path (as GPX).
     * @throws IOException
     */
    private static void run(boolean writeMatchedPath, boolean writeShortestPath) throws IOException, Exception {

        // Check which directories will be used to read and write the data to/from.
        File gpxDir = new File(Directories.ORIG_GPX);
        File matchedDir = new File(Directories.GPX_MATCHED);
        File shortestDir = new File(Directories.GPX_SHORTEST);
        for (File dir : (List<File>)Arrays.asList(new File[]{gpxDir, matchedDir, shortestDir}) ) {
            if (!dir.isDirectory()) {
                throw new IOException("Error: ("+dir+") is not a directory");
            }
        }

        // Read all the gpx files

        System.out.println("Reading directory: "+gpxDir);
        File[] allFiles = gpxDir.listFiles();
        System.out.println("\tThere are "+allFiles.length+" files in the directory.\nReading files.");

        int success = 0; // Remember the number of files successfull processed (or not)
        int failed = 0;
        int ignored = 0;
        for (int i = 0; i < allFiles.length; i++) {
            if (DEBUG && i > 20) {
                System.out.println("Debug mode is on. Stopping now.");
                break;
            }
            if (i % 5000 == 0) {
                System.out.println("\t .. read file "+i);
            }
            File file = allFiles[i];
            if ( file.isFile() && file.getName().endsWith(".gpx") ) {

                System.out.println("Reading file ("+i+"): "+file);
                String matchedFilename =  Directories.GPX_MATCHED + file.getName().substring(0,file.getName().length()-4)+ "-matched.gpx";
                String shortestFilename = Directories.GPX_SHORTEST + file.getName().substring(0,file.getName().length()-4)+ "-shortest.gpx";

                if ( ( new File(matchedFilename).exists() || new File(shortestFilename).exists()) && !OVERWRITE) {
                    System.out.println("\tShortest- or matched-file already exists, ignoring ");
                    ignored++;
                    continue;
                }

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
                    failed++;
                    continue;
                }

                if (writeMatchedPath) {
                    // file should be created in the 'matched' sub directory and have '-matched' inserted into the filename
                    System.out.println("\tWriting matched path to GPX: "+matchedFilename);
                    if (!DEBUG) {
                        GPXFile.write(matchedPath, matchedFilename, hopper.getTranslationMap().get("en_us"));
                    }
                }

                // Now find the shortest path.

                // Find the shortest path
                Path shortestPath = shortest(inputGPXEntries);

                if (writeShortestPath) {
                    System.out.println("\tWriting shortest path to GPX: "+shortestFilename);
                    if (!DEBUG) {
                        GPXFile.write(shortestPath, shortestFilename, hopper.getTranslationMap().get("en_us"));
                    }
                }

                success++;


            } // if isfile
            else {
                System.out.println("\tIgnoring "+file);
                ignored++;
            }
        } // For all files
        if (DEBUG) {
            System.out.println("WARN: Debug is ON, so no output will actually have been created");
        }
        System.out.println("Finished. Processed "+ (failed+success+ignored)+" files."+
                "\n\tSuccess:"+success+
                "\n\tFailed: "+failed+
                "\n\tIgnored: "+ignored
        );


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

        new MapMatcher(OSM_DATA_FILE, CACHE_DIR, WRITE_MATCHED_PATH, WRITE_SHORTEST_PATH);

    }
}
