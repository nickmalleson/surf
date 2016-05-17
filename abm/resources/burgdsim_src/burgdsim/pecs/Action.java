package burgdsim.pecs;
import javax.swing.event.EventListenerList;

import burgdsim.burglars.Burglar;

public abstract class Action {

	protected boolean complete = false; // True when this Action has been completed
	protected String description;
	protected Motive motive;	// The motive which will perform this action
	
	// Whether or not this action can put the agent to 'sleep' (this can occur if the action isn't
	// going to change the agent's behaviour for some time, in this case the agent's step() method
	// can basically be skipped over
	protected boolean sleepable = false;
	
	// Listeners - sometimes actions might need to send changeActionEvents if their motive's actionList
	// needs to be rebuilt but the motive hasn't changed
	protected EventListenerList listeners;
	
	/**
	 * Create a new Action with a satisfaction weight of 1 (not all actions require a satisfaction weight.
	 * Travelling, for example, won't help to directly satisfy a motive)
	 * @param burglar The burglar who will perform this Action.
	 */
	public Action(Motive motive) {
		this.motive = motive;
		this.listeners = new EventListenerList();
	}
	
	/**
	 * Control the burglar to do this Action.
	 * @return true if the action was completed immediately (a 'temporary action), false otherwise.
	 * This is important because an agent might be able to run through a number of actions in one turn
	 * without actually doing anything.<br>
	 * E.g. an agent who is at home might have to go through the following before they are able to start a
	 * Sleeping action: 'check nothing important to do' -> 'travel home' -> 'go to sleep'. Without using
	 * the return type, this action would start after 3 iterations but should start immediately.
	 * @throws Exception If there is a problem(!)
	 */
	public abstract boolean performAction() throws Exception ;

	/**
	 * Convenience function retrieves the burglar from StateVariable associated with this Action's Motive.
	 * @return the burglar who will be controlled by this Action.
	 */
	protected Burglar getBurglar() {
		return this.motive.getStateVariable().getBurglar();
	}
	/**
	 * Convenience function retrieves the StateVariable associated with this Action's Motive.
	 * @return the StateVariable which is influencing the Motive which has created this Action.
	 */
	protected StateVariable getStateVariable() {
		return this.motive.getStateVariable();
	}
	
	/**
	 * Whether or not this action has been completed. The completed varible should be set manually when the
	 * action finishes.
	 * @return true if completed, false otherwise.
	 */
	public boolean isComplete() {
		return this.complete;
	}
	
    /** 
     * Allows Motives to register for ChangeActionEvents.
     */
    public void addChangeActionEventListener(ChangeActionEventListener listener) {
        listeners.add(ChangeActionEventListener.class, listener);
    }

//    /** 
//     * Allows Motives to unregister for ChangeActionEvents.
//     */
//    public void removeChangeActionEventListener(ChangeActionEventListener listener) {
//        listeners.remove(ChangeActionEventListener.class, listener);
//    }
    
    /** 
     * Method can be used to fire ChangeAction events
     */
    protected void fireChangeActionEvent(ChangeActionEvent evt) {
        Object[] listeners = this.listeners.getListenerList();
        // Each listener occupies two elements - the first is the listener class and the second is the listener instance
        for (int i=0; i<listeners.length; i+=2) {
            if (listeners[i]==ChangeActionEventListener.class) {
                ((ChangeActionEventListener)listeners[i+1]).ChangeActionEventOccurred(evt);
            }
        }
    }
    
    public boolean isSleepable() {
    	return this.sleepable;
    }
	
	/**
	 * Return the description of this action
	 * @return the description
	 */
	public String toString() {
		if (this.description == null)
			return this.getClass().getSimpleName();
		else
			return this.description;

	}
		
	public void setDescription(String description) {
		this.description = description;
	}

}
