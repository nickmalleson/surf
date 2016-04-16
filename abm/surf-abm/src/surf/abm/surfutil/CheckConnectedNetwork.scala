package surf.abm.surfutil

import java.io.{IOException, File}

import com.vividsolutions.jts.planargraph.Node
import org.apache.log4j.Logger
import sim.field.geo.GeomVectorField
import sim.io.geo.ShapeFileImporter
import sim.util.geo.GeomPlanarGraph
import surf.abm.SurfABM

/**
  * Checks that the input road network is connected. If not, log a list of roads that are disconnected.
  * Assumes that roads have an ID field.
  * Usage: scala surf.abm.surfutil.CheckConnectedNetwork [roads_file.shp]
  * Note: the classpath needs to be configured. See scripts/checkConnectedNetwork.sh.
  */
case object CheckConnectedNetwork {

  private val LOG: Logger = Logger.getLogger(this.getClass);

  def main(args: Array[String]): Unit = {

    if (args.length != 1) {
      this.LOG.error("The program expects one command-line argument; an input roads shapefile.")
      this.LOG.error("Usage: scala surf.abm.surfutil.CheckConnectedNetwork [roads_file.shp]")
      throw new Exception("Incorrect number of command line arguments: %d".format(args.length))
    }

    this.run(args(0))

  } // main()

  def run ( filename: String): Unit = {
    val roadsField = this.readRoadsFile(filename)
    val roadsNetwork = this.createNetwork(roadsField)
    val disconnected = this.checkDisconnected(roadsNetwork)
  }

  def readRoadsFile(filename: String) : GeomVectorField = {
    val f = new File(filename)
    if ( !f.exists() || !f.canRead) {
      throw new IOException("Cannot read from file %s".format(filename))
    }
    this.LOG.info("Checking that road network in '%s' is connected".format(filename))
    val roadsField = new GeomVectorField(SurfABM.WIDTH, SurfABM.HEIGHT)
    ShapeFileImporter.read(new File(filename).toURI().toURL(), roadsField)
    this.LOG.info("Read %d roads".format(roadsField.getGeometries().size()))
    return roadsField
  }

  def createNetwork(field: GeomVectorField) : GeomPlanarGraph = {
    val network = new GeomPlanarGraph()
    network.createFromGeomField(field)
    return network
  }

  /**
    * Checks whether the input disconnected.
 *
    * @param network
    * @return True if disconnected, false otherwise
    */
  def checkDisconnected(network: GeomPlanarGraph): Boolean = {

    this.LOG.info("Checking if disconnected")

    // Choose node zero to start with.
    val it : java.util.Iterator[Node] =
      network.getNodes().iterator().asInstanceOf[java.util.Iterator[Node]]
    val start : Node = it.next()

    val connected = scala.collection.mutable.HashSet[Node](start)

    // XXXX COULD USE FOLLOWING TO REATE disconnected HASHSET from all nodes?

    val a = ArrayBuffer[AnyRef]()
    for(i <- 1 until bag.size()) {
      a += bag.get(i)
    }
    return List(a:_*)

    val disconnected = scala.collection.mutable.HashSet[Node](
      network.getNodes(): _*
    )
    val dis
    while (it.hasNext()) {
      // XXXX HERE how to get a path to the next node
      // THIS IS DIFFICULT. PROBABLY BETTER TO RECURSE USING getEdgesIn()
      // and move each junction that we touch to the 'connected' list
      start.
      network.getNetwork().XXXX
    }

    return false
  }


}
