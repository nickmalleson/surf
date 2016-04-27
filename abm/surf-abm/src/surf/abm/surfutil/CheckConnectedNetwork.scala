package surf.abm.surfutil

import java.io.{IOException, File}
import java.util

import com.vividsolutions.jts.planargraph.{Edge, Node}
import org.apache.log4j.Logger
import sim.field.geo.GeomVectorField
import sim.io.geo.ShapeFileImporter
import sim.util.geo.{GeomPlanarGraphDirectedEdge, GeomPlanarGraph}
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
    * Note: this function is very slow. A better way to check for a disconnected network is to use the
    * 'disconnected islands' plugin for QGIS. It works in seconds.
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

    val connected = traverse(start)

    if (connected.size > 0) return true

    return false


    // All other nodes are disconnected:
    //val disconnected = Set[Node](
    //  network.getNodes().toArray().toSeq.asInstanceOf[Seq[Node]]: _*
    //)

  }


  /**
    * Traverse the graph using a breadth-first-search. Implementation of the pseudocode on
    * <a href="https://en.wikipedia.org/wiki/Breadth-first_search">wikipedia</a>
    * I'm NOT SURE IF THIS ACTUALLY WORKS! I SUSPECT IT DOESN'T.
    * @param root The node to begin searching from
    * @return A Set of all nodes that can be reached from the root
    */
  def traverse(root:Node) : scala.collection.mutable.Set[Node] = {
    val q = scala.collection.mutable.Queue.empty[Node]
    val visited = scala.collection.mutable.Set.empty[Node]
    CheckConnectedNetwork.LOG.info("Traversing road network, looking for disconnected edges")

    q.enqueue(root)
    var counter = 0
    while (!q.isEmpty) {
      val current : Node = q.dequeue()
      visited+=current
      val edgeIterator = current.getOutEdges().iterator()
      while (edgeIterator.hasNext()) {
        counter += 1
        if (counter % 10000000 == 0) {
          LOG.info("Analysed %d edges. (Queue size: %d)".format(counter, q.size))
        }
        val n: Node = edgeIterator.next().asInstanceOf[GeomPlanarGraphDirectedEdge].getEdge().getOppositeNode(current)
        q.enqueue(n)
      }
    }
    LOG.info("Found %d connected nodes".format(visited.size))
    return visited
  }

  /*
/**
* Recursively traverse a network
*
* @param current The current node in the traversal
* @param visited The nodes that have already been visited
* @param remainder The nodes that have yet to be visited
* @return The set of all nodes that have been visited
*/
def traverse(current: Node, visited: Set[Node], remainder: Set[Node]) : Set[Node] = {

// Remove the current node from the set of unvisited nodes and add it to visited
val r = remainder - current
var v = visited + current

// See if all have been visited (i.e. the graph is connected)
if (remainder.size == 0) {
  return v
}


// Continue to traverse the children of this node (except those that have been traversed already),
// Adding those that can be visited to our list
for ( n : Node <- current.getOutEdges()) {
  if (!v.contains(n)) {
    v += traverse(n, v, r)
  }

}
return v

}8
*/


}
