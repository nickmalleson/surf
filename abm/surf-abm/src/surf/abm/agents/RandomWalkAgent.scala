package surf.abm.agents


import com.vividsolutions.jts.geom.Coordinate
import sim.engine.SimState
import surf.abm.environment.Building
import surf.abm.{SurfABM, SurfGeometry}

import scala.math._

/**
  * This basic Agent type just does a random walk around the environment, not delibrerately following the road
  * network.
  *
  * @param state A pointer to the main model instance (<code>SurfABM</code>)
  * @param home The agent's home (starting location). Usually a <code>Building</code>
  *             where (e.g.) they live, but not necessarily. The agent's initial
  *             location is set to be <code>home</code>
  */
@SerialVersionUID(1L)
class RandomWalkAgent(state:SurfABM, home:SurfGeometry[Building]) extends Agent(state,home) with Serializable {


  /**
    * In this basic implementation, the agents just do a random walk
    *
    * @param state
    */
  override def step(state: SimState): Unit = {

    //LOG.info("Stepping an agent: "+this.toString())

    // Do a random walk
    val current : Coordinate = this.location.getGeometry.getCoordinate
    def r(n:Double) : Double =  { // Randomize the input number by +- the moveRate
      n + ( ( this.state.random.nextDouble() * moveRate * 2 ) - moveRate )
    }
    val newCoord = new Coordinate( r(current.x), r(current.y) )
    // Check that the new position is correct
    assert( {
      // Calculate the Euclidean distance moved, and check it is less than the
      // maximum Eeuclidean distance given the moveRate
      val dist = sqrt( pow(current.x-newCoord.x,2) + pow(current.y-newCoord.y,2) )
      dist <= sqrt(pow(moveRate,2) + pow(moveRate,2) )
    }, s"Agent has moved too far.\n\t" +
        s"Move rate: ${moveRate}, dist: ${sqrt( pow(current.x-newCoord.x,2) + pow(current.y-newCoord.y,2) )},\n\t" +
        s"Coordinates: ${current} - ${newCoord}."
    )
    this.moveToCoordinate(newCoord)

    //println(SurfABM.agentGeoms.getGeometries().get(0).asInstanceOf[SurfGeometry].theObject)


  }

  override def toString() = "RandomWalkAgent %s".format(this.id())

}

