package surf.abm.agents.abbf.activities

/**
  * Used to distinguish between different types of activity
  *
  * I'm not really sure about the difference between using sealed case objects or scala's inbuilt Enumeration
  * package to do this. See: http://underscore.io/blog/posts/2014/09/03/enumerations.html
  */
object ActivityTypes extends Serializable {
  sealed abstract class ActivityType ( val name : String ) {

    // Can define methods, etc. for all ActivityTypes here.
    // Common members are defined in the constructor above.

    override def toString = name
  }

  case object WORKING extends ActivityType("Working")

  case object SUPERMARKET extends ActivityType("Supermarket")

  case object SHOPPING extends ActivityType("Shopping")

  case object LUNCHING extends ActivityType("Lunching")

  case object DINNER extends ActivityType("EatingDinner")

  case object GOING_OUT extends ActivityType("GoingOut")

  case object SPORTS extends ActivityType("DoingSports")

  case object SLEEPING extends ActivityType("Sleeping")

}

