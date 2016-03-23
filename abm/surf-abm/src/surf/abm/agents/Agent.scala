package surf.abm.agents


import com.vividsolutions.jts.geom.Coordinate
import org.apache.log4j.Logger
import sim.engine.{SimState, Steppable}
import sim.util.geo.{PointMoveTo, MasonGeometry}
import surf.abm.SurfABM

/**
  * Main superclass for Agents
  *
  * @param state A pointer to the main model instance (<code>SurfABM</code>)
  * @param home The agent's home (starting location). Usually a <code>Building</code>
  *             where (e.g.) they live, but not necessarily. The agent's initial
  *             location is set to be <code>home</code>
  */
@SerialVersionUID(1L)
class Agent (state:SurfABM, home:MasonGeometry)
  extends MasonGeometry with Steppable with Serializable {

  // The location where the agent currently is. Begins at 'home'
  // It's protected, with a public accessor.
  protected var _location: MasonGeometry = home // (the underscore denotes protected)
  def location() : MasonGeometry = this._location // accessor to location
  //protected def location_=(g:MasonGeometry) { _location = g } // protected mutator

  // For these simple agents the move rate is all the same
  protected val moveRate : Double = Agent.baseMoveRate

  // Convenience for moving a point. Don't want to create this object each iteration.
  val pmt : PointMoveTo = new PointMoveTo();

  /**
    * In this basic implementation, the agents just do a random walk
    *
    * @param state
    */
  override def step(state: SimState): Unit = {

    //LOG.info("Stepping an agent: "+this.toString())

    // Do a random walk
    val current : Coordinate = this.location.getGeometry.getCoordinate
    def r(n:Double) : Double =  { // Randomize the input number
      n + (2 * this.state.random.nextInt(this.moveRate.asInstanceOf[Int]) ) -
        this.moveRate // Take away move rate again to allow for negative movements
    }
    val newCoord = new Coordinate( r(current.x), r(current.y) )
    this.moveToCoordinate(newCoord)

  }

  /**
    * Move the agent to the given coordinate
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
