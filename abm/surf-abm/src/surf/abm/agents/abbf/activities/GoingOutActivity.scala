package surf.abm.agents.abbf.activities

import sim.engine.SimState
import surf.abm.agents.{Agent, UrbanAgent}
import surf.abm.agents.abbf.{ABBFAgent, Place, TimeProfile}
import surf.abm.agents.abbf.activities.ActivityTypes.GOING_OUT
import surf.abm.main.{GISFunctions, SurfABM, SurfGeometry}
import surf.abm.environment.Building


/**
  * An activity (of type [[surf.abm.agents.abbf.activities.ActivityTypes.GOING_OUT]]) that causes the agent to
  * travel to bars or pubs
  */
case class GoingOutActivity (
                       override val timeProfile: TimeProfile,
                       override val agent: ABBFAgent,
                       state: SimState)
  extends FlexibleActivity(GOING_OUT, timeProfile, agent)  with Serializable
{

  // These variables define the different things that the agent could be doing in order to satisfy the work activity
  // (Note: in SleepActivity, these are defined as case classes that extend a sealed trait, but this way is probably
  // more efficient)

  private val AT_THE_EVENT = 1
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
        Agent.LOG.debug(agent, "is initialising GoingOut")
        // See if the agent is in a pub/bar
        if (this.place.location.equalLocation(
          this.agent.location())) {
          Agent.LOG.debug(agent, "has reached a pub/bar. GoingOut starts.")
          currentAction = AT_THE_EVENT // Next iteration the agent will start going out.
        }
        else {
          Agent.LOG.debug(agent, "is not at a bar/pub yet. Travelling there.")
          this.agent.newDestination(Option(this.place.location))
          currentAction = TRAVELLING
        }

      }

      case TRAVELLING => {
        if (this.agent.atDestination()) {
          Agent.LOG.debug(agent, "has reached a bar/pub. GoingOut starts.")
          currentAction = AT_THE_EVENT
        }
        else {
          Agent.LOG.debug(agent, "is travelling to a bar/pub.")
          agent.moveAlongPath()
        }
      }

      case AT_THE_EVENT => {
        Agent.LOG.debug(agent, "is going out.")
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
  /**
    * The amount that the dinner activity should increase at each iteration
    * @return
    */
  override def backgroundIncrease(): Double = {
    return 1d / (25d * SurfABM.ticksPerDay)
  }

  /**
    * The amount that the dinner activity will go down by if an agent is having dinner.
    * @return
    */
  override def reduceActivityAmount(): Double = {
    return 7d / (3d * SurfABM.ticksPerDay)
  }
}
