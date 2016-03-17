package surf.abm

import java.awt.{Color, Paint}
import javax.swing.JFrame

import org.apache.log4j.Logger
import sim.display.{Console, GUIState, Display2D, Controller}
import sim.portrayal.geo.{GeomPortrayal, GeomVectorFieldPortrayal}
import sim.portrayal.{DrawInfo2D, SimplePortrayal2D}
import sim.portrayal.simple.LabelledPortrayal2D
import sim.util.geo.MasonGeometry

/**
  * This class can be used to run the model with a GUI.
  * Created by nick on 16/03/16.
  */
class SurfABMWithUI extends GUIState (new SurfABM(System.currentTimeMillis())) {

  // The display has all the portrayals added to it. It is initialised in init()
  private val display = new Display2D(SurfABM.WIDTH, SurfABM.HEIGHT, this)

  private var displayFrame: JFrame = null
  // Portrayals are used for visualising objects in fields
  private val walkwaysPortrayal: GeomVectorFieldPortrayal = new GeomVectorFieldPortrayal()
  private val buildingPortrayal: GeomVectorFieldPortrayal = new GeomVectorFieldPortrayal()
  private val agentPortrayal: GeomVectorFieldPortrayal = new GeomVectorFieldPortrayal()

  override def init(controller : Controller ) : Unit = {
    super.init(controller);

    display.attach(walkwaysPortrayal, "Walkways", true);
    display.attach(buildingPortrayal, "Buildings", true);
    //        display.attach(roadsPortrayal, "Roads", true);
    display.attach(agentPortrayal, "Agents", true);

    displayFrame = display.createFrame();
    controller.registerFrame(displayFrame);
    displayFrame.setVisible(true);
  }


  override def start() : Unit = {
    super.start();
    //val world : SurfABM = super.state.asInstanceOf[SurfABM]

    //walkwaysPortrayal.setField(world.roads);
    walkwaysPortrayal.setField(SurfABM.roads);
    walkwaysPortrayal.setPortrayalForAll(new GeomPortrayal(Color.CYAN, true));

    //buildingPortrayal.setField(world.buildings);
    buildingPortrayal.setField(SurfABM.buildings);
    val b = new BuildingLabelPortrayal(new GeomPortrayal(Color.DARK_GRAY, true), Color.BLUE);
    buildingPortrayal.setPortrayalForAll(b);

    //agentPortrayal.setField(world.agents);
    agentPortrayal.setField(SurfABM.agents);
    agentPortrayal.setPortrayalForAll(new GeomPortrayal(Color.RED, 10.0, true));

    display.reset();
    display.setBackdrop(Color.WHITE);

    display.repaint();
  }

}
/**
  * Singleton helper class
  */
object SurfABMWithUI {
  private val LOG: Logger = Logger.getLogger(this.getClass);
  def main(args: Array[String]): Unit = {

    try {
      var worldGUI = new SurfABMWithUI()

      val console = new Console(worldGUI)
      console.setVisible(true)
    }
    catch {
      case e: Exception => {
        SurfABMWithUI.LOG.error("Exception while runnning model ", e)
        throw e
      }
    }
  } // main
}


@SerialVersionUID(1L)
class BuildingLabelPortrayal(child : SimplePortrayal2D, paint : Paint)
  extends LabelledPortrayal2D (child, null, paint, true) {

  override def getLabel(o: AnyRef, info: DrawInfo2D): String = {
    /*if (o.isInstanceOf[MasonGeometry]) {
      val mg: MasonGeometry = o.asInstanceOf[MasonGeometry]
      return mg.getStringAttribute("NAME")
    }*/

    // cast the object to a MasonGemoetry using pattern matching and return the
    // building name. Or return 'no name' if the object is not a building
    o match {
      case x: MasonGeometry => x.getStringAttribute(FIELDS.BUILDINGS_NAME.toString)
        //val mg: MasonGeometry = o.asInstanceOf[MasonGeometry]
        //mg.getStringAttribute("NAME")

      case _ => {
        BuildingLabelPortrayal.LOG.warn("Cannot find a label for a building", new Exception())
        "No Building Name" // no label to return, send "No Building Name" back
      }
    }

  }
} // BuldingLabelPortrayal class

object BuildingLabelPortrayal {
  private val LOG: Logger = Logger.getLogger(this.getClass)


}

