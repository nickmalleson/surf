package surf.abm.environment

/**
  * Created by nick on 24/03/2016.
  */
class Building (val id:Int) {

}

object Building {
  def apply(id:Int) : Building = new Building(id)
}