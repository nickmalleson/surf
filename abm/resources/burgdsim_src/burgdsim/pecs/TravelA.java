package burgdsim.pecs;

import burgdsim.environment.Coord;
import burgdsim.environment.EnvironmentFactory;
import burgdsim.environment.Route;
import burgdsim.environment.buildings.Building;
import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;

/**
 * Action to move agents from their current location towards a destination.
 * @author Nick Malleson
 */
public class TravelA extends Action {
	
	private Route route;
	private Coord destination;
	private Building destinationBuilding;

	/**
	 * Create a Travel object which will move the agent towards a specified destination.
	 * @param burglar
	 * @param destination
	 * @param description A meaningful description, i.e. "travelling to work"
	 */
	public TravelA(Motive motive, Coord destination, Building destinationBuilding, String description) {
		super(motive);
		this.description = description;
		this.destination = destination;
		this.destinationBuilding = destinationBuilding;
		Outputter.debugln("TravelA: new action created for '"+this.getBurglar().toString()+"' by motive '"+
				this.motive.toString()+"' to get from "+
				GlobalVars.BURGLAR_ENVIRONMENT.getCoords(getBurglar()).toString()+
				" to "+destination.toString()+"("+(destinationBuilding==null ?"":destinationBuilding.toString())+")",
				Outputter.DEBUG_TYPES.ROUTING);
		// Note: no Route object is created. This is an expensive process so we only want to create
		// a new Route object when the agent needs to travel in case this Travel action is never performed.
	}

	/**
	 * Move the agent towards their destination.
	 * @throws Exception 
	 */
	@Override
	public boolean performAction() throws Exception {

		Outputter.debug("TravelA: Performing action: ",Outputter.DEBUG_TYPES.ROUTING);
		// Check the agent has a route
		if ( this.route == null) {
			this.route = EnvironmentFactory.createRoute(this.getBurglar(), destination, destinationBuilding, null);
			Outputter.debug("..created new route object..",Outputter.DEBUG_TYPES.ROUTING);
		}

		// If not at destination, travel towards it.
		if (route.atDestination()) {
			Outputter.debug("..at destination", Outputter.DEBUG_TYPES.ROUTING);
			this.complete = true;
			this.getBurglar().setLocked(false, this); // Unlock (just in case the burglar was locked previously, although I don't think this will ever happen)
			return true;
		}
		else {
			Outputter.debugln("...travelling:.", Outputter.DEBUG_TYPES.ROUTING);
			route.travel();
			// If the agent is on a transport route lock them
			if (route.isOnTransportRoute()) {
				this.getBurglar().setLocked(true, this);
				// Burglar wants to change Action, tell the Route to unlock the agent asap (e.g. when 
				// they get to the next bus stop).
				route.setAwaitingUnlock(this.getBurglar().isAwaitingUnlock());
			}// if onTransportRoute
			else
				this.getBurglar().setLocked(false, this);
			
			return false;
		}

	} // performAction()
}
