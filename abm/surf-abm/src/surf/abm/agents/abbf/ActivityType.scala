package surf.abm.agents.abbf

/**
  * Created by nick on 09/05/2016.
  */
sealed trait ActivityType

case object WORKING extends ActivityType

case object SHOPPING extends ActivityType