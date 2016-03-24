package surf.abm.tests

import org.scalatest._

/**
  * The base class for Surf tests.
  * Created by nick on 24/03/2016.
  */
abstract class UnitSpec extends FlatSpec with Matchers with
  OptionValues with Inside with Inspectors