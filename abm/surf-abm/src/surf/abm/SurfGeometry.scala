package surf.abm

import java.{lang, util}

import com.vividsolutions.jts.geom.Geometry
import sim.util.geo.{AttributeValue, MasonGeometry}

/**
  * An extension to <code>MasonGeometry</code> that keeps a record of the object used
  * to create it (e.g. an Agent, Building, etc).
  * Created by nick on 23/03/2016.
  *
  * @param masonGeom The underlying MasonGeometry for this SurfGeometry
  * @param theObject The object associated with this SurfGeometry. E.g. an Agent or a Building.
  */
@SerialVersionUID(1L)
class SurfGeometry[T](val masonGeom:MasonGeometry, val theObject:T) extends
  MasonGeometry(masonGeom.getGeometry,theObject) with Serializable{

  /* Auxilliary constructors replicating the behaviour of MasonGeometry.
   * I don't think they're necessary, I can't think of a situation when you
    * wouldn't just use the primary one.*/

  /* def this()
  {
    this(null, null)
  }

  def this( g : MasonGeometry)
  {
    this(g, null)
  }*/

  /*
    Override all methods and call them on the underlying MasonGeometry object,
    not on the parent.
    We can't just call super. because when a new SurfGeometry is constructed, the
    MasonGeometry used to create it isn't passed to the parent, (only the Geometry is)
    so all of the attributes etc. would have been forgotten about.
   */

  override def getAttributes: util.Map[String, AttributeValue] = masonGeom.getAttributes

  override def addAttribute(name: String, value: scala.Any): Unit = masonGeom.addAttribute(name, value)

  override def addIntegerAttribute(name: String, value: Int): Unit = masonGeom.addIntegerAttribute(name, value)

  override def hasAttributes: Boolean = masonGeom.hasAttributes

  override def getAttribute(name: String): AnyRef = masonGeom.getAttribute(name)

  override def addStringAttribute(name: String, value: String): Unit = masonGeom.addStringAttribute(name, value)

  override def hasHiddenAttributes: Boolean = masonGeom.hasHiddenAttributes

  override def getDoubleAttribute(name: String): lang.Double = masonGeom.getDoubleAttribute(name)

  override def addAttributes(attributes: util.Map[String, AttributeValue]): Unit = masonGeom.addAttributes(attributes)

  override def hasAttribute(name: String): Boolean = masonGeom.hasAttribute(name)

  override def getGeometry: Geometry = masonGeom.getGeometry

  override def addDoubleAttribute(name: String, value: Double): Unit = masonGeom.addDoubleAttribute(name, value)

  override def propertiesProxy(): AnyRef = masonGeom.propertiesProxy()

  override def getStringAttribute(name: String): String = masonGeom.getStringAttribute(name)

  override def getIntegerAttribute(name: String): Integer = masonGeom.getIntegerAttribute(name)

  override def toString: String = masonGeom.toString

  override def equals(obj: scala.Any): Boolean = masonGeom.equals(obj)

  override def hashCode(): Int = masonGeom.hashCode()

  override def getUserData: AnyRef = masonGeom.getUserData
}

@SerialVersionUID(1L)
object SurfGeometry extends Serializable {
  def apply[T](g:MasonGeometry, o:T): SurfGeometry[T] = new SurfGeometry[T](g,o)
}