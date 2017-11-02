package surf.abm.main

import org.apache.log4j.Logger
import sim.engine.{SimState, Steppable}
import surf.abm.main.Clock
import surf.abm.main.DefaultOutputter.LOG

import scala.collection.mutable.ListBuffer

/**
  * Created by tomas on 31/10/2017.
  */
object CameraRecorder extends Steppable{

  private var initialised = false

  private var state : SimState = null // This will point to the sim state (useful)

  private val LOG: Logger = Logger.getLogger(this.getClass)


  /**
    * Needs to be called to initialise the CameraRecorder
    */
  def create(state:SurfABM) = {

    if (initialised) {
      throw new Exception("ERROR! CameraRecorder has already been initialised")
    }


    // Init code goes here
    val tempCameraMaps : scala.collection.mutable.Map[Int, ListBuffer[Int]] = null

    this.state = state

    this.initialised = true

    // Schedule the step method
    this.state.schedule.scheduleRepeating(this, Int.MinValue, 1)


  }



  override def step(state: SimState): Unit = {

    // Code to do stuff with the cameras goes here
    LOG.debug("CameraRecorder.step() has been called")

    // Cameras and footfall counts in the model


  }



}
