package surf.abm.agents

import com.vividsolutions.jts.geom.Coordinate
import org.apache.log4j.Logger
import sim.engine.Steppable
import sim.util.geo.{PointMoveTo, MasonGeometry}
import surf.abm.{SurfABM, SurfGeometry}
import surf.abm.environment.Building

/**
  * Base class for all agents
  * Created by geonsm on 08/04/2016.
  */
@SerialVersionUID(1L)
abstract class Agent (state:SurfABM, home:SurfGeometry[Building]) extends Steppable with Serializable {
  // The location where the agent currently is. Begins at 'home'.
  // It's protected, with a public accessor.
  protected var _location: MasonGeometry = home // (the underscore denotes protected)
  def location() = this._location // accessor to location
  //protected def location_=(g:MasonGeometry) { _location = g } // protected mutator

  // Set a default move rate
  protected val moveRate : Double = Agent.baseMoveRate

  // Convenience for moving a point. Don't want to create this object each iteration.
  val pmt : PointMoveTo = new PointMoveTo()

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

  // Initialise the logger. NOTE: will have one logger per Agent
  val LOG: Logger = Logger.getLogger(this.getClass);

  /** The basic (walking) rate that agents move at. */
  val baseMoveRate = SurfABM.conf.getDouble("BaseMoveRate")


}