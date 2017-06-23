package surf.abm.agents.abbf

import org.apache.log4j.Logger
import sim.field.geo.GeomVectorField
import surf.abm.agents.abbf.activities.ActivityTypes.{SHOPPING, SLEEPING, WORKING}
import surf.abm.agents.abbf.activities._
import surf.abm.environment.{Building, Junction}
import surf.abm.main.{BUILDING_FIELDS, GISFunctions, SurfABM, SurfGeometry}

import scala.io.Source

/**
  * An agent loaded specifically for the Otley simulation. This might become generic later. See
  * [[surf.abm.agents.abbf.ABBFAgentLoader]] for more details about ABBF agent loaders.
  */
object ABBFAgentLoaderOtley {

  val N = 1000
  // The number of agents (temporary)
  private val LOG: Logger = Logger.getLogger(this.getClass);

  /**
    * This method is called by [[surf.abm.main.SurfABM]] after initialisation when the model starts.
    * For the Otley scenario, we read a file that has the origin and destination OAs for each commuter
    * in the study area (see Nick's local file ~/mapping/projects/frl/model_data/otley/model_data/)
    * for the details about how these data were created.
    *
    * @param state The model state
    */
  def createAgents(state: SurfABM) = {

    LOG.info(s"Creating agents using ${this.getClass.getName}")

    // First need to work out which buildings of each type are contained within each output area. This is because the commuting
    // behaviour is defined at output area level. When the buildings data were created, fields called "OA" and "TYPE" were
    // created that contain the OA code and the type (small, large or shop). People only live in small buildings in the Otley area,
    // they only shop in shops, and they can work in any type of building.
    LOG.info("Creating map of output areas and their constituent buildings")
    val oaBuildingIDMap: Map[Set[String], Set[Int]] = {
      // Create mutable objects but, when they're ready, return them as immutable
      val b_map = scala.collection.mutable.Map[Set[String], scala.collection.mutable.Set[Int]]() // Temporary map (to return)
      // Iterate over all buildings (their ID and SurfGeometry)
      for ((id: Int, b: SurfGeometry[Building@unchecked]) <- SurfABM.buildingIDGeomMap) {
        val oaCode = b.getStringAttribute(BUILDING_FIELDS.BUILDINGS_OA.toString) // OA code for this building
        val buildingType = b.getStringAttribute(BUILDING_FIELDS.BUILDINGS_TYPE.toString)
        if (b_map.contains(Set(oaCode, buildingType))) {
          // Have already come across this OA and building type; add the new building id
          // Add the building to the set
          b_map(Set(oaCode, buildingType)) = b_map(Set(oaCode, buildingType)) + id
        }
        else {
          // Not found this combination of OA and type yet. Add it to the map and associate it with a new Set of building ids
          var s = scala.collection.mutable.Set.empty[Int]
          s += id
          b_map(Set(oaCode, buildingType)) = s
        }
        //println(i+" : "+b.getStringAttribute(BUILDING_FIELDS.BUILDINGS_OA.toString))
      }
      // Return the map and all sets as immutatable (that's what toMap and toSet do)
      // (https://stackoverflow.com/questions/2817055/converting-mutable-to-immutable-map)
      b_map.map(kv => (kv._1, kv._2.toSet)).toMap
    }
    LOG.debug("Fonnd the following OAs: ${oaBuildingIDMap.keys.toString()}")

    LOG.info(s"\tFound ${oaBuildingIDMap.size} OAs and ${oaBuildingIDMap.values.map(x => x.size).sum} buildings")


    // Now read through the commuting data and create agents appropriately (live in one OA, commute to another)

    val dataDir = SurfABM.conf.getString(SurfABM.ModelConfig + ".DataDir")
    val filename = "./data/" + dataDir + "/oa_flows-study_area.csv"
    LOG.info(s"Reading agents from file: '$filename'")
    // Get line and line number as a tuple
    for ((lineStr, i) <- Source.fromFile(filename).getLines().zipWithIndex) {
      val line: Array[String] = lineStr.split(",")
      //println(s"$i === ${lineStr.toString} === ${line.toString}")
      if (i == 0) {
        // Check the header is as expected. It should have four columns.
        if (line.size != 4) {
          // It should have four columns
          throw new Exception(s"Problem reading the commuting file($filename) - should have four columns but found ${line.size}: '$line'")
        }
      }
      else {
        // Get the origin, destination and flow. First column (0) is not important..
        val orig: String = line(1).replace("\"", "") // (get rid of quotes)
        val dest: String = line(2).replace("\"", "")
        val flow: Int = line(3).toInt
        //val flow = 1
        //Array(orig, dest, flow).foreach( x => println("\t"+x) )

        // Define the possible building types...
        val small: String = "SMALL"
        val large: String = "LARGE"
        val shopType: String = "SHOP"

        LOG.debug(s"Origin: '$orig', Destination: '$dest', Flow: '$flow'")
        // Now create 'flow' agents who live in 'orig' and work in 'dest'
        // It is helpful to have all the buildings in the origin and destinations as lists
        if (oaBuildingIDMap.contains(Set(orig, small))) {
          val homeList = oaBuildingIDMap(Set(orig, small)).toList

          // There might be a more efficient way to define the work buildings list.
          val workListSmallBool = oaBuildingIDMap.contains(Set(dest, small))
          val workListLargeBool = oaBuildingIDMap.contains(Set(dest, large))
          val workListShopBool = oaBuildingIDMap.contains(Set(dest, shopType))
          if (workListSmallBool || workListLargeBool || workListShopBool) {
            val workList = List.concat(
              if (workListSmallBool) oaBuildingIDMap(Set(dest,small)).toList else List.empty,
              if (workListLargeBool) oaBuildingIDMap(Set(dest,large)).toList else List.empty,
              if (workListShopBool) oaBuildingIDMap(Set(dest,shopType)).toList else List.empty)

            //val shopList = oaBuildingIDMap(orig).toList
            for (agent <- 0 until flow) {
              // Get the ID for a random home/work building
              val homeID: Int = homeList(state.random.nextInt(homeList.size))
              val workID: Int = workList(state.random.nextInt(workList.size))

              // Now get the buildings themselves and tell the agent about them
              val home: SurfGeometry[Building] = SurfABM.buildingIDGeomMap(homeID)
              val work: SurfGeometry[Building] = SurfABM.buildingIDGeomMap(workID)
              val shop: SurfGeometry[Building] = GISFunctions.findNearestObject[Building](home, SurfABM.shopGeoms)

              val nearestJunctionToCurrent: SurfGeometry[Junction] = GISFunctions.findNearestObject[Junction](home, SurfABM.junctions)
              val currentNode = SurfABM.network.findNode(nearestJunctionToCurrent.getGeometry.getCoordinate)
              //val shopID: Int = 1
              //val shop: SurfGeometry[Building] = SurfABM.buildingIDGeomMap(shopID)

              //makeAgent(state, home, work)
              makeAgent(state, home, work, shop)
            }
          }
        }

      }
    } // for line in file

    LOG.info(s"Have created ${SurfABM.agentGeoms.getGeometries.size()} agents")

  }

  /* Convenience to make an agent, just makes the loops in createAgent() a bit nicer */
  //def makeAgent(state: SurfABM, home: SurfGeometry[Building], work: SurfGeometry[Building], shop: SurfGeometry[Building]): Unit = {
  def makeAgent(state: SurfABM, home: SurfGeometry[Building], work: SurfGeometry[Building], shop: SurfGeometry[Building]): Unit = {
    // Finally create the agent, initialised with their home
    val a: ABBFAgent = ABBFAgent(state, home)

    // Work place is a building in town
    val workPlace = Place(
      location = work,
      activityType = WORKING,
      openingTimes = null // Assume it's open all the time
    )
    // Work time profile is 0 before 6 and after 10, and 1 between 10-4 with a bit of randomness thrown in
    val rnd = state.random.nextDouble() * 4d
    // A random number between 0 and 2
    val workTimeProfile = TimeProfile(Array((6d, 0d), (7d + rnd, 1d), (14d + rnd, 1d), (22d, 0d)))
    //val workTimeProfile = TimeProfile(Array((5d, 0d), (10d, 1d), (16d, 1d), (22d, 0d))) // without randomness
    val workActivity = WorkActivity(timeProfile = workTimeProfile, agent = a, place = workPlace)

    // Shopping place should be a supermarket or a convenience store of OpenStreetMaps
    val shoppingPlace = Place(
      location = shop,
      //location = null,
      activityType = SHOPPING,
      openingTimes = Array(Place.makeOpeningTimes(7.0, 22.0))
    )

    // SHOPPING
    val shoppingTimeProfile = TimeProfile(Array((6d, 0d), (15d + rnd, 0.3), (17d + rnd, 0.3), (22d, 0d)))
    // Was before a constant, low intensity. Is now a test with higher intensities in late afternoon and early evening.
    val shoppingActivity = ShopActivity(timeProfile = shoppingTimeProfile, agent = a, place = shoppingPlace)


    // School
    /*val schoolPlace = Place(
      location = null,
      activityType = ATTENDING_CLASS,
      openingTimes = null
    )*/

    // ATTENDING CLASS (activity at school)
    //val classTimeProfile = TimeProfile(Array((6d, 0d), (8d+rnd, 1d), (15d+rnd,1d), (17d,0d)))


    // SLEEPING (high between 11pm and 6am)
    val atHomePlace = Place(home, SLEEPING, null)
    val atHomeActivity = SleepActivity(TimeProfile(Array((0d, 1d), (9d, 0d), (23d, 1d))), agent = a)
    //val atHomeActivity = SleepActivity(TimeProfile(Array((0d, 0.5d))), agent=a)
    atHomeActivity.+=(0.1d * rnd) // Make this the most powerful activity to begin with, but a bit random


    // Add these activities to the agent's activity list. At Home is the strongest initially.
    // TODO ADD IN SHOPPING ACTIVITY
    val activities = Set[Activity](workActivity , shoppingActivity , atHomeActivity )
    //val activities = Set[Activity](workActivity, atHomeActivity)

    // Finally tell the agent abount them
    a.activities = activities


    // Last bits of admin required: add the geometry and schedule the agent and the spatial index updater

    SurfABM.agentGeoms.addGeometry(SurfGeometry[ABBFAgent](a.location, a))
    state.schedule.scheduleRepeating(a)

    SurfABM.agentGeoms.setMBR(SurfABM.mbr)
    state.schedule.scheduleRepeating(SurfABM.agentGeoms.scheduleSpatialIndexUpdater, Integer.MAX_VALUE, 1.0)
  }

}
