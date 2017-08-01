package surf.abm.agents

import com.vividsolutions.jts.geom.Coordinate
import org.apache.log4j.Logger
import sim.engine.Steppable
import sim.util.geo.{MasonGeometry, PointMoveTo}
import surf.abm.environment.Building
import surf.abm.main.{SurfABM, SurfGeometry}

/**
  * Base class for all agents
  * Created by geonsm on 08/04/2016.
  */
@SerialVersionUID(1L)
abstract class Agent (val state:SurfABM, val home:SurfGeometry[Building]) extends Steppable with Serializable {

  // A unique id for each agent with a public accessor.
  private val _id : Int = Agent.uniqueID()
  def id() : Int = this._id

  // The location where the agent currently is. Begins at 'home'. It is protected, with a public accessor.
  // Important to create a new MasonGeometry, not use the home geometry. Otherwise bad things happen!
  protected var _location: SurfGeometry[_ <: Any] =  SurfGeometry(new MasonGeometry(this.home.getGeometry().getCentroid()), null)
  def location() = this._location // accessor to location
  //protected def location_=(g:MasonGeometry) { _location = g } // protected mutator

  /**
    * The default (aka 'base') move rate. For the Agent super class, this is set to the
    * 'BaseMoveRate' parameter. See [[surf.abm.agents.Agent._baseMoveRate]] for details. Be careful about changing
    * this value as sub-classes will probably use it to work out what walking pace is, and hence other
    * relative transport speeds. See an example in [[surf.abm.agents.abbf.ABBFAgent]].
    */
  protected def moveRate() : Double = Agent._baseMoveRate

  // Convenience for moving a point. Don't want to create this object each iteration.
  // val pmt : PointMoveTo = new PointMoveTo()

  /**
    * Move the agent to the given coordinate
    *
    * @param c The Coordinate to move to
    * @return Unit
    */
  def moveToCoordinate(c : Coordinate) : Unit = {
    //this.pmt.setCoordinate(c)
    //this.location.getGeometry().apply(this.pmt)
    //this.location.geometry.geometryChanged()
    val p = new PointMoveTo()
    p.setCoordinate(c)
    this.location.getGeometry().apply(p)
    this.location.geometry.geometryChanged()

  }

  override def toString() = s"Agent [${this.id()}]"

}

@SerialVersionUID(1L)
object Agent extends Serializable {

  // Initialise the logger. NOTE: will have one logger per Agent
  //val LOG: Logger = Logger.getLogger(this.getClass);
  def LOG() = AgentLog

  /**
    * The basic (walking) rate that agents move at. It is by the 'BaseMoveRate' in the surf-abm.conf file.
    * This is accessed by the [[surf.abm.agents.Agent.moveRate()]] function which
    * can be overidden by agents who are able to move at different rates for whatever reason.
    * */
  private val _baseMoveRate = SurfABM.conf.getDouble(SurfABM.ModelConfig+".BaseMoveRate")

  /** A unique ID that can be given to each agent */
  private var _uniqueID : Int= 0
  private def uniqueID() : Int = {
    _uniqueID+=1
    _uniqueID
  }

}

/*
 * A logger specifically for agents that writes some extra information before the messages (e.g. agent number, simulation time, etc.)
 */
object AgentLog {

  private val _LOG: Logger = Logger.getLogger(Agent.getClass);

  def msg(agent :Agent ): String = {
    s"[${agent.state.schedule.getTime().toInt}]${agent.toString()}:"
  }

  def warn(agent: Agent, message: scala.Any): Unit = {
    _LOG.warn(s"${msg(agent)}${message}")
  }

  def debug(agent: Agent, message: scala.Any): Unit = {
    _LOG.debug(msg(agent)+message)
  }

  def info(agent: Agent, message: scala.Any): Unit = {
    _LOG.info(s"${msg(agent)}${message}")
  }

}