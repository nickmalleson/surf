package surf.abm.agents.abbf

import java.time.LocalDateTime

import surf.abm.SurfGeometry
import surf.abm.agents.abbf.activities.ActivityTypes
import surf.abm.agents.abbf.activities.ActivityTypes.ActivityType


/**
  * A places where an activities can be undertaken.
  *
  * ==Examplse==
  * The following creates an activity that can be done between 9:30am and 5pm on the 1st of Jan 2015.
  *
  * {{{
  *import java.time.LocalDateTime
  *import sim.util.geo.MasonGeometry
  *import surf.abm.SurfGeometry
  *val t1 = LocalDateTime.of(2015, 1, 1, 9, 30)
  *val t2 = LocalDateTime.of(2015, 1, 1, 17, 0)
  *val location = SurfGeometry(new MasonGeometry(), null)
  *val activity = Activity(XX)
  *val place = Place(location, activity, Array( (t1, t2) ) ) )
  * }}}
  *
  *
  *
  * At the moment, the model doesn't simulate different days, so the companion object provides a convenience
  * method XX that will return a time period with a default day, month and year.
  * {{{
  *   val place = Place(location,activity,Place.makeOpeningTimes(9.5,17.0) )
  * }}}
  *
  * @param location     The spatial location of this Place
  * @param activityType The type of activity that can be undertaken in this Place.
  * @param openingTimes A list of tuples with opening and closing times during which
  *                     the activity can be undertaken. This is either null or an empty array if a place is open all the
  *                     time (default)
  */
case class Place (
              val location:SurfGeometry[_],
              val activityType:ActivityType,
              val openingTimes: Array[(LocalDateTime, LocalDateTime)] = Array.empty[(LocalDateTime, LocalDateTime)] ) {


}

object Place {

  //def apply(location:SurfGeometry[_], activityType: ActivityType, openingTimes: Array[(LocalDateTime, LocalDateTime)]) =
   // new Place(location, activityType, openingTimes)

  def YEAR = 2005
  def MONTH = 1
  def DAY = 1

  /**
    * Convenience method to make a time interval from an openning and closing time specified as decimal hours.
    * Year, month, and day are constant.
    *
    * @param open Opening time. E.g. 09:30 = 9.5
    * @param closed Closing time. E.g. 17:00 = 17.0
    * @return A tuple of [[java.time.LocalDateTime]] constructed from the opening and closing times.
    */
  def makeOpeningTimes(open:Double, closed:Double) : (LocalDateTime, LocalDateTime) = {
    val t1 = LocalDateTime.of(YEAR, MONTH, DAY, open.toInt, ((open - open.toInt)*60).toInt)
    val t2 = LocalDateTime.of(YEAR, MONTH, DAY, closed.toInt, ((closed - closed.toInt)*60).toInt)
    (t1,t2)
  }



}