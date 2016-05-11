package surf.abm.agents.abbf.activities

import org.apache.log4j.Logger
import surf.abm.agents.abbf.activities.ActivityTypes.ActivityType
import surf.abm.agents.abbf.{Place, TimeProfile}

/**
  * [[FixedActivity]]s are identical to [[FlexibleActivity]]s, except
  * that they can only be undertaken in the designated place.
  *
  * @param activityType The type of this activity.
  * @param place        The [[surf.abm.agents.abbf.Place]] where this activity can undertaken.
  * @param timeProfile  (See [[Activity]] for information about the [[surf.abm.agents.abbf.TimeProfile]])
  */
class FixedActivity(val activityType: ActivityType, val timeProfile: TimeProfile, val place: Place) extends Activity {

}

object FixedActivity {
  private val LOG: Logger = Logger.getLogger(this.getClass);
}

