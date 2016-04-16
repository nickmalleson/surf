package surf.abm.agents

import sim.engine.SimState
import surf.abm.exceptions.RoutingException
import surf.abm.{SurfGeometry, SurfABM}
import surf.abm.environment.Building

/**
  * An agent who walks from one randomly chosen building to another
  *
  * Created by geonsm on 08/04/2016.
  */
@SerialVersionUID(1L)
class RandomRoadAgent(state:SurfABM, home:SurfGeometry[Building]) extends UrbanAgent(state,home) with Serializable {



  override def step(s: SimState) {

    try {
      if (this.destination.isEmpty || this.atDestination) {
        Agent.LOG.debug("Agent "+ this.id.toString() + " is looking for a new destination")
        this._destination = Option(SurfABM.getRandomBuilding(state))
        this._atDestination = false
        this.findNewPath() // Set the Agent's path variable (the roads it must pass through)
      }
      assert(this.path != null, "The path shouldn't be null (for agent %s)".format(this.id))
      this.moveAlongPath()
    }
    catch {
      case ex: RoutingException => {
        Agent.LOG.error("Error routing agent " + this.toString() + ". Exitting.", ex)
        state.finish
      }
    }
  }

  override def toString() = "RandomRoadAgent %d".format(this.id())

}
