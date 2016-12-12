package surf.dda

import org.apache.log4j.Logger
import sim.engine.{Schedule, SimState, Steppable}
import sim.field.grid.{DoubleGrid2D, IntGrid2D, SparseGrid2D}

/**
  * Created by nick on 25/11/2016.
  */
class SurfDDA (seed:Long) extends SimState(seed) {

  override def start() {
    super.start() // clear out the schedule
    // Create the world
    SurfDDA.world = new SparseGrid2D(SurfDDA.GRID_WIDTH, SurfDDA.GRID_HEIGHT)

    // Create the agents
    for (i <- 0.until(SurfDDA.NUM_AGENTS)) {
      // Create a new a agent, passing the main model instance and a random new location
      val a = Agent(this)
      SurfDDA.world.setObjectLocation(a,50, 50)

      // Schedule all agents to move at every iteration
      schedule.scheduleRepeating(Schedule.EPOCH , 0, a, 1)

    }

  }

  override def finish(): Unit = {
    super.finish()
  }

}


/**
  * The SurfDDA companion object is the main entry point for the model and also contains some static variables.
  */
object SurfDDA {

  private def apply(l:Long) = new SurfDDA(l) // I don't think that this should ever be called. doLoop creates a new object

  val NUM_AGENTS = 100
  val GRID_WIDTH = 100
  val GRID_HEIGHT = 100


  // Define the environment
  var world : SparseGrid2D = null // The whole world







  /* Main application entry point */
  def main(args: Array[String]): Unit = {

    try {
      LOG.debug("Beginning do loop")
      SimState.doLoop(classOf[SurfDDA], args);
      LOG.debug("Finished do loop")
    }
    catch {
      case e: Exception => {
        LOG.error("Exception thrown in main loop.", e)
        throw e
      }
    }
  }

}
