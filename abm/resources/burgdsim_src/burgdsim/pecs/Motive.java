package burgdsim.pecs;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

import burgdsim.burglars.Burglar;
import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;


/**
 * Superclass for all Motives. Uses a List of Actions which must be completed
 * in order for the agent to satisfy this motive (last in list must be completed first).
 * @author nick
 *
 */
public abstract class Motive implements ChangeActionEventListener {

	// For efficiency only re-calculate intensity if the iteration has changed
	private double intensity;	// The intensity of this motive
	private double time;		// The time that the intensity was calculted
	
	protected List<Action> actionList; // The actions which must be performed, in order, to satisfy this motive
	protected List<Action> allActions; // All actions which this motive might lead to (useful for debugging/analysis)
	protected StateVariable stateVariable; // The state variable which influences this motive.
	protected double factor = 0.5;	// Some agents feel motives more/less strongly. Default 0.5 (normal behaviour)
	
	
	public Motive(StateVariable stateVariable) {
		this.stateVariable = stateVariable;
		this.initialise();
	}
	
	/**
	 * Becasue Motives are consistent (they are only created once) for each burglar this function can be called each
	 * time they need to be reinitialised. See Burglar.step() for an example when the actionGuidingMotive changes
	 * so the new one needs to be reinitialised.
	 * <p>
	 * XXXX - need to check that the aciton should be reinitialised. E.g. a professional burglar could be building
	 * up a list of appropriate burglary targets over a few consecutive days, don't want to erase this list!
	 */
	public void initialise() {
		if (this.actionList==null)
			this.actionList = new ArrayList<Action>();
		else
			this.actionList.clear();
		this.buildActionList();			// Have the subclass generate an actionList
		if (this.actionList.size() < 1) 
			Outputter.errorln("Motive("+this.getClass().getName()+"): action list has not been populated.");
		this.addListenersToActions();	// Go through each Action and add this Motive as a listener		
	}


	/**
	 * Calculate the intensity of this motive by calling calcIntensity (which must be overridden by subclasses).
	 * If the iteration number hasn't changed then re-return a value calculated previously if possible 
	 * (for efficiency)
	 * @return the current intensity of this motive.
	 */
	public double getIntensity() {
		double iter = GlobalVars.getIteration();
		if (this.time!=iter) { // Iteration has changed so re-calculate intensity
			this.intensity = this.calcIntensity();
			this.time =iter;	
		}
		return this.intensity;	
	}

	protected abstract double calcIntensity();

	/**
	 * Return the current action which will help the agent to satisfy this motive. Actions should be
	 * stored in the actionList and removed as they are completed so that the final action (at index 0)
	 * is the one which will actually satisfy the motive.
	 * 
	 * @return the last Action in the list
	 */
	public final Action getCurrentAction() {
		this.removeCompleteActions();
		/* In some cases all the actions required to satisfy this motive will have been completed, but
		 * the motive will still be the strongest (i.e. a drug addict takes drugs but wants more). If this
		 * happens then the actionList here will be empty as it wont have been re-initialised in Burglar.step()
		 * check this and reinitialise if necessary. */
		if (this.actionList.size()==0){
			this.initialise();
		}
		return this.actionList.get(actionList.size()-1);
	}
	
	/**
	 * Run through the actionList, removing actions which have been completed until we find one which
	 * has been completed. (Start at end of list and work back).
	 */
	protected void removeCompleteActions() {
		for (int i=this.actionList.size()-1; i>=0; i--) {
			if (this.actionList.get(i).isComplete()) {
				this.actionList.remove(i);
			}
			else break;
		}
	}	
	
	/**
	 * Add all the actions which must be completed to this Motive's actionList. This should be done
	 * when a new Motive is created. As actions are completed they should be removed from the list.
	 * <p>
	 * This is also called in Burglar.step() if the burglar's action-guiding
	 * motive changes, otherwise the motive will not re-build it's actionList.
	 * <p>
	 * The function should also fill the allActions list when it is created.
	 */
	protected abstract void buildActionList();
	
	/**
	 * Return all the actions which this motive *might* lead to.
	 * @return
	 */
	public List<Action> getAllActions() {
		if (this.allActions==null || this.allActions.size() == 0) {
			Outputter.errorln("Motive.getAllActions() error: allActions list is null or of size 0, this " +
				"motive ("+this.getClass().getName()+") probably didn't fill the list in its constructor.");
		}
		return this.allActions;
	}
	/**
	 * The state variable which is influencing this motive.
	 * @return
	 */
	public StateVariable getStateVariable() {
		return this.stateVariable;
	}
	
	/**
	 * Convenience function retrieves the burglar from this motive's state variable
	 * @return the burglar who has this motive
	 */
	public Burglar getBurglar() {
		return this.stateVariable.getBurglar();
	}
	
	/**
	 * The 'factor' variable allows some agents to feel motives more strongly. E.g. an agent without a drug
	 * addiction would have a DrugsMotive factor of 0. 
	 * @param factor
	 */
	public void setFactor(double factor) {
		this.factor = factor;
	}
	
	/**
	 * Iterates through the actionList and adds this Motive as a listener for ChangeActionEvents which
	 * could be fired by each Action.
	 */
	private void addListenersToActions() {
		for (Action action:this.actionList)
			action.addChangeActionEventListener(this);
	}
	
	/**
	 * Listen for ChangeActionEvents. These can be thrown by external objects if this Motive will need to
	 * re-build its actionList for whatever reason. E.g., if an agent runs out of money whilst they are socialising,
	 * this Motive will need to rebuild its actionList so the agent can make more money.
	 */
	public void ChangeActionEventOccurred(ChangeActionEvent e) {
//		Outputter.describeln("Received a change action event from "+e.getSource().getClass().getName()+" with message:\n" +
//				"\t*"+e.getMessage());
		this.initialise();
	}

	public String toString() {
		return this.getClass().getSimpleName()+" value: "+this.intensity;
	}
	
	public String getName() {
		return this.getClass().getSimpleName();
	}
}

interface ChangeActionEventListener extends EventListener {

	public void ChangeActionEventOccurred(ChangeActionEvent e);
	
}
