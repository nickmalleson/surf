package surf.abm.surfutil

import java.io.FileNotFoundException

import org.apache.log4j.Logger
import surf.abm.SurfABM
import surf.abm.tests.UnitSpec

/**
  * Created by nick on 16/04/2016.
  */
class CheckConnectedNetworkSpec extends UnitSpec {

  // Tests consist of a *subject*, a *verb* (either 'should', 'must', or 'can') and the rest of the sentence

  "A CheckConnectedNetwork" should "throw an error if there are no command line arguments" in {
    an [Exception] should be thrownBy {
      CheckConnectedNetwork.main(Array[String]())
    }
  }

  it should "throw an error if there are more than one line arguments" in {
    an [Exception] should be thrownBy {
      CheckConnectedNetwork.main(Array[String]("arg1", "arg2"))
    }
  }

  it should "throw an error if the roads file cannot be found" in {
    a [FileNotFoundException] should be thrownBy {
      CheckConnectedNetwork.main(Array[String]("not_a_roads_file"))
    }
  }



}

object CheckConnectedNetworkSpec {
  private val LOG: Logger = Logger.getLogger(this.getClass);
}