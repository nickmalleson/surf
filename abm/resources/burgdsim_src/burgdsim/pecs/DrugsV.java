package burgdsim.pecs;

import burgdsim.burglars.Burglar;
import burgdsim.burglary.Burglary;
import burgdsim.main.GlobalVars;

/**
 * A variable which represents drug addiction.
 * @author Nick Malleson
 */
public class DrugsV extends StateVariable {

	public DrugsV(Burglar burglar, double value) {
		super(burglar, value);
	}

	@Override
	protected void setMotive() {
		this.motive = new DrugsM(this);
	}

}

class DrugsM extends Motive {
	
	public DrugsM(StateVariable s) {
		super(s);
	}

	@Override
	public void buildActionList() {
		/* BUILD UP THE ACTIONLIST FROM THE BOTTOM UP */
		this.actionList.add(new BuyDrugsA(this));
		this.actionList.add(new TravelA(this,
				this.getBurglar().getDrugDealer().getCoords(), 
				this.getBurglar().getDrugDealer(), "Travelling to Drug Dealer"));
		if (this.stateVariable.getBurglar().getWealth()<GlobalVars.COST_DRUGS) { // Can't afford to buy drugs
			if (this.stateVariable.getBurglar().canWork()) {
				this.actionList.add(new WorkA(this, "Working for money for drugs", GlobalVars.COST_DRUGS));
				this.actionList.add(new TravelA(this, 
					this.getBurglar().getWork().getCoords(), this.getBurglar().getWork(), "Travelling to Work"));
			}
			else {
				// Unlike all other actions burgling is a unique action which will handle travelling to a victim etc.
				Burglary b = this.getBurglar().getSpecificAction(Burglary.class, this);
				if (b!=null) {  // Burglar provides own Burglary action
					this.actionList.add(b);
				}
				else {  // Use the default Burglary action
					this.actionList.add(new Burglary(this));
				}
			}
		}
	} // setActionList()

	/**
	 * Intensity based purely on level of addiction (the "factor") and level of drugs in agent's system.
	 * Calculated by: DrugsFactor * (1/DrugsLevel)
	 */
	@Override
	protected double calcIntensity() {
		return this.factor * (1/this.stateVariable.getValue());
	}
}
class BuyDrugsA extends Action {

	public BuyDrugsA(Motive m) {
		super(m);
		this.description = "Buying drugs";
	}

	@Override
	public boolean performAction() {
		this.getBurglar().changeWealth(-GlobalVars.COST_DRUGS);
		this.getStateVariable().changeValue(GlobalVars.DRUGS_GAIN);
		this.complete = true;
		return false; // Not a temporary action, takes a whole iteration.
	}
	
}