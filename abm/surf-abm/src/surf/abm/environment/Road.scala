package surf.abm.environment

/**
  * Represents Roads ().
  * Created by nick on 11/10/2017.
  *
  * @param id a unique ID for this road
  * @param cameraID the ID for a camera attached to this road. If -1 then this road does not have a camera.
  */
case class Road (val id:Int, val cameraID:Int)
