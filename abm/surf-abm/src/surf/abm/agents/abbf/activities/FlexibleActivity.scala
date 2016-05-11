package surf.abm.agents.abbf.activities

import surf.abm.agents.abbf.TimeProfile
import surf.abm.agents.abbf.activities.ActivityTypes.ActivityType

/**
  * Flexible activities are a type of activity that can be undertaken in any appropriate place (e.g. Shopping). They
  * are not constrained to particular locations. See also [[surf.abm.agents.abbf.activities.FixedActivity]].
  *
  * @param activityType
  * @param timeProfile (See [[surf.abm.agents.abbf.activities.Activity]] for information about the
  *                    [[surf.abm.agents.abbf.TimeProfile]])
  */
case class FlexibleActivity (val activityType: ActivityType, val timeProfile: TimeProfile) extends Activity {

}

/*object FlexibleActivity {

  def apply(activityType: ActivityType, timeProfile: TimeProfile) : FlexibleActivity = {
    new FlexibleActivity(activityType, timeProfile)
  }

}*/
