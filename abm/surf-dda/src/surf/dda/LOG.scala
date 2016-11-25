package surf.dda

/**
  * Created by nick on 25/11/2016.
  */
object LOG {

  def debug(msg:String) : Unit = {
    System.out.println(msg)
  }

  def error(msg:String, e:Exception) : Unit = {
    System.err.println(msg)
    e.printStackTrace()
  }

}
