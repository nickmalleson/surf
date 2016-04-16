package surf.abm.environment

import com.vividsolutions.jts.planargraph.Node

/**
  * Used to represeent junctions (aka intersections) on the road network. Junctions are used to associate
  * points where roads meet in the geography layer, to nodes in the network layer.
  *
  *
  * @param node A companion node, in the network, associated with this junction.
  */
class Junction(node: Node) {

  // A unique id for each Junction with a public accessor.
  private val _id = Junction.uniqueID()
  def id() = this._id

  override def toString: String =
    "Junction %d (%d,%d)".format(this.id(),this.node.getCoordinate.x, this.node.getCoordinate.y)

}
object Junction {
  /** A unique ID that can be given to each agent */
  private var _uniqueID : Int = 0
  private def uniqueID() = {
    _uniqueID += 1
    _uniqueID
  }

  /**
    * Constructor for Junctions
    * @param node The companion Node associated with this junction
    * @return
    */
  def apply (node:Node) : Junction = {
    new Junction(node)
  }
}
