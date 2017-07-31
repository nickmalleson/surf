package surf.abm.agents.abbf.activities

import surf.abm.agents.abbf.activities.ActivityTypes.WORKING
import surf.abm.agents.{Agent, UrbanAgent}
import surf.abm.agents.abbf.{ABBFAgent, Place, TimeProfile}


/**
  * An activity (of type [[surf.abm.agents.abbf.activities.ActivityTypes.WORKING]]) that causes the agent to
  * go to their work place.
  */
case class WorkActivity(
                    override val timeProfile: TimeProfile,
                    override val agent: ABBFAgent,
                    override val place: Place)
  extends FixedActivity (
    activityType = WORKING,
    timeProfile = timeProfile,
    agent = agent,
    place = Place(agent.home, WORKING))
    with Serializable
{

  // These variables define the different things that the agent could be doing in order to satisfy the work activity
  // (Note: in SleepActivity, these are defined as case classes that extend a sealed trait, but this way is probably
  // more efficient)

  private val WORKING = 1
  private val TRAVELLING = 2
  private val INITIALISING = 3
  private var currentAction = INITIALISING

  /**
    * This makes the agent actually perform the activity. They can either be at work or travelling there
    *
    * @return True if at work, false otherwise
    */
  override def performActivity(): Boolean = {

    this.currentAction match {

      case INITIALISING => {
        Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]$${agent.toString()} is initialising WorkActivity")
        // See if the agent is at work
        if (this.place.location.equalLocation(this.agent.location())) {
          Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]$${agent.toString()} is at work. Starting to work.")
          currentAction = WORKING // Next iteration the agent will start to work
        }
        else {
          Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]$${agent.toString()} is not at work. Travelling there.")
          this.agent.newDestination(Option(this.place.location))
          currentAction = TRAVELLING
        }

      }

      case TRAVELLING => {
        if (this.agent.atDestination()) {
          Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]$${agent.toString()} has reached their workplace. Starting to work")
          currentAction = WORKING
        }
        else {
          Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]$${agent.toString()} is travelling to work.")
          agent.moveAlongPath()
        }
      }

      case WORKING => {
        Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]$${agent.toString()} is working")
        return true
      }

    }
    // Only get here if the agent isn't working, so must return false.
    return false
  }

  override def activityChanged(): Unit = {
    this.currentAction = INITIALISING
    this._currentIntensityDecrease = 0d
  }



}
