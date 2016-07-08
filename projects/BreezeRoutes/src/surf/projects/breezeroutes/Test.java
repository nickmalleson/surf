package surf.projects.breezeroutes;

import com.graphhopper.GraphHopper;
import com.graphhopper.matching.*;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.util.GPXEntry;

import java.util.List;

/**
 * Created by nick on 08/07/2016.
 */
public class Test {

    public static void main(String[] args ) {

        // import OpenStreetMap data
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile("./map-data/leipzig_germany.osm.pbf");
        hopper.setGraphHopperLocation("./target/mapmatchingtest");
        CarFlagEncoder encoder = new CarFlagEncoder();
        hopper.setEncodingManager(new EncodingManager(encoder));
        hopper.getCHFactoryDecorator().setEnabled(false);
        hopper.importOrLoad();

// create MapMatching object, can and should be shared accross threads

        GraphHopperStorage graph = hopper.getGraphHopperStorage();
        LocationIndexMatch locationIndex = new LocationIndexMatch(graph,
                (LocationIndexTree) hopper.getLocationIndex());
        MapMatching mapMatching = new MapMatching(graph, locationIndex, encoder);

        // do the actual matching, get the GPX entries from a file or via stream
        List<GPXEntry> inputGPXEntries = new GPXFile().doImport("nice.gpx").getEntries();
        MatchResult mr = mapMatching.doWork(inputGPXEntries);

        // return GraphHopper edges with all associated GPX entries
        List<EdgeMatch> matches = mr.getEdgeMatches();
// now do something with the edges like storing the edgeIds or doing fetchWayGeometry etc
        matches.get(0).getEdgeState();
    }
}
