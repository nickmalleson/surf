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
  *                If only one (time,intensity) pair is provided, then we assume constant intensity.
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
    *
    * @param t The time period to check. Must be in range [0,24)
    * @return The intensity, calculates as the linear interpolation between the nearest time points.
    */
  def calcIntensity (t:Double) : Double = {

    // Cannot calculate intensities outside the range [0-24)
    if (t < 0 || t >= 24) {
      throw new IllegalArgumentException(s"Input time ($t) needs to be in the range [0-24)")
    }

    // If there is only one reference point, then assume a constant intensity
    if (this.times.size == 1) {
      return this.intensities(0)
    }

    // If the exact time exists, just return the intensity there
    val index = this.times.indexOf(t)
    if (index != -1)
      return this.intensities(index)


    // Find the two nearest time points, t0 and t1 and their indices i0 and 01
    var t0, t1 = -1d // time0 and time1
    var i0, i1 = -1d // intensity0 and intensity1
    for ((time:Double,intensity:Double) <- this.times.zip(this.intensities)) {
      if (t0 <= time) {
        t0 = time
        i0 = intensity
      }
      if (t1 >= t) {
        t1 = time
        i1 = intensity
      }
    }
    assert(t0 != t1) // If this happens then t == t1 == t2 (i.e. the exact time exists in the list) and the earlier if() should have caught it
    if (t0 != -1d && t1 != -1d) {
      return interpolate(t, t0, i0, t1, i1)
    }

    assert ( !( t0 == -1d && t1 == -1d ) ) // At least on of the time points should have been set

    // Special case1: the first intensity actually occurs before midnight (wrap around)
    if (t0 == -1d) {
      t0 = this.times.last - 24 // Need to take 24 away to trick the interpolation
      i0 = this.intensities.last
    }
    // Special case2: the last time occurs after midnight (wraps around)
    if (t1 == -1d) {
      t1 = this.times(0) + 24 // Add 24 to wrap around
      i1 = this.intensities(0)
    }
    return interpolate(t, t0, i0, t1, i1)
  }

  /**
    * Linear interpolation between the two points. Note: x1>x2 or an IllegalArgumentException is thrown
    *
    * @param x The number to interpolate
    * @param x0
    * @param y0
    * @param x1
    * @param y1
    * @return
    */
  private def interpolate ( x:Double, x0:Double,y0:Double, x1:Double,y1:Double ) : Double = {
    if (x1 > x0) {
      return y0 + (y1-y0) * ( (x-x0) / (x1-x0))
    }
    else {
      throw new IllegalArgumentException(s"Second input ($x1) must be greater than first input ($x0)")
    }

  }
}

object TimeProfile {

  /**
    * Checks that the times in the input profile are ascending and in the range [0,24)
    * Throwns a [[java.lang.IllegalArgumentException]] otherwise.
    */
  def checkTimes(profile:  Array[(Double,Double)]) : Unit = {

    // Input array cannot be empty
    if (profile.isEmpty) throw new IllegalArgumentException("Input profile must have at least one (time,intensity) pair")

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