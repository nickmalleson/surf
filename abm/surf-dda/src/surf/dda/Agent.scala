package surf.dda

import sim.engine.{SimState, Steppable}
import sim.portrayal.simple.OvalPortrayal2D

/**
  * Created by nick on 25/11/2016.
  */
class Agent(state: SurfDDA) extends OvalPortrayal2D with Steppable {

  // A unique id for each agent with a public accessor.
  private val _id : Int = Agent.uniqueID()
  def id() : Int = this._id

  def step(state: SimState): Unit = {
    LOG.debug("Ant "+this.id()+" stepping")
  }

}

object Agent {
  /** A unique ID that can be given to each agent */
  private var _uniqueID : Int = 0
  private def uniqueID() : Int = {
    _uniqueID+=1
    _uniqueID
  }

  def apply(state:SurfDDA) : Agent = new Agent(state)

}
