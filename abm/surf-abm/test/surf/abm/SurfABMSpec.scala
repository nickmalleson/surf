package surf.abm

import _root_.surf.abm.tests.UnitSpec
import org.apache.log4j.Logger
import sim.engine.SimState
import com.typesafe.config.{Config, ConfigValueFactory}
import org.scalatest.Ignore
import surf.abm.surfutil.Util

import scala.util.control.Breaks._

/**
  * Test class for the SurfABM class.
  * This uses FlatSpec structure: http://doc.scalatest.org/2.2.6/#org.scalatest.FlatSpec
  * Created by nick on 24/03/2016.
  */
@Ignore
class SurfABMSpec extends UnitSpec {

  // Tests consist of a *subject*, a *verb* (either 'should', 'must', or 'can') and the rest of the sentence

  "A SurfABM" should "load without errors" in {
    SurfABM(1l) // This initialises a model but doesn't start it running
  }

  //TODO Complete these tests

  it should "run with RandomWalkAgents for a while without errors" in {

    // TODO HERE - NEED TO 'reset' THE MODEL, i.e. de-initialise SurfABM so that the
//    configuration is reloaded.

    // Create a bespoke Config object to tell the model to load different kinds of agents
    val default = Util.config() // Get the default configuration (reads a file)
    // Now make a new configuration, setting a different type of agent
    val conf = default.withValue("AgentType", ConfigValueFactory.fromAnyRef("surf.abm.agents.RandomWalkAgent"))
    Util.config(conf) // Set the new config object

    runModel()
  }

  it should "run with RandomRoadAgents for a while without errors " in {

    // Create a bespoke Config object to tell the model to load different kinds of agents
    val default = Util.config() // Get the default configuration (reads a file)
    // Now make a new configuration, setting a different type of agent
    val conf = default.withValue("AgentType", ConfigValueFactory.fromAnyRef("surf.abm.agents.RandomRoadAgent"))
    Util.config(conf) // Set the new config object

    runModel()

  }


  it should "throw an error if it can't instantiate the agent correctly" in {

    // TODO Indicate that an error is expected in this text

    // Create a bespoke Config object to tell the model to load different kinds of agents
    val default = Util.config() // Get the default configuration (reads a file)
    // Now make a new configuration, setting a different type of agent
    val conf = default.withValue("AgentType", ConfigValueFactory.fromAnyRef("surf.abm.agents.RandomRoadAgent"))
    Util.config(conf) // Set the new config object

    runModel()


  }



  def runModel() = {
    // Run the model for 100,000 iterations, printint out the time every 5,000
    // Note that SurfABM.main() isn't called here, because that would use Mason's doLoop(), which
    // exits after the model has finished. We don't want to call System.exit() because it will
    // abort the tests.

    // (P.S. main() could be called like this if we wanted to:
    //val args = Array("-until", "100000", "-time", "5000")
    //SurfABM.main(args) // pass main a new, empty string array (no command-line arguments)
    val jobs = 1; // number of runs
    val state:SimState = SurfABM(System.currentTimeMillis()); // MyModel is our SimState subclass state.nameThread();
    for( job <- 0 to jobs) {
      state.setJob(job)
      state.start()
      breakable { // requires scala.util.control.Breaks._ . See https://stackoverflow.com/questions/2742719/how-do-i-break-out-of-a-loop-in-scala
        do {
          if (!state.schedule.step(state)) {
            break
          }
          if ( state.schedule.getSteps() % 50000 == 0 ) {
            SurfABMSpec.LOG.info("Step: "+state.schedule.getSteps())
          }
        }
        while(state.schedule.getSteps() < 100000)
        state.finish();
      }
    }
  }

}

object SurfABMSpec {
  private val LOG: Logger = Logger.getLogger(SurfABMSpec.getClass);
}
