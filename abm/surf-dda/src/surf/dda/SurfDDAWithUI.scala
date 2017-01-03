package surf.dda

import java.awt.Color
import javax.swing.JFrame

import sim.display.{Console, Controller, Display2D, GUIState}
import sim.engine.SimState
import sim.portrayal.grid.{FastValueGridPortrayal2D, SparseGridPortrayal2D}

/**
  * Created by nick on 09/12/2016.
  */
class SurfDDAWithUI extends GUIState(new SurfDDA(System.currentTimeMillis())) {

  val display : Display2D = new Display2D(SurfDDA.GRID_WIDTH, SurfDDA.GRID_HEIGHT, this)
  var displayFrame : JFrame = null
  val worldPortrayal : SparseGridPortrayal2D = new SparseGridPortrayal2D()

  // allow the user to inspect the model
  override def getSimulationInspectedObject() : Object =  state

  def getName() : String = "Ant Foraging"

  override def init(controller : Controller ) : Unit = {
    super.init(controller);

    display.attach(worldPortrayal, "World", true);

    displayFrame = display.createFrame();
    controller.registerFrame(displayFrame);
    displayFrame.setVisible(true);
  }

  override def start() : Unit = {
    super.start()
    setupPortrayals()
  }

  override def load(state: SimState): Unit = {
    super.load(state)
    setupPortrayals()
  }

  private def setupPortrayals() : Unit = {


    worldPortrayal.setField(SurfDDA.world);


    display.reset()
    display.setBackdrop(Color.WHITE)

    display.repaint()

  }

}

object SurfDDAWithUI {

  def main(args: Array[String]): Unit = new SurfDDAWithUI().createController()

}