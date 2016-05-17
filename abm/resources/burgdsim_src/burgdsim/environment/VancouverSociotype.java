package burgdsim.environment;

import burgdsim.main.Functions;
import burgdsim.main.GlobalVars;
import burgdsim.main.Outputter;

/**
 * A sociotype used to represent Vancouver. Unfortunately a classification similar to OAC isn't available,
 * so, instead, similar values from the Canadian census are used to calculate attractiveness and occupancy.
 * Also, a measure of deprivation is used for collective efficacy. Although the individual variables which
 * make up collective efficacy and occupancy aren't required by the model, they're stored directly here so
 * that the Euclidian distance between variable values can be used in the compare() function.
 * <p>
 * Note: because no explicit community classification is used, average derived values play no part in this
 * Sociotype, the variables are always calculated for each individual area (rather than using the average
 * values for that Sociotype type). Also, it is assumed that all variables from the input data are
 * normalised, so no normalisation takes place here. 
 * @author Nick Malleson
 *
 */
public class VancouverSociotype implements Sociotype {

	private double ownHouse;		// The proportion of people who own their own home
	private double ethnicity;		// The index of heterogeneity (a measure of ethnic heterogeneity)
	private double vandix;			// A measure of deprivation
	private double ce;				// Collective efficacy (combination of ownHouse, ethnicity and vandix)
	
	private double attract;			// The level of attractiveness
	
	private double students;		// The number of students
	private double unemployed;		// The number of unemployed people
	private double workHome;		// The number of people who work from home
	private double lookAftFam;	// Number of people who stay at home to look after the family
	
	private int id;
	
	public VancouverSociotype() {
		
	}
	
	/**
	 * Compare sociotypes by calculating the Euclidean distance between all their variables. The return value
	 * is normalised and "reversed" so that dissimilar areas return 0, identical ones return 1.
	 * Also, each variable is normalised so that those with larger magnitude don't dominate the calculation.
	 * Not using collective efficacy (ce) because this is just a combination of ownHouse, ethnicity and vandix
	 * variables which are included anyway.
	 */
	public <T extends Sociotype> double compare(T sociotype) {
		VancouverSociotype t = (VancouverSociotype) sociotype; // Cast
		double sum = 0; // The sum of squares
		
		sum += (Math.pow( this.ownHouse-t.ownHouse, 2 ));
		sum += (Math.pow( this.ethnicity-t.ethnicity, 2 ));
		sum += (Math.pow( this.vandix-t.vandix, 2 ));
		sum += (Math.pow( this.attract-t.attract, 2 ));
		sum += (Math.pow( this.students-t.students, 2 ));
		sum += (Math.pow( this.unemployed-t.unemployed, 2 ));
		sum += (Math.pow( this.workHome-t.workHome, 2 ));
		sum += (Math.pow( this.lookAftFam-t.lookAftFam, 2 ));
		
		double val = Math.sqrt(sum);
		// Now normalise and 'reverse' so that range is 0 (dissimilar) to 1 (identical)
		double maxSize = Math.sqrt(8); // i.e. difference between every variable is 1 (maximum)
		return 1 - Functions.normalise(val, 0, maxSize);
	}

	public double getAttractiveness() {
		return this.attract;
	}

	public double getCE() {
		if (this.ce == -1) { // collective efficacy not been calculated yet
			this.ce = (this.ethnicity+this.vandix+this.ownHouse) / 3;
		}
		return this.ce;
	}

	public String getDescription() {
		return "Vancouver Sociotype: "+this.id;
	}

	public int getId() {
		return this.id;
	}
	
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Occupancy is calculated from the number of students, unemployed people, part-time workers and 
	 * economically inactive, looking after family. See Environment/RiskProfile thesis chapter for description
	 * of why these variables are used.
	 * <p>
	 * Similar to Community.getTrafficVolume, will use look-up tables to calculate the weights for each of the
	 * variables depending on the time of day by interpolating between two hourly values. All this is actually done
	 * by the OAC class (mainly because I implemented that one first).
	 * @throws Exception 
	 */
	public synchronized double getOccupancy(double time) throws Exception {
		// Get the weight applied to each variable, depending on the time of day.
		// These will be in this order: 1. students, 2. unemployed, 3. part-time, 4. looking after family
		double[] weights = OAC.getOccupancyWeights(time); 
		return ( weights[0]*this.students + 
				weights[1]*this.unemployed + 
				weights[2]*this.workHome+ 
				weights[3]*this.lookAftFam) / 4;    
	}

	/** Because not using average derived values (because no classification scheme is used with
	 * Vancouver data) this doesn't do anything.
	 */
	public void init() throws Exception { }
	
	/** Returns the values of this sociotype, useful for debugging */
	public String debug() {
		try {
			return "\tsocio id: "+this.id+ 	
					"\n\tvandix: "+this.vandix+
					"\n\tethnicity: "+this.ethnicity+
					"\n\tstudents: "+this.students+
					"\n\townhouse: "+this.ownHouse+
					"\n\tattract: "+this.attract+
					"\n\tce: "+this.ce+
					"\n\tunemployed: "+this.unemployed+
					"\n\tworkHome: "+this.workHome+
					"\n\tlookFam: "+this.lookAftFam+
					"\n\tocc(12pm): "+this.getOccupancy(12.00);
		} catch (Exception e) {
			Outputter.errorln("VancouverSociotype.debug() error:");
			Outputter.errorln(e.getStackTrace());
		}
		return null;
	}

	/** Give default values to each variable, used by Null environment. The values don't actually matter,
	 * it's just so that null's don't cause problems later. */
	public void initDefaultSociotype() {
		this.students = 1;
		this.unemployed = 1;
		this.workHome = 1;
		this.lookAftFam = 1;
		this.ownHouse = 1;
		this.ethnicity = 1;
		this.vandix = 1;
		this.ce = 1;
		this.attract = 1;
	}
	
	/* Get / Set methods for the individual variable values which make up a sociotype. These
	 * are contained in the communities shapefile, EnvironmentFactory will create both communities
	 * and their associated sociotype from the same shapefile.*/
	
	/**
	 * @param ownHouse the ownHouse to set
	 */
	public void setOwnHouse(double ownHouse) {
		this.ownHouse = ownHouse;
	}

	/**
	 * @param ethnicity the ethnicity to set
	 */
	public void setEthnicity(double ethnicity) {
		this.ethnicity = ethnicity;
	}

	/**
	 * @param vandix the vandix to set
	 */
	public void setVandix(double vandix) {
		this.vandix = vandix;
	}

	/**
	 * @param attract the attract to set
	 */
	public void setAttract(double attract) {
		this.attract = attract;
	}

	/**
	 * @param students the students to set
	 */
	public void setStudents(double students) {
		this.students = students;
	}

	/**
	 * @param unemployed the unemployed to set
	 */
	public void setUnemployed(double unemployed) {
		this.unemployed = unemployed;
	}

	/**
	 * @param workHome the workHome to set
	 */
	public void setWorkHome(double workHome) {
		this.workHome = workHome;
	}

	/**
	 * @param lookAfterFam the lookAfterFam to set
	 */
	public void setLookAftFam(double lookAfterFam) {
		this.lookAftFam = lookAfterFam;
	}

	public void setCE(double ce) {
		this.ce = ce;
	}
	
	/** This methods not used by simulation, needed by BurglarFactory in Vancouver skytrain scenario */
	public double getVandix() {
		return this.vandix;
	}
	
	@Override
	public String toString() {
		return "VancouverSociotype "+this.id; 
	}

	public String getParameterValuesString() {
		try {
			return "id: "+this.id+" att: "+this.getAttractiveness()+" occ: "+this.getOccupancy(GlobalVars.time);
		} catch (Exception e) {
			Outputter.errorln("OAC.getParamerValuesString error. Message: "+e.getMessage());
			Outputter.errorln(e.getStackTrace());
		}
		return "";
	}
}
