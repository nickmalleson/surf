package surf.abm.agents.abbf.activities

import surf.abm.agents.{Agent, UrbanAgent}
import surf.abm.agents.abbf.TimeProfile
import surf.abm.agents.abbf.activities.ActivityTypes.ActivityType


/**
  * The main trait for the different types of activity:
  * [[surf.abm.agents.abbf.activities.FixedActivity]] and [[surf.abm.agents.abbf.activities.FlexibleActivity]].
  * Activites need a type ([[surf.abm.agents.abbf.activities.ActivityTypes]]) and a [[surf.abm.agents.abbf.TimeProfile]]
  * that determines when they are their most intense.
  *
  * <p>
  * Activities also have a <i>background intensity</i>. This is a base level of intensity that gradually increases
  * over time, unless the agent is actually undertaking the activity in which case it increases. The <code>-=</code>
  * and <code>+=</code> methods can be used to increase or decrease this background intensity.</p>
  *
  * <p>The <code>performAction()</code> function specifies what the agent needs to do in order to perform this
  * activity. It must be overridden by subclasses and might involve some preliminary steps that need to be undertaken
  * before the activity can be completed. E.g. the agent might need to travel somewhere first. If the agent
  * can do this activity, then <code>performAction()</code> returns <code>true</code>, and the underlying
  * agent might choose to reduce the <code>backgroundIntensity</code> (with <code>-=()</code>.</p>
  *
  *
  * @param activityType The type ([[surf.abm.agents.abbf.activities.ActivityTypes]]) of this activity
  * @param timeProfile A definition of the times when this Activity is at its most <i>intense</i>.
  *  E.g. work activities might be the most intense between 9am-5pm. This has the
  *  affect of increasing/decreasing the agent's desire to undertake the activity
  *  depending on the time.
  * @param a The agent who will be performing this activity. Note that this is actually defined as a function
  *          that returns an agent. This is so that Activities can be instantiated before the agent whom they
  *          refer to. (This is pretty neat, see
  *          <a href="https://stackoverflow.com/questions/7507965/instantiating-immutable-paired-objects">
  *            Instantiating immutable paired objects</a> discussion.
  *
  */
abstract class Activity ( val activityType: ActivityType, val timeProfile: TimeProfile, a: => UrbanAgent) {

  lazy val agent =  a

  private var _backgroundIntensity = 0d // Private variable with public accessor

  /**
    * The current background intensity of the activity. I.e. the base amount that gradually increases until the
    * activity is undertaken.
    */
  def backgroundIntensity: Double = _backgroundIntensity


  /**
    * This makes the agent actually perform the activity. It must be implemented by sub-classes.
    * Perorming this action might require the agent to perform some preliminary activites before this one
    * can be performed.
    *
    * @return True if the agent has performed this activity, false if they have not (e.g. if they are still travelling
    *         to a particular destination).
    */
  abstract def performActivity() : Boolean

  /**
    * Increase the background intensity
    *
    * @param d
    */
  def +=(d:Double) : Unit = {
    this._backgroundIntensity += d
  }

  /**
    * Decrease the intensity of this activity, i.e. if the agent is doing something to satisfy it.
    * @param d The amount to decrease the intensity by,
    */
  def -= (d:Double) : Unit = {
    this._backgroundIntensity -= d
  }

  /**
    * Calculate the current intensity of this activity, given the time.
    * @param currentTime
    * @return
    */
  def getIntensity(currentTime: Double) = this._backgroundIntensity + this.timeProfile.calcIntensity(currentTime)



}
