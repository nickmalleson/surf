package surf.abm.agents.abbf.activities

import surf.abm.agents.abbf.TimeProfile
import surf.abm.agents.abbf.activities.ActivityTypes.ActivityType


/**
  * [[surf.abm.agents.abbf.activities.Activity]] is the main trait for the different types of activity: [[surf.abm.agents.abbf.activities.FixedActivity]] and
  * [[surf.abm.agents.abbf.activities.FlexibleActivity]]. Activites need a type ([[surf.abm.agents.abbf.activities.ActivityTypes]]) and
  * a [[surf.abm.agents.abbf.TimeProfile]] (that determines when they are their most intense).
  */
trait Activity {
  def activityType: ActivityType

  /**
    *  A definition of the times when this Activity is at its most <i>intense</i>.
    *                     E.g. work activities might be the most intense between 9am-5pm. This has the
    *                     affect of increasing/decreasing the agent's desire to undertake the activity
    *                     depending on the time.
    *
    */
  def timeProfile: TimeProfile
}

