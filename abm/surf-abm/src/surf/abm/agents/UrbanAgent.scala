package surf.abm.agents


import com.vividsolutions.jts.geom.{LineString, Coordinate}
import com.vividsolutions.jts.linearref.LengthIndexedLine
import com.vividsolutions.jts.planargraph.Node
import sim.util.geo.{GeomPlanarGraphEdge, GeomPlanarGraphDirectedEdge}
import surf.abm.environment.Building
import surf.abm.exceptions.RoutingException
import surf.abm.{GISFunctions, SurfGeometry, SurfABM}

import scala.collection.JavaConversions._ // TODO: this won't be necessary once I have re-written A* Path

/**
  * The base class for 'Urban' agents - i.e. those that can navigate a road network.
  *
  * @param state A pointer to the main model instance (<code>SurfABM</code>)
  * @param home The agent's home (starting location). Usually a <code>Building</code>
  *             where (e.g.) they live, but not necessarily. The agent's initial
  *             location is set to be <code>home</code>
  */
@SerialVersionUID(1L)
abstract class UrbanAgent (state:SurfABM, home:SurfGeometry[Building]) extends Agent(state,home) with Serializable {

  // A destination that the agent might be heading to.
  // This can be null, so wrap in Option() to make this explicit
  //protected var _destination: Option[SurfGeometry[_ <: Any]] = Option(null) // (Option tells us this could be null)
  protected var _destination: Option[SurfGeometry[_]] = Option(null) // (Option tells us this could be null)
  def destination() = this._destination // Accessor to destination
  protected var _atDestination = false
  def atDestination() = this._atDestination

  // Used by agent to walk along line segment; assigned in setNewRoute()
  private var segment: LengthIndexedLine = null
  private var startIndex = 0.0
  private var endIndex = 0.0
  private var currentIndex = 0.0

  // A list of roads that need to be followed to reach the destination
  private var _path: List[GeomPlanarGraphDirectedEdge] = null
  def path(): List[GeomPlanarGraphDirectedEdge] = _path

  private var indexOnPath = 0
  private var pathDirection = 1
  private var linkDirection = 1


  protected def moveAlongPath() : Unit = {

    currentIndex += moveRate * linkDirection

    // check to see if the progress has taken the current index beyond its goal
    // given the direction of movement. If so, proceed to the next edge
    if (linkDirection == 1 && currentIndex > endIndex) {
      this._atDestination = transitionToNextEdge(currentIndex - endIndex)
    }
    else if (linkDirection == -1 && currentIndex < startIndex) {
      this._atDestination = transitionToNextEdge(startIndex - currentIndex)
    }
    val currentPos: Coordinate = segment.extractPoint(currentIndex)
    moveToCoordinate(currentPos)

  }

  /**
    * Sets this Agent up to proceed along an Edge
    *
    * @param edge the GeomPlanarGraphEdge to traverse next
    *
    */
  protected def setupEdge(edge: GeomPlanarGraphEdge) {
    val line: LineString = edge.getLine
    this.segment = new LengthIndexedLine(line)
    this.startIndex = this.segment.getStartIndex
    this.endIndex = this.segment.getEndIndex
    val distanceToStart: Double = line.getStartPoint.distance(this.location.geometry)
    val distanceToEnd: Double = line.getEndPoint.distance(this.location.geometry)
    if (distanceToStart <= distanceToEnd) {
      this.currentIndex = this.startIndex
      this.linkDirection = 1
    }
    else if (distanceToEnd < distanceToStart) {
      this.currentIndex = this.endIndex
      this.linkDirection = -1
    }
  }

  /**
    * Transition to the next edge in the path
    *
    * @param residualMove the amount of distance the agent can still travel
    *                     this turn
    * @return True if the agent has reached the end of the path, false otherwise.
    */
  private def transitionToNextEdge(residualMove: Double): Boolean = {
    assert(this.path != null, "The path shouldn't be null (for agent %s)".format(this.id))

    indexOnPath += pathDirection
    if ((this.pathDirection > 0 && this.indexOnPath >= this.path.size) ||
      (this.pathDirection < 0 && this.indexOnPath < 0)) {

      // (nasty way of checking that destination!=null before calling moveToCoordinate)
      this.destination() match { // Check that destination is not None
        case Some(s) => this.moveToCoordinate(this.destination().get.getGeometry.getCoordinate())
        case None => throw new RoutingException("Cannot transitiontoNextEdge - destination is None")
      }
//      this.moveToCoordinate(this.destination.get.getGeometry.getCoordinate)
      this.startIndex = 0.0
      this.endIndex = 0.0
      this.currentIndex = 0.0
      this._destination = Option(null)
      this._path = null
      this.indexOnPath = 0
      return true
    }
    val edge: GeomPlanarGraphEdge = this.path()(indexOnPath).getEdge.asInstanceOf[GeomPlanarGraphEdge]
    this.setupEdge(edge)
    val speed: Double = residualMove * linkDirection
    currentIndex += speed
    if (linkDirection == 1 && currentIndex > endIndex) {
      return transitionToNextEdge(currentIndex - endIndex)
    }
    else if (linkDirection == -1 && currentIndex < startIndex) {
      return transitionToNextEdge(startIndex - currentIndex)
    }
    return false
  }

  /**
    * Sets the <code>UrbanAgent</code>'s path - i.e. the roads that it must pass through
    * in order to reach its destination
    */
  protected def findNewPath(): List[GeomPlanarGraphDirectedEdge] = {

    // TODO HERE - find out why a proper path isn't being created and returned

    // Check that we have a destination to head to:
    val dest : SurfGeometry[_] = this.destination() match { // Check that destination is not None
      case Some(s) => s
      case None => throw new RoutingException("Cannot findNewPath - destination is None")
    }

    /* First, find the nearest junction to our current position. */

    var currentJunction: Node = SurfABM.network.findNode(location.getGeometry.getCoordinate)

    // Not exactly on a junction, find the nearest and move onto it
    if (currentJunction == null) {
      val nearestJunction: SurfGeometry[_] = GISFunctions.findNearestObject(this.location, SurfABM.junctions)
      currentJunction = SurfABM.network.findNode(nearestJunction.getGeometry.getCoordinate)
    }
    //        assert currentJunction != null : "Could not find a junction for agent " + this.id + " at " + location;
    /* Now find the junction that is closest to the destination */
    var destinationJunction: Node = SurfABM.network.findNode(dest.getGeometry().getCoordinate)
    if (destinationJunction == null) {
      val nearestJunction: SurfGeometry[_] = GISFunctions.findNearestObject(dest, SurfABM.junctions)
      destinationJunction = SurfABM.network.findNode(nearestJunction.getGeometry.getCoordinate)
    }
    assert(destinationJunction != null, String.format("Could not find a junction for the destination %s for agent %s", destination, this.toString))

    if (currentJunction eq destinationJunction) {
      Agent.LOG.warn("Current and destination junctions are same for agent " + this.toString)
      return List[GeomPlanarGraphDirectedEdge](currentJunction.getOutEdges.getEdges.get(0).asInstanceOf[GeomPlanarGraphDirectedEdge])
    }

    // find the appropriate A* path between them
    val pathfinder: AStar = new AStar
    val paths: List[GeomPlanarGraphDirectedEdge] = List(pathfinder.astarPath(currentJunction, destinationJunction): _*) // (Splat the java list)

    // if the path works, lay it in
    if (paths != null && paths.size > 0) {
      return paths
    }
    else {
      if (paths == null) {
        throw new RoutingException("Internal error: Could not find a path. Path is null")
      }
      else {
        //throw new RoutingException("Agent " + this.toString + "(home " + this.getHomeID(state) + ", destination " + state.buildingIDs.inverse.get(destination) + ")" + " got an empty path between junctions " + currentJunction + " and " + destinationJunction + ". Probably the network is disconnected.")
        throw new RoutingException("Agent " + this.toString + " got an empty path between junctions " + currentJunction + " and " + destinationJunction + ". Probably the network is disconnected.")
      }
    }


    // Also need to work out what the following did (in Agent.java step() method):
//    val edge: GeomPlanarGraphEdge = path.get(0).getEdge.asInstanceOf[GeomPlanarGraphEdge]
//    Agent.setupEdge(edge, this)
//    Agent.moveToCoordinate(segment.extractPoint(currentIndex), this)

    //assert(!(segment == null && this.path.isEmpty), "Segment empty and paths null for agent " + this.toString + " whose home is " + getHomeID(state) + " and destination " + state.buildingIDs.inverse.get(this.destination))
//    return

    throw new RoutingException("Internal error, should not have got here")

  }


}
