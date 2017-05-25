package surf.abm.agents.abbf.activities

import surf.abm.agents.{Agent, UrbanAgent}
import surf.abm.agents.abbf.{ABBFAgent, Place, TimeProfile}
import surf.abm.agents.abbf.activities.ActivityTypes.SHOPPING


/**
  * An activity (of type [[surf.abm.agents.abbf.activities.ActivityTypes.SHOPPING]]) that causes the agent to
  * travel to the shops
  */
case class ShopActivity(
                     override val timeProfile: TimeProfile,
                     override val agent: ABBFAgent)
  extends FlexibleActivity(SHOPPING, timeProfile, agent)  with Serializable
{

  // These variables define the different things that the agent could be doing in order to satisfy the work activity
  // (Note: in SleepActivity, these are defined as case classes that extend a sealed trait, but this way is probably
  // more efficient)

  private val SHOPPING = 1
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
        Agent.LOG.debug(s"${agent.toString()} is initialising ShopActivity")
        // See if the agent is in the shop
        if (this.place.location.equalLocation(this.agent.location())) {
          Agent.LOG.debug(s"${agent.toString()} is in the shop. Start shopping.")
          currentAction = SHOPPING // Next iteration the agent will start to shop
        }
        else {
          Agent.LOG.debug(s"${agent.toString()} is not at the shop yet. Travelling there.")
          this.agent.newDestination(Option(this.place.location))
          currentAction = TRAVELLING
        }

      }

      case TRAVELLING => {
        if (this.agent.atDestination()) {
          Agent.LOG.debug(s"${agent.toString()} has reached the shop. Starting to shop")
          currentAction = SHOPPING
        }
        else {
          Agent.LOG.debug(s"${agent.toString()} is travelling to the shop.")
          agent.moveAlongPath()
        }
      }

      case SHOPPING => {
        Agent.LOG.debug(s"${agent.toString()} is shopping")
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
}
