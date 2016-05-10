package surf.abm

import java.time.LocalDateTime

import org.apache.log4j.Logger
import sim.engine.{SimState, Steppable}

/**
  * Keeps track of the simulated time in the model. Mason has it's own scheduler that keeps track of ticks, this
  * class simply maps ticks onto simulated time. See create() for how to initialise the clock.
  * Use apply to get the current time. E.g.:
  *
  *
  *
  */
object Clock extends Steppable {

  private var clock : Clock = null // Clock is not defined until it has been created with create()

  /**
    * Create a new clock. This should be called at the start of the simulation, and can only be called once.
    *
    * @param state The model state. Needed to schedule the clock.
    * @param startTime The initial time (i.e. the time when ticks=0).
    * @param minsPerTick The number of simualted minutes that elapse after each tick.
    * @return
    */
  def create (
               state:SurfABM,
               startTime: LocalDateTime = LocalDateTime.of(2011, 1, 1, 0, 0),
               minsPerTick: Int = 60 ) {

    if (this.clock != null) {
      throw new Exception("Cannot create more than one Clock instance")
    }
    this.clock = new Clock(startTime, minsPerTick)
    state.schedule.scheduleRepeating(this)
    LOG.info(s"Simulation clock initialised to ${this.clock.currentTime}")
  }

  /**
    * Get the current time
    *
    * @return The current time object. It is immutable, so it doesn't matter if others manipulate it.
    */
  def apply () : LocalDateTime = {
    if (this.clock == null) {
      throw new Exception("There is no clock to get the time from. Have you called create() ?")
    }
    clock.currentTime
  }


  /**
    * The step function has been scheduled to be called at every tick and updates the simulated time.
    * It should not be called by the programmer, only by the Mason scheduler.
    */
  override def step(state: SimState): Unit = {
    assert(this.clock!=null)
    // Increment the clock by x minutes. It is immutable, so need to create a copy
    this.clock.currentTime = this.clock.currentTime.plusMinutes(this.clock.minsPerTick)
    LOG.debug(s"Stepping clock. Time: ${apply().toString}")
  }

  // underlying clock object
  private class Clock (val startTime: LocalDateTime = LocalDateTime.of(2011, 1, 1, 0, 0),
                       val minsPerTick: Int = 60 ) {

    var currentTime:LocalDateTime = startTime // Current time is initially the start time.

    assert(minsPerTick > 0)

  }

  private val LOG: Logger = Logger.getLogger(this.getClass);

}



