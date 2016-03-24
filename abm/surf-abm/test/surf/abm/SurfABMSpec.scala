package surf.abm

import surf.abm.tests.UnitSpec
import org.scalatest._

/**
  * Test class for the SurfABM class
  * Created by nick on 24/03/2016.
  */
class SurfABMSpec extends UnitSpec  {

  // Tests consist of a *subject*, a *verb* (either 'should', 'must', or 'can') and the rest of the sentence
  "A SurfABM" should "load without errors" in {
    SurfABM.main(new Array[String](0)) // pass main a new, empty string array (no command-line arguments)
    assert(true)
  }

}
