package surf.abm.agents.abbf

import sim.engine.SimState
import surf.abm.agents.UrbanAgent
import surf.abm.environment.Building
import surf.abm.{SurfABM, SurfGeometry}

/**
  * Created by nick on 09/05/2016.
  */
class ABBFAgent(state:SurfABM, home:SurfGeometry[Building]) extends UrbanAgent(state, home) {

  override def step(state: SimState): Unit = {

    // TODO implement step!!

  }

}
