package burgdsim.environment;

public interface Sociotype {
	
	/**
	 * Build the central store of all variable values. This must be called *after* this OAC has been
	 * created because sometimes it is necessary to know which group this OAC is part of, which could be
	 * read in from a shapefile and set by the Shapefile loader while the new object is created.
	 * @throws Exception If there was a problem initialising the Sociotype (e.g. accessing required data).
	 */
	void init() throws Exception;
	
	/**
	 * Compare this Sociotype with the given one.
	 * @param <T> The type of Sociotype being implemented (e.g. OAC or Mosaic).  
	 * @param sociotype the sociotype to compare to.
	 * @return A value in the range 0 (if the types are dissimilar) to 1 (if they are the same).
	 */
	<T extends Sociotype> double compare(T sociotype);

	/**
	 * Get a description of this Sociotype.
	 * @return
	 */
	String getDescription();
	
	/**
	 * Get the attractiveness of this sociotype to potential burglary (e.g. a measure of income).
	 * @return
	 */
	double getAttractiveness();
	
	/**
	 * Get the expected occupancy levels for this Sociotype given the time of day.
	 * @param time The time of day
	 * @return A number in the range 0 (all houses are unoccupied) to 1 (every house is occupied).
	 * @throws Exception 
	 */
	double getOccupancy(double time) throws Exception;
	
	/**
	 * An id which is used to link Sociotypes and communities when the data is read in.
	 * <p>
	 * (This is a long rather than an int because that's how it is created when my Mapinfo
	 * files are converted to shapefiles).
	 * @return the unique id of this Sociotype
	 */
	int getId();
	
	/**
	 * Initialises a "default" sociotype which will have some default set of parameters. This is used
	 * by the NULL environment instead of init() because the this Sociotype will not be
	 * populated by input data like it would be in a GIS or GRID environment and the resulting null
	 * parameters cause errors.
	 */
	void initDefaultSociotype();

	
	
	/**
	 * This shouldn't be used other that when initialising Communities in a Grid environment. It's a bit 
	 * of a hack.
	 * <p>
	 * When using a grid environment, the file which the communities are read in from only contains a unique
	 * identifier, no other data (unlike in GIS environment where the Communities shapefile contains all the
	 * data for the community and the associted Sociotype). Therefore the community collective efficacy
	 * and traffic volume are stored with the sociotype data and, when Communities are created, this function
	 * can be used to get the collective efficacy out of the Sociotype and into the Community (where it should be). 
	 * @return
	 */
	double getCE();

	/**
	 * Return the Sociotype's parameter values as a string, useful for debugging.
	 */
	String getParameterValuesString();

//	/**
//	 * This shouldn't be used other that when initialising Communities in a Grid environment. It's a bit 
//	 * of a hack.
//	 * <p>
//	 * When using a grid environment, the file which the communities are read in from only contains a unique
//	 * identifier, no other data (unlike in GIS environment where the Communities shapefile contains all the
//	 * data for the community and the associted Sociotype). Therefore the community collective efficacy
//	 * and traffic volume are stored with the sociotype data and, when Communities are created, this function
//	 * can be used to get the traffic volume out of the Sociotype and into the Community (where it should be). 
//	 * @return
//	 */
//	double getTV();
}
