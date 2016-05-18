package surf.abm.agents.abbf.activities

import surf.abm.agents.{Agent, UrbanAgent}
import surf.abm.agents.abbf.{ABBFAgent, Place, TimeProfile}
import surf.abm.agents.abbf.activities.ActivityTypes.WORKING

/**
  * An activity (of type [[surf.abm.agents.abbf.activities.ActivityTypes.WORKING]]) that causes the agent to
  * go to their work place.
  */
case class WorkActivity(
                    override val timeProfile: TimeProfile,
                    override val agent: ABBFAgent,
                    override val place: Place)
  extends FixedActivity (WORKING, timeProfile, agent, place)
{

  /**
    * This makes the agent actually perform the activity.
    *
    * @return True if the agent has performed this activity, false if they have not (e.g. if they are still travelling
    *         to a particular destination).
    */
  override def performActivity(): Boolean = {
    throw new NotImplementedError("Have not implemented Working activity yet")
  }



}
