package surf.abm

import _root_.surf.abm.exceptions.RoutingException
import org.apache.log4j.Logger
import sim.field.geo.GeomVectorField
import sim.util.Bag
import sim.util.geo.MasonGeometry

object GISFunctions {

  private val LOG: Logger = Logger.getLogger(GISFunctions.getClass)

  private val MIN_SEARCH_RADIUS_DENOMINATOR: Double = 1000000.0

  /**
    * Find the nearest object to the given input coordinate.
    */
  def findNearestObject(centre: SurfGeometry, geom: GeomVectorField): SurfGeometry = {
    var radius: Double = SurfABM.mbr.getArea / GISFunctions.MIN_SEARCH_RADIUS_DENOMINATOR
    var closeObjects: Bag = null
    while (radius <  SurfABM.mbr.getArea) {
      closeObjects = geom.getObjectsWithinDistance(centre, radius)
      if (closeObjects.isEmpty) {
        GISFunctions.MIN_SEARCH_RADIUS_DENOMINATOR *= 0.1
        radius = SurfABM.mbr.getArea / GISFunctions.MIN_SEARCH_RADIUS_DENOMINATOR
        LOG.warn("Increasing search radius to " + radius + ". This is very inefficient if it happens regularly.")
      }
      else {
        var minDist: Double = Double.MAX_VALUE
        var dist: Double = .0
        var mg: MasonGeometry = null
        var closest: MasonGeometry = null
        import scala.collection.JavaConversions._
        for (o <- closeObjects) {
          mg = o.asInstanceOf[MasonGeometry]
          if (mg == centre) {
            continue //todo: continue is not supported
          }
          dist = centre.geometry.distance(mg.geometry)
          if (dist < minDist) {
            closest = mg
            minDist = dist
          }
        }
        assert(closest != null)
        return closest
      }
    }
    throw new RoutingException("Could not find any objects near to " + centre.toString)
  }
}