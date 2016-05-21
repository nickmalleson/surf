package surf.abm.main

/**
  * Place to record any String values that the model relies on, e.g. data column
  * names. Storing them here makes it easier to keep track and change later.
  *
  * @author Nick Malleson
  */
object BUILDING_FIELDS extends Enumeration {

  type FIELDS = Value

  // Fields for GIS data files
  val BUILDINGS_ID = Value("ID")
  val BUILDINGS_TOID = Value("TOID")

}
