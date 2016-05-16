package surf.abm.agents.abbf.activities

import surf.abm.agents.abbf.TimeProfile
import surf.abm.agents.abbf.activities.ActivityTypes.ActivityType


/**
  * The main trait for the different types of activity:
  * [[surf.abm.agents.abbf.activities.FixedActivity]] and [[surf.abm.agents.abbf.activities.FlexibleActivity]].
  * Activites need a type ([[surf.abm.agents.abbf.activities.ActivityTypes]]) and a [[surf.abm.agents.abbf.TimeProfile]]
  * that determines when they are their most intense.
  *
  * @param activityType The type ([[surf.abm.agents.abbf.activities.ActivityTypes]]) of this activity
  * @param timeProfile A definition of the times when this Activity is at its most <i>intense</i>.
  *  E.g. work activities might be the most intense between 9am-5pm. This has the
  *  affect of increasing/decreasing the agent's desire to undertake the activity
  *  depending on the time.
  *
  */
abstract case class Activity ( val activityType: ActivityType, val timeProfile: TimeProfile) {

  private var _backgroundIntensity = 0d // Private variable with public accessor

  /**
    * The current background intensity of the activity. I.e. the base amount that gradually increases until the
    * activity is undertaken.
    */
  def backgroundIntensity: Double = _backgroundIntensity

  /**
    * Increase the background intensity
    *
    * @param d
    */
  def incrementIntensity(d:Double) : Unit = {
    this._backgroundIntensity += d
  }

  XXXX NEXT - DECIDE HOW TO DECREASE ACTIVITIES WHILE THEY'RE BEING UNDERTAKEN

  /**
    * Calculate the current intensity of this activity, given the time.
    * @param currentTime
    * @return
    */
  def getIntensity(currentTime: Double) = this._backgroundIntensity + this.timeProfile.calcIntensity(currentTime)




}
