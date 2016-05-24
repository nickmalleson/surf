package surf.abm.agents.abbf

import java.io.{BufferedWriter, File, FileWriter}

import org.apache.log4j.Logger
import sim.engine.{SimState, Steppable}
import surf.abm.agents.abbf.activities.{Activity, SleepActivity, ShopActivity, WorkActivity}
import surf.abm.main.{Clock, OutputFactory, SurfABM, SurfGeometry}

/**
  * An outputter written specifically for the ABBF agents. To use this, include the following in the configuation file:<br/>
  *
  * <code>Outputter="surf.abm.agents.abbf.ABBFOutputter"</code>
  *
  * <p>An outputter needs to implement the <code>apply(s:SurfABM)</code> method, which must initialise the output
  * and schedule a step method to be called each time output should be written. See the source for this class for an example.</p>
  */
object ABBFOutputter extends Steppable with Serializable {

  // BufferedWriters to write the output
  private var agentMainBR : BufferedWriter = null
  private var agentActivitiesBR : BufferedWriter = null

  def apply(state: SurfABM) : Unit = {

    state.schedule.scheduleRepeating(this, Int.MinValue, 1)

    val AGENT_MAIN_HEADER = "Iterations, Time, Agent, Activity, x, y\n" // Main csv file; one line per agent
    val AGENT_ACTIVITY_HEADER = "Iterations, Time, Agent, WorkActivity, SleepActivity, GoHomeActivity, ShopActivity\n" // More detailed information about all activities

    // Make a new directory for this model
    val dir = new File("./out/"+SurfABM.ModelConfig+"/"+System.currentTimeMillis()+"/")
    dir.mkdirs()
    LOG.info(s"Initialising ABBFOutputter and writing results to: $dir")


    // Create the output files
    this.agentMainBR = new BufferedWriter( new FileWriter ( new File(dir.getAbsolutePath+"/agents.csv")))
    this.agentActivitiesBR = new BufferedWriter( new FileWriter ( new File(dir.getAbsolutePath+"/agent-activities.csv")))

    // Write the headers
    agentMainBR.write(AGENT_MAIN_HEADER)
    agentActivitiesBR.write(AGENT_ACTIVITY_HEADER)

  }

  /**
    * This is scheduled to be called at every iteration and write out model info.
    *
    * @param state
    */
  def step(state: SimState): Unit = {

    LOG.info("WRITING OUTPUT")
    val ticks = state.schedule.getTime()
    val time = Clock.getTime
    for (i <- 0 to SurfABM.agentGeoms.getGeometries.size()) {
      val agentGeom = SurfABM.agentGeoms.getGeometries.get(i).asInstanceOf[SurfGeometry[ABBFAgent]]
      val agent = agentGeom.theObject // The object that is represented by the SurfGeometry
      val coord = agent.location().geometry.getCoordinate // The agent's location
      val act = agent.currentActivity.getClass().toString

      // Write the main agent file
      this.agentMainBR.write(s"${ticks},${time},${agent.id()},${act},${coord.x},${coord.y},")

      // Need the intensities of each activity to write the main file

      var work, sleep, gohome, shop = -1d // The intensities
      val hour = Clock.currentHour() // Need to know the time of day for the intensity
      // Iterate over all Activities, find their intensity, and match them to the appropriate variable,
      agent.activities.foreach ( a => {
        a match {
          case x:WorkActivity   => work  = x.getIntensity(Clock.currentHour)
          case x:SleepActivity  => sleep = x.getIntensity(Clock.currentHour)
          case x:SleepActivity => work  = x.getIntensity(Clock.currentHour)
          case x:ShopActivity   => shop  = x.getIntensity(Clock.currentHour)
        } // match
      } ) // foreach
      // Each variable should have been set, or a MatchError should be thrown
      assert(work != -1d && sleep != -1d && gohome != -1d && shop != -1d)
    } // for geometries



    //this.agentMainBR.write(s"${},${},${},${},${},${},")

  }


  private val LOG: Logger = Logger.getLogger(this.getClass);

}
