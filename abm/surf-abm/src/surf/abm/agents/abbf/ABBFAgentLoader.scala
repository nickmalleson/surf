package surf.abm.agents.abbf

import org.apache.log4j.Logger
import surf.abm.agents.{Agent, UrbanAgent}
import surf.abm.agents.abbf.activities.ActivityTypes.{AT_HOME, SHOPPING, WORKING}
import surf.abm.agents.abbf.activities._
import surf.abm.main.{SurfABM, SurfGeometry}
import surf.abm.main.{SurfGeometry}

/**
  * The purpose of this loader is to create agents who behave according to the ABBF framework.
  * Assuming that the model has been configured to use the framework (see [[surf.abm.surf-abm.conf]]), then once
  * the model has been initialised, [[SurfABM]] will call the
  * [[surf.abm.agents.abbf.ABBFAgentLoader.createAgents()]] method.
  */
object ABBFAgentLoader {

  /**
    * This method is called by [[main.SurfABM]] after initialisation when the model starts.
    *
    * @param state The model state
    */
  def createAgents(state: SurfABM) = {

    // Instantiate the agent. We haven't defined the activities yet, but this is lazy so it doesn't matter.
    // This is because  agents and activities are immutable and paired which means they have to be instantiated together.
    // See https://stackoverflow.com/questions/7507965/instantiating-immutable-paired-objects

    val home = SurfABM.getRandomBuilding(state)
    val a: ABBFAgent = ABBFAgent(state, home)

    // Next the activities that this agent can do (later this will be done by reading data)

    // WORKING

    // Work place is a building in town
    val workPlace = Place(
      location = SurfABM.buildingIDGeomMap(SurfABM.conf.getInt(SurfABM.ModelConfig + ".WorkAddress")),
      activityType = WORKING,
      openingTimes = null // Assume it's open all the time
    )
    // Work time profile is 0 before 6 and after 10, and 1 between 10-4
    val workTimeProfile = TimeProfile(Array((6d, 0d), (10d, 1d), (16d, 1d), (22d, 0d)))
    val workActivity = WorkActivity(timeProfile = workTimeProfile, agent=a, place = workPlace)

    // SHOPPING
    val shoppingTimeProfile = TimeProfile(Array((0d, 0.2d))) // A constant, low intensity
    val shoppingActivity = ShopActivity(timeProfile = shoppingTimeProfile, agent=a)


    // BEING AT HOME
    val atHomePlace = Place(home, AT_HOME, null)
    val atHomeActivity = AtHomeActivity(TimeProfile(Array((0d, 0.5d))), agent=a)
    atHomeActivity.+=(0.5d)// Make this the most powerful activity to begin with.

    // Add these activities to the agent's activity list. At Home is the strongest initially.
    val activities = Set[Activity](workActivity , shoppingActivity , atHomeActivity )

    // Finally tell the agent abount them
    a.activities = activities




    //    XXXX now - need to think about 1 - how the intensities change over time (presumably this code goes into ABBFAgent) and 2 - how the agent's behaviour is controlled by them (again probably in ABBFAgent)


    // Last bits of admin required: add the geometry and schedule the agent and the spatial index updater

    SurfABM.agentGeoms.addGeometry(SurfGeometry[Agent](a.location, a))
    state.schedule.scheduleRepeating(a)

    SurfABM.agentGeoms.setMBR(SurfABM.mbr)
    state.schedule.scheduleRepeating(SurfABM.agentGeoms.scheduleSpatialIndexUpdater, Integer.MAX_VALUE, 1.0)


  }

  private val LOG: Logger = Logger.getLogger(this.getClass);

}
