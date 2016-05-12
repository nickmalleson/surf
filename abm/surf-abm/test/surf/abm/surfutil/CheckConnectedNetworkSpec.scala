package surf.abm.surfutil

import java.io.IOException

import com.vividsolutions.jts.planargraph.Node
import org.apache.log4j.Logger
import sim.field.geo.GeomVectorField
import surf.abm.tests.UnitSpec

/**
  * Created by nick on 16/04/2016.
  */
class CheckConnectedNetworkSpec extends UnitSpec {

  // Tests consist of a *subject*, a *verb* (either 'should', 'must', or 'can') and the rest of the sentence

  "A CheckConnectedNetwork".should("throw an error if there are no command line arguments").in ( {

    an [Exception] should be thrownBy {
      CheckConnectedNetwork.main(Array[String]())
    }
  } )

  it should "throw an error if there are more than one line arguments" in {
    an [Exception] should be thrownBy {
      CheckConnectedNetwork.main(Array[String]("arg1", "arg2"))
    }
  }

  it should "throw an error if the roads file cannot be found" in {
    an [IOException] should be thrownBy {
      CheckConnectedNetwork.main(Array[String]("not_a_roads_file"))
    }
  }

  it should "return a GeomVectorField from a valid file" in {
    val field = CheckConnectedNetwork.readRoadsFile("./data/leeds-easel/roads_disconnected.shp")
    field.getClass() should be (new GeomVectorField(1,1).getClass())


  }
  it should "read more than 8614 roads from the leeds_disconnected file" in {
    val field = CheckConnectedNetwork.readRoadsFile("./data/leeds-easel/roads_disconnected.shp")
    field.getGeometries().size() should be (8614)
  }

  it should "return a network with 8614 edges" in {
    val network =CheckConnectedNetwork.createNetwork(
      CheckConnectedNetwork.readRoadsFile("./data/leeds-easel/roads_disconnected.shp")
    )
    network.getEdges().size() should be (8614)

  }

  it should "return a network with 6749 nodes" in {
    val network =CheckConnectedNetwork.createNetwork(
      CheckConnectedNetwork.readRoadsFile("./data/leeds-easel/roads_disconnected.shp")
    )
    network.getNodes().size() should be (6749)

  }

  ignore should "return X nodes that are connected in the disconnected road file" in {

    val network = CheckConnectedNetwork.createNetwork(
      CheckConnectedNetwork.readRoadsFile("./data/leeds-easel/roads_disconnected.shp")
    )
    val root = network.getNodes.iterator().next().asInstanceOf[Node]
    val connected = CheckConnectedNetwork.traverse(root)
    connected.size should be (3)

  }


  ignore should "return no disconnected roads in the connected roads file" in {
    val network = CheckConnectedNetwork.createNetwork(
      CheckConnectedNetwork.readRoadsFile("./data/leeds-easel/roads_disconnected.shp")
    )
    val root = network.getNodes.iterator().next().asInstanceOf[Node]
    val connected = CheckConnectedNetwork.traverse(root)
    connected.size should be (0)
  }


}

object CheckConnectedNetworkSpec {
  private val LOG: Logger = Logger.getLogger(this.getClass);
}