package surf.abm

import java.awt.Paint
import javax.swing.JFrame

import org.apache.log4j.Logger
import sim.display.{GUIState, Display2D}
import sim.portrayal.geo.GeomVectorFieldPortrayal
import sim.portrayal.{DrawInfo2D, SimplePortrayal2D}
import sim.portrayal.simple.LabelledPortrayal2D
import sim.util.geo.MasonGeometry

/**
  * This class can be used to run the model with a GUI.
  * Created by nick on 16/03/16.
  */
class SurfABMWithUI extends GUIState (new SurfABM(System.currentTimeMillis())) {
  private var display: Display2D = null
  private var displayFrame: JFrame = null
  // Portrayals are used for visualising objects in fields
  private var walkwaysPortrayal: GeomVectorFieldPortrayal = new GeomVectorFieldPortrayal()
  private var buildingPortrayal: GeomVectorFieldPortrayal = new GeomVectorFieldPortrayal()
  private var agentPortrayal: GeomVectorFieldPortrayal = new GeomVectorFieldPortrayal()

  XXXX HERE CORRECT SYNTAX OF FOLLOWING METHODS (COPY FROM JAVA)

  @Override
  public void init(Controller controller) {
    super.init(controller);

    display = new Display2D(SimBurglar.WIDTH, SimBurglar.HEIGHT, this);

    display.attach(walkwaysPortrayal, "Walkways", true);
    display.attach(buildingPortrayal, "Buildings", true);
    //        display.attach(roadsPortrayal, "Roads", true);
    display.attach(agentPortrayal, "Agents", true);

    displayFrame = display.createFrame();
    controller.registerFrame(displayFrame);
    displayFrame.setVisible(true);
  }

  @Override
  public void start() {
    super.start();
    setupPortrayals();
  }

  private void setupPortrayals() {
    SimBurglar world = (SimBurglar) state;

    walkwaysPortrayal.setField(world.roads);
    walkwaysPortrayal.setPortrayalForAll(new GeomPortrayal(Color.CYAN, true));

    buildingPortrayal.setField(world.buildings);
    BuildingLabelPortrayal b = new BuildingLabelPortrayal(new GeomPortrayal(Color.DARK_GRAY, true), Color.BLUE);
    buildingPortrayal.setPortrayalForAll(b);

    agentPortrayal.setField(world.agents);
    agentPortrayal.setPortrayalForAll(new GeomPortrayal(Color.RED, 10.0, true));

    display.reset();
    display.setBackdrop(Color.WHITE);

    display.repaint();
  }

  public static void main(String[] args) throws Exception {


    try {
      SimBurglar.preInitialise();
    }
    catch (Exception e) {
      LOG.error("Exception thrown during pre-initialisation", e);
      return;
    }

    SimBurglarWithUI worldGUI = null;

    try {
      worldGUI = new SimBurglarWithUI();

    }
    catch (ParseException ex) {
      LOG.error("Error initialising model.", ex);
    }
    catch (MalformedURLException ex) {
      LOG.error("Error initialising model.", ex);
    }
    catch (FileNotFoundException ex) {
      LOG.error("Error initialising model.", ex);
    }

    try {
      Console console = new Console(worldGUI);
      console.setVisible(true);
    }
    catch (Exception e) {
      LOG.error("Error caught while running model.", e);
    }
  } // main

}
/**
  * Singleton helper class
  */
object SurfABMWithUI {
  private val LOG: Logger = Logger.getLogger(this.getClass);
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
  private val LOG: Logger = Logger.getLogger(this.getClass);
}

