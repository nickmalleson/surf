package surf.abm.agents.abbf

import org.apache.log4j.Logger

import scala.util.control.Breaks._

/**
  * Created by nick on 09/05/2016.
  */
trait Activity {
  def activityType: ActivityType
  def timeProfile: TimeProfile
}

