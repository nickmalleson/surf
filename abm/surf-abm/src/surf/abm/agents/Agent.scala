package surf.abm.agents


import org.apache.log4j.Logger
import sim.engine.{SimState, Steppable}
import sim.util.geo.MasonGeometry
import surf.abm.SurfABM

/**
  * Main superclass for Agents
  * @param state A pointer to the main model instance (<code>SurfABM</code>)
  * @param home The agent's home (starting location). Usually a <code>Building</code>
  *             where (e.g.) they live, but not necessarily. The agent's initial
  *             location is set to be <code>home</code>
  */
class Agent (state:SurfABM, home:MasonGeometry) extends Steppable with Serializable {

  // Initialise the logger
  private val LOG: Logger = Logger.getLogger(this.getClass);

  // The location where the agent currently is. Begins at 'home'
  // It's protected, with a public accessor.
  protected var _location: MasonGeometry = home // (the underscore denotes protected)
  def location() : MasonGeometry = this._location // accessor to location
  //protected def location_=(g:MasonGeometry) { _location = g } // protected mutator

  //val home : MasonGeometry = null // Public home variable

  override def step(state: SimState): Unit = {

    //LOG.info("Stepping an agent: "+this.toString())

  }


}
