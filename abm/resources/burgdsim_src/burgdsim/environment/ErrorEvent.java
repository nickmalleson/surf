package burgdsim.environment;

import java.util.EventObject;

import burgdsim.environment.buildings.Building;

/**
 * ErrorEvents can be thrown whenever there is an error. At the moment the only class to make use of these
 * is TestEnvironment. These actions are thrown by GISRoute when it has problems creating routes so that
 * TestEnvironment knows there is something wrong with the environment.
 * <p>
 * The ERROR_TYPE is used so that TestEnvironment knows what the problem was.
 * @author nick
 *
 */
public class ErrorEvent extends EventObject {

	private static final long serialVersionUID = 1L;
	
	private Building building; // There might be a problem with a building.
	
	private String message;

	public ErrorEvent(Object source, String description) {
		super(source);
		this.message = description;
	}
	
	public ErrorEvent(Object source, String description, Building building) {
		super(source);
		this.message = description;
		this.building = building;
	}
	
	public String getMessage() {
		return this.message;
	}
	
	public Building getBuilding() {
		return this.building;
	}

}
