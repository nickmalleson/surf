package surf.abm.agents.abbf

import sim.engine.SimState
import surf.abm.agents.abbf.activities.Activity
import surf.abm.agents.{Agent, UrbanAgent}
import surf.abm.environment.Building
import surf.abm.exceptions.RoutingException
import surf.abm.{Clock, SurfABM, SurfGeometry}

/**
  *
  * @param state A pointer to the main model instance (<code>SurfABM</code>)
  * @param home The agent's home (starting location). Usually a <code>Building</code>
  *             where (e.g.) they live, but not necessarily. The agent's initial
  *             location is set to be <code>home</code>
  * @param activities A map of the [[surf.abm.agents.abbf.activities.Activity]]s that are driving an agent, along
  *                   with the current intensity of the activity. Private because it's a var and we don't
  *                   want other classes changing it later.
  */
class ABBFAgent(val state:SurfABM, val home:SurfGeometry[Building], private var activities: Set[Activity]) extends UrbanAgent(state, home) {

 // Temporary variables while the agent just walks from home and back.
  var goingHome = false
  // Find where the agent works (specified temporarily in the config file)
  val workBuilding = SurfABM.buildingIDGeomMap(SurfABM.conf.getInt(SurfABM.ModelConfig+".WorkAddress"))


  // XXXX FIX THE INCREMENT - I DON"T THINK IT ADDS UP TO 1.0 OVER THE WHOLE DAY


  // AMount to increase intensity by at each iteration. Set so that each activity increases by 1.0 each day
  private val ticksPerDay = 1440d / Clock.minsPerTick.toDouble // Minutes per day / ticks per minute = ticks per day
  private val BACKGROUND_INCREASE = (1d/ticksPerDay)


  override def step(state: SimState): Unit = {

    println(s"\n******  ${Clock.getTime.toString} ********* \n")
    //this.activities.foreach( {case (a,i) => println(s"$a : $i" )}); println("\n") // print activities
    this.activities.foreach( a => println(s"$a : ${a.tempGetBackgroundIntensity}" )); println("\n") // print activities


    // OLD CODE TO MAKE MAPS OF ACTIVITY -> INTENSITY
    // Begin by increasing the intensities of all activities.
    // This way does it by making a new map using 'yield'
    // this.activities = for ( (activity, intensity) <- this.activities ) yield { activity -> intensity*1.1 }
    // This way uses map() (Note that m._X give you the X element of a tuple, so m._1 is key, m._2 is the value)
    //this.activities = this.activities.map(m => (m._1, m._1.calcIntensity(1.01, m._2)) )

    // TODO: Maybe messing around with the activities should be done less frequently. Less computationally expensive and also stops frequent activity changes

    // Update all activity intensities. They should go up by one unit per day overall (TEMPORARILY)

    this.activities.foreach(a => a.incrementIntensity(BACKGROUND_INCREASE) )
    this.activities.foreach( a => println(s"$a : ${a.tempGetBackgroundIntensity}" )); println("\n") // print activities

    // Now find the most intense one, given the current time.
    val highestActivity:Activity = this.activities.maxBy( a => a.getIntensity(Clock.currentHour) )

    println(s"HIGHEST: $highestActivity : ${highestActivity.getIntensity(Clock.currentHour)}" )







    // TODO implement step!!

    // Temporary code
    try {
      if (this.destination.isEmpty || this.atDestination) {

        if (goingHome) {
          Agent.LOG.debug("Agent "+ this.id.toString() + " has arrived home. Going to work")
          goingHome = false
          this._destination = Option(workBuilding)
          this._atDestination = false
          this.findNewPath() // Set the Agent's path variable (the roads it must pass through)
        }
        else {
          Agent.LOG.debug("Agent "+ this.id.toString() + " has arrived at work. Going home")
          goingHome = true
          this._destination = Option(this.home)
          this._atDestination = false
          this.findNewPath() // Set the Agent's path variable (the roads it must pass through)

        }

      }
      assert(this.path != null, "The path shouldn't be null (for agent %s)".format(this.id))
      this.moveAlongPath()
    }
    catch {
      case ex: RoutingException => {
        Agent.LOG.error("Error routing agent " + this.toString() + ". Exitting.", ex)
        state.finish
      }
    }

  }



}

object ABBFAgent {
  def apply(state: SurfABM, home: SurfGeometry[Building], activities: Set[Activity]): ABBFAgent =
    new ABBFAgent(state, home, activities)



}
