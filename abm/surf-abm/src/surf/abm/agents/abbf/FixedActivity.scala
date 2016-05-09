package surf.abm.agents.abbf

import org.apache.log4j.Logger
import surf.abm.agents.abbf.ActivityTypes.ActivityType

import scala.util.control.Breaks._

/**
  * [[surf.abm.agents.abbf.FixedActivity]]s are identical to [[surf.abm.agents.abbf.FlexibleActivity]]s, except
  * that they can only be undertaken in the designated place.
  *
  * @param activityType The type of this activity.
  * @param place        The [[surf.abm.agents.abbf.Place]] where this activity can undertaken.
  * @param timeProfile  (See [[surf.abm.agents.abbf.Activity]] for information about the [[surf.abm.agents.abbf.TimeProfile]])
  */
class FixedActivity(val activityType: ActivityType, val timeProfile: TimeProfile, val place: Place) extends Activity {

}

object FixedActivity {
  private val LOG: Logger = Logger.getLogger(this.getClass);
}

