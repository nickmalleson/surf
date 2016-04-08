package surf.abm

import _root_.surf.abm.exceptions.RoutingException
import org.apache.log4j.Logger
import sim.field.geo.GeomVectorField
import sim.util.Bag
import scala.util.control.Breaks._

object GISFunctions {

  private val LOG: Logger = Logger.getLogger(GISFunctions.getClass)

  private var MIN_SEARCH_RADIUS_DENOMINATOR: Double = 1000000.0

  /**
    * Find the nearest object to the given input coordinate.
    *
    */
  def findNearestObject(centre: SurfGeometry[_], geom: GeomVectorField) : SurfGeometry = {
    var radius: Double = SurfABM.mbr.getArea / GISFunctions.MIN_SEARCH_RADIUS_DENOMINATOR
    var closeObjects: Bag = null
    var closest: SurfGeometry = null
    while (radius < SurfABM.mbr.getArea) {
      closeObjects = geom.getObjectsWithinDistance(centre, radius)
      if (closeObjects.isEmpty) {
        GISFunctions.MIN_SEARCH_RADIUS_DENOMINATOR = GISFunctions.MIN_SEARCH_RADIUS_DENOMINATOR * 0.1
        radius = SurfABM.mbr.getArea / GISFunctions.MIN_SEARCH_RADIUS_DENOMINATOR
        LOG.warn("Increasing search radius to " + radius + ". This is very inefficient if it happens regularly.")
      }
      else {
        var minDist = Double.MaxValue
        var dist = 0.0
        for (o <- closeObjects) {
          val sg = o match {
            // Cast to a MasonGeometry
            case x: SurfGeometry => x
            case _ => throw new ClassCastException
          }
          if (sg != centre) {
            dist = centre.geometry.distance(sg.geometry)
            if (dist < minDist) {
              closest = sg
              minDist = dist
            }
          }
        } // for close objects
        assert(closest != null)
        return closest
      } // else
    } // while searchRadius
    throw new RoutingException("Could not find any objects near to " + centre.toString)
  } // findNearestObject

}