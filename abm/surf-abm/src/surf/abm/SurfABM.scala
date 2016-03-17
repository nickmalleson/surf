package surf.abm


import java.io.File
import java.lang.reflect.Constructor

import com.typesafe.config.{ConfigFactory}
import org.apache.log4j.{Logger}
import sim.engine.SimState
import sim.field.geo.GeomVectorField
import sim.io.geo.ShapeFileImporter
import sim.util.Bag
import sim.util.geo.{MasonGeometry, GeomPlanarGraph}
import com.vividsolutions.jts.geom.Envelope
import surf.abm.agents.Agent

import scala.collection.JavaConversions._
import scala.collection.immutable._


/**
  * Main class for the surf agent-based model. This doesn't actually do very much,
  * almost all of the logic is in the companion singleton object.
  *
  * Use this class to run the model in headless model (without a GUI). E.g. to run
  * for 5000 iterations, outputting the time each 1000 steps use:
  * <pre><code>
  *   scala surf.abm.SurfABM -for 5000 -time 1000
  * </pre></code>
  * To get more help options, do:
  * <pre><code>
  *   scala surf.abm.SurfABM -help
  * </pre></code>
  * Note that you need to set up the classpath for the above commands to work. See
  * the file <code>run.sh</code> for an example.
  *
  */
@SerialVersionUID(1L)
class SurfABM(seed: Long) extends SimState(seed) {

  override def start(): Unit = {
    super.start

    SurfABM.agents.clear
    try {
      // Find the class to use to create agents.
      val className: String = SurfABM.conf.getString("AgentType")
      val cls: Class[Agent] = Class.forName(className).asInstanceOf[Class[Agent]]
      val c: Constructor[Agent] = cls.getConstructor(classOf[SurfABM], classOf[MasonGeometry])

      SurfABM.LOG.info(s"Creating ${SurfABM.numAgents} agents of type ${cls.toString}")

      for (i <- 0.until(SurfABM.numAgents)) {
        // Create a new a agent, passing the main model instance and a random new location
        val a: Agent = c.newInstance(this, this.getRamdomBuilding())
        SurfABM.agents.addGeometry(a.location)
        schedule.scheduleRepeating(a)
      }
      assert(SurfABM.numAgents == SurfABM.agents.getGeometries().size())

    }
    catch {
      case e: Exception => {
        SurfABM.LOG.error("Exception while creating agents, cannot continue", e)
        throw e
      }
    }
    SurfABM.agents.setMBR(SurfABM.mbr)

    // Ensure that the spatial index is made aware of the new agent
    // positions.  Scheduled to guaranteed to run after all agents moved.
    schedule.scheduleRepeating(SurfABM.agents.scheduleSpatialIndexUpdater, Integer.MAX_VALUE, 1.0)

  }

  override def finish(): Unit = super.finish()


  def getRamdomBuilding(): MasonGeometry = {
    // Get a random building
    val o = SurfABM.buildings.getGeometries.get(this.random.nextInt(SurfABM.buildings.getGeometries().size()))
    // cast it to a MasonGemoetry using pattern matching (throwing an error if not possible)
    o match {
      case x: MasonGeometry => x
      case _ => throw new ClassCastException
    }
  }

}

// class surfABM

object SurfABM {

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

  // A list of all the agents
  val agents = new GeomVectorField(WIDTH, HEIGHT);


  // Spatial layers. One function to read them all
  val (buildings, roads, network, junctions, mbr) = _readEnvironmentData

  /**
    * Read and configure the buildings, roads, networks and junctions.
    * This is written as a function so that it can be tested elsewhere.
    * @return
    */
  private def _readEnvironmentData: (GeomVectorField, GeomVectorField, GeomPlanarGraph, GeomVectorField, Envelope) = {

    /* Read the GIS files into the relevant fields */

    // Maintain a maximum bounding envelope for all layers
    var MBR: Envelope = null // Minimum envelope surrounding whole world

    // Directory where the data are stored
    val dataDir = SurfABM.conf.getString("DataDir")

    // Start with buildings
    val buildings = new GeomVectorField(WIDTH, HEIGHT);

    // Declare the fields from the shapefile that should be read in with the geometries
    // GeoMason wants these to be a Bag
    val attributes: Bag = new Bag(Iterable[String](
      FIELDS.BUILDINGS_ID.toString(), FIELDS.BUILDINGS_NAME.toString(), FIELDS.BUILDING_FLOORS.toString()
    ))
    // Read the shapefile (path relative from 'surf' directory)
    val bldgURI = new File("data/" + dataDir + "/buildings.shp").toURI().toURL();
    LOG.info("Reading buildings  from file: " + bldgURI + " ... ");
    ShapeFileImporter.read(bldgURI, buildings, attributes);

    // Keep a link between the building IDs and their geometries (ID -> geometry)
    // Use a for comprehension to create a temp array of (Int, MasonGeometry) then use that as input to a map
    // Note, to go 'backwards' (i.e. from a location to an ID) do: origMap.map(_.swap)
    // (https://stackoverflow.com/questions/2338282/elegant-way-to-invert-a-map-in-scala)

    val tempArray: collection.immutable.Seq[(Int, MasonGeometry)] =
      (
        for (o <- buildings.getGeometries())
          yield {
            val g = o.asInstanceOf[MasonGeometry]
            Int.unbox(g.getIntegerAttribute(FIELDS.BUILDINGS_ID.toString())) -> g
          }
        ).to[collection.immutable.Seq]
    val buildingsIDs: scala.collection.immutable.Map[Int, MasonGeometry] =
      scala.collection.immutable.Map[Int, MasonGeometry](tempArray: _*) // Splat the array with :_*

    assert (buildings.getGeometries.size() == buildingsIDs.size)
    SurfABM.LOG.info(s"\t ... read ${buildingsIDs.size} buildings")

    // We want to save the MBR so that we can ensure that all GeomFields
    // cover identical area.
    MBR = buildings.getMBR() // Minimum envelope surrounding whole world


    // Read roads
    val roads = new GeomVectorField(WIDTH, HEIGHT);
    val roadsURI = new File("data/" + dataDir + "/roads.shp").toURI().toURL()
    LOG.info(s"Reading roads file: ${roadsURI} ...")
    ShapeFileImporter.read(roadsURI, roads);
    LOG.info(s"\t... read ${roads.getGeometries().size()} roads")
    MBR.expandToInclude(roads.getMBR());
    LOG.info("Finished reading data.");

    // Now synchronize the MBR for all GeomFields to ensure they cover the same area
    buildings.setMBR(MBR);
    roads.setMBR(MBR);

    // Stores the network connections.  We represent the walkways as a PlanarGraph, which allows
    // easy selection of new waypoints for the agents.
    val network = new GeomPlanarGraph()
    val junctions = new GeomVectorField(WIDTH, HEIGHT) // nodes for intersections

    network.createFromGeomField(roads)
    // XXXX HERE - FIX addIntersaectionNode() code below:
    //this.addIntersectionNodes(network.nodeIterator)
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

    LOG.info("Finished initialising model environment")

    // Return the layers
    (buildings, roads, network, junctions, MBR)

  }

  // Probably not necessary. This lets you do var a = SurfABM(seed) (no 'new')
  def apply(seed: Long): SurfABM = new SurfABM(seed)


  /* Main application entry point */
  def main(args: Array[String]): Unit = {

    try {
      LOG.debug("Beginning do loop")
      SimState.doLoop(classOf[SurfABM], args);
      LOG.debug("Finished do loop")
    }
    catch {
      case e: Exception => {
        SurfABM.LOG.error("Exception thrown in main loop", e)
        throw e
      }
    }
  }

}