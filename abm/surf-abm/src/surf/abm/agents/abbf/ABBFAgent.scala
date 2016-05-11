package surf.abm.agents.abbf

import java.time.LocalDateTime

import sim.engine.SimState
import surf.abm.agents.abbf.activities.ActivityTypes.{ActivityType, WORKING}
import surf.abm.agents.abbf.activities.FixedActivity
import surf.abm.agents.{Agent, UrbanAgent}
import surf.abm.environment.Building
import surf.abm.exceptions.RoutingException
import surf.abm.{SurfABM, SurfGeometry}

/**
  * Created by nick on 09/05/2016.
  */
class ABBFAgent(state:SurfABM, home:SurfGeometry[Building]) extends UrbanAgent(state, home) {

  var goingHome = false
  // Find where the agent works (specified temporarily in the config file)
  val workBuilding = SurfABM.buildingIDGeomMap(SurfABM.conf.getInt(SurfABM.ModelConfig+".WorkAddress"))

  // Temporarily define the activities that this agent can do (later this will be done by reading data)

  // WORKING

  // Work place is a building in town
  val workPlace = Place (
    location = SurfABM.buildingIDGeomMap(SurfABM.conf.getInt(SurfABM.ModelConfig+".WorkAddress")),
    activityType = WORKING,
    openingTimes = null // Assume it's open all the time
  )
  // Work time profile is 0 before 6 and after 10, and 1 between 10-4
  val workTimeProfile = TimeProfile(Array( (6d,0d), (10d,1d), (16d,1d), (22d,0d) ) )
  val workActivity = new FixedActivity(
    activityType = WORKING,
    timeProfile = workTimeProfile,
    place = workPlace )

  // SHOPPING
  XXX

  // BEING AT HOME

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
  def apply(state: SurfABM, home: SurfGeometry[Building]): ABBFAgent = new ABBFAgent(state, home)
}
