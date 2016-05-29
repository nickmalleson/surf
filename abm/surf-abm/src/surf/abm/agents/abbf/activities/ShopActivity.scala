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

  /**
    * This makes the agent actually perform the activity.
    *
    * @return True if the agent has performed this activity, false if they have not (e.g. if they are still travelling
    *         to a particular destination).
    */
  override def performActivity(): Boolean = {
    throw new NotImplementedError("Have not implemented Shopping activity yet")
  }

  override def activityChanged(): Unit = {
    throw new NotImplementedError("Have not implemented Shopping activity yet")
  }
}
