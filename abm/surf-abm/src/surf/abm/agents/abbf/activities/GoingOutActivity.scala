package surf.abm.agents.abbf.activities

import surf.abm.agents.{Agent, UrbanAgent}
import surf.abm.agents.abbf.{ABBFAgent, Place, TimeProfile}
import surf.abm.agents.abbf.activities.ActivityTypes.GOING_OUT


/**
  * An activity (of type [[surf.abm.agents.abbf.activities.ActivityTypes.GOING_OUT]]) that causes the agent to
  * travel to bars or pubs
  */
class GoingOutActivity (
                       override val timeProfile: TimeProfile,
                       override val agent: ABBFAgent)
  extends FlexibleActivity(GOING_OUT, timeProfile, agent)  with Serializable
{

  // These variables define the different things that the agent could be doing in order to satisfy the work activity
  // (Note: in SleepActivity, these are defined as case classes that extend a sealed trait, but this way is probably
  // more efficient)

  private val GOING_OUT = 1
  private val TRAVELLING = 2
  private val INITIALISING = 3
  private var currentAction = INITIALISING
  private val place : Place = null // start with a null place

  /**
    * This makes the agent actually perform the activity.
    *
    * @return True if the agent has performed this activity, false if they have not (e.g. if they are still travelling
    *         to a particular destination).
    */
  override def performActivity(): Boolean = {

    this.currentAction match {

      case INITIALISING => {
        Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]${agent.toString()} is initialising GoingOut")
        // See if the agent is in a pub/bar
        if (this.place.location.equalLocation(
          this.agent.location())) {
          Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]${agent.toString()} has reached a pub/bar. GoingOut starts.")
          currentAction = GOING_OUT // Next iteration the agent will start going out.
        }
        else {
          Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]${agent.toString()} is not at a bar/pub yet. Travelling there.")
          this.agent.newDestination(Option(this.place.location))
          currentAction = TRAVELLING
        }

      }

      case TRAVELLING => {
        if (this.agent.atDestination()) {
          Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]${agent.toString()} has reached a bar/pub. GoingOut starts.")
          currentAction = GOING_OUT
        }
        else {
          Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]${agent.toString()} is travelling to a bar/pub.")
          agent.moveAlongPath()
        }
      }

      case GOING_OUT => {
        Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]${agent.toString()} is going out.")
        return true
      }

    }
    // Only get here if the agent isn't going out, so must return false.
    return false

  }

  override def activityChanged(): Unit = {
    this.currentAction = INITIALISING
    this._currentIntensityDecrease = 0d
  }

}
