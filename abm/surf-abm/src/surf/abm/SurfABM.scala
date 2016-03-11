package surf.abm


import java.lang.reflect.Constructor
import javax.media.j3d.AuralAttributes

import com.typesafe.config.{ConfigFactory, Config}
import org.apache.log4j.{RollingFileAppender, Appender, Logger}
import sim.engine.SimState
import sim.field.geo.GeomVectorField
import sim.util.geo.{MasonGeometry, GeomPlanarGraph}
import com.vividsolutions.jts.geom.Envelope
import surf.abm.agents.Agent
;


/**
  * Main class for the surf agent-based model. This doesn't actually do very much,
  * almost all of the logic is in the companion singleton object
  */
class SurfABM (seed:Long) extends SimState (seed) {

  override def start(): Unit = {
    super.start

    SurfABM.agents.clear
    try {
      // Find the class to use to create agents.
      val className: String = SurfABM.conf.getString("AgentType")
      val cls: Class[Agent] = Class.forName(className).asInstanceOf[Class[Agent]]
      val c: Constructor[Agent] = cls.getConstructor(classOf[SurfABM], classOf[MasonGeometry])

      SurfABM.LOG.info("Creating agents of type " + cls.toString)

      for (i <- 0.until(SurfABM.numAgents)) {
        // Create a new a agent, passing the main model instance and a random new location
        val a: Agent = c.newInstance(this,this.getRamdomBuilding())


        SurfABM.agents.addGeometry(a.location)
        schedule.scheduleRepeating(a)
      }

    }
    catch {
      case e: Exception => {
        SurfABM.LOG.error("Exception while creating agents, cannot continue", e)
        throw e
      }
    }
    SurfABM.agents.setMBR(SurfABM.MBR)

    // Ensure that the spatial index is made aware of the new agent
    // positions.  Scheduled to guaranteed to run after all agents moved.
    schedule.scheduleRepeating(SurfABM.agents.scheduleSpatialIndexUpdater, Integer.MAX_VALUE, 1.0)

  }

  override def finish(): Unit = super.finish()




  def getRamdomBuilding() : MasonGeometry = {
    // Get a random building
    val o = SurfABM.buildings.getGeometries.get(this.random.nextInt(SurfABM.buildings.getGeometries().size()))
    // cast it to a MasonGemoetry using pattern matching (throwing an error if not possible)
    o match {
      case x:MasonGeometry => x
      case _ => throw new ClassCastException
    }
  }

} // class surfABM

object SurfABM  {

  // Initialise the logger
  private val LOG: Logger = Logger.getLogger(SurfABM.getClass);

  // ****** Initialise the model ******


    // Initialise the configuration reader
    val conf = ConfigFactory.load("surf-abm.conf")
    // println("DATA DIR:",conf.getString("DataDir"))

    // Number of agents
    val numAgents: Int = conf.getInt("NumAgents");
    LOG.info("Creating " + numAgents + " agents");

    // Not sure why these are necessary. Probably just for initialisation
    val WIDTH = conf.getInt("WIDTH");
    val HEIGHT = conf.getInt("HEIGHT");

    // Spatial layers
    val roads = new GeomVectorField(WIDTH, HEIGHT);
    val buildings = new GeomVectorField(WIDTH, HEIGHT);
    val agents = new GeomVectorField(WIDTH, HEIGHT);


    // Stores the network connections.  We represent the walkways as a PlanarGraph, which allows
    // easy selection of new waypoints for the agents.
    val network = new GeomPlanarGraph()
    val junctions = new GeomVectorField(WIDTH, HEIGHT) // nodes for intersections
    val MBR : Envelope  = null ; // Minimum envelope surrounding whole world

    // Map to link ids to environment objects. Bi-directional so can get keys from values.
    //val BiMap<Integer, MasonGeometry> buildingIDs;
    this.readData()

    network.createFromGeomField(roads)
    //this.addIntersectionNodes(network.nodeIterator)

    LOG.debug("Finished initialising model")



//  catch {
//    case e : Exception => LOG.error("Problem initialising the model", e);
//  }

  def apply(seed:Long): SurfABM = new SurfABM(seed) // Probably not necessary


  /* Read the GIS files into the relevant fields */
  def readData(): Unit = {

    val dataDir = SurfABM.conf.getString("leeds")

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

/*




        URL walkwaysURI = new File(dataDir.getAbsolutePath() + "/roads.shp").toURI().toURL();
        LOG.info("Reading roads file: " + walkwaysURI);
        ShapeFileImporter.read(walkwaysURI, roads);

        ShapeFileImporter.read(walkwaysURI, roads);

        this.MBR.expandToInclude(roads.getMBR());

        LOG.info("Finished reading data.");

        // Now synchronize the MBR for all GeomFields to ensure they cover the same area
        buildings.setMBR(MBR);
        roads.setMBR(MBR);
        */
  }




  /* Main application entry point */
  def main(args: Array[String]): Unit = {

    try {
      LOG.info("An Info message");
      LOG.debug("A debug message");
      SimState.doLoop(classOf[SurfABM], args);
    }
    catch {
      case e: Exception => {
        //SurfABM.LOG.error("Exception thrown in main loop", e)
        println("Exception thrown in main loop", e)
      }
    }
  }

}