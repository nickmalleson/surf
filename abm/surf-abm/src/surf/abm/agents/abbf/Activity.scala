package surf.abm.agents.abbf

/**
  * Represents activities that ([[surf.abm.agents]]) can do.
  *
  * @param places      An array of [[Place]]s where this activity can undertaken.
  * @param timeProfile A definition of the times when this Activity is at its most <i>intense</i>.
  *                    E.g. work activities might be the most intense between 9am-5pm. This has the
  *                    affect of increasing/decreasing the agent's desire to undertake the activity
  *                    depending on the time.
  */
class Activity (val places: Array[Place], val timeProfile: TimeProfile) {

}


case class TimeProfile (val none: Int) {}