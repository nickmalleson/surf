package surf.abm.agents.abbf

import org.apache.log4j.Logger
import surf.abm.agents.{Agent, UrbanAgent}
import surf.abm.agents.abbf.activities.ActivityTypes.{SLEEPING, WORKING}
import surf.abm.agents.abbf.activities._
import surf.abm.main.{SurfABM, SurfGeometry}
import surf.abm.main.SurfGeometry

/**
  * The purpose of this loader is to create agents who behave according to the ABBF framework.
  * Assuming that the model has been configured to use the framework (see [[surf.abm.surf-abm.conf]]), then once
  * the model has been initialised, [[SurfABM]] will call the
  * [[surf.abm.agents.abbf.ABBFAgentLoader.createAgents()]] method.
  */
object ABBFAgentLoader {

  val N = 100 // The number of agents (temporary)

  /**
    * This method is called by [[surf.abm.main.SurfABM]] after initialisation when the model starts.
    *
    * @param state The model state
    */
  def createAgents(state: SurfABM) = {

    for (i <- 0.until(N)) {

      // Instantiate the agent.

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
      // Work time profile is 0 before 6 and after 10, and 1 between 10-4 with a bit of randomness thrown in
      val rnd = state.random.nextDouble()*4d // A random number between 0 and 2
      val workTimeProfile = TimeProfile(Array((6d, 0d), (7d+rnd, 1d), (14d+rnd, 1d), (22d, 0d)))
      //val workTimeProfile = TimeProfile(Array((5d, 0d), (10d, 1d), (16d, 1d), (22d, 0d))) // without randomness
      val workActivity = WorkActivity(timeProfile = workTimeProfile, agent=a, place = workPlace)

      // SHOPPING
      val shoppingTimeProfile = TimeProfile(Array((0d, 0.2d))) // A constant, low intensity
      val shoppingActivity = ShopActivity(timeProfile = shoppingTimeProfile, agent=a)


      // SLEEPING (high between 11pm and 6am)
      val atHomePlace = Place(home, SLEEPING, null)
      val atHomeActivity = SleepActivity(TimeProfile(Array( (0d, 1d), (9d, 0d), (23d, 1d) )), agent=a)
      //val atHomeActivity = SleepActivity(TimeProfile(Array((0d, 0.5d))), agent=a)
      atHomeActivity.+=(0.1d*rnd)// Make this the most powerful activity to begin with, but a bit random


      // Add these activities to the agent's activity list. At Home is the strongest initially.
      // TODO ADD IN SHOPPING ACTIVITY
      //val activities = Set[Activity](workActivity , shoppingActivity , atHomeActivity )
      val activities = Set[Activity](workActivity , atHomeActivity )

      // Finally tell the agent abount them
      a.activities = activities


      // Last bits of admin required: add the geometry and schedule the agent and the spatial index updater

      SurfABM.agentGeoms.addGeometry(SurfGeometry[ABBFAgent](a.location, a))
      state.schedule.scheduleRepeating(a)

      SurfABM.agentGeoms.setMBR(SurfABM.mbr)
      state.schedule.scheduleRepeating(SurfABM.agentGeoms.scheduleSpatialIndexUpdater, Integer.MAX_VALUE, 1.0)

    }




  }

  private val LOG: Logger = Logger.getLogger(this.getClass);

}
