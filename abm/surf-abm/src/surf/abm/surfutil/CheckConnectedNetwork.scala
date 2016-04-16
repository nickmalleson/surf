package surf.abm.surfutil

import java.io.File
import java.net.URL

import org.apache.log4j.Logger
import sim.field.geo.GeomVectorField
import sim.io.geo.ShapeFileImporter
import surf.abm.SurfABM

/**
  * Checks that the input road network is connected. If not, log a list of roads that are disconnected.
  * Assumes that roads have an ID field.
  * Usage: scala surf.abm.surfutil.CheckConnectedNetwork [roads_file.shp]
  * Note: the classpath needs to be configured. See scripts/checkConnectedNetwork.sh.
  */
case object CheckConnectedNetwork {

  private val LOG: Logger = Logger.getLogger(this.getClass);

  def main(args: Array[String]): Unit = {

      if (args.length != 0) {
        this.LOG.error("The program expects one command-line argument; an input roads shapefile.")
        this.LOG.error("Usage: scala surf.abm.surfutil.CheckConnectedNetwork [roads_file.shp]")
        throw new Exception("Incorrect number of command line arguments: %d".format(args.length))
      }
      val roadsFileName = args(0)
      this.LOG.info("Checking that road network in '%s' is connected".format(roadsFileName))
      val roadsURI= new File(roadsFileName).toURI().toURL()
      val roadsField = new GeomVectorField(SurfABM.WIDTH, SurfABM.HEIGHT)
      ShapeFileImporter.read(roadsURI, roadsField)


    /*catch {
      case e: Exception => {
        this.LOG.error("Exception thrown in main loop", e)
        throw e
      }
    }*/

  } // main()

}
