package surf.abm.agents.abbf

import sim.engine.SimState

import surf.abm.agents.{Agent, UrbanAgent}
import surf.abm.environment.Building
import surf.abm.exceptions.RoutingException
import surf.abm.{SurfABM, SurfGeometry}
import surf.abm.agents.abbf.activities.Activity

/**
  *
  * @param state A pointer to the main model instance (<code>SurfABM</code>)
  * @param home The agent's home (starting location). Usually a <code>Building</code>
  *             where (e.g.) they live, but not necessarily. The agent's initial
  *             location is set to be <code>home</code>
  * @param activities A map of the [[surf.abm.agents.abbf.activities.Activity]]s that are driving an agent, along
  *                   with the current intensity of the activity.
  */
class ABBFAgent(val state:SurfABM, val home:SurfGeometry[Building], val activities: Map[Activity,Double]) extends UrbanAgent(state, home) {

 // Temporary variables while the agent just walks from home and back.
  var goingHome = false
  // Find where the agent works (specified temporarily in the config file)
  val workBuilding = SurfABM.buildingIDGeomMap(SurfABM.conf.getInt(SurfABM.ModelConfig+".WorkAddress"))


  override def step(state: SimState): Unit = {

    // TODO implement step!!

    // Temporary code
    try {
      if (this.destination.isEmpty || this.atDestination) {

        if (goingHome) {
          Agent.LOG.debug("Agent "+ this.id.toString() + " has arrived home. Going to work")
          goingHome = false
          this._destination = Option(workBuilding)
          this._atDestination = false
          this.findNewPath() // Set the Agent's path variable (the roads it must pass through)
        }
        else {
          Agent.LOG.debug("Agent "+ this.id.toString() + " has arrived at work. Going home")
          goingHome = true
          this._destination = Option(this.home)
          this._atDestination = false
          this.findNewPath() // Set the Agent's path variable (the roads it must pass through)

        }

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

}

object ABBFAgent {
  def apply(state: SurfABM, home: SurfGeometry[Building], activities: Map[Activity,Double]): ABBFAgent =
    new ABBFAgent(state, home, activities)
}
