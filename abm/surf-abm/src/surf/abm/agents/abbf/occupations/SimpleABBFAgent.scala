package surf.abm.agents.abbf.occupations
import surf.abm.agents.abbf.ABBFAgent
import surf.abm.environment.Building
import surf.abm.main.{SurfABM, SurfGeometry}

/**
  * SimpleABBFAgent is actually NOT BEING USED.
  * Only necessary if you want to use the older and simpler [[../ABBFAgentLoader]], which is NOT UP TO DATE,
  * instead of the new [[../ABBFAgentLoaderOtley]].
  */

class SimpleABBFAgent(override val state:SurfABM, override val home:SurfGeometry[Building])
  extends ABBFAgent(state, home) {



}

object SimpleABBFAgent {

  def apply(state: SurfABM, home: SurfGeometry[Building]): SimpleABBFAgent =
    new SimpleABBFAgent(state, home )

}