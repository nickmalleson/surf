package surf.abm.agents.abbf

import sim.engine.SimState
import sim.util.geo.{GeomPlanarGraphDirectedEdge, GeomPlanarGraphEdge}
import surf.abm.agents.abbf.activities.ActivityTypes.SHOPPING
import surf.abm.agents.abbf.activities._
import surf.abm.agents.{Agent, UrbanAgent}
import surf.abm.environment.Building
import surf.abm.exceptions.RoutingException
import surf.abm.main.{Clock, SurfABM, SurfGeometry}

/**
  *
  * @param state A pointer to the main model instance (<code>SurfABM</code>)
  * @param home The agent's home (starting location). Usually a <code>Building</code>
  *             where (e.g.) they live, but not necessarily. The agent's initial
  *             location is set to be <code>home</code>
  */
class ABBFAgent(override val state:SurfABM, override val home:SurfGeometry[Building])
  extends UrbanAgent(state, home) {

  /**
    * A set of of the [[surf.abm.agents.abbf.activities.Activity]]s that are driving an agent
    */
  var activities: Set[_ <: Activity] = Set.empty



  /**
    * The current activity that the agent is doing. Can be None.
    */
  var currentActivity: Option[_ <: Activity] = None

  /**
    * The previous activity that the agent was doing. Can be none.
    */
  var previousActivity: Option[_ <: Activity] = None

  /**
    * Work out which activity is the most intense at the moment.
    *
    * @return
    */
  def highestActivity(): Activity = this.activities.maxBy(a => a.intensity())


  override def step(state: SimState): Unit = {

    //println(s"\n******  ${Clock.getTime.toString} ********* \n")
    //this.activities.foreach( {case (a,i) => println(s"$a : $i" )}); println("\n") // print activities


    // OLD CODE TO MAKE MAPS OF ACTIVITY -> INTENSITY
    // Begin by increasing the intensities of all activities.
    // This way does it by making a new map using 'yield'
    // this.activities = for ( (activity, intensity) <- this.activities ) yield { activity -> intensity*1.1 }
    // This way uses map() (Note that m._X give you the X element of a tuple, so m._1 is key, m._2 is the value)
    //this.activities = this.activities.map(m => (m._1, m._1.calcIntensity(1.01, m._2)) )

    // TODO: Maybe messing around with the activities should be done less frequently. Less computationally expensive and also stops frequent activity changes
    // Update all activity intensities. They should go up by one unit per day overall (TEMPORARILY)
    // Now an activity-specific test. These settings should be moved to activity classes...
    for (a <- this.activities){
      //printf("a = %s \n",a.toString())
      if (a.toString == "ShopActivity") {
        a += 1d / (3d * ABBFAgent.ticksPerDay)
      } else if (a.toString == "LunchActivity") {
        // don't increase => lunch is determined with random time intensity that stays 0 outside lunch hours
      } else {
        a += 1d / ABBFAgent.ticksPerDay
      }
    }
    if (this.currentActivity.isDefined) {
      //printf("currentAct = %s \n",this.currentActivity.toString)
      if (this.currentActivity.toString == "Some(ShopActivity)" || this.currentActivity.toString == "Some(LunchActivity)") {
        //ABBFAgent.BACKGROUND_INCREASE = 1d / (3d * ABBFAgent.ticksPerDay)
        ABBFAgent.REDUCE_ACTIVITY = 40d / (3d * ABBFAgent.ticksPerDay)
        //printf("Shopping: reduce with %7.5f \n", ABBFAgent.REDUCE_ACTIVITY)
      } else {
        ABBFAgent.REDUCE_ACTIVITY = 2.5 / ABBFAgent.ticksPerDay
        //ABBFAgent.REDUCE_ACTIVITY = 2.5 * ABBFAgent.BACKGROUND_INCREASE
        //printf("Not shopping: reduce with %7.5f \n", ABBFAgent.REDUCE_ACTIVITY)
      }
    }
    /*this.activities.foreach(a => {
      a += ABBFAgent.BACKGROUND_INCREASE
    })*/
    //this.activities.foreach( a => println(s"$a : ${a.backgroundIntensity}" )); print("\n") // print activities

    // Check that the agent has increased the current activity by a sufficient amount before changing *and* it
    // is possible to decrease the amount further (if it is almost zero then don't keep going, even if the agent
    // has only been working on the activity for a short while!)
    if (
      ( this.currentActivity.isDefined ) &&
      ( this.currentActivity.get.currentIntensityDecrease() < ABBFAgent.MINIMUM_INSTENSITY_DECREASE ) &&
      ( this.currentActivity.get.-=(d=ABBFAgent.REDUCE_ACTIVITY, simulate=true) ) // Check that the intensity can be reduced
      ) {
      Agent.LOG.debug(s"Activity (${this.currentActivity.toString}) increase for ${this.toString()} = ${this.currentActivity.get.currentIntensityDecrease()} < ${ABBFAgent.MINIMUM_INSTENSITY_DECREASE}")
    }
    else {
      // Either there is no activity at the moment, or it has been worked on sufficiently to decrease intensity by a threshold. So now see if it should change.

      // Now find the most intense one, given the current time.
      val highestActivity: Activity = this.highestActivity()
      //println(s"HIGHEST: $highestActivity : ${highestActivity.intensity()}" )

      // Is the highest activity high enough to take control?
      if (highestActivity.intensity() < ABBFAgent.HIGHEST_ACTIVITY_THRESHOLD) {
        // If not, then make the current activity None
        Agent.LOG.debug(s"${this.toString()}: Highest activity ${this.highestActivity()} not high enough (${this.highestActivity().intensity()}, setting to None")
        this.changeActivity(None)
        //Agent.LOG.debug(s"Agent ${this.id.toString()} is not doing any activity")
        return // No point in continuing
      }

      // See if the activity needs to change (taking into account that there might not be a current activity)
      if (highestActivity != this.currentActivity.getOrElse(None)) {
        this.changeActivity(Some(highestActivity))
        //Agent.LOG.debug(s"Agent ${this.id.toString()} has changed current activity to ${this.currentActivity.get.toString}")
      }

    }
    // Perform the action to satisfy the current activity
    val satisfied = highestActivity.performActivity()
    if (satisfied) {
      if (highestActivity().toString == "ShopActivity" || highestActivity().toString == "LunchActivity") {
        //printf("Shop: %s\n",highestActivity().toString())
        highestActivity -= 40d / (3d * ABBFAgent.ticksPerDay)
      } else {
        //printf("Else: %s\n",highestActivity().toString())
        highestActivity -= 2.5 / ABBFAgent.ticksPerDay
      }
      //highestActivity -= (ABBFAgent.REDUCE_ACTIVITY) // (For now, just decrease at a constant rate proportional to increase)
    }

  }

  /**
    * Perform the necessary things to make this agent change its activity.
    *
    * @param newActivity
    */
  private def changeActivity(newActivity: Option[Activity]): Unit = {
    // Tell the current activity (if there is one) that it's no longer in control.
    this.currentActivity.foreach(a => a.activityChanged()) // Note: the for loop only iterates if an Activity has been defined (nice!)
    this.previousActivity = this.currentActivity // Remember what the current activity was
    this.currentActivity = newActivity
    Agent.LOG.debug(s"${this.toString()} has changed activity from ${this.previousActivity.getOrElse("[None]")} to ${this.currentActivity.getOrElse("[None]")}")
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
      path.foreach( length += _.getEdge.asInstanceOf[GeomPlanarGraphEdge].getLine.getLength)
      // Calculate new move rate. If it is less than the default move rate, then just use that
      val iterPerMin = ( 1d / Clock.minsPerTick ) // Iterations per minute
      val moveRate = length / ( iterPerMin * ABBFAgent.COMMUTE_TIME_MINS ) // distance to travel per iteration in order to move distance in X minutes
      _cachedCommuterMoveRate = if (moveRate < super.moveRate()) super.moveRate() else moveRate
      Agent.LOG.debug(s"commuterMoveRate: commute length for ${this.toString()} is ${length} so move rate is ${_cachedCommuterMoveRate}")
      /*println("*********")
      println(length)
      println(iterPer30Min)
      println(super.moveRate())
      println(_cachedCommuterMoveRate)*/
    }

    return _cachedCommuterMoveRate
  }

}


object ABBFAgent {
  def apply(state: SurfABM, home: SurfGeometry[Building]): ABBFAgent =
    new ABBFAgent(state, home )

  /**
    * The limit for an activity intensity being large enough to take control of the agent. Below this, the activity
    * is not deemed intense enough.
    */
  private val HIGHEST_ACTIVITY_THRESHOLD = 0.2

  /**
    * The minimum amount that the intensity of an activity must decrease before the agent stops trying to satisfy it.
    * This prevents the agents quickly switching from one activity to another
    */
  private val MINIMUM_INSTENSITY_DECREASE = 0.1

  assert(HIGHEST_ACTIVITY_THRESHOLD > MINIMUM_INSTENSITY_DECREASE ) // Otherwise background activity intensities could be reduced below 0

  /**
    * The number of minutes that agents should spend commuting
    */
  private val COMMUTE_TIME_MINS = 30d



  private val ticksPerDay = 1440d / Clock.minsPerTick.toDouble // Minutes per day / minutes per tick = ticks per day
  /**
    * Amount to increase intensity by at each iteration. Set so that each activity increases by 1.0 each day
    */
  private var BACKGROUND_INCREASE = (1d / ticksPerDay)

  /**
    * Amount to reduce the intensity by if the agent is satisfying it
    */
  private var REDUCE_ACTIVITY = BACKGROUND_INCREASE * 2.5



}
