package surf.abm


import java.io.File
import java.lang.reflect.Constructor

import _root_.surf.abm.agents.Agent
import _root_.surf.abm.environment.Building
import com.typesafe.config.{ConfigFactory}
import org.apache.log4j.{Logger}
import sim.engine.{Schedule, SimState}
import sim.field.geo.GeomVectorField
import sim.io.geo.ShapeFileImporter
import sim.util.Bag
import sim.util.geo.{MasonGeometry, GeomPlanarGraph}
import com.vividsolutions.jts.geom.{GeometryFactory, Envelope}
import com.vividsolutions.jts.planargraph.Node

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
    SurfABM.createAgents(this)
  }

  override def finish(): Unit = super.finish()


}

// class surfABM

@SerialVersionUID(1L)
object SurfABM extends Serializable {

  // Initialise the logger
  private val LOG: Logger = Logger.getLogger(SurfABM.getClass);

  // ****** Initialise the model ******

  // Get a configuration reader

  // Get the configuration reader.
  val conf = Util.config()
  // println("DATA DIR:",conf.getString("DataDir"))

  // Number of agents
  val numAgents: Int = conf.getInt("NumAgents");
  LOG.info("Creating " + numAgents + " agents");

  // Not sure why these are necessary. Probably just for initialisation
  val WIDTH = conf.getInt("WIDTH");
  val HEIGHT = conf.getInt("HEIGHT");

  // A list of all the agent geometries
  val agentGeoms = new GeomVectorField(WIDTH, HEIGHT);

  // Keep a map of agents and their geometries. This is created after the agents have been created
  ///var agentGeomMap : Map[SurfGeometry,Agent] = null


  // Spatial layers. One function to read them all
  val (buildingGeoms, roadGeoms, network, junctions, mbr) =
    _readEnvironmentData

    /**
      * Read and configure the buildings, roads, networks and junctions.
      * This is written as a function so that it can be tested elsewhere.
      *
      * @return
      */
    private def _readEnvironmentData
    //: (GeomVectorField, Map[Int, SurfGeometry], GeomVectorField, GeomPlanarGraph, GeomVectorField, Envelope)
    = {

      try {
        /* Read the GIS files into the relevant fields */

        // Maintain a maximum bounding envelope for all layers
        var MBR: Envelope = null // Minimum envelope surrounding whole world

        // Directory where the data are stored
        val dataDir = SurfABM.conf.getString("DataDir")
        SurfABM.LOG.info(s"Reading GIS data for the environment from ${dataDir}")

        // Start with buildings
        val buildings = new GeomVectorField(WIDTH, HEIGHT);
        // Declare the fields from the shapefile that should be read in with the geometries
        // GeoMason wants these to be a Bag
        val attributes: Bag = new Bag(Iterable[String](
          FIELDS.BUILDINGS_TOID.toString
          //(for (v <- FIELDS.values) yield v.toString()): _* // Add all fields, doesn't work
        ))
        // Read the shapefile (path relative from 'surf' directory)
        val bldgURI = new File("data/" + dataDir + "/buildings.shp").toURI().toURL();
        LOG.debug("Reading buildings  from file: " + bldgURI + " ... ");
        ShapeFileImporter.read(bldgURI, buildings, attributes);
        // Now cast all buildings from MasonGeometrys to SurfGeometrys
        val sgoms = scala.collection.mutable.ListBuffer.empty[SurfGeometry[Building]]
        for (o <- buildings.getGeometries()) {
          val g : MasonGeometry = o.asInstanceOf[MasonGeometry]
          val s = SurfGeometry(g,Building())
          sgoms += s
        }
        buildings.clear()
        for (s:SurfGeometry[Building] <- sgoms) {
          buildings.addGeometry(s)
        }
        buildings.updateSpatialIndex()
        //println("Gometryies", buildings.getGeometries.size())

        // Keep a link between the building IDs and their geometries (ID -> geometry)
        // NO LONGER NEEDED NOW THAT BUILDINGS ARE PART OF SURFGEOMETRY
        /*
        // Use a for comprehension to create a temp array of (Int, MasonGeometry) then use that as input to a map
        // Note, to go 'backwards' (i.e. from a location to an ID) do: origMap.map(_.swap)
        // (https://stackoverflow.com/questions/2338282/elegant-way-to-invert-a-map-in-scala)

        val tempArray: collection.immutable.Seq[(Int, SurfGeometry)] =
          (
            for (o <- buildings.getGeometries())
              yield {
                val g = o.asInstanceOf[SurfGeometry]
                Int.unbox(g.getIntegerAttribute(FIELDS.BUILDINGS_ID.toString())) -> g
              }
            ).to[collection.immutable.Seq]
        val b_ids: Map[Int, SurfGeometry] =
          scala.collection.immutable.Map[Int, SurfGeometry](tempArray: _*) // Splat the array with :_*

        assert(buildings.getGeometries.size() == b_ids.size)
        SurfABM.LOG.debug(s"\t ... read ${b_ids.size} buildings")
        */
        // We want to save the MBR so that we can ensure that all GeomFields
        // cover identical area.
        MBR = buildings.getMBR() // Minimum envelope surrounding whole world


        // Read roads
        val roads = new GeomVectorField(WIDTH, HEIGHT)
        val roadsURI = new File("data/" + dataDir + "/roads.shp").toURI().toURL()
        LOG.debug(s"Reading roads file: ${roadsURI} ...")
        ShapeFileImporter.read(roadsURI, roads);
        LOG.debug(s"\t... read ${roads.getGeometries().size()} roads")
        MBR.expandToInclude(roads.getMBR())
        LOG.debug("Finished reading roads and buildings data.");


        // Now synchronize the MBR for all GeomFields to ensure they cover the same area
        buildings.setMBR(MBR);
        roads.setMBR(MBR);

        // Stores the network connections.  We represent the walkways as a PlanarGraph, which allows
        // easy selection of new waypoints for the agents.
        val network = new GeomPlanarGraph()
        val junctions = new GeomVectorField(WIDTH, HEIGHT) // nodes for intersections

        SurfABM.LOG.debug("Creating road network")
        network.createFromGeomField(roads)
        val fact = new GeometryFactory();
        // Now add the associated junctions to the junctions geometry.
        network.getNodes().foreach( x => {
          junctions.addGeometry(
            SurfGeometry(
              new MasonGeometry(fact.createPoint(x.asInstanceOf[Node].getCoordinate())),
              x.asInstanceOf[Node]
            )
          )
        } ) // foreach


        SurfABM.LOG.info("Finished creating network and junctions")


        LOG.info("Finished initialising model environment")

        // Return the layers
        (buildings, roads, network, junctions, MBR)
      }
      catch {
        case e: Exception => {
          SurfABM.LOG.error("Error while reading GIS data", e)
          throw e
        }
      }

  }

  def createAgents(state : SurfABM ) = {
    SurfABM.agentGeoms.clear
    try {
      // Find the class to use to create agents.
      val className: String = SurfABM.conf.getString("AgentType")
      val cls: Class[Agent] = Class.forName(className).asInstanceOf[Class[Agent]]
      val c: Constructor[Agent] = cls.getConstructor(classOf[SurfABM], classOf[SurfGeometry[Agent]])

      SurfABM.LOG.info(s"Creating ${SurfABM.numAgents} agents of type ${cls.toString}")

      // Keep a list of the Agents and their Geometries. This will be turned into a Map shortly,
      //val agentArray = collection.mutable.ListBuffer.empty[(SurfGeometry[Agent],Agent)]

      // Create the agents
      for (i <- 0.until(SurfABM.numAgents)) {
        // Create a new a agent, passing the main model instance and a random new location
        val a: Agent = c.newInstance(state, SurfABM.getRandomBuilding(state))
        SurfABM.agentGeoms.addGeometry(SurfGeometry[Agent](a.location, a))

        state.schedule.scheduleRepeating(a)
        //agentArray += ( (a.location, a) ) // Need two  parantheses to make a tuple?
      }

      // Now store the agents and their geometries in a map so we can get back to the
      // agents from their geometry. This is necessary because most of the GeoMason containers
      // only store the geometry, not the underlying agent.
      //SurfABM.agentGeomMap = Map[SurfGeometry,Agent](agentArray: _*)

      assert(
        SurfABM.numAgents == SurfABM.agentGeoms.getGeometries().size()
        //SurfABM.numAgents == agentArray.size &&
        //SurfABM.numAgents == SurfABM.agentGeomMap.size,
        , s"Lengths of agent arrays differ. \n\t" +
          s"numAgents: ${SurfABM.numAgents}\n\t" +
          s"agentGeoms: ${SurfABM.agentGeoms}\n\t"
          //s"agentGeomMap: ${SurfABM.agentGeomMap.size}"
      )


    }
    catch {
      case e: Exception => {
        SurfABM.LOG.error("Exception while creating agents, cannot continue", e)
        throw e
      }
    }
    SurfABM.agentGeoms.setMBR(SurfABM.mbr)

    // Ensure that the spatial index is made aware of the new agent
    // positions.  Scheduled to guaranteed to run after all agents moved.
    state.schedule.scheduleRepeating(SurfABM.agentGeoms.scheduleSpatialIndexUpdater, Integer.MAX_VALUE, 1.0)
  }

  /**
    * Find a building, chosen at random.
    *
    * @param state An instance of the SimState that is running the model
    * @return
    */
  def getRandomBuilding(state: SimState): SurfGeometry[Building] = {
    // Get a random building
    val o = SurfABM.buildingGeoms.getGeometries.get(state.random.nextInt(SurfABM.buildingGeoms.getGeometries().size()))
    // cast it to a MasonGemoetry using pattern matching (throwing an error if not possible)
    o match {
      case x: SurfGeometry[Building @unchecked] => x
      case _ => throw new ClassCastException
    }
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