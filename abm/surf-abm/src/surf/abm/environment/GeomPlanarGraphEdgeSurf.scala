package surf.abm.environment

import com.vividsolutions.jts.geom.LineString
import com.vividsolutions.jts.planargraph.Edge
import surf.abm.main.SurfGeometry


/**
  * This is an extended version of GeomPlanarGraphEdge, originally written by: Mark Coletti, Keith Sullivan, Sean Luke, and
  * George Mason University Mason University Licensed under the Academic Free License version 3.0
  *
  * All I have done is extend it slightly so that a link can be made between this edge and an underlying road
  * (a SurfGeometry) on a road network (in Scala).
  *
  * The reason I have done this is so that, when an agent is moving along the road network, we can find out which
  * Road they are on.  We could have used the 'attributes' parameter of the original GeomPlanarGraphEdge, but
  * that would require HashTable lookups which could slow the simulation down considerably.
  *
  * Created by Nick Malleson on 11/10/2017.
  *
  * @param line The line that corresponds to this edge
  * @param geometry The underlying geometry that represents this line (e.g. a [SurfGeometry]).
  *
  */
class GeomPlanarGraphEdgeSurf[T](var line: LineString, var geometry: SurfGeometry[T]) extends Edge {

  def getLine: LineString = line

  def getGeometry: SurfGeometry[T] = this.geometry

}


