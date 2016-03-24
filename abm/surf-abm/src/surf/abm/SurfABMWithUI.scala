package surf.abm

import java.awt.geom.Rectangle2D
import java.awt.{Graphics2D, Color, Paint}
import javax.swing.JFrame

import org.apache.log4j.Logger
import sim.display.{Console, GUIState, Display2D, Controller}
import sim.engine.SimState
import sim.portrayal.geo.{GeomPortrayal, GeomVectorFieldPortrayal}
import sim.portrayal.{simple, DrawInfo2D, SimplePortrayal2D}
import sim.portrayal.simple.{OvalPortrayal2D, RectanglePortrayal2D, LabelledPortrayal2D}
import surf.abm.environment.Building

import scala.collection.JavaConversions._
import scala.collection.immutable._

/**
  * This class can be used to run the model with a GUI.
  * Created by nick on 16/03/16.
  */
class SurfABMWithUI extends GUIState (new SurfABM(System.currentTimeMillis())) {

  // The display has all the portrayals added to it. It is initialised in init()
  private val display = new Display2D(SurfABM.WIDTH, SurfABM.HEIGHT, this)
  // Portrayals are used for visualising objects in fields
  private val roadsPortrayal = new GeomVectorFieldPortrayal()
  private val buildingPortrayal = new GeomVectorFieldPortrayal()
  private val agentPortrayal = new GeomVectorFieldPortrayal() // For the agents (they're a circle)
  private var displayFrame: JFrame = null
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
    super.start()
    setupPortrayals()

  }

  private def setupPortrayals() : Unit = {
    super.start();

    SurfABMWithUI.LOG.debug("Creating portrayals.")
    //val world : SurfABM = super.state.asInstanceOf[SurfABM]

    roadsPortrayal.setField(SurfABM.roadGeoms)
    roadsPortrayal.setPortrayalForAll(new GeomPortrayal(Color.GRAY, true));

    buildingPortrayal.setField(SurfABM.buildingGeoms)
    buildingPortrayal.setPortrayalForAll(
      new BuildingLabelPortrayal(new GeomPortrayal(Color.BLUE, true), Color.BLACK))

    // Give the agents a round oval to represent them.
    agentPortrayal.setField(SurfABM.agentGeoms)

    // Each agent should have a different, random colour.
    // For some reason this doesn't work. I think there must be a bug in setPortrayalForObject when
    // used with GeomPortrayal. :-( Instead just make them all red.
    agentPortrayal.setPortrayalForAll(new OvalPortrayal2D(Color.RED, 10.0, true))


    /* // This bit uses the setPortrayalForAgent:
    for ((o, i) <- SurfABM.agentGeoms.getGeometries.zipWithIndex) {
      val colour =  new Color(
        128 - guirandom.nextInt(128),
        128 - guirandom.nextInt(128),
        128 - guirandom.nextInt(128))
      agentPortrayal.setPortrayalForObject(o,
        new GeomPortrayal(Color.GRAY, 5.0, true))
        //new OvalPortrayal2D(colour,5.0, true))
      SurfABMWithUI.LOG.debug(s"Agent $i is color $colour")
    }
    println(agentPortrayal.getPortrayalForObject(SurfABM.agentGeoms.getGeometries.get(2)))
    */
    /* // (An alternative way to loop through the agents:
    SurfABM.agents.getGeometries.zipWithIndex.foreach {
      case(o, i) => println(s"Geometry $o is number $i")
    } */

    /*
    // This is how to give each agent a bespoke portrayal that is updated on each iteration (so
    // in the example below the agents randomly change colour)
    agentPortrayal.setPortrayalForAll(
      new OvalPortrayal2D() {
        override def draw(o: AnyRef, graphics: Graphics2D, info: DrawInfo2D) {
          val draw: Rectangle2D.Double = info.draw
          val width: Double = ( draw.width * scale + offset ) * 10
          val height: Double = ( draw.height * scale + offset ) * 10

          graphics.setPaint(paint)

          val x: Int = (draw.x - width / 2.0).toInt
          val y: Int = (draw.y - height / 2.0).toInt
          val w: Int = (width).toInt
          val h: Int = (height).toInt
          val colour =  new Color(
            128 - guirandom.nextInt(128),
            128 - guirandom.nextInt(128),
            128 - guirandom.nextInt(128))
          graphics.setColor(colour)

          // draw centered on the origin
          graphics.fillOval(x, y, w, h)
        }
      } // new OvalPortrayal2D
    ) // setPortrayalForAll
    */



    display.reset()
    display.setBackdrop(Color.WHITE)

    display.repaint()
  }

  override def load(state: SimState): Unit = {
    super.load(state)
    setupPortrayals()
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
      case x: SurfGeometry[Building @unchecked] => x.getStringAttribute(FIELDS.BUILDINGS_NAME.toString)
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

