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

  // A unique id for each agent with a public accessor.
  private val _id : Int = Agent.uniqueID()
  def id() : Int = this._id

  // The location where the agent currently is. Begins at 'home'. It is protected, with a public accessor.
  // Important to create a new MasonGeometry, not use the home geometry. Otherwise bad things happen!
  protected var _location: SurfGeometry[_ <: Any] =
    SurfGeometry(new MasonGeometry(this.home.getGeometry().getCentroid()), null)
  def location() = this._location // accessor to location
  //protected def location_=(g:MasonGeometry) { _location = g } // protected mutator

  // Set a default move rate
  protected val moveRate : Double = Agent.baseMoveRate

  // Convenience for moving a point. Don't want to create this object each iteration.
  // val pmt : PointMoveTo = new PointMoveTo()

  /**
    * Move the agent to the given coordinate
    *
    * @param c The Coordinate to move to
    * @return Unit
    */
  def moveToCoordinate(c : Coordinate) : Unit = {
    //this.pmt.setCoordinate(c)
    //this.location.getGeometry().apply(this.pmt)
    //this.location.geometry.geometryChanged()
    val p = new PointMoveTo()
    p.setCoordinate(c)
    this.location.getGeometry().apply(p)
    this.location.geometry.geometryChanged()

  }

  override def toString() = "Agent %s".format(this.id())

}

@SerialVersionUID(1L)
object Agent extends Serializable {

  // Initialise the logger. NOTE: will have one logger per Agent
  val LOG: Logger = Logger.getLogger(this.getClass);

  /** The basic (walking) rate that agents move at. */
  val baseMoveRate = SurfABM.conf.getDouble("BaseMoveRate")

  /** A unique ID that can be given to each agent */
  private var _uniqueID : Int= 0
  private def uniqueID() : Int = {
    _uniqueID+=1
    _uniqueID
  }

}