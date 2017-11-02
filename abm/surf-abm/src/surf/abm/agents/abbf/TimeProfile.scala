package surf.abm.agents.abbf

import org.apache.log4j.Logger
import surf.abm.agents.abbf.activities.FixedActivity

import scala.util.control.Breaks._

/**
  * Used to defines the importance/attractiveness of an [[FixedActivity]] over time.
  * Profiles are created by providing the importance at a number of points of time on a 24 hour clock.
  * If the importance at a different time is requested then the class interpolates between the two
  * nearest time points.
  *
  * @param profile An array of tuples defining time periods and intensities. E.g. for a profile that
  *                starts to increase from 0 at 9am, peaks at midday, and decreases again from 5pm:
  *                {{{
  *                                                val d = Array( (0,0), (9,0.8), (14,1.0), (17,0.8) )
  *                                                val t = TimeProfile(d)
  *                }}}
  *                If only one (time,intensity) pair is provided, then we assume constant intensity.
  *                Note: all times must (obviously) be in the range [0,24) and should be in
  *                ascending order (to make my life easier).
  *
  */
case class TimeProfile(val profile: Array[(Double, Double)]) {

  // Check that the times in the input profile are correct (will throw an exception if not)
  TimeProfile.checkTimes(profile)

  // Break up the times and the intensities
  private val times: Array[Double] = for ((i, j) <- profile) yield i
  private val intensities: Array[Double] = for ((i, j) <- profile) yield j

  /**
    * Calculate the intensity at time t
    *
    * @param t The time period to check. Must be in range [0,24). TODO: Improve this so that it takes a time and date.
    * @return The intensity, calculates as the linear interpolation between the nearest time points.
    */
  def calcIntensity(t: Double): Double = {

    val debug = false
    if (debug) println(s"**** $t ****")
    // Cannot calculate intensities outside the range [0-24)
    if (t < 0 || t >= 24) {
      throw new IllegalArgumentException(s"Input time ($t) needs to be in the range [0-24)")
    }

    // If there is only one reference point, then assume a constant intensity
    if (this.times.length == 1) {
      return this.intensities(0)
    }

    // If the exact time exists, just return the intensity there
    val index = this.times.indexOf(t)
    if (index != -1)
      return this.intensities(index)


    // Find the two nearest time points, t0 and t1 and their indices i0 and i1
    var t0, t1 = -1d // time0 and time1
    var i0, i1 = -1d // intensity0 and intensity1
    breakable {
      for ((time: Double, intensity: Double) <- this.times.zip(this.intensities)) {
        if (debug) print(s"$t :: in loop beginning :: ($t0 $i0) ($t1 $i1) -- $time $intensity\t\t")
        if (time <= t) {
          // time point is before the current t0
          t0 = time
          i0 = intensity
        }
        if (time >= t) {
          // time point is after current t0 (once we've found this then break out
          t1 = time
          i1 = intensity
          if (debug) println(s"($t0 $i0) ($t1 $i1) -- $time $intensity") // print for info.
          break
        }
        if (debug) println(s"($t0 $i0) ($t1 $i1) -- $time $intensity")
      }
    }

    assert(t0 != t1) // If this happens then t == t1 == t2 (i.e. the exact time exists in the list) and the earlier if() should have caught it
    if (t0 != -1d && t1 != -1d) {
      return interpolate(t, t0, i0, t1, i1)
    }

    assert(!(t0 == -1d && t1 == -1d)) // At least on of the time points should have been set

    // Special case1: the first intensity actually occurs before midnight (wrap around)
    if (t0 == -1d) {
      t0 = this.times.last - 24 // Need to take 24 away to trick the interpolation
      i0 = this.intensities.last
      if (debug) println(s"$t :: after special case1 :: $t0 $i0 -- $t1 $i1")
    }
    // Special case2: the last time occurs after midnight (wraps around)
    if (t1 == -1d) {
      t1 = this.times(0) + 24 // Add 24 to wrap around
      i1 = this.intensities(0)
      if (debug) println(s"$t :: after special case2 :: $t0 $i0 -- $t1 $i1")
    }
    if (debug) println(s"$t :: final:: $t $t0, $i0, $t1, $i1 ")
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
  private def interpolate(x: Double, x0: Double, y0: Double, x1: Double, y1: Double): Double = {

    if (!((x > x0) && (x < x1)))
      throw new IllegalArgumentException(s"x ($x) must be between x0 and x1 ($x0 , $x1 )")

    if (!(x1 > x0)) {
      throw new IllegalArgumentException(s"Second input ($x1) must be greater than first input ($x0)")
    }
    //val temp = y0 + ((y1 - y0) * ((x - x0) / (x1 - x0)))
    //println(s"INTER: $x $x0 $y0 $x1 $y1 = $temp")
    return y0 + ((y1 - y0) * ((x - x0) / (x1 - x0)))

  }
}


object TimeProfile {

  private val LOG: Logger = Logger.getLogger(this.getClass)

  /**
    * Checks that the times in the input profile are ascending and in the range [0,24)
    * Throwns a [[java.lang.IllegalArgumentException]] otherwise.
    */
  def checkTimes(profile: Array[(Double, Double)]): Unit = {

    // Input array cannot be empty
    if (profile.isEmpty) throw new IllegalArgumentException("Input profile must have at least one (time,intensity) pair")

    // Check that all times are in the range [0,24)
    for ((i, j) <- profile) {
      if (i < 0.0 || i >= 24.0) {
        //LOG.error(profile)
        throw new IllegalArgumentException("Times must be in range [0,24]. See previous log message for the profile.")
      }
    }

    // Check that times are in ascending order
    var count = 0d
    for ((i, j) <- profile) {
      if (i < count) {
        //for (s <- profile.map( { case (i, j) => s"($i $j),\t"} ) ) LOG.error(s)
        throw new IllegalArgumentException("Times must be in ascending order. See previous log message for the profile.")
      }
      count = i
    }
  }

}

