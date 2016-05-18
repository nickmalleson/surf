package surf.abm.agents.abbf.activities

import surf.abm.agents.{Agent, UrbanAgent}
import surf.abm.agents.abbf.activities.ActivityTypes.ActivityType
import surf.abm.agents.abbf.{Place, TimeProfile}

/**
  * [[surf.abm.agents.abbf.activities.FixedActivity]]s are identical to [[surf.abm.agents.abbf.activities.FlexibleActivity]]s,
  * except that they can only be undertaken in the designated place.
  *
  * @see [[surf.abm.agents.abbf.activities.Activity]] for more information
  *
  * @param activityType The type of this activity.
  * @param place        The [[surf.abm.agents.abbf.Place]] where this activity can undertaken.
  * @param agent        The agent who will perform this activity
  * @param timeProfile  (See [[surf.abm.agents.abbf.activities.Activity]] for information about the [[surf.abm.agents.abbf.TimeProfile]])
  */
abstract class FixedActivity(
                              override val activityType: ActivityType,
                              override val timeProfile: TimeProfile,
                              override val agent: UrbanAgent,
                              val place: Place)
  extends Activity (activityType, timeProfile, agent)
{



}


