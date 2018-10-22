package surf.abm.agents.abbf

import org.apache.log4j.Logger
import sim.field.geo.GeomVectorField
import surf.abm.agents.abbf.activities.ActivityTypes.{LUNCHING, DINNER, GOING_OUT, SHOPPING, SLEEPING, WORKING}
import surf.abm.agents.abbf.activities._
import surf.abm.agents.abbf.occupations._
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
  def createAgents(state: SurfABM): Unit = {

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
    LOG.debug("Found the following OAs: ${oaBuildingIDMap.keys.toString()}")

    LOG.info(s"\tFound ${oaBuildingIDMap.size} OAs and ${oaBuildingIDMap.values.map(x => x.size).sum} buildings")


    // Now read through the commuting data and create agents appropriately (live in one OA, commute to another)
    // Also read a file of agents that don't commute (just live in one OA and have flexible activities)

    val dataDir = SurfABM.conf.getString(SurfABM.ModelConfig + ".DataDir")
    val inputfilesVersion = SurfABM.conf.getInt(SurfABM.ModelConfig + ".InputFilesVersion")
    val flowFilename = "./data/" + dataDir + "/oa_flows-study_area.csv" // Commuters between all output areas
    //val flowFilename = "./data/" + dataDir + "/oa_flows-burley_adel.csv" // Testing with commuters from Burley-i-W to Adel (longest distance in study area)
    //val flowFilename = "./data/" + dataDir + "/oa_flows-study_area_test10.csv" // Testing with 10 combinations of output areas
    //val flowFilename = "./data/" + dataDir + "/oa_flows-study_area_test1000.csv" // Testing with 1000 combinations of output areas
    //LOG.info("ABBFAGENTLOADEROTLEY is temporarily only creating a few agents")

    val stayFilename = if (inputfilesVersion == 2) "./data/" + dataDir + "/oa_retired-study_area.csv" else null // Retired people in every output area
    //val stayFilename = if (inputfilesVersion == 2) "./data/" + dataDir + "/oa_retired-study_area_test10.csv" else null // Retired people in 10 output areas


    // Define the possible building types...
    val small: String = "SMALL"
    val large: String = "LARGE"
    val cafeType: String = "CAFE"
    val restType: String = "REST" // restaurant
    val pubType: String = "PUB"
    val ffType: String = "FF" // fastfood
    val barType: String = "BAR"
    val shopType: String = "SHOP" // shops not in a mall and not supermarkets or convenience stores
    val supmType: String = "SUPM" // supermarkets and convenience stores
    val mallType: String = "MALL"
    val sportType: String = "SPORT"


    // READ COMMUTER DATA
    LOG.info(s"Reading agents from file: '$flowFilename'")
    // Get line and line number as a tuple
    for ((lineStr, i) <- Source.fromFile(flowFilename).getLines().zipWithIndex) {
      val line: Array[String] = lineStr.split(",")
      //println(s"$i === ${lineStr.toString} === ${line.toString}")
      if (i == 0) {
        // Check the header is as expected. It should have four columns.
        if (line.size != 4) {
          // It should have four columns
          throw new Exception(s"Problem reading the commuting file($flowFilename) - should have four columns but found ${line.size}: '$line'")
        }
      }
      else {
        // Get the origin, destination and flow. First column (0) is not important..
        val orig: String = line(1).replace("\"", "") // (get rid of quotes)
        val dest: String = line(2).replace("\"", "")
        val flow: Int = line(3).toInt
        //val flow = 1
        //Array(orig, dest, flow).foreach( x => println("\t"+x) )
        LOG.debug(s"Origin: '$orig', Destination: '$dest', Flow: '$flow'")

        // Now create 'flow' agents who live in 'orig' and work in 'dest'
        // It is helpful to have all the buildings in the origin and destinations as lists
        if (oaBuildingIDMap.contains(Set(orig, small))) {
          val homeList = oaBuildingIDMap(Set(orig, small)).toList

          // There might be a more efficient way to define the work buildings list.
          // TO DO: Define that you can work in any building!!
          val workListSmallBool = oaBuildingIDMap.contains(Set(dest, small))
          val workListLargeBool = oaBuildingIDMap.contains(Set(dest, large))
          val workListShopBool = oaBuildingIDMap.contains(Set(dest, shopType))
          val workListCafeBool = oaBuildingIDMap.contains(Set(dest, cafeType))
          val workListRestBool = oaBuildingIDMap.contains(Set(dest, restType))
          val workListPubBool = oaBuildingIDMap.contains(Set(dest, pubType))
          val workListFFBool = oaBuildingIDMap.contains(Set(dest,  ffType))
          val workListBarBool = oaBuildingIDMap.contains(Set(dest, barType))
          val workListSupmBool = oaBuildingIDMap.contains(Set(dest, supmType))
          val workListMallBool = oaBuildingIDMap.contains(Set(dest, mallType))
          val workListSportBool = oaBuildingIDMap.contains(Set(dest, sportType))
          if (workListSmallBool || workListLargeBool || workListShopBool || workListCafeBool ||
            workListRestBool || workListPubBool || workListFFBool || workListBarBool ||
            workListSupmBool || workListMallBool || workListSportBool) {
            val workList = List.concat(
              if (workListSmallBool) oaBuildingIDMap(Set(dest,small)).toList else List.empty,
              if (workListLargeBool) oaBuildingIDMap(Set(dest,large)).toList else List.empty,
              if (workListShopBool) oaBuildingIDMap(Set(dest,shopType)).toList else List.empty,
              if (workListCafeBool) oaBuildingIDMap(Set(dest,cafeType)).toList else List.empty,
              if (workListRestBool) oaBuildingIDMap(Set(dest,restType)).toList else List.empty,
              if (workListPubBool) oaBuildingIDMap(Set(dest,pubType)).toList else List.empty,
              if (workListFFBool) oaBuildingIDMap(Set(dest,ffType)).toList else List.empty,
              if (workListBarBool) oaBuildingIDMap(Set(dest,barType)).toList else List.empty,
              if (workListSupmBool) oaBuildingIDMap(Set(dest,supmType)).toList else List.empty,
              if (workListMallBool) oaBuildingIDMap(Set(dest,mallType)).toList else List.empty,
              if (workListSportBool) oaBuildingIDMap(Set(dest,sportType)).toList else List.empty)

            for (agent <- 0 until flow) {
              // Get the ID for a random home/work building
              val homeID: Int = homeList(state.random.nextInt(homeList.size))
              val workID: Int = workList(state.random.nextInt(workList.size))

              // Now get the buildings themselves and tell the agent about them
              val home: SurfGeometry[Building] = SurfABM.buildingIDGeomMap(homeID)
              val work: SurfGeometry[Building] = SurfABM.buildingIDGeomMap(workID)

              //val nearestJunctionToCurrent: SurfGeometry[Junction] = GISFunctions.findNearestObject[Junction](home, SurfABM.junctions)
              //val currentNode = SurfABM.network.findNode(nearestJunctionToCurrent.getGeometry.getCoordinate)

              //makeAgent(state, home, work)
              val a: CommuterAgent = CommuterAgent(state, home, work)
              a.defineActivities()

              SurfABM.agentGeoms.addGeometry(SurfGeometry[ABBFAgent](a.location(), a))
              state.schedule.scheduleRepeating(a, SurfABM.AGENTS_STEP, 1)

              SurfABM.agentGeoms.setMBR(SurfABM.mbr)
              state.schedule.scheduleRepeating(SurfABM.agentGeoms.scheduleSpatialIndexUpdater, SurfABM.AGENTS_STEP, 1.0)
            }
          }
        }

      }
    } // for line in file

    LOG.info(s"Have created ${SurfABM.agentGeoms.getGeometries.size()} commuter agents")


    // READ RETIRED DATA (and other agent classes that only have one fixed activity (home) could be added)
    if (inputfilesVersion == 2) {
      LOG.info(s"Reading retired agents from file: '$stayFilename'")
      // Get line and line number as a tuple
      for ((lineStr, i) <- Source.fromFile(stayFilename).getLines().zipWithIndex) {
        val line: Array[String] = lineStr.split(",")
        //println(s"$i === ${lineStr.toString} === ${line.toString}")
        if (i == 0) {
          // Check the header is as expected. It should have four columns.
          if (line.size != 4) {
            // It should have four columns
            throw new Exception(s"Problem reading the retired people file($flowFilename) - should have four columns but found ${line.size}: '$line'")
          }
        }
        else {
          // Get the oa and number of retired. First columns (0, 1) are not important..
          val oa: String = line(2).replace("\"", "") // (get rid of quotes)
          val retired: Int = line(3).toInt
          LOG.debug(s"OA: '$oa', Retired: '$retired'")

          // Now create agents who live in 'oa'
          // It is helpful to have all the buildings as lists
          if (oaBuildingIDMap.contains(Set(oa, small))) {
            val homeList = oaBuildingIDMap(Set(oa, small)).toList

            for (agent <- 0 until retired) {
              // Get the ID for a random home/work building
              val homeID: Int = homeList(state.random.nextInt(homeList.size))
              // Now get the buildings themselves and tell the agent about them
              val home: SurfGeometry[Building] = SurfABM.buildingIDGeomMap(homeID)

              //makeAgent(state, home)
              val a: RetiredAgent = RetiredAgent(state, home)
              a.defineActivities()

              SurfABM.agentGeoms.addGeometry(SurfGeometry[ABBFAgent](a.location(), a))
              state.schedule.scheduleRepeating(a, SurfABM.AGENTS_STEP, 1)

              SurfABM.agentGeoms.setMBR(SurfABM.mbr)
              state.schedule.scheduleRepeating(SurfABM.agentGeoms.scheduleSpatialIndexUpdater, SurfABM.AGENTS_STEP, 1.0)
            }

          }
        }
      } // for line in file
      LOG.info(s"Have created ${SurfABM.agentGeoms.getGeometries.size()} retired agents")
    }

  }


}
