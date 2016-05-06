package surf.abm.agents.abbf

import java.time.{LocalDateTime, Instant}

import surf.abm.SurfGeometry

/**
  * A places where an activities can be undertaken.
  *
  * @param location The spatial location of this Place
  * @param activity The [[surf.abm.agents.abbf.Activity]] that can be undertaken in this
  *                 [[surf.abm.agents.abbf.Place]]
  * @param openingTimes A list of tuples with opening and closing times during which
  *                      the activity can be undertaken
  */
class Place (
              val location:SurfGeometry[_],
              val activity:Activity,
              val openingTimes: Array[(LocalDateTime, LocalDateTime)]) {


}
