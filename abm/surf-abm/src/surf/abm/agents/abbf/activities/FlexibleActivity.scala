package surf.abm.agents.abbf.activities

import surf.abm.agents.abbf.TimeProfile
import surf.abm.agents.abbf.activities.ActivityTypes.ActivityType

/**
  * Flexible activities are a type of activity that can be undertaken in any appropriate place (e.g. Shopping). They
  * are not constrained to particular locations. See also [[FixedActivity]].
  *
  * @param activityType
  * @param timeProfile (See [[Activity]] for information about the [[surf.abm.agents.abbf.TimeProfile]])
  */
class FlexibleActivity (val activityType: ActivityType, val timeProfile: TimeProfile) extends Activity {

}
