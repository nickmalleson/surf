package surf.abm.agents.abbf

import org.apache.log4j.Logger

/**
  * Represents activities that ([[surf.abm.agents]]) can do.
  *
  * @param places      An array of [[surf.abm.agents.abbf.Place]]s where this activity can undertaken.
  * @param timeProfile A definition of the times when this Activity is at its most <i>intense</i>.
  *                    E.g. work activities might be the most intense between 9am-5pm. This has the
  *                    affect of increasing/decreasing the agent's desire to undertake the activity
  *                    depending on the time.
  */
class Activity (val places: Array[Place], val timeProfile: TimeProfile) {

}

object Activity {
  private val LOG: Logger = Logger.getLogger(this.getClass);
}

/**
  * Used to defines the importance/attractiveness of an [[surf.abm.agents.abbf.Activity]] over time.
  * Profiles are created by providing the importance at a number of points of time on a 24 hour clock.
  * If the importance at a different time is requested then the class interpolates between the two
  * nearest time points.
  *
  * @param profile An array of tuples defining time periods and intensities. E.g. for a profile that
  *                starts to increase from 0 at 9am, peaks at midday, and decreases again from 5pm:
  *                {{{
  *                val d = Array( (0,0), (9,0.8), (14,1.0), (17,0.8) )
  *                val t = TimeProfile(d)
  *                }}}
  *                Note: all times must (obviously) be in the range [0,24) and should be in
  *                ascending order (to make my life easier).
  *
  */
case class TimeProfile (val profile: Array[(Double,Double)] ) {

  // Check that the times in the input profile are correct (will throw an exception if not)
  TimeProfile.checkTimes(profile)

  // Break up the times and the intensities
  private val times : Array[Double] = for ( (i,j) <- profile) yield i
  private val intensities : Array[Double] = for ( (i,j) <- profile) yield j

  /**
    * Calculate the intensity at time t
    * @param t The time period to check. Must be in range [0,24)
    * @return The intensity, calculates as the linear interpolation between the nearest time points.
    */
  def calcIntensity (t:Double) : Double = {
  return 1.0
  }
}

object TimeProfile {

  /**
    * Checks that the times in the input profile are ascending and in the range [0,24)
    * Throwns a [[java.lang.IllegalArgumentException]] otherwise.
    */
  def checkTimes(profile:  Array[(Double,Double)]) : Unit = {

    // Check that all times are in the range [0,24)
    for ( (i,j) <- profile) {
      if (i < 0.0 || i >=24.0 ) {
        //LOG.error(profile)
        throw new IllegalArgumentException("Times must be in range [0,24). See previous log message for the profile.")
      }
    }

    // Check that times are in ascending order
    var count = 0d
    for ( (i,j) <- profile) {
      if (i < count) {
        //for (s <- profile.map( { case (i, j) => s"($i $j),\t"} ) ) LOG.error(s)
        throw new IllegalArgumentException("Times must be in ascending order. See previous log message for the profile.")
      }
      count = i
    }
  }

  private val LOG: Logger = Logger.getLogger(this.getClass)

}