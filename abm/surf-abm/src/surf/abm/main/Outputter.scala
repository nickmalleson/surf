package surf.abm.main

import java.lang.reflect.Method

import com.typesafe.config.ConfigException
import org.apache.log4j.Logger
import sim.engine.{SimState, Steppable}

/**
  * Trait for classes that write information about the model. Outputters are created by the  [[surf.abm.main.OutputFactory]].
  * For an example implementation, see [[surf.abm.agents.abbf.ABBFOutputter]].
  */
trait Outputter extends Steppable{
  /**
    * Initialise the outputter. E.g. Create output files etc.
    * @return
    */
  def apply() : Outputter

  /**
    * Will be called at each iteration (scheduled by [[surf.abm.main.OutputFactory]]).
    * @param state
    */
  def step(state: SimState): Unit

  /**
    * Will be called at the end of the simulation, e.g. to close files etc. Scheduled by [[surf.abm.main.SurfABM]].
    */
  def finish(): Unit
}


/**
  * This class delegates the outputting of model data to more specific classes. E.g. see
  * [[surf.abm.agents.abbf.ABBFOutputter]]. To tell the model to use a particular outputter, add the
  * 'Outputter' field to the configuration file. E.g.:<br/>
  *
  * <code>Outputter="surf.abm.agents.abbf.ABBFOutputter</code><br/>
  *
  * <p>If no configuration is present, then a default will be used (not implemented yet).</p>
  *
  * <p>The OutputFactory also keeps a reference to the outputter that it creates so it can be accesssed again later
  * through apply()</p>
  *
  */

object OutputFactory  {

  private var _outputter : Outputter = null // Remember the outputter that we create


  /**
    * Initialise the Output and schedule its step method
    *
    * @param state
    * @return A new Outputter, or the one created prviously if this method has already been called.
    *
    */
  def apply(state: SurfABM) : Outputter = {

    if (_outputter == null) { // Make a new outputter, call its apply method to initialise and schedule its step method

      // Find out which Outputter to use
      try {

        val outName = SurfABM.conf.getString(SurfABM.ModelConfig + ".Outputter")
        // An Outputter has been specified, try to make a class out of it.
        val cls = Class.forName(outName)
        LOG.info(s"Using the following outputter: ${cls.toString()}")

        val applyMethod: Method = cls.getMethod("apply");
        val o = applyMethod.invoke(null).asInstanceOf[Outputter];
        this._outputter = o
        state.schedule.scheduleRepeating(this._outputter, Int.MinValue, 1)

      }
      catch {
        case e: ConfigException.Missing => {
          // No outputter, create a default
          this._outputter = DefaultOutputter()
          state.schedule.scheduleRepeating(this._outputter, Int.MinValue, 1)
        }
        case e: Exception => {
          // Some other exception with reflection
          LOG.error(s"Exception while trying to create the outputter. Cannot continue", e)
          throw e
        }
      }
    }
    return this._outputter

  }

  private val LOG: Logger = Logger.getLogger(this.getClass);
}



/**
  * A default Outputter that writes basic information that all models will have in common.
  */
object DefaultOutputter extends Outputter with Serializable {
  /**
    * Initialise the outputter. E.g. Create output files etc.
    *
    * @return
    */
  override def apply(): Outputter = {

    LOG.warn("DefaultOutputter has not been implemented")

    return this
  }

  /**
    * Will be called at the end of the simulation, e.g. to close files etc. Scheduled by [[surf.abm.main.SurfABM]].
    */
  override def finish(): Unit = {

    LOG.warn("DefaultOutputter has not been implemented")
  }

  /**
    * Will be called at each iteration (scheduled by [[surf.abm.main.OutputFactory]]).
    *
    * @param state
    */
  override def step(state: SimState): Unit = {

    LOG.warn("DefaultOutputter has not been implemented")
  }


  private val LOG: Logger = Logger.getLogger(this.getClass);
}