package surf.abm

import java.awt.{Color, Paint}
import javax.swing.JFrame

import org.apache.log4j.Logger
import sim.display.{Console, GUIState, Display2D, Controller}
import sim.engine.SimState
import sim.portrayal.continuous.ContinuousPortrayal2D
import sim.portrayal.geo.{GeomPortrayal, GeomVectorFieldPortrayal}
import sim.portrayal.{DrawInfo2D, SimplePortrayal2D}
import sim.portrayal.simple.{OvalPortrayal2D, LabelledPortrayal2D}
import sim.util.geo.MasonGeometry

import scala.collection.JavaConversions._
import scala.collection.immutable._

/**
  * This class can be used to run the model with a GUI.
  * Created by nick on 16/03/16.
  */
class SurfABMWithUI extends GUIState (new SurfABM(System.currentTimeMillis())) {

  // The display has all the portrayals added to it. It is initialised in init()
  private val display = new Display2D(SurfABM.WIDTH, SurfABM.HEIGHT, this)

  private var displayFrame: JFrame = null
  // Portrayals are used for visualising objects in fields
  private val roadsPortrayal = new GeomVectorFieldPortrayal()
  private val buildingPortrayal = new GeomVectorFieldPortrayal()
  private val agentPortrayal = new GeomVectorFieldPortrayal() // For the agents (they're a circle)
  //private val trailsPortrayal = new ContinuousPortrayal2D() // For trails behind agents

  override def init(controller : Controller ) : Unit = {
    super.init(controller);

    display.attach(roadsPortrayal, "Walkways", true);
    display.attach(buildingPortrayal, "Buildings", true);
    //display.attach(roadsPortrayal, "Roads", true);
    display.attach(agentPortrayal, "Agents", true);

    displayFrame = display.createFrame();
    controller.registerFrame(displayFrame);
    displayFrame.setVisible(true);
  }


  override def start() : Unit = {

    setupPortrayals()

  }


  override def load(state: SimState): Unit = {
    super.load(state)
    setupPortrayals()
  }

  private def setupPortrayals() : Unit = {
    super.start();
    //val world : SurfABM = super.state.asInstanceOf[SurfABM]

    roadsPortrayal.setField(SurfABM.roads)
    roadsPortrayal.setPortrayalForAll(new GeomPortrayal(Color.GRAY, true));

    buildingPortrayal.setField(SurfABM.buildings)
    buildingPortrayal.setPortrayalForAll(
      new BuildingLabelPortrayal(new GeomPortrayal(Color.BLUE, true), Color.BLACK))

    // Give the agents a round oval to represent them.
    agentPortrayal.setField(SurfABM.agents)
    //agentPortrayal.setPortrayalForAll(new OvalPortrayal2D(Color.RED, 10.0, true))
    // Each agent should have a different, random colour:

    // (An alternative way to loop through the agents:
    //SurfABM.agents.getGeometries.zipWithIndex.foreach {
    //  case(o, i) => println(s"Geometry $o is number $i")
    //}
    for ((o, i) <- SurfABM.agents.getGeometries.zipWithIndex) {
      val colour =  new Color(
        128 + guirandom.nextInt(128),
        128 + guirandom.nextInt(128),
        128 + guirandom.nextInt(128))
      agentPortrayal.setPortrayalForObject(o, new OvalPortrayal2D(colour,10.0, true))
      SurfABMWithUI.LOG.debug(s"Agent $i is color $colour")
    }


    display.reset()
    display.setBackdrop(Color.WHITE)

    display.repaint()
  }

}
/**
  * Singleton helper class
  */
object SurfABMWithUI {
  private val LOG: Logger = Logger.getLogger(this.getClass);
  def main(args: Array[String]): Unit = {

    try {
      val worldGUI = new SurfABMWithUI()

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


/**
  * A special portrayal for buildings that provides labels for the buildings in the GUI.
  * Note: see the Mason manual (section 9.3.4, page 222) for a definitions of the different
  * Portrayals that are available.
  */
@SerialVersionUID(1L)
class BuildingLabelPortrayal(child : SimplePortrayal2D, paint : Paint)
  extends LabelledPortrayal2D (child, null, paint, true) {

  override def getLabel(o: AnyRef, info: DrawInfo2D): String = {
    // cast the object to a MasonGemoetry using pattern matching and return the
    // building name. Or return 'no name' if the object is not a building
    o match {
      case x: MasonGeometry => x.getStringAttribute(FIELDS.BUILDINGS_NAME.toString)
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

