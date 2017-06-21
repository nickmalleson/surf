package surf.abm.main

import org.apache.log4j.Logger
import sim.field.geo.GeomVectorField
import sim.util.Bag
import surf.abm.exceptions.RoutingException
import surf.abm.surfutil.Util

/**
  * Created by nick on 21/05/2016.
  */
object GISFunctions {

  private val LOG: Logger = Logger.getLogger(GISFunctions.getClass)

  private var MIN_SEARCH_RADIUS_DENOMINATOR: Double = 1000000.0

  private var initRadius= true // The object is currently trying to find the most suitable value for the search radius
  private var numCalls : Int = 0 // The number of times the findNearestObject function has been called
  private val NUM_CALLS_TO_INIT = 5000 // The number of times the function is called before initialisation finishes
  private val distanceList = scala.collection.mutable.ArrayBuffer.empty[Double] // An list to store all the distances during initialisation

  /**
    * Find the nearest object to the given input coordinate. It is used quite a lot to do agent routing.
    *
    * For efficiency, the function needs to decide on an appropraiate search radius. Too large and it becomes very
    * inneficient, too small and the radius needs to be increased a few times before the nearest object is found
    * which is also inefficient. To get round this problem, each time the function is called it stores the final
    * distance that it calculates. Then after x calls, it sets the radius to a value such that most queries will
    * be satisfied, and it will only need to be increased on occasion.
    *
    * Note that the function also starts with a relatively small radius and allows it to increase as needed.
    */
  def findNearestObject[T](centre: SurfGeometry[_], geom: GeomVectorField) : SurfGeometry[T] = {

    numCalls+=1 // Increment the number of times this function has been called

    var radius: Double = SurfABM.mbr.getArea / GISFunctions.MIN_SEARCH_RADIUS_DENOMINATOR
    var closest: SurfGeometry[T] = null // The closest object
    var minDist = Double.MaxValue // The distance to the closest objet

    // Find the nearest object, increasing the search radius if necessary

    var currentDenominator = GISFunctions.MIN_SEARCH_RADIUS_DENOMINATOR // Necessary to memorise the denominator as we loop

    //while (radius < SurfABM.mbr.getArea) {
    while (closest == null) { // Gradually increase the radius until the closest object has been found

        val bag : Bag = geom.getObjectsWithinDistance(centre, radius)
        val closeObjects : List[_]  = Util.bagToList(bag)

        if (closeObjects.isEmpty) { // Could not find an object, increase the radius

          val oldRadius = SurfABM.mbr.getArea / currentDenominator
          val oldDenominator = currentDenominator

          // Update the current denominator and radius, increasing ready for next time
          currentDenominator = oldDenominator * 0.5
          radius = SurfABM.mbr.getArea / currentDenominator

          //println(newDenominator+" "+numCalls.toString)

          // If we're initialising then make this change permanent (for the meantime anyway)
          if (initRadius) {
            GISFunctions.MIN_SEARCH_RADIUS_DENOMINATOR = currentDenominator
            LOG.warn("Initialising radius. Increased from %s(%s) to %s(%s).".format(
              oldRadius, oldDenominator, radius, GISFunctions.MIN_SEARCH_RADIUS_DENOMINATOR))
          }
          else {
            LOG.warn("Increasing search radius from %s(%s) to %s(%s).\n\tThis is very inefficient if it happens regularly.".format(
              oldRadius, oldDenominator, radius, currentDenominator))
          }
        }

        else { // Have found some objects. Work out which is closest
          var dist = 0.0
          for (o <- closeObjects) {
            // Cast the object to a SurfGeometryGeometry
            val sg = o match {
              case x: SurfGeometry[T @unchecked] => x
              case _ => throw new ClassCastException
            }
            if (sg != centre) { // Ignore the geometry if it is actually the centre point that we're searching around
              dist = centre.geometry.distance(sg.geometry) // Calculate the distance to the object
              if (dist < minDist) { // If it is the closest one so far:
                closest = sg
                minDist = dist
              }
            }
          } // for close objects
        } // else

    } // while searchRadius

    // At this point we should have the closest object
    if (closest==null) {
      throw new RoutingException("Could not find any objects near to " + centre.toString)
    }
    // Sanity checking:
    assert(minDist < Double.MaxValue )
    assert(radius < SurfABM.mbr.getArea)

    if (initRadius) { // We havn't found the optimal radius yet. Store the distance.
      distanceList += minDist

      if (numCalls > NUM_CALLS_TO_INIT) { // This has been called enough times, find the optimal radius

        // println(distanceList.mkString("\n"))

        // There are a few posibilities for calculating the 'optimal' radius. We could use the mean, but the distances
        // are probably positively skewed so the mean might be unnecessarily large.
        // For now just go with twice the median (still smaller than mean in my tests) and can experiment later

        val median = distanceList.sortWith(_ < _)( (distanceList.size/2).toInt)
        val finalRadius = median * 2
        val finalDenominator = SurfABM.mbr.getArea / finalRadius

        GISFunctions.MIN_SEARCH_RADIUS_DENOMINATOR = finalDenominator

        initRadius = false
        LOG.info("Finished finding optimal radius: %s(%s)".format(
          finalRadius, // The final radius
          GISFunctions.MIN_SEARCH_RADIUS_DENOMINATOR // The final denominator
        ))
      }
    }

    else { // The radius has been set, don't try to find an optimal one



    }


    return closest
  } // findNearestObject

}
