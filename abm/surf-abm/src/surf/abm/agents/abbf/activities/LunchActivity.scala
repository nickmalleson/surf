package surf.abm.agents.abbf.activities

import sim.engine.SimState
import surf.abm.agents.{Agent, UrbanAgent}
import surf.abm.agents.abbf.{ABBFAgent, Place, TimeProfile}
import surf.abm.agents.abbf.activities.ActivityTypes.LUNCHING
import surf.abm.main.{GISFunctions, SurfABM, SurfGeometry}
import surf.abm.environment.Building

/**
  * An activity (of type [[surf.abm.agents.abbf.activities.ActivityTypes.LUNCHING]]) that causes the agent to
  * travel to lunch places
  */
case class LunchActivity(
                         override val timeProfile: TimeProfile,
                         override val agent: ABBFAgent,
                         state: SimState)
  extends FlexibleActivity(LUNCHING, timeProfile, agent)  with Serializable
{

  // These variables define the different things that the agent could be doing in order to satisfy the lunch activity

  private val HAVING_LUNCH = 1
  private val TRAVELLING = 2
  private val INITIALISING = 3
  private var currentAction = INITIALISING

  private val place = Place(
    location = null,
    activityType = LUNCHING,
    openingTimes = Array(Place.makeOpeningTimes(11.0, 16.0))
    // TimeIntensity of lunch is 0 outside lunch hours, but not BackgroundIntensity, so might be necessary anyway
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
        Agent.LOG.debug(agent, "is initialising LunchActivity")
        val lunchLocation: SurfGeometry[Building] = GISFunctions.findNearestObject[Building](this.agent.location(), SurfABM.lunchGeoms, true, state)
        this.place.location = lunchLocation
        // See if the agent is in a lunch place
        if (this.place.location.equalLocation(
          this.agent.location())) {
          Agent.LOG.debug(agent, "has reached a lunch place. Starting lunch.")
          currentAction = HAVING_LUNCH // Next iteration the agent will start to have lunch.
        }
        else {
          Agent.LOG.debug(agent, "is not at a lunch place yet. Travelling there.")
          this.agent.newDestination(Option(this.place.location))
          currentAction = TRAVELLING
        }

      }

      case TRAVELLING => {
        if (this.agent.atDestination()) {
          Agent.LOG.debug(agent, "has reached a lunch place. Starting lunch")
          currentAction = HAVING_LUNCH
        }
        else {
          Agent.LOG.debug(agent, "is travelling to a lunch place.")
          agent.moveAlongPath()
        }
      }

      case HAVING_LUNCH => {
        Agent.LOG.debug(agent, "is having lunch")
        return true
      }

    }
    // Only get here if the agent isn't lunching, so must return false.
    return false

  }

  override def activityChanged(): Unit = {
    this.currentAction = INITIALISING
    this._currentIntensityDecrease = 0d
    //throw new NotImplementedError("Have not implemented Shopping activity yet")
  }

  /**
    * The amount that the lunch activity should increase at each iteration
    * @return
    */
  override def backgroundIncrease(): Double = {
    return 1d / (15d * SurfABM.ticksPerDay)
  }

  /**
    * The amount that the lunch activity will go down by if an agent is lunching.
    * @return
    */
  override def reduceActivityAmount(): Double = {
    return 80d / (3d * SurfABM.ticksPerDay)
  }
}
