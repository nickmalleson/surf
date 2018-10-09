package surf.abm.agents.abbf.occupancies

import sim.util.geo.GeomPlanarGraphDirectedEdge
import surf.abm.agents.{Agent, UrbanAgent}
import surf.abm.agents.abbf.{ABBFAgent, Place, TimeProfile}
import surf.abm.agents.abbf.activities.ActivityTypes.WORKING
import surf.abm.agents.abbf.activities._
import surf.abm.environment.{Building, GeomPlanarGraphEdgeSurf}
import surf.abm.main.{Clock, SurfABM, SurfGeometry}

/**
  * Created by geotcr on 27/09/2018.
  */
class CommuterAgent(override val state:SurfABM, override val home:SurfGeometry[Building], val work: SurfGeometry[Building])
  extends ABBFAgent(state, home) {

  def defineActivities(): Unit = {

    val tempActivities = scala.collection.mutable.Set[Activity]()

    // Definition of random numbers
    val rnd = state.random.nextDouble() * 4d
    // A random number between 0 and 4
    val rndLunchPreference = state.random.nextDouble()
    // Test with a random preference for eating and leisure activities. Should become activity specific.
    val rndDinnerPreference = state.random.nextDouble() // maybe not good because should mainly be at random day in week, not only done by random agents regularly
    val rndGoingOutPreference = state.random.nextDouble() / 2d
    val rndSportPreference = state.random.nextDouble()
    val rndLunchShoppingPreference = state.random.nextDouble() / 2d

    // WORKING
    val workPlace = Place(
      location = work,
      activityType = WORKING,
      openingTimes = null // Assume it's open all the time
    )
    val workTimeProfile = TimeProfile(Array((6d, 0d), (7d + rnd, 1d), (14d + rnd, 1d), (22d, 0d)))
    val workActivity = WorkActivity(timeProfile = workTimeProfile, agent = this, place = workPlace)
    tempActivities += workActivity

    // SHOPPING
    val shoppingTimeProfile = TimeProfile(Array((7d, 0d), (11.5 + rnd/2d, rndLunchShoppingPreference), (16d + 3d*rnd/4d, 1d - rndLunchShoppingPreference), (22d, 0d)))
    val shoppingActivity = ShopActivity(timeProfile = shoppingTimeProfile, agent = this, state)
    tempActivities += shoppingActivity

    // LUNCHING
    val lunchTimeProfile = TimeProfile(Array((11d, 0d), (11.5 + rnd/2d, rndLunchPreference), (12d + rnd/2d, rndLunchPreference), (15d, 0d)))
    val lunchActivity = LunchActivity(timeProfile = lunchTimeProfile, agent = this, state)
    tempActivities += lunchActivity

    // DINNER
    val dinnerTimeProfile = TimeProfile(Array((17d, 0d), (18d + rnd/2d, rndDinnerPreference), (19.5 + rnd/2d, rndDinnerPreference), (22.5, 0d)))
    val dinnerActivity = DinnerActivity(timeProfile = dinnerTimeProfile, agent = this, state)
    tempActivities += dinnerActivity

    // GOING OUT
    val goingOutTimeProfile = TimeProfile(Array((0d, rndGoingOutPreference / 4d), (1d, 0d), (16d, 0d), (21d, rndGoingOutPreference)))
    val goingOutActivity = GoingOutActivity(timeProfile = goingOutTimeProfile, agent = this, state)
    tempActivities += goingOutActivity

    // SPORTS
    val sportTimeProfile = TimeProfile(Array((17d, 0d), (18d + rnd/2d, rndSportPreference), (22d, 0d)))
    val sportActivity = SportActivity(timeProfile = sportTimeProfile, agent = this, state)
    tempActivities += sportActivity

    // SLEEPING
    val atHomeActivity = SleepActivity(TimeProfile(Array((0d, 1d), (4d, 1d), (12d, 0d), (23d, 1d))), agent = this)
    // Increase this activity to make it the most powerful activity to begin with, but with a bit of randomness
    // (repeatedly call the ++ function to increase it)
    for (i <- 1.until( (90 + rnd * 7.5).toInt) ) {
      atHomeActivity.++()
    }
    tempActivities += atHomeActivity


    // Add these activities to the agent's activity list. At Home is the strongest initially.
    //val activities = Set[Activity](workActivity , shoppingActivity , atHomeActivity , lunchActivity , dinnerActivity, goingOutActivity )

    // Finally tell the agent about them
    //a.activities = activities
    this.activities ++= tempActivities

  }

  /**
    * The rate that agents can move is heterogeneous. At the moment, this is calculated such that if the agent is going
    * to or from work then journey will take about 30 mins. If they are doing anything else then the default rate is used
    * See [[surf.abm.agents.Agent.moveRate()]]. (That default rates is set by the BaseMoveRate parameter which is
    * defined in surf-abm.conf).
    */
  override def moveRate(): Double = {

    val act  = this.currentActivity.getOrElse(
      throw new Exception("Internal error - ABBFAgent.moveRate() has been called, but the agent doesn't have an activity")
    )
    // Is the agent travelling to work now? (If the agent has no activity then throw an exception this shouldn't have been called)
    if (act.isInstanceOf[WorkActivity])
      return _commuterMoveRate(act.asInstanceOf[WorkActivity])

    // Alternatively, are they going home from work ?
    // TODO poor assumption: if they are going home to sleep then they must be coming from work
    if (act.isInstanceOf[SleepActivity])
      return _commuterMoveRate(act.asInstanceOf[SleepActivity])
    // Only set commuting rate if current activity is sleeping and previous is working. DOESN'T WORK BECAUSE SOMETIMES THEY HAVE A NONE ACTIVITY IN BETWEEN
    //val prevAct  = this.currentActivity.getOrElse(None) // The previous activity
    //if (act.isInstanceOf[SleepActivity] && prevAct.isInstanceOf[WorkActivity])
    //    return _commuterMoveRate(prevAct.asInstanceOf[WorkActivity])

    // If here then not commuting. Use the default move rate
    return super.moveRate()
  }

  // Variable to remember the cached commuter move rate and a function to calculate it (once)
  private var _cachedCommuterMoveRate: Double = -1d
  private def _commuterMoveRate(act : FixedActivity) : Double = {
    if (_cachedCommuterMoveRate == -1d) { // Need to work out what the commuting rate is for this agent
      // Make a route from home to work
      val path: List[GeomPlanarGraphDirectedEdge] = UrbanAgent.findNewPath(this.home, act.place.location)
      // Calculate its length (at least between the junctions)
      var length = 0d // The total length of their commute
      path.foreach( length += _.getEdge.asInstanceOf[GeomPlanarGraphEdgeSurf[_]].getLine.getLength)
      // Calculate new move rate. If it is less than the default move rate, then just use that
      val iterPerMin = ( 1d / Clock.minsPerTick ) // Iterations per minute
      val moveRate = length / ( iterPerMin * CommuterAgent.COMMUTE_TIME_MINS ) // distance to travel per iteration in order to move distance in X minutes
      _cachedCommuterMoveRate = if (moveRate < super.moveRate()) super.moveRate() else moveRate
      Agent.LOG.debug(this, s"commuterMoveRate: commute length is ${length} so move rate is ${_cachedCommuterMoveRate}")
      /*println("*********")
      println(length)
      println(iterPer30Min)
      println(super.moveRate())
      println(_cachedCommuterMoveRate)*/
    }

    return _cachedCommuterMoveRate
  }

}

object CommuterAgent {

  def apply(state: SurfABM, home: SurfGeometry[Building], work: SurfGeometry[Building]): CommuterAgent =
    new CommuterAgent(state, home, work )


  /**
    * The number of minutes that agents should spend commuting (used temporarily to balance the requirements of agents
    * who have to travel long distances. At some point commute will depend on the route and method of travel, and
    * the activities will have to be balanced more intelligently so that
    */
  private val COMMUTE_TIME_MINS = 30d


}
