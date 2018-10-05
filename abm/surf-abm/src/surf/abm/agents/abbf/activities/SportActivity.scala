package surf.abm.agents.abbf.activities

import org.apache.log4j.Logger
import sim.engine.SimState
import surf.abm.agents.Agent
import surf.abm.agents.abbf.{ABBFAgent, Place, TimeProfile}
import surf.abm.agents.abbf.activities.ActivityTypes.SPORTS
import surf.abm.agents.abbf.occupancies.{CommuterAgent, RetiredAgent}
import surf.abm.main.{GISFunctions, SurfABM, SurfGeometry}
import surf.abm.environment.Building

/**
  * An activity (of type [[surf.abm.agents.abbf.activities.ActivityTypes.SPORTS]]) that causes the agent to
  * travel to sport centres, pitches, etc.
  */
case class SportActivity (
  override val timeProfile: TimeProfile,
  override val agent: ABBFAgent,
  state: SimState)
  extends FlexibleActivity(SPORTS, timeProfile, agent)  with Serializable
  {

    // These variables define the different things that the agent could be doing in order to satisfy the work activity
    // (Note: in SleepActivity, these are defined as case classes that extend a sealed trait, but this way is probably
    // more efficient)

    private val LOG: Logger = Logger.getLogger(this.getClass)

    private val DOING_SPORTS = 1
    private val TRAVELLING = 2
    private val INITIALISING = 3
    private var currentAction = INITIALISING

    private val place = Place(
    location = null,
    activityType = SPORTS,
    openingTimes = Array(Place.makeOpeningTimes(7.0, 22.0))
  )

    /**
      * This makes the agent actually perform the activity.
      *
      * @return True if the agent has performed this activity, false if they have not (e.g. if they are still travelling
      *         to a particular destination).
      */
    override def performActivity(): Boolean = {

    this.currentAction match {

      case INITIALISING => {
        Agent.LOG.debug(agent, "initialising SportActivity")
        val sportLocation: SurfGeometry[Building] = GISFunctions.findNearestObject[Building](this.agent.location(), SurfABM.sportGeoms, true, state)
        this.place.location = sportLocation
        // See if the agent is in the sports location
        if (this.place.location.equalLocation(
          this.agent.location())) {
          Agent.LOG.debug(agent, "is somewhere where they can start doing sports.")
          currentAction = DOING_SPORTS // Next iteration the agent will start to do sports
        }
        else {
          Agent.LOG.debug(agent, "is not at the sports location yet. Travelling there.")
          this.agent.newDestination(Option(this.place.location))
          currentAction = TRAVELLING
        }

      }

      case TRAVELLING => {
        if (this.agent.atDestination()) {
          Agent.LOG.debug(agent, "has reached the location where they can start doing sports.")
          currentAction = DOING_SPORTS
        }
        else {
          Agent.LOG.debug(agent, "is travelling to the sports location.")
          agent.moveAlongPath()
        }
      }

      case DOING_SPORTS => {
        Agent.LOG.debug(agent, "is doing sports.")
        return true
      }

    }
    // Only get here if the agent isn't shopping, so must return false.
    return false

    //throw new NotImplementedError("Have not implemented Sport activity yet")
  }

    override def activityChanged(): Unit = {
    this.currentAction = INITIALISING
    this._currentIntensityDecrease = 0d
    //throw new NotImplementedError("Have not implemented Sport activity yet")
  }


    /**
      * The amount that the sport activity should increase at each iteration
      * @return
      */
    override def backgroundIncrease(): Double = {
    if (this.agent.getClass == classOf[CommuterAgent]) {
      return 1d / (3d * SurfABM.ticksPerDay)
    } else if (this.agent.getClass == classOf[RetiredAgent]) {
      return 1d / (5d * SurfABM.ticksPerDay)
    } else {
      return 1d / (5d * SurfABM.ticksPerDay)
    }
  }

    /**
      * The amount that a sport activity will go down by if an agent is doing sports.
      * @return
      */
    override def reduceActivityAmount(): Double = {
    if (this.agent.getClass == classOf[CommuterAgent]) {
      return 18d / SurfABM.ticksPerDay
    } else if (this.agent.getClass == classOf[RetiredAgent]) {
      return 24d / SurfABM.ticksPerDay
    } else {
      return 12d / SurfABM.ticksPerDay
    }
  }

    override val MINIMUM_INTENSITY_DECREASE = 0.3

  }
