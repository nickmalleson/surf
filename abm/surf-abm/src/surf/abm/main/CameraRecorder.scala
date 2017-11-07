package surf.abm.main

import java.io.{BufferedWriter, File, FileWriter}

import org.apache.log4j.Logger
import sim.engine.{SimState, Steppable}

import collection.JavaConverters._
import scala.collection.mutable.ListBuffer

/**
  * Created by tomas on 31/10/2017.
  */
object CameraRecorder extends Steppable{

  private var initialised = false

  private var state : SimState = null // This will point to the sim state (useful)

  private val LOG: Logger = Logger.getLogger(this.getClass)

  val tempCameraMaps = scala.collection.mutable.Map[Int, ListBuffer[Int]]()

  private val cameraList: List[Integer] = SurfABM.conf.getIntList(SurfABM.ModelConfig+".CameraList").asScala.toList


  /**
    * Needs to be called to initialise the CameraRecorder
    */
  def create(state:SurfABM): Unit = {

    if (initialised) {
      throw new Exception("ERROR! CameraRecorder has already been initialised")
    }


    // Init code goes here
    for (c <- cameraList) {
      var b = new ListBuffer[Int]()
      b += 0
      this.tempCameraMaps.put(c,b)
     // LOG.info("camera %d gets a listBuffer\n".format(c))
    }

    this.state = state

    this.initialised = true

    // Schedule the step method
    this.state.schedule.scheduleRepeating(this, SurfABM.CAMERA_RECORDER_STEP, 60.0/Clock.minsPerTick)


  }



  override def step(state: SimState): Unit = {

    // Code to do stuff with the cameras goes here
    LOG.debug("CameraRecorder.step() has been called")

    // Every hour, a new element has to be added to the camera counts, starting with value zero.
    for (c <- cameraList) {
      this.tempCameraMaps(c) += 0
      //LOG.info("new element added to camera %d\n".format(c))
    }


  }

  /**
    * Can be called by an agent if the agent has passed by a camera
    */
  def add(cameraID: Int): Unit = {

    if(!this.tempCameraMaps.contains(cameraID)){
      throw new Exception ("ERROR! This camera was not initialised! It was probably not defined in the configuration file.")
    }

    val hoursList = this.tempCameraMaps(cameraID)
    val currentVal = hoursList.last

    this.tempCameraMaps(cameraID).update(hoursList.size - 1, currentVal+1)
    //LOG.info("camera %d at index %d, hour %d, time %s and iteration %d is passed by %d agents\n".format(cameraID,hoursList.size -1, Clock.currentHour().toInt, Clock.getTime().toString, Clock.getIterations(), this.tempCameraMaps(cameraID).last))

  }


}
