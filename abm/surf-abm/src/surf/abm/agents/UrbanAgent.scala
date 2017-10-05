package surf.abm.agents


import com.vividsolutions.jts.geom.{Coordinate, LineString}
import com.vividsolutions.jts.linearref.LengthIndexedLine
import sim.util.geo.{GeomPlanarGraphDirectedEdge, GeomPlanarGraphEdge}
import surf.abm.environment.{Building, Junction}
import surf.abm.exceptions.RoutingException
import surf.abm.main.{GISFunctions, SurfABM, SurfGeometry}

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
  private var _destination: Option[SurfGeometry[_]] = Option(null) // (Option tells us this could be null)
  def destination() = this._destination // Accessor to destination
  private var _atDestination = false
  def atDestination() = this._atDestination

  // Used by agent to walk along line segment; assigned in setNewRoute()
  private var segment: LengthIndexedLine = null
  private var startIndex = 0.0
  private var endIndex = 0.0
  private var currentIndex = 0.0

  // A list of roads that need to be followed to reach the destination
  private var _path: List[GeomPlanarGraphDirectedEdge] = null
  protected def path(): List[GeomPlanarGraphDirectedEdge] = _path // Protected accessor

  private var indexOnPath = 0
  private var pathDirection = 1
  private var linkDirection = 1

  /**
    * Set a new destination for this agent
    *
    * @param newDest
    */
  def newDestination(newDest:Option[SurfGeometry[_]]) = {
    this._destination = newDest
    this._atDestination = false
    // Make a new path
    // Check that we have a destination to head to:
    val dest : SurfGeometry[_] = this.destination() match { // Check that destination is not None
      case Some(s) => s
      case None => throw new RoutingException("Cannot findNewPath - destination is None")
    }
    // Now find a path
    this._path = UrbanAgent.findNewPath(this.location, dest) // Set the Agent's path variable (the roads it must pass through)
  }


  /**
    * Move the agent towards their destination
    */
  def moveAlongPath(): Unit = {

    try {

      // A path should have been created already
      if (this._path == null) {
        throw new Exception(s"The path shouldn't be null (for agent ${this.toString()}. Have you called newDestination() first?")
      }

      // Check that the agent has some destination to go to
      this._destination match {

        case Some(_) => {
          // There is a destination

          this.currentIndex += (moveRate * linkDirection)

          // check to see if the progress has taken the current index beyond its goal
          // given the direction of movement. If so, proceed to the next edge
          if (this.linkDirection == 1 && this.currentIndex > this.endIndex) {
            this._atDestination = transitionToNextEdge(this.currentIndex - this.endIndex)
          }
          else if (this.linkDirection == -1 && this.currentIndex < this.startIndex) {
            this._atDestination = transitionToNextEdge(this.startIndex - this.currentIndex)
          }

          // In some cases, where the origin and destination are the same, the segment will be null here,
          // as the agent has already reached their destination without actually creating a path.
          // Check for this. If it is not the case, then just move along the path as normal.
          if (this._atDestination) {
            return
          }
          assert(this.segment != null,
            s"Internal error, segment should not be null. Debug info:\n\tAgent: $this\n\tdestination:${this._destination}\n\tcurrentIndex:${this.currentIndex}\n\tstartIndex:${this.startIndex}\n\tlinkDirection:${this.linkDirection}\n\tatDestination?:${this._atDestination}\n\tpath:${this._path}" )

          val currentPos: Coordinate = this.segment.extractPoint(currentIndex)
          this.moveToCoordinate(currentPos)
          //println(currentPos.x+","+currentPos.y+","+this.state.schedule.getSteps)

        }

        // If no destination, then thrown an exception
        case None => throw new Exception(s"${this.toString()} cannot move along their path because no destination has been set")
      }

    } // try
    catch {
      case ex: RoutingException => {
        //Agent.LOG.error("Error routing agent " + this.toString() + ". Exiting.", ex)
      }
      case ex: Exception => {
        //Agent.LOG.error("Exception in MoveAlongPath for agent " + this.toString() + ". Exiting.", ex)
      }
      state.finish
    }

  }

  /**
    * Sets this Agent up to proceed along an Edge
    *
    * @param edge the GeomPlanarGraphEdge to traverse next
    *
    */
  private def setupEdge(edge: GeomPlanarGraphEdge) {
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
    assert(this._path != null, s"The path shouldn't be null ${this.toString()}")

    indexOnPath += pathDirection
    // See if the agent has reached the end of the path. If so, reset the counters and return true.
    if ((this.pathDirection > 0 && this.indexOnPath >= this._path.size) ||
      (this.pathDirection < 0 && this.indexOnPath < 0)) {

      // (nasty way of checking that destination!=null before calling moveToCoordinate)
      this.destination() match { // Check that destination is not None
        case Some(_) => this.moveToCoordinate(this.destination().get.getGeometry.getCoordinate())
        case None => throw new RoutingException("Cannot transitiontoNextEdge - destination is None")
      }
//      this.moveToCoordinate(this.destination.get.getGeometry.getCoordinate)
      this.startIndex = -1.0
      this.endIndex = -1.0
      this.currentIndex = -1.0
      this._destination = None
      this._path = null
      this.indexOnPath = -1
      return true
    }

    // move to the next edge in the path
    val edge: GeomPlanarGraphEdge = this._path(indexOnPath).getEdge.asInstanceOf[GeomPlanarGraphEdge]
    assert(edge != null)
    this.setupEdge(edge)
    val speed: Double = residualMove * linkDirection
    currentIndex += speed

    // check to see if the progress has taken the current index beyond its goal
    // given the direction of movement. If so, proceed to the next edge
    if (linkDirection == 1 && currentIndex > endIndex) {
      return transitionToNextEdge(currentIndex - endIndex)
    }
    else if (linkDirection == -1 && currentIndex < startIndex) {
      return transitionToNextEdge(startIndex - currentIndex)
    }

    // If here then don't need to transition onto another edge and haven't
    // reached the destination yet.
    return false
  }


}

object UrbanAgent {

  /**
    * Find a path from an origin to a destination.
    *
    * <p>This is static so that objects other than Agents can use it.</p>
    *
    * @param start The start of the path
    * @param end The end of the path
    * @param caller The (optional) object who wants the path. Useful for debugging, but doesn't change the behaviour
    */
  def findNewPath( start:SurfGeometry[_ <: Any] ,
                   end:SurfGeometry[_ <: Any],
                   caller : Option[_] = None
                 ) : List[GeomPlanarGraphDirectedEdge] = {

    // TODO - break this method up and test it properly. (remember Sam's advice - each function should have simple, clear inputs and outputs

    //var thePath: List[GeomPlanarGraphDirectedEdge] = null

    /* First, find the nearest node to the start. We're very unlikely to be at the exact location of a Node,
    so find the nearest junction. */

    val nearestJunctionToCurrent: SurfGeometry[Junction] = GISFunctions.findNearestObject[Junction](start, SurfABM.junctions)
    val currentNode = SurfABM.network.findNode(nearestJunctionToCurrent.getGeometry.getCoordinate)

    assert(currentNode != null, s"Could not find the current junction for the object ${caller.getOrElse("[Not Provided]")}")

    /* Now find the junction that is closest to the destination */
    val nearestJunctionToDestination: SurfGeometry[Junction] = GISFunctions.findNearestObject[Junction](end, SurfABM.junctions)
    val destinationNode = SurfABM.network.findNode(nearestJunctionToDestination.getGeometry.getCoordinate)

    assert(destinationNode != null, s"Could not find a junction for the destination ${end.toString} for the object ${caller.getOrElse("[Not Provided]")}")

    // Current and destination junctions are the same. Just return a single edge as the path.
    if (currentNode eq destinationNode) {
      //Agent.LOG.warn(s"Current and destination junctions are same for the object ${caller.getOrElse("[Not Provided]")}")
      return List[GeomPlanarGraphDirectedEdge](currentNode.getOutEdges.getEdges.get(0).asInstanceOf[GeomPlanarGraphDirectedEdge])

    }

    // find the appropriate A* path between them
    val pathfinder: AStar = new AStar
    val paths: List[GeomPlanarGraphDirectedEdge] = List(pathfinder.astarPath(currentNode, destinationNode): _*) // (Splat the java list)

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
        throw new RoutingException(
          s"Object ${caller.getOrElse("[Not Provided]")} got an empty path between junctions\n\t" +
            s"Current: ${nearestJunctionToCurrent.toString} \n\tDestination: ${nearestJunctionToDestination.toString}.\n\t" +
            "The network is probably disconnected. Run disconnected-islands plugin in QGIS"
        )
      }
    }

  }






}
