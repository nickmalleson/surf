package surf.abm.agents.abbf.activities

import surf.abm.agents.{Agent, UrbanAgent}
import surf.abm.agents.abbf.{ABBFAgent, Place, TimeProfile}
import surf.abm.agents.abbf.activities.ActivityTypes.DINNER


/**
  * An activity (of type [[surf.abm.agents.abbf.activities.ActivityTypes.DINNER]]) that causes the agent to
  * travel to restaurants, pubs or fast food places
  */
class DinnerActivity (
                       override val timeProfile: TimeProfile,
                       override val agent: ABBFAgent)
  extends FlexibleActivity(DINNER, timeProfile, agent)  with Serializable
{

  // These variables define the different things that the agent could be doing in order to satisfy the work activity
  // (Note: in SleepActivity, these are defined as case classes that extend a sealed trait, but this way is probably
  // more efficient)

  private val EATING = 1
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
        Agent.LOG.debug(s"${agent.toString()} is initialising EatingDinner")
        // See if the agent is in a lunch place
        if (this.place.location.equalLocation(
          this.agent.location())) {
          Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]$${agent.toString()} has reached a restaurant/pub. Starting dinner.")
          currentAction = EATING // Next iteration the agent will start to have dinner.
        }
        else {
          Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]$${agent.toString()} is not at a restaurant/pub yet. Travelling there.")
          this.agent.newDestination(Option(this.place.location))
          currentAction = TRAVELLING
        }

      }

      case TRAVELLING => {
        if (this.agent.atDestination()) {
          Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]$${agent.toString()} has reached a restaurant/pub. Starting dinner")
          currentAction = EATING
        }
        else {
          Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]$${agent.toString()} is travelling to a restaurant/pub.")
          agent.moveAlongPath()
        }
      }

      case EATING => {
        Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]${agent.toString()} is having dinner")
        return true
      }

    }
    // Only get here if the agent isn't eating, so must return false.
    return false

  }

  override def activityChanged(): Unit = {
    this.currentAction = INITIALISING
    this._currentIntensityDecrease = 0d
  }

}
