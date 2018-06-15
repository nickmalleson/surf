package surf.abm.agents.abbf

import org.apache.log4j.Logger
import sim.field.geo.GeomVectorField
import surf.abm.agents.abbf.activities.ActivityTypes.{LUNCHING, DINNER, GOING_OUT, SHOPPING, SLEEPING, WORKING}
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
  private val LOG: Logger = Logger.getLogger(this.getClass)

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
    val filename = "./data/" + dataDir + "/oa_flows-burley_adel.csv" // All output areas
    //val filename = "./data/" + dataDir + "/oa_flows-study_area_test10.csv" // Testing with 10 output areas
    //val filename = "./data/" + dataDir + "/oa_flows-study_area_test1000.csv" // Testing with 1000 output areas
    //LOG.info("ABBFAGENTLOADEROTLEY is temporarily only creating a few agents")
    //val filename = "./data/" + dataDir + "/oa_flows-study_area.csv"
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
        val cafeType: String = "CAFE"
        val restType: String = "REST" // restaurant
        val pubType: String = "PUB"
        val ffType: String = "FF" // fastfood
        val barType: String = "BAR"

        LOG.debug(s"Origin: '$orig', Destination: '$dest', Flow: '$flow'")
        // Now create 'flow' agents who live in 'orig' and work in 'dest'
        // It is helpful to have all the buildings in the origin and destinations as lists
        if (oaBuildingIDMap.contains(Set(orig, small))) {
          val homeList = oaBuildingIDMap(Set(orig, small)).toList

          // There might be a more efficient way to define the work buildings list.
          val workListSmallBool = oaBuildingIDMap.contains(Set(dest, small))
          val workListLargeBool = oaBuildingIDMap.contains(Set(dest, large))
          val workListShopBool = oaBuildingIDMap.contains(Set(dest, shopType))
          val workListCafeBool = oaBuildingIDMap.contains(Set(dest, cafeType))
          val workListRestBool = oaBuildingIDMap.contains(Set(dest, restType))
          val workListPubBool = oaBuildingIDMap.contains(Set(dest, pubType))
          val workListFFBool = oaBuildingIDMap.contains(Set(dest,  ffType))
          val workListBarBool = oaBuildingIDMap.contains(Set(dest, barType))
          if (workListSmallBool || workListLargeBool || workListShopBool || workListCafeBool ||
            workListRestBool || workListPubBool || workListFFBool || workListBarBool) {
            val workList = List.concat(
              if (workListSmallBool) oaBuildingIDMap(Set(dest,small)).toList else List.empty,
              if (workListLargeBool) oaBuildingIDMap(Set(dest,large)).toList else List.empty,
              if (workListShopBool) oaBuildingIDMap(Set(dest,shopType)).toList else List.empty,
              if (workListCafeBool) oaBuildingIDMap(Set(dest,cafeType)).toList else List.empty,
              if (workListRestBool) oaBuildingIDMap(Set(dest,restType)).toList else List.empty,
              if (workListPubBool) oaBuildingIDMap(Set(dest,pubType)).toList else List.empty,
              if (workListFFBool) oaBuildingIDMap(Set(dest,ffType)).toList else List.empty,
              if (workListBarBool) oaBuildingIDMap(Set(dest,barType)).toList else List.empty)


            //val shopList = oaBuildingIDMap(orig).toList
            for (agent <- 0 until flow) {
              // Get the ID for a random home/work building
              val homeID: Int = homeList(state.random.nextInt(homeList.size))
              val workID: Int = workList(state.random.nextInt(workList.size))

              // Now get the buildings themselves and tell the agent about them
              val home: SurfGeometry[Building] = SurfABM.buildingIDGeomMap(homeID)
              val work: SurfGeometry[Building] = SurfABM.buildingIDGeomMap(workID)
              val shop: SurfGeometry[Building] = GISFunctions.findNearestObject[Building](home, SurfABM.shopGeoms)
              //val lunchLocation: SurfGeometry[Building] = GISFunctions.findNearestObject[Building](work, SurfABM.lunchGeoms)
              //val dinnerLocation: SurfGeometry[Building] = SurfABM.getRandomFunctionalBuilding(state, SurfABM.dinnerGeoms)

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
    // A random number between 0 and 4
    val rndLunchPreference = state.random.nextDouble()
    // Test with a random preference for eating and leisure activities. Should become activity specific.
    val rndDinnerPreference = state.random.nextDouble() // maybe not good because should mainly be at random day in week, not only done by random agents regularly
    val rndGoingOutPreference = state.random.nextDouble() / 2d
    val workTimeProfile = TimeProfile(Array((6d, 0d), (7d + rnd, 1d), (14d + rnd, 1d), (22d, 0d)))
    val workActivity = WorkActivity(timeProfile = workTimeProfile, agent = a, place = workPlace)

    // SHOPPING
    // Shopping place should be a supermarket or a convenience store of OpenStreetMap
    /*val shoppingPlace = Place(
      location = shop,
      //location = null,
      activityType = SHOPPING,
      openingTimes = Array(Place.makeOpeningTimes(7.0, 22.0))
    )*/

    //val shoppingTimeProfile = TimeProfile(Array((7d, 0d), (15d + rnd, 0.3), (17d + rnd, 0.3), (22d, 0d)))
    val shoppingTimeProfile = TimeProfile(Array((7d, 0d), (11d + rnd, 1d), (17d + rnd, 0.3), (22d, 0d)))
    val shoppingActivity = ShopActivity(timeProfile = shoppingTimeProfile, agent = a, state)

    // LUNCHING
    //val lunchLocationRnd = state.random.nextDouble()
    //val lunchLocation: SurfGeometry[Building] = GISFunctions.findRandomObject[Building](work, SurfABM.lunchGeoms)
    /*val lunchPlace = Place(
      location = lunchLocation,
      activityType = LUNCHING,
      openingTimes = Array(Place.makeOpeningTimes(11.0, 16.0))
      // TimeIntensity of lunch is 0 outside lunch hours, but not BackgroundIntensity, so might be necessary anyway
    )*/
    val lunchTimeProfile = TimeProfile(Array((11d, 0d), (11.5 + rnd/2, rndLunchPreference), (12d + rnd/2, rndLunchPreference), (15d, 0d)))
    val lunchActivity = LunchActivity(timeProfile = lunchTimeProfile, agent = a, state)

    // DINNER
    val dinnerTimeProfile = TimeProfile(Array((17d, 0d), (18d + rnd/2, rndDinnerPreference), (19.5 + rnd/2, rndDinnerPreference), (22.5, 0d)))
    val dinnerActivity = DinnerActivity(timeProfile = dinnerTimeProfile, agent = a, state)

    // GOING OUT
    val goingOutTimeProfile = TimeProfile(Array((0d, rndGoingOutPreference / 4.0), (2d, 0d), (20d, 0d), (22d, rndGoingOutPreference)))
    val goingOutActivity = GoingOutActivity(timeProfile = goingOutTimeProfile, agent = a, state)


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
    val atHomeActivity = SleepActivity(TimeProfile(Array((0d, 1d), (4d, 1d), (12d, 0d), (23d, 1d))), agent = a)
    //val atHomeActivity = SleepActivity(TimeProfile(Array((0d, 0.5d))), agent=a)
    // Increase this activity to make it the most powerful activity to begin with, but with a bit of randomness
    // (repeatedly call the ++ function to increase it)
    for (i <- 1.until(72 +(rnd * 25).toInt) ) {
      atHomeActivity.++()
    }


    // Add these activities to the agent's activity list. At Home is the strongest initially.
    val activities = Set[Activity](workActivity , shoppingActivity , atHomeActivity , lunchActivity , dinnerActivity, goingOutActivity )
    //val activities = Set[Activity](workActivity, atHomeActivity)

    // Finally tell the agent about them
    a.activities = activities


    // Last bits of admin required: add the geometry and schedule the agent and the spatial index updater

    SurfABM.agentGeoms.addGeometry(SurfGeometry[ABBFAgent](a.location, a))
    state.schedule.scheduleRepeating(a, SurfABM.AGENTS_STEP, 1)

    SurfABM.agentGeoms.setMBR(SurfABM.mbr)
    state.schedule.scheduleRepeating(SurfABM.agentGeoms.scheduleSpatialIndexUpdater, SurfABM.AGENTS_STEP, 1.0)
  }

}
