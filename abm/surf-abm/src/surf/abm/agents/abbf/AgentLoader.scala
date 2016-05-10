package surf.abm.agents.abbf

import org.apache.log4j.Logger
import surf.abm.{SurfABM, SurfGeometry}
import surf.abm.agents.{Agent, RandomRoadAgent}

/**
  * The purpose of this loader is to create agents who behave according to the ABBF framework.
  * Assuming that the model has been configured to use the framework (see [[surf.abm.surf-abm.conf]]), then once
  * the model has been initialised, [[surf.abm.SurfABM]] will call the
  * [[surf.abm.agents.abbf.AgentLoader.createAgents()]] method.
  */
object AgentLoader {

  /**
    * This method is called by [[surf.abm.SurfABM]] after initialisation when the model starts.
    * @param state The model state
    */
  def createAgents (state:SurfABM) = {

    // Create one agent in random building

    val a: Agent = ABBFAgent(state, SurfABM.getRandomBuilding(state))
    SurfABM.agentGeoms.addGeometry(SurfGeometry[Agent](a.location, a))
    state.schedule.scheduleRepeating(a)



    SurfABM.agentGeoms.setMBR(SurfABM.mbr)
    state.schedule.scheduleRepeating(SurfABM.agentGeoms.scheduleSpatialIndexUpdater, Integer.MAX_VALUE, 1.0)


  }

  private val LOG: Logger = Logger.getLogger(this.getClass);

}
