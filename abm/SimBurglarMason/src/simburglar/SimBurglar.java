/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simburglar;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.planargraph.Node;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.RollingFileAppender;
import sim.engine.SimState;
import sim.field.geo.GeomVectorField;
import sim.io.geo.ShapeFileImporter;
import sim.util.Bag;
import sim.util.geo.GeomPlanarGraph;
import sim.util.geo.MasonGeometry;
import simburglar.agents.Agent;
import simburglar.agents.Burglar;

/**
 *
 * @author nick
 */
public class SimBurglar extends SimState {

    private static final long serialVersionUID = 1L;
    public int numAgents; // Number of agents in the model.
    // Sizes (pixels) of the geographic space (read from the Properties file).
    public static int WIDTH;
    public static int HEIGHT;
    // Fields 
    public GeomVectorField roads;
    public GeomVectorField buildings;
    public GeomVectorField agents;
    // Stores the network connections.  We represent the walkways as a PlanarGraph, which allows 
    // easy selection of new waypoints for the agents.  
    public GeomPlanarGraph network;
    public GeomVectorField junctions;// nodes for intersections
    public Envelope MBR; // Minimum envelope surrounding whole world
    
    // Map to link ids to environment objects. Bi-directional so can get keys from values.
    public BiMap<Integer, MasonGeometry> buildingIDs;
    private static Logger LOG;
    
    static {
        // This static block initialises logging and causes a new file to be
        // created for each model run (rollOver). From:
        // http://stackoverflow.com/questions/13585921/log4j-roll-file-for-each-execution
        LOG = Logger.getLogger(SimBurglar.class);
        for (Enumeration<Appender> e = Logger.getRootLogger().getAllAppenders(); e.hasMoreElements();) {
            Appender a = e.nextElement();
            if (a instanceof RollingFileAppender) {
                ((RollingFileAppender) a).rollOver();
            }
        }
    }

    public SimBurglar(long seed) throws Exception {
        super(seed);
        try {
            // Check that the pre-initialisation stuff has been done
            if (GlobalVars.getInstance().ROOT_DIR == null) {
                throw new Exception("It looks like the root mode directory has not been "
                        + "set. Has the SimBurglar.preInitialise() function been called "
                        + "before trying to create a SimBurglar object?");
            }

            // Set the number of agents
            this.numAgents = Integer.parseInt(GlobalVars.getInstance().properties.getProperty("NumAgents"));
            LOG.debug(String.format("Number of agents: %s", this.numAgents));

            // Create the references to the environments
            SimBurglar.WIDTH = Integer.parseInt(GlobalVars.getProperty("WIDTH"));
            SimBurglar.HEIGHT = Integer.parseInt(GlobalVars.getInstance().properties.getProperty("HEIGHT"));
            this.roads = new GeomVectorField(WIDTH, HEIGHT);
            this.buildings = new GeomVectorField(WIDTH, HEIGHT);
            this.agents = new GeomVectorField(WIDTH, HEIGHT);

            this.network = new GeomPlanarGraph();
            this.junctions = new GeomVectorField(WIDTH, HEIGHT); // nodes for intersections

            this.readData();

            network.createFromGeomField(roads);
            this.addIntersectionNodes(network.nodeIterator());

            LOG.debug("Finished initialising model");
        }
        catch (Exception ex) {
            LOG.error("Exception during model initialisation. ", ex);
            throw ex;
        }
    }

    @Override
    public void start() {
        super.start();

        agents.clear(); // clear any existing agents from previous runs
        try {
        addAgents();
        } catch (Exception e) {
            LOG.error("Exception while creating agents", e);
            this.finish();
            return;
        }
        agents.setMBR(this.MBR);

        // Ensure that the spatial index is made aware of the new agent
        // positions.  Scheduled to guaranteed to run after all agents moved.
        schedule.scheduleRepeating(agents.scheduleSpatialIndexUpdater(), Integer.MAX_VALUE, 1.0);


    }

    @Override
    public void finish() {
        super.finish();
    }

    /**
     * Read the GIS data
     *
     * @throws MalformedURLException
     * @throws FileNotFoundException
     */
    private void readData() throws MalformedURLException, FileNotFoundException, IOException, Exception {

        /* Read the GIS files into the relevant fields */

        File dataDir = new File(GlobalVars.getInstance().ROOT_DIR + "resources/data/" + GlobalVars.getProperty("DataDir"));

        // Start with buildings

        // Declare the fields from the shapefile that should be read in with the geometries
        Bag attributes = new Bag(Arrays.asList(new String[]{
                    FIELDS.BUILDINGS_ID.toString(), FIELDS.BUILDINGS_NAME.toString(), FIELDS.BUILDING_FLOORS.toString()
                }));
        URL bldgURI = new File(dataDir.getAbsolutePath() + "/buildings.shp").toURI().toURL();
        LOG.info("Reading buildings file: " + bldgURI);
        ShapeFileImporter.read(bldgURI, buildings, attributes);


        // Put the building IDs in a hashtable
        this.buildingIDs = HashBiMap.create(this.buildings.getGeometries().size());
        for (Object o : this.buildings.getGeometries()) {
            MasonGeometry g = (MasonGeometry) o;
            this.buildingIDs.put(g.getIntegerAttribute(FIELDS.BUILDINGS_ID.toString()), g);
        }

        // We want to save the MBR so that we can ensure that all GeomFields
        // cover identical area.
        this.MBR = buildings.getMBR();

        URL walkwaysURI = new File(dataDir.getAbsolutePath() + "/roads.shp").toURI().toURL();
        LOG.info("Reading roads file: " + walkwaysURI);
        ShapeFileImporter.read(walkwaysURI, roads);

        ShapeFileImporter.read(walkwaysURI, roads);

        this.MBR.expandToInclude(roads.getMBR());

        LOG.info("Finished reading data.");

        // Now synchronize the MBR for all GeomFields to ensure they cover the same area
        buildings.setMBR(MBR);
        roads.setMBR(MBR);
    }

    private void addAgents() throws ClassNotFoundException, SecurityException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        
        // Find the class to use to create agents.
        
        String className = GlobalVars.getProperty("AgentType");
        Class<Agent> cls = (Class<Agent>) Class.forName(className);
        Constructor<Agent> c = cls.getConstructor(SimBurglar.class);
       
        LOG.info("Creating agents of type "+cls.toString());

        for (int i = 0; i < this.numAgents; i++) {
                       
            Agent a = c.newInstance(this);
//            Agent a = new Burglar(this);

            agents.addGeometry(a.getGeometry());

            schedule.scheduleRepeating(a);
        }

    }

    /**
     * adds nodes corresponding to road intersections to GeomVectorField
     *
     * @param nodeIterator Points to first node
     *
     * Nodes will belong to a planar graph populated from LineString network.
     */
    private void addIntersectionNodes(Iterator nodeIterator) {
        GeometryFactory fact = new GeometryFactory();
        Coordinate coord;
        Point point;
        int counter = 0;

        while (nodeIterator.hasNext()) {
            Node node = (Node) nodeIterator.next();
            coord = node.getCoordinate();
            point = fact.createPoint(coord);

            junctions.addGeometry(new MasonGeometry(point));
            counter++;
        }
    }

    /**
     * Do some initialisation that must happen before the model itself can be
     * configured (e.g. read properties file etc.).
     */
    protected static void preInitialise() throws IOException {
        
        LOG.debug("SimBurglar pre-initialisation called");

        GlobalVars g = GlobalVars.getInstance();

        // Set the root model directory.
        String currentDir = new java.io.File(".").getCanonicalPath();
        g.ROOT_DIR = currentDir.substring(0, currentDir.indexOf("SimBurglarMason") + 15) + "/";

        // Read the model properties
        g.properties = new Properties();
        g.properties.load(new FileInputStream(g.ROOT_DIR + "simburglar.properties"));

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {

        try {
            SimBurglar.preInitialise();
        }
        catch (Exception e) {
            LOG.error("Exception thrown during pre-initialisation", e);
            return;
        }

        try {
            doLoop(SimBurglar.class, args);
        }
        catch (Exception e) {
            LOG.error("Exception thrown in main loop", e);
        }
        System.exit(0);
    }

}
