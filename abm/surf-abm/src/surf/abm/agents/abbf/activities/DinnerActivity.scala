package surf.abm.agents.abbf.activities

import sim.engine.SimState
import surf.abm.agents.{Agent, UrbanAgent}
import surf.abm.agents.abbf.{ABBFAgent, Place, TimeProfile}
import surf.abm.agents.abbf.activities.ActivityTypes.DINNER
import surf.abm.main.{GISFunctions, SurfABM, SurfGeometry}
import surf.abm.environment.Building


/**
  * An activity (of type [[surf.abm.agents.abbf.activities.ActivityTypes.DINNER]]) that causes the agent to
  * travel to restaurants, pubs or fast food places
  */
case class DinnerActivity (
                       override val timeProfile: TimeProfile,
                       override val agent: ABBFAgent,
                       state: SimState)
  extends FlexibleActivity(DINNER, timeProfile, agent)  with Serializable
{

  // These variables define the different things that the agent could be doing in order to satisfy the dinner activity

  private val HAVING_DINNER = 1
  private val TRAVELLING = 2
  private val INITIALISING = 3
  private var currentAction = INITIALISING

  private val place = Place(
    location = null,
    activityType = DINNER,
    openingTimes = Array(Place.makeOpeningTimes(17.0, 23.0))
    // TimeIntensity of dinner is 0 outside dinner hours, but not BackgroundIntensity, so might be necessary anyway
  )

  /**
    * This makes the agent actually perform the activity.
    *
    * @return True if the agent has performed this activity, false if they have not (e.g. if they are still travelling
    *         to a particular destination).
    */
  override def performActivity(): Boolean = {

    this.currentAction match {

      case INITIALISING => {
        Agent.LOG.debug(agent, "is initialising DinnerActivity")
        val dinnerLocation: SurfGeometry[Building] = GISFunctions.findNearestObject[Building](this.agent.location(), SurfABM.dinnerGeoms, true, state)
        this.place.location = dinnerLocation
        // See if the agent is in a dinner place
        if (this.place.location.equalLocation(
          this.agent.location())) {
          Agent.LOG.debug(agent, "has reached a restaurant. Starting dinner.")
          //Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]$${agent.toString()} has reached a restaurant/pub. Starting dinner.")
          currentAction = HAVING_DINNER // Next iteration the agent will start to have dinner.
        }
        else {
          Agent.LOG.debug(agent, "is not at a restaurant yet. Travelling there.")
          //Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]$${agent.toString()} is not at a restaurant/pub yet. Travelling there.")
          this.agent.newDestination(Option(this.place.location))
          currentAction = TRAVELLING
        }

      }

      case TRAVELLING => {
        if (this.agent.atDestination()) {
          Agent.LOG.debug(agent, "has reached a restaurant. Starting dinner")
          //Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]$${agent.toString()} has reached a restaurant/pub. Starting dinner")
          currentAction = HAVING_DINNER
        }
        else {
          Agent.LOG.debug(agent, "is travelling to a restaurant.")
          //Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]$${agent.toString()} is travelling to a restaurant/pub.")
          agent.moveAlongPath()
        }
      }

      case HAVING_DINNER => {
        Agent.LOG.debug(agent, "is having dinner")
        //Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]${agent.toString()} is having dinner")
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

  /**
    * The amount that the dinner activity should increase at each iteration
    * @return
    */
  override def backgroundIncrease(): Double = {
    return 1d / (15d * SurfABM.ticksPerDay)
  }

  /**
    * The amount that the dinner activity will go down by if an agent is having dinner.
    * @return
    */
  override def reduceActivityAmount(): Double = {
    return 16d / SurfABM.ticksPerDay
  }

}
