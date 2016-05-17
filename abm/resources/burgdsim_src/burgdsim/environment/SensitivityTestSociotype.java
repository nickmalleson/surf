package burgdsim.environment;

import burgdsim.main.ContextCreator;
import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;

/** Sociotype used in Grid senstivity tests */
public class SensitivityTestSociotype implements Sociotype, Cacheable {
	
	private int sociotypeNumber = -1;
	private static int maxSociotypeNumber = 0;
	private int id = -1;
	
	

	public SensitivityTestSociotype() {
		super();
		if (!GlobalVars.ENVIRONMENT_TYPE.equals(GlobalVars.ENVIRONMENT_TYPES.GRID)) {
			Outputter.errorln("SensitivityTestSociotype() error: this class has only been designed to work on the " +
					"grid environment sensitivity tests.");
		}
	}

	/** Sociotypes are just given ascending numbers, so this returns how similar the numbers are on a scale of 0 - 1 */
	public <T extends Sociotype> double compare(T sociotype) {
		if (ContextCreator.isTrue(GlobalVars.MAKE_SENSITIVITY_TEST_SOCIOTYPES_SAME) ){
			return 1.0;
		}
		else {
			int maxDifference = maxSociotypeNumber - 1; // (because id's in grid environment start at 1, not 0).
			SensitivityTestSociotype s = (SensitivityTestSociotype) sociotype; 
			return  1 - ( (double) Math.abs(s.sociotypeNumber - this.sociotypeNumber) / (double) maxDifference ) ;
		}
	}

	public double getAttractiveness() {
		return 0.5;
	}

	public double getCE() {
		return 0.5;
	}

	public String getDescription() {
		return "TestSociotype "+this.id;
	}

	public int getId() {
		return this.id ;
	}
	
	public void setId(int id) {
		this.id = id;
	}

	public double getOccupancy(double time) throws Exception {
		return 0.5;
	}

	public String getParameterValuesString() {
		return null;
	}

	/* ID will have been set when Sociotype was created, this ensures that the type and ID are the same (and also
	 * if many Community objects created that represent same community, i.e. grid environment, they all have same
	 * sociotype number). */
	public void init() throws Exception {
		this.sociotypeNumber = this.id;
		if (this.sociotypeNumber > maxSociotypeNumber) {
			maxSociotypeNumber = this.sociotypeNumber;
		}

	}

	public void initDefaultSociotype() {
		Outputter.errorln("SensitivityTestSociotype.initDefaultSociotype() not implemented this method.");

	}

	public void clearCaches() {
		maxSociotypeNumber = 0;

	}

}
