package surf.abm.agents.abbf

import org.apache.log4j.Logger
import sim.field.geo.GeomVectorField
import surf.abm.agents.abbf.activities.ActivityTypes.{SLEEPING, WORKING}
import surf.abm.agents.abbf.activities._
import surf.abm.environment.Building
import surf.abm.main.{BUILDING_FIELDS, SurfABM, SurfGeometry}

/**
  * An agent loaded specifically for the Otley simulation. This might become generic later. See
  * [[surf.abm.agents.abbf.ABBFAgentLoader]] for more details about ABBF agent loaders.
  */
object ABBFAgentLoaderOtley {

  val N = 1000 // The number of agents (temporary)

  /**
    * This method is called by [[surf.abm.main.SurfABM]] after initialisation when the model starts.
    * For the Otley scenario, we read a file that has the origin and destination OAs for each commuter
    * in the study area (see Nick's local file ~/mapping/projects/frl/model_data/otley/model_data/)
    * for the details about how these data were created.
    *
    *
    * @param state The model state
    */
  def createAgents(state: SurfABM) = {

    LOG.info(s"Creating agents using ${this.getClass.getName}")

    // First need to work out which buildings are contained within each output area. This is because the commuting
    // behaviour is defined at output area level. When the buildings data were created a field called "OA" was
    // created that contains the OA code.
    LOG.info("Creating map of output areas and their constituent buildings")
    val oaBuildingIDMap: Map[String, Set[Int]] = { // Create mutable obejects but, when they're ready, return them as immutable
      val b_map = scala.collection.mutable.Map[String, scala.collection.mutable.Set[Int]]() // Temporary map (to return)
      // Iterate over all buildings (their ID and SurfGeometry)
      for (  (id:Int, b:SurfGeometry[Building @unchecked]) <- SurfABM.buildingIDGeomMap) {
        val oaCode = b.getStringAttribute(BUILDING_FIELDS.BUILDINGS_OA.toString) // OA code for this building
        if (b_map.contains(oaCode)) { // Have already come across this OA; add the new building id
          // Add the building to the set
          b_map(oaCode) = b_map(oaCode) + id
        }
        else { // Not found this OA yet. Add it to the map and associate it with a new Set of building ids
          var s = scala.collection.mutable.Set.empty[Int]
          s += id
          b_map(oaCode) = s
        }
        //println(i+" : "+b.getStringAttribute(BUILDING_FIELDS.BUILDINGS_OA.toString))
      }
      // Return the map and all sets as immutatable (that's what toMap and toSet do)
      // (https://stackoverflow.com/questions/2817055/converting-mutable-to-immutable-map)
      b_map.map(kv => (kv._1,kv._2.toSet)).toMap
    }

    LOG.info(s"\tFound ${oaBuildingIDMap.size} OAs and ${oaBuildingIDMap.values.map( x => x.size).sum} buildings")

    //for ( (k,v) <- oaBuildingIDMap ) println(k+":"+v.size+" - "+v.toString())

    // XXXX HERE - Now read through the commuting data and create agents appropriately (live in one OA, commute to another)
    // FIRST NEED TO WRITE OUT THE FLOW DATA - SEE otley.Rmd

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
