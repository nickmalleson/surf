package surf.abm

import _root_.surf.abm.exceptions.RoutingException
import org.apache.log4j.Logger
import sim.field.geo.GeomVectorField
import sim.util.Bag
import sim.util.geo.MasonGeometry

object GISFunctions {

  private val LOG: Logger = Logger.getLogger(GISFunctions.getClass)

  private var MIN_SEARCH_RADIUS_DENOMINATOR: Double = 1000000.0

  /**
    * Find the nearest object to the given input coordinate.
    * The function will search within a given radius (<code>GISFunctions.MIN_SEARCH_RADIUS_DENOMINATOR</code>),
    * gradually expanding the circle until it finds an object. It logs a WARNING each time the radius is
    * increased.
    */
  def findNearestObject(centre: MasonGeometry, geom: GeomVectorField) : MasonGeometry = {
    var radius: Double = SurfABM.mbr.getArea / GISFunctions.MIN_SEARCH_RADIUS_DENOMINATOR
    var closest: MasonGeometry = null
    while (radius < SurfABM.mbr.getArea) {
      val bag : Bag = geom.getObjectsWithinDistance(centre, radius)
      val closeObjects : List[_]  = Util.bagToList(bag)
      if (closeObjects.isEmpty) {
        val oldRadius = SurfABM.mbr.getArea / GISFunctions.MIN_SEARCH_RADIUS_DENOMINATOR
        val oldDenominator = GISFunctions.MIN_SEARCH_RADIUS_DENOMINATOR
        GISFunctions.MIN_SEARCH_RADIUS_DENOMINATOR = GISFunctions.MIN_SEARCH_RADIUS_DENOMINATOR * 0.8
        radius = SurfABM.mbr.getArea / GISFunctions.MIN_SEARCH_RADIUS_DENOMINATOR
        LOG.warn("Increasing search radius from %s(%s) to %s(%s). This is very inefficient if it happens regularly.".format(
          oldRadius, oldDenominator, radius, GISFunctions.MIN_SEARCH_RADIUS_DENOMINATOR))
      }
      else {
        var minDist = Double.MaxValue
        var dist = 0.0
        for (o <- closeObjects) {
          val sg = o match {
            // Cast
            case x: MasonGeometry => x
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