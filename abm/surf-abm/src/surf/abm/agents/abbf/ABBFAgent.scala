package surf.abm.agents.abbf

import sim.engine.SimState
import surf.abm.agents.abbf.activities.Activity
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
  extends UrbanAgent(state, home)
{

  /**
    * A set of of the [[surf.abm.agents.abbf.activities.Activity]]s that are driving an agent
    */
  var activities : Set[ _ <: Activity] = Set.empty

  // Amount to increase intensity by at each iteration. Set so that each activity increases by 1.0 each day
  // TODO make this internal to each activity
  private val ticksPerDay = 1440d / Clock.minsPerTick.toDouble // Minutes per day / ticks per minute = ticks per day
  private val BACKGROUND_INCREASE = (1d/ticksPerDay)

  /**
    * The current activity that the agent is doing. Can be None.
    */
  var currentActivity : Option[Activity] = None

  /**
    * Work out which activity is the most intense at the moment.
 *
    * @return
    */
  def highestActivity(): Activity = this.activities.maxBy( a => a.intensity() )


  override def step(state: SimState): Unit = {

    println(s"\n******  ${Clock.getTime.toString} ********* \n")
    //this.activities.foreach( {case (a,i) => println(s"$a : $i" )}); println("\n") // print activities


    // OLD CODE TO MAKE MAPS OF ACTIVITY -> INTENSITY
    // Begin by increasing the intensities of all activities.
    // This way does it by making a new map using 'yield'
    // this.activities = for ( (activity, intensity) <- this.activities ) yield { activity -> intensity*1.1 }
    // This way uses map() (Note that m._X give you the X element of a tuple, so m._1 is key, m._2 is the value)
    //this.activities = this.activities.map(m => (m._1, m._1.calcIntensity(1.01, m._2)) )

    // TODO: Maybe messing around with the activities should be done less frequently. Less computationally expensive and also stops frequent activity changes

    // Update all activity intensities. They should go up by one unit per day overall (TEMPORARILY)

    this.activities.foreach( a => { a += BACKGROUND_INCREASE } )
    //this.activities.foreach( a => println(s"$a : ${a.backgroundIntensity}" )); print("\n") // print activities

    // Now find the most intense one, given the current time.
    val highestActivity:Activity = this.highestActivity()
    println(s"HIGHEST: $highestActivity : ${highestActivity.intensity()}" )

    // Is the highest activity high enough to take control?
    if (highestActivity.intensity() < 0.2) {
      // Tell the current activity (if there is one) that it's no longer in control.
      this.currentActivity.foreach( a => a.activityChanged() ) // Note: the for loop only iterates if an Activity has been defined (nice!)
      //if (this.currentActivity.isDefined) this.currentActivity.get.activityChanged()
      this.currentActivity = None
      Agent.LOG.info(s"Agent ${this.id.toString()} is not doing any activity")
      return
    }

    // See if the activity has changed (taking into account that there might not be a current activity)
    if (highestActivity!=this.currentActivity.getOrElse(None)) {
      //if (this.currentActivity.isDefined) this.currentActivity.get.activityChanged() // Tell the activity that it is no longer in charge
      this.currentActivity.foreach( a => a.activityChanged())
      this.currentActivity = Some(highestActivity) // Set the new current activity
    }


    // Perform the action to satisfy the current activity
    val satisfied = highestActivity.performActivity()
    if (satisfied) {
      highestActivity-=(BACKGROUND_INCREASE * 5) // (For now, decrease at considerably less than the rate that it increases.
    }

  }



}


object ABBFAgent {
  def apply(state: SurfABM, home: SurfGeometry[Building]): ABBFAgent =
    new ABBFAgent(state, home )



}
