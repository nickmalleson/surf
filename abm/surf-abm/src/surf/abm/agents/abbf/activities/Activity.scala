package surf.abm.agents.abbf.activities

import surf.abm.agents.{Agent, UrbanAgent}
import surf.abm.agents.abbf.{ABBFAgent, TimeProfile}
import surf.abm.agents.abbf.activities.ActivityTypes.ActivityType
import surf.abm.main.Clock


/**
  * The main trait for the different types of activity:
  * [[surf.abm.agents.abbf.activities.FixedActivity]] and [[surf.abm.agents.abbf.activities.FlexibleActivity]].
  * Activites need a type ([[surf.abm.agents.abbf.activities.ActivityTypes]]) and a [[surf.abm.agents.abbf.TimeProfile]]
  * that determines when they are their most intense.
  *
  * <p>
  * Activities also have a <i>background intensity</i>. This is a base level of intensity that gradually increases
  * over time, unless the agent is actually undertaking the activity in which case it decreases. The <code>-=</code>
  * and <code>+=</code> methods can be used to increase or decrease this background intensity.</p>
  *
  * <p>The <code>performAction()</code> function specifies what the agent needs to do in order to perform this
  * activity. It must be overridden by subclasses and might involve some preliminary steps that need to be undertaken
  * before the activity can be completed. E.g. the agent might need to travel somewhere first. If the agent
  * can do this activity, then <code>performAction()</code> returns <code>true</code>, and the underlying
  * agent might choose to reduce the <code>backgroundIntensity</code> (with <code>-=()</code>.</p>
  *
  * @param activityType The type ([[surf.abm.agents.abbf.activities.ActivityTypes]]) of this activity
  * @param timeProfile A definition of the times when this Activity is at its most <i>intense</i>.
  *  E.g. work activities might be the most intense between 9am-5pm. This has the
  *  affect of increasing/decreasing the agent's desire to undertake the activity
  *  depending on the time.
  * @param agent The agent who will be performing this activity.
  *
  */
abstract class Activity ( val activityType: ActivityType, val timeProfile: TimeProfile, val agent: ABBFAgent)
  extends Serializable {

  private var _backgroundIntensity = 0d // Private variable with public accessor

  /**
    * The current background intensity of the activity. I.e. the base amount that will gradually increase until the
    * activity is undertaken.
    */
  def backgroundIntensity() = _backgroundIntensity

  /**
    * The current extra time-of-day intensity for this activity.
    *
    * @param currentTime Current decimal 24-hour of the day (e.g. 3.5 = 03:30)
    */
  def timeIntensity(currentTime: Double) = this.timeProfile.calcIntensity(currentTime)

  /**
    * Calculate the current total intensity of this activity at the current time. (I.e. background intensity plus
    * time-specific intensity).
    *
    * @return
    */
  def intensity() = this._backgroundIntensity + this.timeProfile.calcIntensity(Clock.currentHour())


  /**
    * Find out how much this activity has been reduced by in one 'sitting' (i.e. from when the agent most recently
    * started satisfying the activity.
    * <p>This is useful because it makes it possible the agent from chaining activity too rapidly.</p>
    */
  def currentIntensityDecrease() = _currentIntensityDecrease
  protected var _currentIntensityDecrease = 0d


  /**
    * This makes the agent actually perform the activity. It must be implemented by sub-classes.
    * Performing this action might require the agent to perform some preliminary activities before this one
    * can be performed.
    *
    * @return True if the agent has performed this activity, false if they have not (e.g. if they are still travelling
    *         to a particular destination).
    */
  def performActivity() : Boolean

  /**
    * This method will be called if this Activity was being performed, but now a new one is in charge. This gives
    * the Activity the opportunity to reset itself ready for the next time it is called. For example, in
    * [[surf.abm.agents.abbf.activities.SleepActivity]], the activity wants to be told if it needs to re-initialise
    * itself to avoid having to check lots of criteria (is the agent at home? are the travelling? etc) at every iteration.
    */
  def activityChanged() : Unit

  /**
    * Increase the background intensity. This usually happens at every iteration
    *
    * @param d
    */
  def +=(d:Double) : Unit = {
    this._backgroundIntensity += d
  }

  /**
    * Decrease the intensity of this activity, i.e. if the agent is doing something to satisfy it.
    *
    * @param d The amount to decrease the intensity by
    * @param simulate Whether to simulate the increase but not actually apply it. If this is set to true, then no
    *                 change is made to the background intensity. It will affect the return value of the function though.
    * @return If 'simulate' is false (default), then always return true. Otherwise, return true if reducing the
    *         background intensity will *not* reduce the overall intensity below 0. This should never happen because
    *         why would an agent ever want to satisfy an activity if the overall intensity was 0? Otherwise return false
    *         (i.e. it would have been fine to reduce the intensity).
    * @throws IllegalArgumentException if this call attempts to take the total intensity
    * (i.e. background + time component ) below 0. That should never happen (why would the agent want to do that??). If
    * 'simulate' is true then under those conditions no Exception is thrown and the function returns false.
    *
    */
  def -= (d:Double, simulate:Boolean = false) : Boolean = {
    if (!simulate) {
      this._backgroundIntensity -= d
      if (this.intensity() < 0) {
        throw new IllegalArgumentException(s"Overall intenity of activity ${this.toString} for agent ${this.agent.toString()} has dropped below 0")
      }
      // Also remember how much the activity has gone
      this._currentIntensityDecrease = this._currentIntensityDecrease + d
      return true
    }
    else {
      // Stupidity check - make sure that the intensity calculation here matches that in the intensity() function (I don't
      // want to change the intensity() function and forget to change the calculation here!)
      assert (this._backgroundIntensity + this.timeProfile.calcIntensity(Clock.currentHour()) == this.intensity())
      if ( ( this._backgroundIntensity - d + this.timeProfile.calcIntensity(Clock.currentHour()) ) < 0 ) {
        return false // Reducing the intensity would take the total intensity below zero
      }
      return true
    }
  }


  override def toString() : String = this.getClass.getSimpleName





}
