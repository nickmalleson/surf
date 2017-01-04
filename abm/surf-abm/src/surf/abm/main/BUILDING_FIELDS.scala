package surf.abm.main

/**
  * Place to record any String values that the model relies on, e.g. data column
  * names. Storing them here makes it easier to keep track and change later.
  *
  * @author Nick Malleson
  */
object BUILDING_FIELDS extends Enumeration {

  type FIELDS = Value

  // Fields for GIS data files.

  // These are default (used for all scenarios)
  val BUILDINGS_ID = Value("ID")
  val BUILDINGS_TOID = Value("TOID")

  // Others used in some of the other scenarios. When reading the buildings data it doesn't matter if the
  // ShapefileReader can't find the attributes
  val BUILDINGS_OA = Value("OA")
}
