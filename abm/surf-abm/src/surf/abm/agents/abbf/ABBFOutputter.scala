package surf.abm.agents.abbf

import java.io.{BufferedWriter, File, FileWriter}

import org.apache.log4j.Logger
import sim.engine.{SimState, Steppable}
import surf.abm.agents.abbf.activities.{Activity, ShopActivity, SleepActivity, WorkActivity}
import surf.abm.main._

/**
  * An outputter written specifically for the ABBF agents. To use this, include the following in the configuation file:<br/>
  *
  * <code>Outputter="surf.abm.agents.abbf.ABBFOutputter"</code>
  *
  * <p>An outputter needs to implement the <code>apply(s:SurfABM)</code> method, which must initialise the output
  * and schedule a step method to be called each time output should be written. See the source for this class for an example.</p>
  */
object ABBFOutputter extends Outputter with Serializable {

  // BufferedWriters to write the output
  private var agentMainBR : BufferedWriter = null
  private var agentActivitiesBR : BufferedWriter = null

  def apply() : Outputter = {



    val AGENT_MAIN_HEADER = "Iterations,Time,Agent,Activity,x,y\n" // Main csv file; one line per agent
    val AGENT_ACTIVITY_HEADER = "Iterations,Time,Agent,Activity,Intensity,BackgroundIntensity,TimeIntensity,CurrentActivity\n" // More detailed information about all activities (multiple lines per agent)

    // Make a new directory for this model
    val dir = new File("./results/out/"+SurfABM.ModelConfig+"/"+System.currentTimeMillis()+"/")
    dir.mkdirs()
    LOG.info(s"Initialising ABBFOutputter and writing results to: $dir")


    // Create the output files
    this.agentMainBR = new BufferedWriter( new FileWriter ( new File(dir.getAbsolutePath+"/agents.csv")))
    this.agentActivitiesBR = new BufferedWriter( new FileWriter ( new File(dir.getAbsolutePath+"/agent-activities.csv")))

    // Write the headers
    agentMainBR.write(AGENT_MAIN_HEADER)
    agentActivitiesBR.write(AGENT_ACTIVITY_HEADER)

    return this

  }

  /**
    * This should be scheduled to be called at every iteration and write out model info.
    *
    * @param state
    */
  def step(state: SimState): Unit = {

    val ticks = state.schedule.getTime()
    val time = Clock.getTime
    for (i <- 0 until SurfABM.agentGeoms.getGeometries().size()) {
      val agentGeom = SurfABM.agentGeoms.getGeometries.get(i).asInstanceOf[SurfGeometry[ABBFAgent]]
      val agent = agentGeom.theObject // The object that is represented by the SurfGeometry
      val coord = agent.location().geometry.getCoordinate // The agent's location
      val act = agent.currentActivity.getOrElse(None) // The current activity. An Option, so will either be Some[Activity] or None.

      // Write the main agent file
      this.agentMainBR.write(s"${ticks},${time},${agent.id()},${act.getClass.getSimpleName},${coord.x},${coord.y}\n")

      // Now write the intensities of each activity (one line per agent-activity)
      val hour = Clock.currentHour() // Need to know the time of day for the intensity
      agent.activities.foreach(a => {
        // Find the current activity, first checking that there is an activity (it can be empty)
        val current = if (agent.currentActivity == None) 0 else { if (agent.currentActivity.get.getClass == a.getClass) 1 else 0 }
        this.agentActivitiesBR.write(s"${ticks},${time},${agent.id()},${a.getClass.getSimpleName},${a.intensity()},${a.backgroundIntensity()},${a.timeIntensity(hour)},$current\n")
      }
      )

      // Sanity check that each activity has been written (can get rid of this code later)
      /*
      var work, sleep, shop = false // Check that each activity is set
      // Iterate over all Activitiesdnd match them to the appropriate variable,
      agent.activities.foreach ( a => {
        a match {
          case x:WorkActivity   => work  = true
          case x:SleepActivity  => sleep = true
          case x:ShopActivity   => shop  = true
        } // match
      } ) // foreach
      // Each variable should have been set, or a MatchError should be thrown
      assert( ! Array(work,sleep,shop).contains(false) )
      */

    } // for geometries (agents)


  } // step()

  /**
    * This should be scheduled to be called at the end of the model to write the output files
    *
    */
  def finish() : Unit  = {
    LOG.info("Closing output files")
    this.agentActivitiesBR.close()
    this.agentMainBR.close()
  }


  private val LOG: Logger = Logger.getLogger(this.getClass);

}
