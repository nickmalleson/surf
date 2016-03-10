package surf.abm

import sim.engine.SimState;

/**
  * Main class for the surf agent-based model
  */
class SurfABM(seed:Long) extends SimState (seed:Long) {



}


object SurfABM {

  def main(args: Array[String]) {
    try {
      SimState.doLoop(classOf[SurfABM], args)
    }
    catch {
      case e: Exception => {
        //SurfABM.LOG.error("Exception thrown in main loop", e)
        println("Exception thrown in main loop", e)
      }
    }
    System.exit(0)
  }

}