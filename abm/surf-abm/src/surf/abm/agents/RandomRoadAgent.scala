package surf.abm.agents

import sim.engine.SimState
import surf.abm.environment.Building
import surf.abm.exceptions.RoutingException
import surf.abm.main.{SurfABM, SurfGeometry}

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

        Agent.LOG.info(this, "is looking for a new destination")
        this.newDestination(Option(SurfABM.getRandomBuilding(state)))
        //this._destination = Option(SurfABM.getRandomBuilding(state))
        //this._atDestination = false
        //this.findNewPath() // Set the Agent's path variable (the roads it must pass through)
      }
      assert(this.path() != null, "The path shouldn't be null (for agent %s)".format(this.id))
      this.moveAlongPath()
    }
    catch {
      case ex: RoutingException => {
        
        Agent.LOG.error(this, "Routing error, Exitting.", ex)
        state.finish
        
      }
    }
  }

  override def toString() = "RandomRoadAgent %d".format(this.id())

}
