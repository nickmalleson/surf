package surf.abm

import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import sim.util.Bag

import scala.collection.mutable.ArrayBuffer

/**
  * Created by nick on 09/04/2016.
  */
object Util {

  /* ***** Configuration Object ***** */
  // By default, read the configuration for the model from a file. But also offer the option to
  // provide an alternative configuration.
  private var _conf = ConfigFactory.load("surf-abm.conf")

  /**
    * Get the Configuration object for this model. This can be used to find model parameters
    * @return The <code>Config</code> object that is being used to configure the model.
    */
  def config() = _conf // getter
  /**
    * Set the Configuration object for this model. By default, the model reads parameters from the file
    * "surf-abm.conf", but this behaviour can be overridden by providing a different Config object.
    * E.g. the following would set the type of agent to be created:
    * <pre><code>
    *   val conf = Util.config().withValue("AgentType", ConfigValueFactory.fromAnyRef("surf.abm.agents.RandomRoadAgent"))
    *   Util.config(conf)
    * </code></pre>
    *
    * @param c
    */
  def config(c : Config) = { _conf = c} // setter


  /**
    * Convert a Mason <code>Bag</code> to a scala <code>List</code>
    *
    * @param bag
    * @return
    */
  def bagToList (bag: Bag) : List[_] = {

    val a = ArrayBuffer[AnyRef]()
    for(i <- 1 until bag.size()) {
      a += bag.get(i)
    }
    return List(a:_*)
  }

}
