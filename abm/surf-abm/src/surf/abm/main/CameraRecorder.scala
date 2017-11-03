package surf.abm.main

import org.apache.log4j.Logger
import sim.engine.{SimState, Steppable}
import surf.abm.main.Clock
import surf.abm.main.DefaultOutputter.LOG
import collection.JavaConverters._

import scala.collection.mutable.ListBuffer

/**
  * Created by tomas on 31/10/2017.
  */
object CameraRecorder extends Steppable{

  private var initialised = false

  private var state : SimState = null // This will point to the sim state (useful)

  private val LOG: Logger = Logger.getLogger(this.getClass)

  private val tempCameraMaps = scala.collection.mutable.Map[Int, ListBuffer[Int]]()

  private val cameraList: List[Integer] = SurfABM.conf.getIntList(SurfABM.ModelConfig+".CameraList").asScala.toList


  /**
    * Needs to be called to initialise the CameraRecorder
    */
  def create(state:SurfABM) = {

    if (initialised) {
      throw new Exception("ERROR! CameraRecorder has already been initialised")
    }


    // Init code goes here
    for (c <- cameraList) {
      var b = new ListBuffer[Int]()
      b += 0
      this.tempCameraMaps.put(c,b)
      LOG.info("camera %d gets a listBuffer\n".format(c))
      //this.tempCameraMaps(c) += 0
    }

    this.state = state

    this.initialised = true

    // Schedule the step method
    this.state.schedule.scheduleRepeating(this, Int.MinValue, (60.0/Clock.minsPerTick).toInt)


  }



  override def step(state: SimState): Unit = {

    // Code to do stuff with the cameras goes here
    LOG.debug("CameraRecorder.step() has been called")

    // Cameras and footfall counts in the model
    for (c <- cameraList) {
      this.tempCameraMaps(c) += 0
      LOG.info("new element added to camera %d\n".format(c))
    }


  }

  def add(cameraID: Int): Unit = {

    if(!this.tempCameraMaps.contains(cameraID)){
      throw new Exception ("ERROR! This camera was not initialised!")
    }

    val hoursList = this.tempCameraMaps(cameraID)
    val currentVal = hoursList.last

    this.tempCameraMaps(cameraID).update(hoursList.size - 1, currentVal+1)
    LOG.info("camera %d at hour %d is passed by %d agents\n".format(cameraID,hoursList.size -1, this.tempCameraMaps(cameraID).last))

  }


}
