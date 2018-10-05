package surf.abm.agents.abbf.occupancies
import surf.abm.agents.abbf.ABBFAgent
import surf.abm.environment.Building
import surf.abm.main.{SurfABM, SurfGeometry}

/**
  * Created by geotcr on 01/10/2018.
  */
class SimpleABBFAgent(override val state:SurfABM, override val home:SurfGeometry[Building])
  extends ABBFAgent(state, home) {



}

object SimpleABBFAgent {

  def apply(state: SurfABM, home: SurfGeometry[Building]): SimpleABBFAgent =
    new SimpleABBFAgent(state, home )

}