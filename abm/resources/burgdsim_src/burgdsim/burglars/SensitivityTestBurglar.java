package burgdsim.burglars;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import burgdsim.burglary.Burglary;
import burgdsim.burglary.ITargetChooser;
import burgdsim.burglary.IVictimChooser;
import burgdsim.burglary.TargetChooser;
import burgdsim.burglary.VictimChooser;
import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;
import burgdsim.pecs.Action;
import burgdsim.pecs.Motive;
import burgdsim.pecs.StateVariable;

/**
 * Burglar used in null environment sensitivity tests. Will set VictimChooser or TargetChooser weights as
 * defined in the appropriate constructor. 
 * @author Nick Malleson
 *
 */
public class SensitivityTestBurglar extends Burglar {

	IVictimChooser vc = null;
	ITargetChooser tc = null;
	
	public SensitivityTestBurglar(List<StateVariable> stateVariables) {
		super(stateVariables);
	}
	
	/**
	 * Create a burglar and also set the burglar behaviour weight (in VictimChooser or TargetChooser)
	 * appropriately. 
	 * @param stateVariables
	 * @param behaviourWeight
	 */
	public SensitivityTestBurglar(List<StateVariable> stateVariables, double behaviourWeight, int id) {
		super(stateVariables);
		
		if (GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.BURGLAR_SENSITIVITY_TEST_PARAMETER.equals("")) {
			Outputter.errorln("SensitivityTestBurglar constructor has ben called but there is no " +
					"BURGLAR_SENSITIVITY_TEST_PARAMETER defined, don't know how to set Victim/Target chooser" +
					"weights.");
			return;
		}
		this.setTheID(id);
		String testParam = GlobalVars.BURGLARY_SENSITIVITY_TEST_PARAMETERS.BURGLAR_SENSITIVITY_TEST_PARAMETER;
		// Iterate over all VictimChooser weights, looking for one that matches the BURGLAR_SENSITIVITY_TEST_PARAMETER
		// string. If nothing then try TargetChooser weights, otherwise fail.
		for (GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER s:GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER.values()) {
			if (testParam.equals(s.toString())) {
				Map<GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER, Double> weightMap = 
					new Hashtable<GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER, Double>();
				weightMap.put(s, behaviourWeight);
				this.vc = new VictimChooser(this, weightMap);
				this.tc = new TargetChooser(this); // No weight defined for target chooser
				Outputter.debugln("SensitivityTestBurglar() creating a new burglar with VictimChooser weight "+
						s.toString()+" set to "+behaviourWeight, Outputter.DEBUG_TYPES.INIT);
				return;
			}
		}
		// Nothing matched VictimChooser weights, try TargetChooser
		for (GlobalVars.BURGLARY_WEIGHTS.TARGET_CHOOSER s:GlobalVars.BURGLARY_WEIGHTS.TARGET_CHOOSER.values()) {
			if (testParam.equals(s.toString())) {
				Map<GlobalVars.BURGLARY_WEIGHTS.TARGET_CHOOSER, Double> weightMap = 
					new Hashtable<GlobalVars.BURGLARY_WEIGHTS.TARGET_CHOOSER, Double>();
				weightMap.put(s, behaviourWeight);
				this.tc = new TargetChooser(this, weightMap);
				this.vc = new VictimChooser(this); // No weight defined for victim chooser
				Outputter.debugln("SensitivityTestBurglar() creating a new burglar with TargetChooser weight "+
						s.toString()+" set to "+behaviourWeight, Outputter.DEBUG_TYPES.INIT);
				return;
			}
		}
		// If here then failed
		String debug = "SensitivityTestBurglar() could not find a matching behaviour weight in VictimChooser " +
				"or TargetChooser for the string "+testParam+". Possible weights in VictimChooser are:\n";
		for (GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER s:GlobalVars.BURGLARY_WEIGHTS.VICTIM_CHOOSER.values())
			debug+="\t"+s+"\n";
		debug +="and in TargetChooser:\n";
		for (GlobalVars.BURGLARY_WEIGHTS.TARGET_CHOOSER s:GlobalVars.BURGLARY_WEIGHTS.TARGET_CHOOSER.values())
			debug+="\t"+s+"\n";
		Outputter.errorln(debug);
	}
	
 

	@Override
	public boolean canWork() {
		return false;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Action> T getSpecificAction(Class<T> actionClass, Motive motive) {
		T obj = null;
		if (actionClass.isAssignableFrom(Burglary.class)) {
			Burglary b = new Burglary(motive);
			b.setVictimChooser(this.vc);
			b.setTargetChooser(this.tc);
//			b.setSearchAlg(new SearchAlg(this));
			obj = (T) b;
		}
		return obj;
	}
	
}