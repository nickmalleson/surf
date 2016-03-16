package surf.abm

import java.awt.Paint

import sim.portrayal.{DrawInfo2D, SimplePortrayal2D}
import sim.portrayal.simple.LabelledPortrayal2D
import sim.util.geo.MasonGeometry

/**
  * This class can be used to run the model with a GUI.
  * Created by nick on 16/03/16.
  */
class SimBurglarWithUI {

}

@SerialVersionUID(1L)
class BuildingLabelPortrayal(child : SimplePortrayal2D, paint : Paint)
  extends LabelledPortrayal2D (child, null, paint, true) {

  override def getLabel(o: AnyRef, info: DrawInfo2D): String = {
    if (o.isInstanceOf[MasonGeometry]) {
      val mg: MasonGeometry = o.asInstanceOf[MasonGeometry]
      return mg.getStringAttribute("NAME")
    }
    return "No Name"
// XXXX HERE
    case e: Exception => {
      SurfABM.LOG.error("Exception while creating agents, cannot continue", e)
      throw e
    }
  }
}