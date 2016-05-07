package surf.abm.agents.abbf

import org.apache.log4j.Logger
import org.scalatest.GivenWhenThen
import surf.abm.surfutil.CheckConnectedNetwork
import surf.abm.tests.UnitSpec

/**
  * Created by nick on 07/05/2016.
  */
class ActivitySpec extends UnitSpec with GivenWhenThen {

    // Tests consist of a *subject*, a *verb* (either 'should', 'must', or 'can') and the rest of the sentence

    "The  checkTimes function" should "throw an illegal argument exception if any of the times are not in the range [0-24)" in {
      val i = 1.0 // the intensity
      val t = 1.0 // the time

      Given("A profile with a time of 24.0")
      val profile1: Array[(Double,Double)] = Array( (t,i) , (24.0, i) , (t,i), (t,i)  )

      Then("An Illegal argument should be thrown")
      an [IllegalArgumentException] should be thrownBy {
        TimeProfile.checkTimes(profile1)
      }

      And("")
      Given("A profile with a time of 24.01")
      val profile2: Array[(Double,Double)] = Array( (t,i) , (24.01,i) , (t,i), (t,i)  )

      Then("An Illegal argument should be thrown")
      an [IllegalArgumentException] should be thrownBy {
        TimeProfile.checkTimes(profile2)
      }

      And("")
      Given("A profile with a time of -0.01")
      val profile3: Array[(Double,Double)] = Array( (t,i) , (-0.01,i) , (t,i), (t,i)  )

      Then("An Illegal argument should be thrown")
      an [IllegalArgumentException] should be thrownBy {
        TimeProfile.checkTimes(profile3)
      }

  }

  it should "throw an illegal argument exception if the input times are not in ascending order" in {

    val i = 1.0 // the intensity

    val p = Array( (0d,i) , (10d, i) , (9d,i), (15d,i)  )
    an [IllegalArgumentException] should be thrownBy {
      TimeProfile.checkTimes(p)
    }
  }

  it should "return Unit if the times are in ascending order and in the range [0-24)" in {

    val i = 1.0 // the intensity

    val p2 = Array( (0d,i) , (10d, i) , (13d,i), (15d,i)  )
    TimeProfile.checkTimes(p2)
  }


  "The calcIntensity() function" should "throw an error if the input time is not in the range [0-24]" in {
    val tp = TimeProfile(Array((0d, 0d), (10d, 8.0), (13d, 14.0), (15d, 10.0), (23d, 5.0)))
    Given("An imput less than 0")
    Then("An illegal argument should be thrown")
    an [IllegalArgumentException] should be thrownBy {
      tp.calcIntensity(-0.01)
    }
    And("")
    Given("An input of 24")
    an [IllegalArgumentException] should be thrownBy {
      tp.calcIntensity(24.0)
    }
  }

  it should "return an intensity of XX" in {

    val tp = TimeProfile(Array((0d, 0d), (10d, 8.0), (13d, 14.0), (15d, 10.0), (23d, 5.0)))
    tp.calcIntensity(9.5) should equal(XX)

  }


}


object ActivitySpec {
  private val LOG: Logger = Logger.getLogger(this.getClass);
}
