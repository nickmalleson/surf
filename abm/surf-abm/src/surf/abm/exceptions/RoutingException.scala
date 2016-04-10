package surf.abm.exceptions

/**
  * Thrown if there is a problem with agent routing.
  * @author Nick Malleson
  */
case class RoutingException(message: String) extends Exception(message)

