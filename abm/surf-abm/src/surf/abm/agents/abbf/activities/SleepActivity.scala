package surf.abm.agents.abbf.activities

import org.apache.commons.collections.functors.NullIsFalsePredicate
import surf.abm.agents.{Agent, UrbanAgent}
import surf.abm.agents.abbf.{ABBFAgent, Place, TimeProfile}
import surf.abm.agents.abbf.activities.ActivityTypes.{SLEEPING}
import surf.abm.exceptions.RoutingException
import surf.abm.main.SurfABM

/**
  * An activity of type [[surf.abm.agents.abbf.activities.ActivityTypes.SLEEPING]] that causes the agent to go home and
  * sleep.
  */
case class SleepActivity(
                    override val timeProfile: TimeProfile,
                    override val agent: ABBFAgent)
  extends FixedActivity (
    activityType = SLEEPING,
    timeProfile = timeProfile,
    agent = agent,
    place = Place(agent.home, SLEEPING))
    with Serializable
{

  // These variables define the different things that the agent could be doing. They could be Booleans, but I
  // think I prefer them as case classes (they behave like Enums) because you can use pattern matching, so nicer
  // syntax (although probably not simpler to read than the if/else way).
  // Actually, the best way is probably with unique integers and a switch statement, but I'm leaving it as it is
  // for now because it's an example of how scala can do enums. It's also a bit more powerful, as each class
  // could have their own members. Another advantage because the parent trait is 'sealed', the compiler checks
  // that all alternatives must be exhausted. You can't have a match statement without all of the subclasses.
  //private var sleeping = false // Actually at home and asleep?
  //private var travellingHome = false // On the way home
  //private var init = true // The activity has just been initialised
  private sealed trait Action
  private case class Sleeping() extends Action
  private case class TravellingHome() extends Action
  private case class Initialising() extends Action
  private var currentAction : Action = Initialising()
  //private val SLEEPING = 1
  //private val TRAVELLING_HOME = 2
  //private val INITIALISING = 3
  //private var currentAction = INITIALISING

  private var goingHome = false

  // Find where the agent works (specified temporarily in the config file)
  val workBuilding = SurfABM.buildingIDGeomMap(SurfABM.conf.getInt(SurfABM.ModelConfig+".WorkAddress"))

  /**
    * Cause the agent to go home (if necessary first) and then sleep (perform the activity).
    *
    * @return True if the agent has performed this activity, false if they have not (e.g. if they are still travelling
    *         to a particular destination).
    */
  override def performActivity(): Boolean = {

    // Determine what the currentAction is.
    currentAction match {
      case _ : Initialising => {
        Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]$${agent.toString()} is initialising SleepActivity")
        // See if the agent is at home

        //if (this.agent.home.==(this.agent.location())) {
        if (this.place.location.equalLocation(this.agent.location())) {
          Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]$${agent.toString()} is at home. Starting to sleep")
          currentAction = Sleeping() // Next iteration the agent will start to sleep
        }
        else {
          Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]$${agent.toString()} is not at home. Travelling there.")
          this.agent.newDestination(Option(this.place.location))
          currentAction = TravellingHome()
        }
      } // initialising

      case _ : TravellingHome => {
        if (this.agent.atDestination()) {
          Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]$${agent.toString()} has reached home. Starting to sleep.")
          currentAction = Sleeping() // Next iteration the agent will start to sleep
        }
        else {
          Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]$${agent.toString()} is travelling home.")
          agent.moveAlongPath()
        }
      } // TravellingHome

      case _ : Sleeping => {
        Agent.LOG.debug(s"[${agent.state.schedule.getTime()}]$${agent.toString()} is sleeping")
        return true
      }  // Sleeping

    } // match

    // If here then we can't be sleeping, because otherwise we would have returned true
    return false

    /* Useful (?) old code for moving randomly from home to another building


      if (agent.destination.isEmpty || agent.atDestination) {

        if (goingHome) {
          Agent.LOG.debug("Agent "+ agent.id.toString() + " has arrived home. Going to Somewhere else")
          goingHome = false
          agent.newDestination(Option(workBuilding))

        }
        else {
          Agent.LOG.debug("Agent "+ agent.id.toString() + " has arrived at work. Going home")
          goingHome = true
          agent.newDestination(Option(agent.home))
        }

      }

      agent.moveAlongPath()
      */

    }

  /**
    *  Reset the private members that control the state of this activity
    */
  override def activityChanged(): Unit = {
    //this.sleeping = false // Actually at home and asleep?
    //this.travellingHome = false // On the way home
    //this.init = true // The activity has just been initialised
    this.currentAction = Initialising()
    this._currentIntensityDecrease = 0d
  }
}
