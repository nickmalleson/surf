package surf.abm

import java.{lang, util}

import com.vividsolutions.jts.geom.Geometry
import sim.util.geo.{AttributeValue, MasonGeometry}

/**
  * Created by nick on 23/03/2016.
  */
@SerialVersionUID(1L)
class SurfGeometry(g:MasonGeometry, o:AnyRef) extends MasonGeometry(g.getGeometry,o) with Serializable{

  // When constructing a new SurfGeometry obejct, all the attributes from the original
  // MasonGeometry need to be preserved




  /* Auxilliary constructors */

  def this()
  {
    this(null, null);
  }

  def this( g : MasonGeometry)
  {
    this(g, null);
  }

  override def getAttributes: util.Map[String, AttributeValue] = g.getAttributes

  override def addAttribute(name: String, value: scala.Any): Unit = g.addAttribute(name, value)

  override def addIntegerAttribute(name: String, value: Int): Unit = g.addIntegerAttribute(name, value)

  override def hasAttributes: Boolean = g.hasAttributes

  override def getAttribute(name: String): AnyRef = g.getAttribute(name)

  override def addStringAttribute(name: String, value: String): Unit = g.addStringAttribute(name, value)

  override def hasHiddenAttributes: Boolean = g.hasHiddenAttributes

  override def getDoubleAttribute(name: String): lang.Double = g.getDoubleAttribute(name)

  override def addAttributes(attributes: util.Map[String, AttributeValue]): Unit = g.addAttributes(attributes)

  override def hasAttribute(name: String): Boolean = g.hasAttribute(name)

  override def getGeometry: Geometry = g.getGeometry

  override def addDoubleAttribute(name: String, value: Double): Unit = g.addDoubleAttribute(name, value)

  override def propertiesProxy(): AnyRef = g.propertiesProxy()

  override def getStringAttribute(name: String): String = g.getStringAttribute(name)

  override def getIntegerAttribute(name: String): Integer = g.getIntegerAttribute(name)

  override def toString: String = g.toString

  override def equals(obj: scala.Any): Boolean = g.equals(obj)

  override def hashCode(): Int = g.hashCode()

  override def getUserData: AnyRef = g.getUserData
}

@SerialVersionUID(1L)
object SurfGeometry extends Serializable {
  def apply(g:MasonGeometry, o:AnyRef): SurfGeometry = new SurfGeometry(g,o)
}