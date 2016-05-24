package surf.abm.main

import java.lang.reflect.{Constructor, Method}

import com.typesafe.config.{Config, ConfigException}
import sim.engine.{SimState, Steppable}
import org.apache.log4j.Logger
import surf.abm.agents.abbf.ABBFOutputter


/**
  * This class delegates the outputting of model data to more specific classes. E.g. see
  * [[surf.abm.agents.abbf.ABBFOutputter]]. To tell the model to use a particular outputter, add the
  * 'Outputter' field to the configuration file. E.g.:<br/>
  *
  * <code>Outputter="surf.abm.agents.abbf.ABBFOutputter</code>
  *
  * If no configuration is present, then a default will be used (not implemented yet).
  *
  */

object OutputFactory  {
  /**
    * Initialise the Output and schedule its step method
    *
    * @param state
    */
  def apply(state: SurfABM) = {

    // Find out which Outputter to use
    try {

      val outName = SurfABM.conf.getString(SurfABM.ModelConfig+".Outputter")
      // An Outputter has been specified, try to make a class out of it.
      val cls = Class.forName(outName)
      LOG.info(s"Using the following outputter: ${cls.toString()}")

      val method : Method = cls.getMethod("apply", state.getClass);
      method.invoke(null, state);

    }
    catch {
      case e : ConfigException.Missing => { // No outputter, create a default
        //DefaultOutputter(state)
        throw new NotImplementedError("Have not implemented the default outputter")
      }
      case e: Exception => { // Some other exception with reflection
        LOG.error(s"Exception while trying to create the outputter cannot continue", e)
        throw e
      }
    }

  }

  private val LOG: Logger = Logger.getLogger(this.getClass);
}
