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

    }

}

object SurfDDA {

  def apply(l:Long) = new SurfDDA(l)

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
