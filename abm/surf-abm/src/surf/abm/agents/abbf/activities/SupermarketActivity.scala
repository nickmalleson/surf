package surf.abm.agents.abbf.activities

import org.apache.log4j.Logger
import sim.engine.SimState
import surf.abm.agents.Agent
import surf.abm.agents.abbf.{ABBFAgent, Place, TimeProfile}
import surf.abm.agents.abbf.activities.ActivityTypes.SUPERMARKET
import surf.abm.agents.abbf.occupations.{CommuterAgent, RetiredAgent}
import surf.abm.main.{GISFunctions, SurfABM, SurfGeometry}
import surf.abm.environment.Building


/**
  * An activity (of type [[surf.abm.agents.abbf.activities.ActivityTypes.SUPERMARKET]]) that causes the agent to
  * travel to supermarkets and convenience stores.
  */
case class SupermarketActivity(
                         override val timeProfile: TimeProfile,
                         override val agent: ABBFAgent,
                         state: SimState)
  extends FlexibleActivity(SUPERMARKET, timeProfile, agent)  with Serializable
{

  // These variables define the different things that the agent could be doing in order to satisfy the work activity
  // (Note: in SleepActivity, these are defined as case classes that extend a sealed trait, but this way is probably
  // more efficient)

  private val LOG: Logger = Logger.getLogger(this.getClass)

  private val IN_THE_SUPERMARKET = 1
  private val TRAVELLING = 2
  private val INITIALISING = 3
  private var currentAction = INITIALISING
  //private val place : Place = null // start with a null place

  private val place = Place(
    location = null,
    activityType = SUPERMARKET,
    openingTimes = Array(Place.makeOpeningTimes(7.0, 22.0))
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
        Agent.LOG.debug(agent, "initialising SupermarketActivity")
        val supermarketLocation: SurfGeometry[Building] = GISFunctions.findNearestObject[Building](this.agent.location(), SurfABM.supermarketGeoms, true, state)
        this.place.location = supermarketLocation
        // See if the agent is in the supermarket
        if (this.place.location.equalLocation(
          this.agent.location())) {
          Agent.LOG.debug(agent, "is in the supermarket. Start shopping.")
          currentAction = IN_THE_SUPERMARKET // Next iteration the agent will start to shop
        }
        else {
          Agent.LOG.debug(agent, "is not at the supermarket yet. Travelling there.")
          this.agent.newDestination(Option(this.place.location))
          currentAction = TRAVELLING
        }

      }

      case TRAVELLING => {
        if (this.agent.atDestination()) {
          Agent.LOG.debug(agent, "has reached the supermarket. Starting to shop")
          currentAction = IN_THE_SUPERMARKET
        }
        else {
          Agent.LOG.debug(agent, "is travelling to the supermarket.")
          agent.moveAlongPath()
        }
      }

      case IN_THE_SUPERMARKET => {
        Agent.LOG.debug(agent, "is shopping in a supermarket")
        return true
      }

    }
    // Only get here if the agent isn't shopping, so must return false.
    return false

    //throw new NotImplementedError("Have not implemented Shopping activity yet")
  }

  override def activityChanged(): Unit = {
    this.currentAction = INITIALISING
    this._currentIntensityDecrease = 0d
    //throw new NotImplementedError("Have not implemented Shopping activity yet")
  }


  /**
    * The amount that the shopping activity should increase at each iteration
    * @return
    */
  override def backgroundIncrease(): Double = {
    if (this.agent.getClass == classOf[CommuterAgent]) {
      return 1d / (3d * SurfABM.ticksPerDay)
    } else if (this.agent.getClass == classOf[RetiredAgent]) {
      return 1d / (2d * SurfABM.ticksPerDay)
    } else {
      return 1d / (5d * SurfABM.ticksPerDay)
    }
  }

  /**
    * The amount that a shopping activity will go down by if an agent is shopping.
    * @return
    */
  override def reduceActivityAmount(): Double = {
    if (this.agent.getClass == classOf[CommuterAgent]) {
      return 36d / SurfABM.ticksPerDay
    } else if (this.agent.getClass == classOf[RetiredAgent]) {
      return 20.5 / SurfABM.ticksPerDay
    } else {
      return 25d / SurfABM.ticksPerDay
    }
  }

  override val MINIMUM_INTENSITY_DECREASE = 0.5
}
