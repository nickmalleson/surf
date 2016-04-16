package surf.abm.surfutil

import org.apache.log4j.Logger

/**
  * Checks that the input road network is connected. If not, log a list of roads that are disconnected.
  * Assumes that roads have an ID field.
  * Usage: scala surf.abm.surfutil.CheckConnectedNetwork [roads_file.shp]
  * Note: the classpath needs to be configured. See scripts/checkConnectedNetwork.sh.
  */
case object CheckConnectedNetwork {

  private val LOG: Logger = Logger.getLogger(this.getClass);

  def main(args: Array[String]): Unit = {

    try {
      if (args.length != 0) {
        this.LOG.error("The program expects one command-line argument; an input roads shapefile.")
        this.LOG.error("Usage: scala surf.abm.surfutil.CheckConnectedNetwork [roads_file.shp]")
        return
      }
      val roadsFile = args(0)
      this.LOG.info("Checking that road network in '%s' is connected".format(roadsFile))


    }
    catch {
      case e: Exception => {
        this.LOG.error("Exception thrown in main loop", e)
        throw e
      }
    }
  }

}
