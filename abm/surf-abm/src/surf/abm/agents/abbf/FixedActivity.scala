package surf.abm.agents.abbf

import org.apache.log4j.Logger
import scala.util.control.Breaks._

/**
  * Represents activities that ([[surf.abm.agents]]) can do.
  *
  * @param places      An array of [[surf.abm.agents.abbf.Place]]s where this activity can undertaken.
  * @param timeProfile A definition of the times when this Activity is at its most <i>intense</i>.
  *                    E.g. work activities might be the most intense between 9am-5pm. This has the
  *                    affect of increasing/decreasing the agent's desire to undertake the activity
  *                    depending on the time.
  */
class FixedActivity(val activityType: ActivityType, val timeProfile: TimeProfile, val place: Place) extends Activity {

}

object FixedActivity {
  private val LOG: Logger = Logger.getLogger(this.getClass);
}

