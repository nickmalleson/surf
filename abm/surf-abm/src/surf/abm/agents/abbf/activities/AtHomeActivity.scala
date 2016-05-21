package surf.abm.agents.abbf.activities

import org.apache.commons.collections.functors.NullIsFalsePredicate
import surf.abm.agents.{Agent, UrbanAgent}
import surf.abm.agents.abbf.{ABBFAgent, Place, TimeProfile}
import surf.abm.agents.abbf.activities.ActivityTypes.{AT_HOME, WORKING}
import surf.abm.exceptions.RoutingException
import surf.abm.main.SurfABM

/**
  * An activity of type [[surf.abm.agents.abbf.activities.ActivityTypes.AT_HOME]] that causes the agent to go home and
  * hang around there.
  */
case class AtHomeActivity(
                    override val timeProfile: TimeProfile,
                    override val agent: ABBFAgent)
  extends FixedActivity (AT_HOME, timeProfile, agent, Place(agent.home, AT_HOME))
{

  // Temporary variables while the agent just walks from home and back.
  private var goingHome = false
  // Find where the agent works (specified temporarily in the config file)
  val workBuilding = SurfABM.buildingIDGeomMap(SurfABM.conf.getInt(SurfABM.ModelConfig+".WorkAddress"))

  /**
    * This makes the agent actually perform the activity.
    *
    * @return True if the agent has performed this activity, false if they have not (e.g. if they are still travelling
    *         to a particular destination).
    */
  override def performActivity(): Boolean = {

    // Temporary code
      if (agent.destination.isEmpty || agent.atDestination) {

        if (goingHome) {
          Agent.LOG.debug("Agent "+ agent.id.toString() + " has arrived home. Going to Somewhere else")
          goingHome = false
          agent.newDestination(Option(workBuilding))

        }
        else {
          Agent.LOG.debug("Agent "+ agent.id.toString() + " has arrived at work. Going home")
          goingHome = true
          agent.newDestination(Option(agent.home))
        }

      }

      agent.moveAlongPath()

      // TODO: temporarily always returning true. This will need to be false if the agent is travelling, and true if they are at home.
      return true
    }



}
