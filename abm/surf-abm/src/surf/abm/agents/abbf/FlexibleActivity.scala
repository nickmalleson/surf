package surf.abm.agents.abbf

import surf.abm.agents.abbf.ActivityTypes.ActivityType

/**
  * Flexible activities are a type of activity that can be undertaken in any appropriate place (e.g. Shopping). They
  * are not constrained to particular locations. See also [[surf.abm.agents.abbf.FixedActivity]].
  *
  * @param activityType
  * @param timeProfile  (See [[surf.abm.agents.abbf.Activity]] for information about the [[surf.abm.agents.abbf.TimeProfile]])
  */
class FlexibleActivity (val activityType: ActivityType, val timeProfile: TimeProfile) extends Activity {

}
