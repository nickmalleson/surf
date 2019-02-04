package surf.abm.agents.abbf

import org.apache.log4j.Logger
import surf.abm.agents.abbf.occupations._
import surf.abm.environment.Building
import surf.abm.main.{BUILDING_FIELDS, SurfABM, SurfGeometry}

import scala.io.Source

/**
  * An agent loaded specifically for the Otley simulation. This might become generic later. See
  * [[surf.abm.agents.abbf.ABBFAgentLoader]] for more details about ABBF agent loaders.
  */
object ABBFAgentLoaderOtleyPRIME {

  val N = 1000
  // The number of agents (temporary)
  private val LOG: Logger = Logger.getLogger(this.getClass)

  /**
    * This method is called by [[surf.abm.main.SurfABM]] after initialisation when the model starts.
    * For the Otley PRIME scenario, we read a file that has an origin and destination OA, and for each of those
    * it has the number of commuters and number of retired people.
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
    val oaBuildingIDMap: Map[Set[String], Set[Int]] = ABBFAgentLoaderOtley.createOABuildingIDMap
    LOG.debug("Found the following OAs: ${oaBuildingIDMap.keys.toString()}")

    LOG.info(s"\tFound ${oaBuildingIDMap.size} OAs and ${oaBuildingIDMap.values.map(x => x.size).sum} buildings")

    // Now read each line of the file and create commuters (who live in one OA and commute to another) and retired people
    // (who just live in one OA and have flexible activities)

    val dataDir = SurfABM.conf.getString(SurfABM.ModelConfig + ".DataDir")
    val flowFilename = "./data/" + dataDir + "/oa_flows-study_area-PRIME.csv" // Contains commuters and retired
    val sample = SurfABM.conf.getDouble("SampleAgents") // A proportion of agents to create (runs too slowly if creating all)


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


    // READ DATA
    LOG.info(s"Reading agents from file: '$flowFilename'")
    // Get line and line number as a tuple
    for ((lineStr, i) <- Source.fromFile(flowFilename).getLines().zipWithIndex) {
      val line: Array[String] = lineStr.split(",")
      // The file has the following columns:
      // LineNo,MSOA,OA,DestinationOA,year,totalpopul,commuters,noncommuters
      if (i == 0) {
        // Check the header is as expected. It should have eight columns.
        if (line.size != 8) {
          throw new Exception(s"Problem reading the commuting file($flowFilename) - should have 8 columns but found ${line.size}: '$line'")
        }
      }
      else {
        // Get the origin, destination and the number of commuters and retired. Columns start counting at 0
        val orig: String = line(2).replace("\"", "") // (get rid of quotes)
        val dest: String = line(3).replace("\"", "")
        // Might take a sample of commuters and non-commuters to speed up runtime
        val commuters: Int = math.ceil(line(6).toInt * sample).toInt
        val noncommuters: Int = math.ceil(line(7).toInt * sample).toInt
        LOG.debug(s"Origin: '$orig', Destination: '$dest', Commuters: '${line(6).toInt}', Non-Commuters: '${line(7).toInt}'")
        LOG.debug(s"\tHave sampled $sample agents: Commuters: '$commuters', Non-Commuters: '$noncommuters'")




        // Now create 'commuters' agents who live in 'orig' and work in 'dest'
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
          val workListFFBool = oaBuildingIDMap.contains(Set(dest, ffType))
          val workListBarBool = oaBuildingIDMap.contains(Set(dest, barType))
          val workListSupmBool = oaBuildingIDMap.contains(Set(dest, supmType))
          val workListMallBool = oaBuildingIDMap.contains(Set(dest, mallType))
          val workListSportBool = oaBuildingIDMap.contains(Set(dest, sportType))
          if (workListSmallBool || workListLargeBool || workListShopBool || workListCafeBool ||
            workListRestBool || workListPubBool || workListFFBool || workListBarBool ||
            workListSupmBool || workListMallBool || workListSportBool) {
            val workList = List.concat(
              if (workListSmallBool) oaBuildingIDMap(Set(dest, small)).toList else List.empty,
              if (workListLargeBool) oaBuildingIDMap(Set(dest, large)).toList else List.empty,
              if (workListShopBool) oaBuildingIDMap(Set(dest, shopType)).toList else List.empty,
              if (workListCafeBool) oaBuildingIDMap(Set(dest, cafeType)).toList else List.empty,
              if (workListRestBool) oaBuildingIDMap(Set(dest, restType)).toList else List.empty,
              if (workListPubBool) oaBuildingIDMap(Set(dest, pubType)).toList else List.empty,
              if (workListFFBool) oaBuildingIDMap(Set(dest, ffType)).toList else List.empty,
              if (workListBarBool) oaBuildingIDMap(Set(dest, barType)).toList else List.empty,
              if (workListSupmBool) oaBuildingIDMap(Set(dest, supmType)).toList else List.empty,
              if (workListMallBool) oaBuildingIDMap(Set(dest, mallType)).toList else List.empty,
              if (workListSportBool) oaBuildingIDMap(Set(dest, sportType)).toList else List.empty)

            for (agent <- 0 until commuters) {
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
            } // for commuters
          } // if worklist


          // CREATED RETIRED DATA (and other agent classes that only have one fixed activity (home) could be added)
          for (agent <- 0 until noncommuters) {
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
          } // for noncommuters (retired)


        }
        else { // Not sure why this would happen - but I tink it's an error
          LOG.warn(s"ABBFAgentLoaderOtleyPRIME - couldn't find Building in the map for line $line: '${lineStr.toString}'")
        }


      } // if lineNo == 0

    } // for line in file

    LOG.info(s"Have created ${SurfABM.agentGeoms.getGeometries.size()} agents")

  } // createAgents
}
