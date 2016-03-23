package surf.abm.agents


import java.awt.print.Book

import com.vividsolutions.jts.geom.Coordinate
import org.apache.log4j.Logger
import sim.engine.{SimState, Steppable}
import sim.util.geo.{PointMoveTo}
import surf.abm.{SurfGeometry, SurfABM}
import scala.math._

/**
  * Main superclass for Agents
  *
  * @param state A pointer to the main model instance (<code>SurfABM</code>)
  * @param home The agent's home (starting location). Usually a <code>Building</code>
  *             where (e.g.) they live, but not necessarily. The agent's initial
  *             location is set to be <code>home</code>
  */
@SerialVersionUID(1L)
class Agent (state:SurfABM, home:SurfGeometry) extends Steppable with Serializable {

  // The location where the agent currently is. Begins at 'home'
  // It's protected, with a public accessor.
  protected var _location: SurfGeometry = home // (the underscore denotes protected)
  def location() : SurfGeometry = this._location // accessor to location
  //protected def location_=(g:MasonGeometry) { _location = g } // protected mutator

  // For these simple agents the move rate is all the same
  protected val moveRate : Double = Agent.baseMoveRate

  // Convenience for moving a point. Don't want to create this object each iteration.
  val pmt : PointMoveTo = new PointMoveTo()

  /**
    * In this basic implementation, the agents just do a random walk
    *
    * @param state
    */
  override def step(state: SimState): Unit = {

    //LOG.info("Stepping an agent: "+this.toString())

    // Do a random walk
    val current : Coordinate = this.location.getGeometry.getCoordinate
    def r(n:Double) : Double =  { // Randomize the input number by +- the moveRate
      n + ( ( this.state.random.nextDouble() * moveRate * 2 ) - moveRate )
    }
    val newCoord = new Coordinate( r(current.x), r(current.y) )
    // Check that the new position is correct
    assert( {
      // Calculate the Euclidean distance moved, and check it is less than the
      // maximum Eeuclidean distance given the moveRate
      val dist = sqrt( pow(current.x-newCoord.x,2) + pow(current.y-newCoord.y,2) )
      dist <= sqrt(pow(moveRate,2) + pow(moveRate,2) )
    }, s"Agent has moved too far.\n\t" +
        s"Move rate: ${moveRate}, dist: ${sqrt( pow(current.x-newCoord.x,2) + pow(current.y-newCoord.y,2) )},\n\t" +
        s"Coordinates: ${current} - ${newCoord}."
    )
    this.moveToCoordinate(newCoord)

  }

  /**
    * Move the agent to the given coordinate
 *
    * @param c The Coordinate to move to
    * @return Unit
    */
  def moveToCoordinate(c : Coordinate) : Unit = {
    this.pmt.setCoordinate(c)
    this.location.getGeometry().apply(this.pmt)
    this.location.geometry.geometryChanged()
  }

}

@SerialVersionUID(1L)
object Agent extends Serializable {
  // Initialise the logger. NOTE: will have one logger per
  private val LOG: Logger = Logger.getLogger(this.getClass);

  /** The basic (walking) rate that agents move at. */
  val baseMoveRate = SurfABM.conf.getDouble("BaseMoveRate")

}
