package surf.abm.agents.abbf

import java.io
import java.io.{BufferedWriter, File, FileWriter}
import java.time.temporal.TemporalAmount

import com.vividsolutions.jts.geom
import com.vividsolutions.jts.geom.Coordinate
import org.apache.log4j.Logger
import org.scalatest.time.Days
import sim.engine.{SimState, Steppable}
import surf.abm.agents.abbf.ABBFOutputter.AgentsToOutput
import surf.abm.agents.abbf.activities.{Activity, ShopActivity, SleepActivity, WorkActivity}
import surf.abm.main.SurfABM.conf
import surf.abm.main._

import scala.collection.mutable.ListBuffer

/**
  * An outputter written specifically for the ABBF agents. To use this, include the following in the configuation file:<br/>
  *
  * <code>Outputter="surf.abm.agents.abbf.ABBFOutputter"</code>
  *
  * <p>An outputter needs to implement the <code>apply(s:SurfABM)</code> method, which must initialise the output
  * and schedule a step method to be called each time output should be written. See the source for this class for an example.</p>
  */
object ABBFOutputter extends Outputter with Serializable {

  private val LOG: Logger = Logger.getLogger(this.getClass);

  // BufferedWriters to write the output
  private var agentMainBR : BufferedWriter = null // Locations etc. of agents at every iteration
  private var agentActivitiesBR : BufferedWriter = null // Info about agent activities
  private var cameraCountsBR: BufferedWriter = null // Camera counts
  private var agentChangeActivity: BufferedWriter = null // Information written each time an agent changes activity

  // Might only write information for some agents. This will be populated shortly
  private var AgentsToOutput : List[Int] = null


  def apply() : Outputter = {

    // See if we are going to write information for some agents, or all
    val NumAgentsToOutput = SurfABM.conf.getInt(SurfABM.ModelConfig+".NumAgentsToOutput");
    // Make a list of all agent IDs
    val agentIDs : List[Int] = { for (i <- 0 until SurfABM.agentGeoms.getGeometries().size()) yield {
      SurfABM.agentGeoms.getGeometries.get(i).asInstanceOf[SurfGeometry[ABBFAgent]].theObject.id()
    } }.toList
    ABBFOutputter.AgentsToOutput = { // Choose which IDs to write about (all, or a random sample)
      if (NumAgentsToOutput < 0) agentIDs
      else scala.util.Random.shuffle(agentIDs).take(NumAgentsToOutput)
    }
    LOG.info(s"ABBFOutputter will write information about the following agents: "+AgentsToOutput.toString())


    val AGENT_MAIN_HEADER = "Iterations,Time,Agent,Class,Activity,x,y\n" // Main csv file; one line per agent
    val AGENT_ACTIVITY_HEADER = "Iterations,Time,Agent,AgentClass,Activity,Intensity,BackgroundIntensity,TimeIntensity,CurrentActivity\n" // More detailed information about all activities (multiple lines per agent)
    val CAMERA_COUNTS_HEADER = "Camera,Date,Hour,Count\n" // Camera counts of agents passing by every hour
    val CHANGE_ACTIVITY_HEADER = "Iteration,Time,Agent,AgentClass,PreviousActivity,Px,Py,NextActivity,Nx,Ny\n" // Info about previous and next activities each time an agent changes

    // Make a new directory for this model
    val dir = new File("./results/out/"+SurfABM.ModelConfig+"/"+System.currentTimeMillis()+"/")
    dir.mkdirs()
    LOG.info(s"Initialising ABBFOutputter and writing results to: $dir")


    // Create the output files
    this.agentMainBR = new BufferedWriter( new FileWriter ( new File(dir.getAbsolutePath+"/agents.csv")))
    this.agentActivitiesBR = new BufferedWriter( new FileWriter ( new File(dir.getAbsolutePath+"/agent-activities.csv")))
    this.cameraCountsBR = new BufferedWriter( new FileWriter( new File(dir.getAbsolutePath+"/camera-counts.csv")))
    this.agentChangeActivity = new BufferedWriter( new FileWriter( new File(dir.getAbsolutePath+"/agent-change-activity.csv")))


    // Write the headers
    this.agentMainBR.write(AGENT_MAIN_HEADER)
    this.agentActivitiesBR.write(AGENT_ACTIVITY_HEADER)
    this.cameraCountsBR.write(CAMERA_COUNTS_HEADER)
    this.agentChangeActivity.write(CHANGE_ACTIVITY_HEADER)

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
    //for (i <- 0 until SurfABM.agentGeoms.getGeometries().size()) {
    for (i <- AgentsToOutput) { // Only iterate over the agents who we are outputting
      //LOG.info(i)
      val agentGeom = SurfABM.agentGeoms.getGeometries.get(i).asInstanceOf[SurfGeometry[ABBFAgent]]
      val agent = agentGeom.theObject // The object that is represented by the SurfGeometry
      val coord = agent.location().geometry.getCoordinate // The agent's location
      val act = agent.currentActivity.getOrElse(None) // The current activity. An Option, so will either be Some[Activity] or None.

      val agentClass = agent.getClass.getSimpleName // The agent's occupation class

      // Write the main agent file
      this.agentMainBR.write(s"$ticks,$time,${agent.id()},$agentClass,${act.getClass.getSimpleName},${coord.x},${coord.y}\n")

      // Now write the intensities of each activity (one line per agent-activity)
      val hour = Clock.currentHour() // Need to know the time of day for the intensity
      agent.activities.foreach(a => {
        // Find the current activity, first checking that there is an activity (it can be empty)
        val current = if (agent.currentActivity == None) 0 else { if (agent.currentActivity.get.getClass == a.getClass) 1 else 0 }
        this.agentActivitiesBR.write(s"$ticks,$time,${agent.id()},$agentClass,${a.getClass.getSimpleName},${a.intensity()},${a.backgroundIntensity()},${a.timeIntensity(hour)},$current\n")
      }
      )

     // Write information about new and previous activities each time an agent's activity changes.
      if (agent.changedActivity()) { // This agent has changed their activity. Write information about the old and new activities

        // Get the next and previous activity, checking that it not none
        val prevAct = if (agent.previousActivity() == None) None else agent.previousActivity()
        val nextAct = if (agent.currentActivity() == None)  None else agent.currentActivity()

        /*println()
        print(prevAct)
        print(nextAct)
        print(nextAct.get)
        print(nextAct.get.currentPlace())
        print(nextAct.get.currentPlace().location)
        print(nextAct.get.currentPlace().location.getGeometry)
        print(nextAct.get.currentPlace().location.getGeometry.getCentroid)
        print(nextAct.get.currentPlace().location.getGeometry.getCentroid.getCoordinate.x)
        */

        // Now get the coordinates. Note that some places don't have their location set until the next iteration
        // (sometimes places are only set once the activity is 'initialised', which happens on the iteration after
        // the activity changes.
        // IF THIS CAUSES PROBLEMS FOR STELIOS I'LL HAVE TO FIGURE OUT HOW TO EITHER GET THE LOCAITON NOW, OR DELAY
        // SETTING agent.activity_changed UNTIL THE NEXT ITERATION.
        val px = get_coord_from_activity(prevAct).x
        val py = get_coord_from_activity(prevAct).y
        val nx = get_coord_from_activity(nextAct).x
        val ny = get_coord_from_activity(nextAct).y

        // Write out the info:
        // Iteration,Time,Agent,AgentClass,PreviousActivity,Px,Py,NextActivity,Nx,Ny
        this.agentChangeActivity.write(s"$ticks,$time,${agent.id()},$agentClass,"+
          s"${prevAct.getOrElse(None).getClass.getSimpleName},${px},${py},"+
          s"${nextAct.getOrElse(None).getClass.getSimpleName},${nx},${ny},\n")

      }

    } // for geometries (agents)


  } // step()

  /**
    * Get the x,y coordinate of an activity.
    * @param activity
    * @return The Coordinate, or Coordinate(-1,-1) if the activity doesn't have a location yet
    */
  def get_coord_from_activity(activity: Option[_ <: Activity]) : Coordinate = {
    if (activity==None || activity.get.currentPlace().location==null)
      return new geom.Coordinate(-1.0,-1.0)

    return activity.get.currentPlace().location.getGeometry.getCentroid.getCoordinate
  }

  /**
    * This should be scheduled to be called at the end of the model to write the output files and (maybe) spawn a
    * knitr session to analyse them
    */
  def finish() : Unit  = {

    // Write the camera count file
    val camCounts: Map[Int, ListBuffer[Int]] = collection.immutable.Map(CameraRecorder.tempCameraMaps.toSeq: _*)

    for (c <- camCounts) {
      val camCountsIndex = c._2.zipWithIndex

      for (h <- camCountsIndex) {
        val extraDays: Int = (Clock.getStartHour + h._2) / 24
        this.cameraCountsBR.write(s"${c._1},${Clock.getStartDate.plusDays(extraDays)},${(Clock.getStartHour + h._2) % 24},${h._1}\n") // cameraID (key of map), date, hour (index of list mod 24), count (value of list)
      }

    }

    // Close files
    LOG.info("Closing output files")
    this.agentActivitiesBR.close()
    this.agentMainBR.close()
    this.cameraCountsBR.close()
    this.agentChangeActivity.close()
    // Start knitr and generate the output file
    // TODO this should generate outputs in the same directory as the results, not the same directory as the script
    /*try {
      LOG.info("Attempting to execute R results analysis")
      import sys.process._ // For executing an R script
      val cmd : String = "R -e rmarkdown::render('results/surf_results.Rmd') " // The command to execute
      val result : String = cmd !! // Run the command

      LOG.debug(s"Output from R: \n $result")

      // Try to 'open' the results file (this only works on macs I think)
      "open results/surf_results.html" !

    }
    catch {
      case e : Exception => LOG.error("Exceptoin running R results analysis", e)
    }
    finally {
      LOG.info("Finished (attempting to) run R results analysis")
    }*/


  }



}
