/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simburglar;

import com.vividsolutions.jts.planargraph.Node;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import sim.field.geo.GeomVectorField;
import sim.field.network.Edge;
import sim.field.network.Network;
import sim.io.geo.ShapeFileExporter;
import sim.util.geo.GeomPlanarGraph;
import sim.util.geo.GeomPlanarGraphDirectedEdge;
import sim.util.geo.MasonGeometry;
import simburglar.agents.AStar;
import simburglar.agents.Agent;
import simburglar.agents.BurglarTest;
import simburglar.exceptions.RoutingException;

/**
 *
 * @author Nick Malleson
 */
public class SimBurglarTest {

    private static SimBurglar state;
    Logger LOG = Logger.getLogger(SimBurglar.class);

    public SimBurglarTest() {
    }

    /**
     * Runs some pre-initialisation stuff
     *
     * @throws IOException
     */
    @BeforeClass
    public static void setUpClass() throws IOException {

        // Run some pre-initialisation stuff (reading properties files etc).

        try {
            // This should fail because not initialised.
            new SimBurglar(1L);
            fail("SimBurglar should have failed because not initialised");
        }
        catch (Exception e) {
            // Good.
        }

        SimBurglar.preInitialise();
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        // (Executed before every test)
    }

    @After
    public void tearDown() {
        // (Executed after every test)
    }

    @Test
    public void testContstruction() {

        try {
            // Test that SimBurglar objects can be created properly, and appropriate
            // exceptions are thrown if not.

            // See what happens if a property is the wrong type ...
            int agents = Integer.parseInt(GlobalVars.getInstance().properties.getProperty("NumAgents"));
            GlobalVars.getInstance().properties.setProperty("NumAgents", "a string");
            try {
                new SimBurglar(1L);
                fail("I was expecting an exception to be thrown here.");
            }
            catch (Exception ex) { // (SimBurglar wraps the specific exception in Exception class).
                // Good. Now reset the property
                GlobalVars.getInstance().properties.setProperty("NumAgents", String.valueOf(agents));
            }

            // .. or if a property doesn't exist.

            GlobalVars.getInstance().properties.remove("NumAgents");
            try {
                new SimBurglar(1L);
                fail("I was expecting an exception to be thrown here.");
            }
            catch (Exception ex) {
                GlobalVars.getInstance().properties.setProperty("NumAgents", String.valueOf(agents));
            }


            // Finally check that a model can be created successfully
            new SimBurglar(1L);



        }
        catch (Exception ex) {
            // Shouldn't get here.
            fail("Unexpected exception testing construction: " + ex.toString());
        }




    }

    /**
     * Tests the road network for consistency. Makes sure it is connected and
     * checks to see that a route can be created from every building to one that
     * I know can be reached. Disabled at the moment because there is a possible
     * bug in getNetwork().
     */
    @Test
    public void testNetwork() throws RoutingException {

        // Get a state (use convenience class to make sure only one instance).
        state = TestInit.setupModel();

        LOG.info("Testing if network is connected");

        // Find a base building to start the search from 
        MasonGeometry base = state.buildingIDs.get(GIS_FIELDS.CHOSEN_DATA.BUILDING_A);

        // Find the node nearest to the base.
        MasonGeometry nearestJunc = GISFunctions.findNearestObject(
                base, 
                state.junctions, 
                state);
        Object start = state.network.findNode(nearestJunc.geometry.getCoordinate());

        // Search all nodes reachable from the abse
        Set<Object> found = new HashSet<Object>();

        search(start, state.network, found);

        // Compare this set to all the nodes
        Set<Object> all = new HashSet<Object>(state.network.getNetwork().getAllNodes());


        if (found.size() != all.size()) {
            fail("Graph is not connected. Found " + found.size() + " connected edges out of " + all.size() + " in total ");
        }
        else {
            LOG.info("Found " + found.size() + " connected edges out of " + all.size() + " in total ");
        }


    }

    /**
     * This would test to see if routes can be created to all houses. 
     * 
     * Any that are inaccessible are written out and also saved in a shapefile 
     * called 'bad_buildings.shp'. Disabled at the moment because it takes
     * some time to run.
     * @throws RoutingException 
     */
//    @Test
    public void testRoutesToAllHouses() throws RoutingException {

        state = TestInit.setupModel();

        LOG.info("Testing if routes can be created to all houses.");

        // Get rid of all agents and add a single one at the starting location

        state.agents.clear();
        MasonGeometry base = state.buildingIDs.get(GIS_FIELDS.CHOSEN_DATA.BUILDING_A);
        Agent b = new Agent(state, base);
        Node currentJunction = state.network.findNode(GISFunctions.findNearestObject(b.getGeometry(), state.junctions, state).getGeometry().getCoordinate());
        assert currentJunction != null : "Could not find a junction for agent at house " + GIS_FIELDS.CHOSEN_DATA.BUILDING_A;

        // Now loop through all hosues and see which ones we can't create routes to
        Node destJunc;
        List<Integer> badBuildings = new ArrayList<Integer>();
        GeomVectorField badBuildingsField = new GeomVectorField(SimBurglar.WIDTH, SimBurglar.HEIGHT); // Store bad buildings to write shapefile later
        int counter = 0;
        for (Object o : state.buildings.getGeometries()) {
            counter++;
            MasonGeometry buildingGeometry = (MasonGeometry) o;
            destJunc = state.network.findNode(GISFunctions.findNearestObject(buildingGeometry, state.junctions, state).getGeometry().getCoordinate());
            assert destJunc != null : String.format("Could not find a junction for the destination %s ", state.buildingIDs.inverse().get(buildingGeometry));

            AStar pathfinder = new AStar();
            List<GeomPlanarGraphDirectedEdge> paths = pathfinder.astarPath(currentJunction, destJunc);
            
            if (paths==null || paths.isEmpty()) {
                int buildingID = state.buildingIDs.inverse().get(buildingGeometry);
                badBuildings.add(buildingID);
                LOG.info("No route to building "+buildingID);
                badBuildingsField.addGeometry(buildingGeometry);
            }
            
            if (counter % 5000 == 0) {
                LOG.info("Analysed building "+counter+" / "+state.buildings.getGeometries().size());
            }
        }
        
        String msg = "Found the following buildings (as QGIS-formatted selection string): ID = ";
        for (int id: badBuildings) {
            msg += ( id +" OR ID = ");
        }
        String filename = new File(GlobalVars.getInstance().ROOT_DIR + "resources/data/" + GlobalVars.getProperty("DataDir") + "/bad_buildings.shp").getAbsolutePath();
        
        
        LOG.info("Writing buildings to shapefile: "+filename);
        ShapeFileExporter.write(filename, badBuildingsField);
        
        LOG.info(msg);
        
        // Write these buildings out to shapefile

    }

    /**
     * Depth first search to find all connecteed nodes from the start.
     *
     *
     *
     * @param start The node to start from
     * @param network The network
     * @param found A set to contain all found nodes (the start will be added).
     * @return All nodes found (including the start)
     */
    private void search(Object start, GeomPlanarGraph network, Set<Object> found) {


        found.add(start);
        // Find all nodes connected to the start. Have to iterate over each edge
        // connected to start and add the nodes the edge goes from and to.
        Collection<Edge> edges = network.getNetwork().getEdges(start, null);
        for (Edge e : edges) {
            // Need to search 'to' or 'from' node - not the one that we started from;
            Object node = (e.getTo() == start ? e.getFrom() : e.getTo());
            if (!found.contains(node)) { // Don't re-search nodes that have been found already
                search(node, network, found);
            }
        } // for edges
    } // search
    
    
    /**
     * Basically just check that the agent class can be instantiated.
     * @throws ClassNotFoundException
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InstantiationException 
     */
    @Test
    public void testAddAgents() throws ClassNotFoundException, SecurityException, 
        NoSuchMethodException, InvocationTargetException, IllegalArgumentException, 
        IllegalAccessException, InstantiationException {
        
        state = TestInit.setupModel();
        
        // Check that the required properties exist
        String nAgents = GlobalVars.getProperty("NumAgents");
        String className = GlobalVars.getProperty("AgentType");
        
        assertNotNull("NumAgents property not found", nAgents);
        assertNotNull("AgentType property not found", className);
        
        Integer.parseInt(nAgents);
        
        Class<Agent> cls = (Class<Agent>) Class.forName(className);
        
        Constructor<Agent> c = cls.getConstructor(SimBurglar.class);
       
        c.newInstance(state);
    }
    
    

//    /**
//     * Test of start method, of class SimBurglar.
//     */
//    @Test
//    public void testStart() {
//        System.out.println("start");
//        SimBurglar instance = null;
//        instance.start();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of finish method, of class SimBurglar.
//     */
//    @Test
//    public void testFinish() {
//        System.out.println("finish");
//        SimBurglar instance = null;
//        instance.finish();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
    /**
     * Test of main method, of class SimBurglar.
     */
//    @Test
//    public void testMain() throws Exception {
//        System.out.println("main");
//        String[] args = null;
//        SimBurglar.main(args);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
}
