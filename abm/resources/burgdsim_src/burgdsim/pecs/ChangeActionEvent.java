package burgdsim.pecs;
import java.util.EventObject;

/**
 * A ChangeAction event is thrown when a Motive might need to generate a new actionList. For example, a
 * Burglar who is working might run out of temporary work so will need to generate an actionList which allows
 * them to burgle. The event is caught by the Motive class and generates a new actionList.
 * Throwing these events isn't necessary if the Burglar's action guiding motive changes
 * because a new actionList is generated automatically in these cases.
 *   
 * @author Nick Malleson
 *
 */

public class ChangeActionEvent extends EventObject {

	protected String message;	// Description of this event

	public ChangeActionEvent(Object source, String description) {
		super(source);
		this.message = description;
	}	
	
	public String getMessage() {
		return this.message;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
