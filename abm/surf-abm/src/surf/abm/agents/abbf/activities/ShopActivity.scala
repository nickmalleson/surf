package surf.abm.agents.abbf.activities

import org.apache.log4j.Logger
import sim.engine.SimState
import surf.abm.agents.{Agent, UrbanAgent}
import surf.abm.agents.abbf.{ABBFAgent, Place, TimeProfile}
import surf.abm.agents.abbf.activities.ActivityTypes.SHOPPING
import surf.abm.agents.abbf.occupancies.{CommuterAgent, RetiredAgent}
import surf.abm.main.{GISFunctions, SurfABM, SurfGeometry}
import surf.abm.environment.Building


/**
  * An activity (of type [[surf.abm.agents.abbf.activities.ActivityTypes.SHOPPING]]) that causes the agent to
  * travel to the shops
  */
case class ShopActivity(
                     override val timeProfile: TimeProfile,
                     override val agent: ABBFAgent,
                     state: SimState)
  extends FlexibleActivity(SHOPPING, timeProfile, agent)  with Serializable
{

  // These variables define the different things that the agent could be doing in order to satisfy the work activity
  // (Note: in SleepActivity, these are defined as case classes that extend a sealed trait, but this way is probably
  // more efficient)

  private val LOG: Logger = Logger.getLogger(this.getClass)

  private val IN_THE_SHOP = 1
  private val TRAVELLING = 2
  private val INITIALISING = 3
  private var currentAction = INITIALISING
  //private val place : Place = null // start with a null place

  private val place = Place(
    location = null,
    activityType = SHOPPING,
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
        Agent.LOG.debug(agent, "initialising ShopActivity")
        //LOG.info(s"x coordinate is ${this.agent.location().getGeometry.getCentroid.getX}")
        //LOG.info(s"y coordinate is ${this.agent.location().getGeometry.getCentroid.getY}")
        val shoppingLocation: SurfGeometry[Building] = GISFunctions.findNearestObject[Building](this.agent.location(), SurfABM.shopGeoms, true, state)
        this.place.location = shoppingLocation
        // See if the agent is in the shop
        if (this.place.location.equalLocation(
            this.agent.location())) {
          Agent.LOG.debug(agent, "is in the shop. Start shopping.")
          currentAction = IN_THE_SHOP // Next iteration the agent will start to shop
        }
        else {
          Agent.LOG.debug(agent, "is not at the shop yet. Travelling there.")
          this.agent.newDestination(Option(this.place.location))
          currentAction = TRAVELLING
        }

      }

      case TRAVELLING => {
        if (this.agent.atDestination()) {
          Agent.LOG.debug(agent, "has reached the shop. Starting to shop")
          currentAction = IN_THE_SHOP
        }
        else {
          Agent.LOG.debug(agent, "is travelling to the shop.")
          agent.moveAlongPath()
        }
      }

      case IN_THE_SHOP => {
        Agent.LOG.debug(agent, "is shopping")
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
      return 1d / (5d * SurfABM.ticksPerDay)
    } else if (this.agent.getClass == classOf[RetiredAgent]) {
      return 2d / (5d * SurfABM.ticksPerDay)
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
      return 25d / (1d * SurfABM.ticksPerDay)
    } else if (this.agent.getClass == classOf[RetiredAgent]) {
      return 12d / (1d * SurfABM.ticksPerDay)
    } else {
      return 25d / (1d * SurfABM.ticksPerDay)
    }
  }

}
