package burgdsim.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Used to get model results from a database and store them in a csv file. Then they can
 * be displayed in a GIS. 
 * @author Nick Malleson
 */
public class ResultsExtractor {

	private File resultsDir;
	private Connection connection;
	private Statement statement;
	public static final int iterationIncrement = 15000; // How often to record information
	public static int maxIter = -1; // The maximum number of iterations of all models, used for averaging results
	
	private ResultsCollection resultsCollection; // Used to store multiple results
	
//	private long startTime; // Used to group many results into same directory (named by current time)

	/** Extract results from the database for models with the given IDs. */
	public ResultsExtractor(List<Integer> modelids) {
		this.resultsCollection = new ResultsCollection();
//		this.startTime = System.currentTimeMillis();
		try {
			// Connect to the database
			System.out.print("Connecting to database...");
			this.connect();
			System.out.println("...connected.");
			
			
			
			// Extract results for each id
			for (Integer modelid:modelids) {				
				Map<Integer, BuildingInfo> buildings = new Hashtable<Integer, BuildingInfo>();
				Map<Integer, CommunityInfo> communities = new Hashtable<Integer, CommunityInfo>();
				Map<Integer, BurglaryInfo> burglars = new Hashtable<Integer, BurglaryInfo>();

				buildings = new Hashtable<Integer, BuildingInfo>();
				communities = new Hashtable<Integer, CommunityInfo>();
				burglars = new Hashtable<Integer, BurglaryInfo>();
				Outputter.debugOn(false) ; // Turn off all model output
						

				// Check the model ID is valid
				boolean correctid = this.checkModelID(modelid);
				if (correctid) {
					System.out.println("Analysing model: "+modelid);
				}
				else {
					System.err.println("Incorrect model id: "+modelid+" not extracting results for this one. " +
							"Contunuing with other models though.");
					continue;
				}

				// Run some queries to get the history data
				this.getBuildingInfo(buildings, modelid); // Get security and burglary information at certain iterations

				this.getComunityInfo(communities, modelid);

				this.getBurglarInfo(burglars, modelid); // Write information about the burglars when the committed the burglaries

				// TODO get information about the burglars in, i.e. what they were doing during the day?
				
				// Remember these results
				this.resultsCollection.addResults(buildings, communities, burglars);

				// Create a directory for model results
				if (modelids.size()==1) { // Only analysing one model, store it in one directory
					this.resultsDir = new File("data/results/model"+modelid+"/");
					if (!resultsDir.exists()) {
						System.out.println("Creating new directory for model results: "+resultsDir.getPath());
						resultsDir.mkdirs();
					}
				}
				else { // Analysing more than one model, group all results in same directory (specified by time)
					this.resultsDir = this.buildResultsSubdir(modelids);
				}

				System.out.println("Writing results to directory: "+this.resultsDir.getAbsolutePath());

				// Write out the data to text files.
				this.writeBuildingData(buildings, modelid);

				this.writeCommunityData(communities, modelid);

				this.writeBurglaryData(burglars, modelid);

				System.out.println("Finished writing results for model "+modelid);

			} // for modelids

			// Close the database connection
			System.out.print("Closing database connection...");
			this.connection.close();
			System.out.println("...closed.");
			
			// Create new data for average results
			System.out.println("Creating average model results...");
			Map<Integer, BuildingInfo> avgBuildings = new Hashtable<Integer, BuildingInfo>();
			Map<Integer, CommunityInfo> avgCommunities = new Hashtable<Integer, CommunityInfo>();
			Map<Integer, BurglaryInfo> avgBurglars = new Hashtable<Integer, BurglaryInfo>();
			this.resultsCollection.getAverageResults(avgBuildings, avgCommunities, avgBurglars);
			
			System.out.println("Writing average results to file...");
			this.writeBuildingData(avgBuildings, -1);
			this.writeCommunityData(avgCommunities, -1);
			

		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally { // Close the database
			try {
				this.connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		System.out.println("ALL FINISHED SUCCESSFULLY, EXITING");
	}
	
	/** Convenience function to generate a directory name for collecting multiple results.
	 * @param modelids The model ids
	 * @return
	 */
	private File buildResultsSubdir(List<Integer> modelids) {
		String dirName = "data/results/models";
		for (int i=0; i<modelids.size(); i++) {
			if (i>5) { // Don't add more than five model ids to name, will be too long
				break;
			}
			else {
				dirName+= (new String("_"+modelids.get(i)));
			}
		}
		dirName+="/";
		File dir = new File(dirName);
		if (!dir.exists()) {
			System.out.println("Creating new directory for multiple model results: "+dir.getPath());
			dir.mkdirs();
		}
		return dir;
	}

	/* ****************** DATABASE QUERY FUNCTIONS ******************/

	private boolean checkModelID(int modelid) throws SQLException {
		String sql = "SELECT modelname FROM modelnames WHERE modelid = "+modelid;
		ResultSet rs = this.statement.executeQuery(sql);
		if (!rs.next()) { // No rows returned
			System.err.println("Error, model ID "+modelid+" isn't found in the database.");
			return false;
		}
		else {
			System.out.println("Found model name for model "+modelid+": "+rs.getString(1));
			return true;
		}
	}

	/** Find out the maximum time value in the given table. */
	private int getMaxIterations(String tableName, int modelid) throws SQLException {
		String sql = "SELECT MAX(time) FROM "+tableName+" WHERE modelid = "+modelid;
		ResultSet rs = this.statement.executeQuery(sql);
		rs.next();
		int maxtime = rs.getInt(1);
		System.out.println("Found max time for table '"+tableName+"': "+maxtime);
		if (maxtime > ResultsExtractor.maxIter) {
			ResultsExtractor.maxIter = maxtime; // Record the maximum number of iterations
		}
		return maxtime;
	}

	/** Get the number of burglaries and security values in each building every iterationIncrement
	 * number of iterations */
	private void getBuildingInfo(Map<Integer, BuildingInfo>  buildings, int modelid) throws SQLException {
		int maxIter = this.getMaxIterations("burglary", modelid); // Get the max time value in the database
		PreparedStatement ps = this.connection.prepareStatement(
				"SELECT b.buildingid, COUNT(burg.buildingid) FROM buildings b "+ 
				"LEFT OUTER JOIN burglary burg ON b.buildingID = burg.buildingid "+
				"WHERE  burg.modelid = "+modelid+" AND burg.time <= ? "+
				"GROUP BY b.buildingid "+ 
				"ORDER BY b.buildingid ASC"
		);
		ResultSet rs;
		for (int i=0; i<maxIter; i+=ResultsExtractor.iterationIncrement) { // Iterate over time values, getting info
			System.out.println("getBuildingInfo executing burglary query at time: "+i);
			ps.setInt(1, i);
			rs = ps.executeQuery();
			this.addBuildingBurglaries(rs, i, buildings);
		}
		// Also get the final values
		System.out.println("getBuildingInfo adding final burglary values (time "+maxIter+")");
		ps.setInt(1, maxIter);
		rs = ps.executeQuery();
		this.addBuildingBurglaries(rs, maxIter, buildings);

		/* Now add security as well. */
		// First find out when security values have been recorded (unlike burglaries, security is
		// recorded for every building at the same time every n iterations)
		List<Integer> securityIterations = new ArrayList<Integer>();
		System.out.print("getBuildingInfo finding out when security info was stored...");
		rs = this.statement.executeQuery("SELECT DISTINCT time FROM secure ORDER BY time ASC");
		while (rs.next())
			securityIterations.add(rs.getInt(1));
		System.out.println("...security was recorded at times: "+securityIterations.toString());
		ps = this.connection.prepareStatement(
				"SELECT b.buildingid, s.value FROM buildings b "+
				"LEFT OUTER JOIN secure s ON b.buildingID = s.buildingid "+
				"WHERE s.modelid = "+modelid+" AND s.time = ?"
		);
		// Now loop, getting appropriate security values (these wont the i values exactly, have to find closest)
		for (int i=0; i<maxIter; i+=ResultsExtractor.iterationIncrement) {
			int time = 0; // The time to get security values
			// Find largest time value which isn't smaller than i (lazy way of finding closest time)
			for (int j=0; j<securityIterations.size(); j++) {
				if (securityIterations.get(j)>=i || securityIterations.get(j)+1>=i) { // (Allowing for first time stored as -1 not 0)
					time = securityIterations.get(j);
					break;
				}
			} // for securityIterations
			System.out.println("getBuildingInfo executing security query at time: "+time);
			ps.setInt(1, time);
			rs = ps.executeQuery();
			this.addBuildingSecurity(rs, time, buildings);
		} // for iterationIncrement
//		// Finally add the last security values (don't thnk this is necessary, will always add last?)
//		int last = securityIterations.get(securityIterations.size()-1);
//		System.out.println("getBuildingInfo adding final securityvalues (time "+last+")");
//		ps.setInt(1, last);
//		rs = ps.executeQuery();
//		this.addBuildingSecurity(rs, last, buildings);

	}

	// Convenience funcion, adds building information
	private void addBuildingBurglaries(ResultSet rs, int time, Map<Integer, BuildingInfo> buildings) throws SQLException {
		while (rs.next()) { // Go through buildings, adding the information to the hashtables
			int buildingID = rs.getInt(1);
			int numBurglaries = rs.getInt(2);
			//			System.out.println("\tfound building "+buildingID+" with burglaries: "+numBurglaries);
			if (buildings.containsKey(buildingID)) {
				BuildingInfo bi = buildings.get(buildingID);
				bi.burglaries.add((double)numBurglaries);
				bi.burglariesTime.add(time);
			}
			else {
				//				System.out.println("\t\tadding to hashatable");
				BuildingInfo bi = new BuildingInfo(buildingID);
				bi.burglaries.add((double)numBurglaries);
				bi.burglariesTime.add(time);
				buildings.put(buildingID, bi);
			}
		} // while (rs.next)
	}
	// Convenience funcion, adds building information
	private void addBuildingSecurity(ResultSet rs, int time, Map<Integer, BuildingInfo> buildings) throws SQLException {
		while (rs.next()) { // Go through buildings, adding the information to the hashtables
			int buildingID = rs.getInt(1);
			double security = rs.getDouble(2);
			//			System.out.println("\tFound building "+buildingID+" with security: "+security);
			if (buildings.containsKey(buildingID)) {
				BuildingInfo bi = buildings.get(buildingID);
				bi.security.add(security);
				bi.securityTime.add(time);
			}
			else {
				//				System.out.println("\t\tadding to hashatable");
				BuildingInfo bi = new BuildingInfo(buildingID);
				bi.security.add(security);
				bi.securityTime.add(time);
				buildings.put(buildingID, bi);
			}
		}
	}

	/** Get the number of burglaries committed in each community at different time intervals. Will only store
	 * communities where a burglary has occurred. */
	private void getComunityInfo(Map<Integer, CommunityInfo> communities, int modelid) throws SQLException {
		int maxIter = this.getMaxIterations("burglary", modelid); // Get the max time value in the database
		PreparedStatement ps = this.connection.prepareStatement( 
				"SELECT c.communityid, COUNT(burg.buildingid) FROM communities c "+
				"RIGHT OUTER JOIN buildings b ON b.communityid = c.communityid "+
				"INNER JOIN burglary burg ON b.buildingID = burg.buildingid " +
				"WHERE  burg.modelid = "+modelid+" AND burg.time <= ? " +
				"GROUP BY c.communityid " +
				"ORDER BY c.communityid ASC"
		);
		ResultSet rs;
		for (int i=0; i<maxIter; i+=ResultsExtractor.iterationIncrement) { // Iterate over time values, getting info
			System.out.println("getCommunityInfo executing burglary query at time: "+i);
			ps.setInt(1, i);
			rs = ps.executeQuery();
			this.addCommunityBurglaries(rs, i, communities);
		}
		// Also get the final values
		System.out.println("getCommunityInfo adding final burglary values (time "+maxIter+")");
		ps.setInt(1, maxIter);
		rs = ps.executeQuery();
		this.addCommunityBurglaries(rs, maxIter, communities);
	}
	// Convenience funcion, adds building information
	private void addCommunityBurglaries(ResultSet rs, int time, Map<Integer, CommunityInfo> communities) throws SQLException {
		while (rs.next()) { // Go through buildings, adding the information to the hashtables
			int communityID = rs.getInt(1);
			int numBurglaries = rs.getInt(2);
			//						System.out.println("\tfound community"+communityID+" with burglaries: "+numBurglaries);
			if (communities.containsKey(communityID)) {
				CommunityInfo ci = communities.get(communityID);
				ci.burglaries.add((double)numBurglaries);
				ci.burglariesTime.add(time);
			}
			else {
				//				System.out.println("\t\tadding to hashatable");
				CommunityInfo ci = new CommunityInfo(communityID);
				ci.burglaries.add((double)numBurglaries);
				ci.burglariesTime.add(time);
				communities.put(communityID, ci);
			}
		} // while (rs.next)
	}

	/** Get the burglars' important addresses (home, work etc) and also times and coordinates of burglaries.
	 * NOTE: can't get social/drug dealer addresses, not storing these in db yet 
	 * @throws SQLException */
	private void getBurglarInfo(Map<Integer, BurglaryInfo> burglars, int modelid) throws SQLException {
		// Get the burglars' home, work, drugdealer and social addresses
		System.out.println("Reading burglar information.");
		ResultSet rs = this.statement.executeQuery("SELECT burglarid, home FROM burglars WHERE modelid = "+modelid);
		while (rs.next()) {
			int burglarID = rs.getInt(1);
			int homeID = rs.getInt(2);
			int workID = -1;	// Not storing work and social IDs at the moment
			int socialID = -1;
			BurglaryInfo bi = new BurglaryInfo(burglarID, homeID, socialID, workID);
			burglars.put(burglarID, bi);
			//			System.out.println("\tfound burglar "+burglarID+" with home: "+homeID);
		}

		// Now get burglary information
		System.out.println("Reading burglary information");
		rs = this.statement.executeQuery(
				"SELECT b.burglarid, i.xcoord, i.ycoord, b.time, h.buildingid FROM burglary b "+
				"INNER JOIN burglarinfo i on b.burglarid = i.burglarid AND b.time = i.time "+
				"INNER JOIN buildings h on h.buildingid = b.buildingid "+
				"WHERE b.modelid = "+modelid+" AND i.modelid = "+modelid
		);
		while (rs.next()) {
			int burglarid = rs.getInt(1);
			double xcoord = rs.getDouble(2);
			double ycoord = rs.getDouble(3);
			int time = rs.getInt(4);
			int buildingID = rs.getInt(5);
			BurglaryInfo bi = burglars.get(burglarid); // All burglars will have been added to map by prev query
			bi.addBurglary(buildingID, xcoord, ycoord, time);
			//			System.out.println("\tfound burglary by "+burglarid+" of building "+buildingID+" at time "+time);
		}

	}

	/* ****************** CSV OUTPUT FUNCTIONS ******************/

	/** Write the building information to a csv file. File will show, for each building, the security values
	 * and number of burglaries that occurred by a certain number of iterations. Process is as follows:
	 * <ol><li>Go through security values, finding the times that security information has been stored and
	 * using these to write the headers (one column for each security value). Note that security values for
	 * all buildings are written at same time (i.e. once per day) but burglaries occur sporadically.<li>
	 * <li>Use these to write headers for burglaries as well (i.e. the total number of burglaries that have
	 * occurred in each building by the given time).
	 * <li>Go through the security and burglaries information writing out the data.</li>
	 * </ol>
	 * @throws IOException */
	private void writeBuildingData(Map<Integer, BuildingInfo> buildings, int modelid) throws IOException {
		File buildingCSV = new File(this.resultsDir.getAbsolutePath()+"/buildings"+modelid+".csv");
		System.out.println("Writing building data to "+buildingCSV.getAbsolutePath());
		BufferedWriter writer = new BufferedWriter(new FileWriter(buildingCSV));
		String header = "BuildingID, ";
		int numSecurityTimes = 0; // Need to know how security info was written for writing out csv file (if no data for building have to add null)
		for (Integer buildingID:buildings.keySet()) { // Write a header first (only do this for loop once!)
			BuildingInfo bi = buildings.get(buildingID);
			for (Integer securityTime:bi.securityTime) {
				header += ("Secure"+securityTime+", ");
				header += ("Burgs"+securityTime+", ");
				numSecurityTimes++;
			}
			header = header.substring(0, header.length()-1); // delete the last comma and space (two characters)
			header += "\n";
			writer.write(this.cleanString(header));
			//			System.out.println("Writing headers line: "+header);
			break;
		} // for buildings

		// Header has been written, go through every building, writing security and burglary information about it
		for (Integer build:buildings.keySet()) {
			String line = ""+build+", "; // the building id starts the line
			BuildingInfo buildingInfo = buildings.get(build);
			for (int i=0; i<numSecurityTimes; i++) {
				// Write the security values that happened at each time.
				try { 
					line+=buildingInfo.security.get(i)+", ";
				} catch (IndexOutOfBoundsException e) {
					line += "0, ";
				}
				// Write the total number of burglaries up to the given time by finding the closest time
				// that burglaries were written out (remember burglaries written out sporadicaly, not at
				// set times like security values)
				int time = buildingInfo.securityTime.get(i);
				double numBurglaries = 0;
				int difference = Integer.MAX_VALUE;
				for (int j=0; j<buildingInfo.burglariesTime.size(); j++) {
					int burgTime = buildingInfo.burglariesTime.get(j);
					if ( 
							Math.abs(time - burgTime) < difference	// this time value is closer to 'time' 
							&& burgTime <= time)						// but not greater than it
					{
						difference = Math.abs(time - burgTime);
						try { 
							numBurglaries = buildingInfo.burglaries.get(j);
						} catch (IndexOutOfBoundsException e) {
							System.err.println("Caught an IndexOutOfBounds error trying to get cumulative burglaries " +
									"for building: "+buildingInfo.id+" at time: "+burgTime+". Not sure why!!");
						}
					}
				}
				line+=numBurglaries+", ";
			}
			line = line.substring(0, line.length()-1); // delete the last comma and space (two characters)
			line += "\n";
			writer.write(line);
		}
		writer.close();
	}
	private void writeCommunityData(Map<Integer, CommunityInfo> communities, int modelid) throws IOException {
		File communityCSV = new File(this.resultsDir.getAbsolutePath()+"/communities"+modelid+".csv");
		System.out.println("Writing community data to "+communityCSV.getAbsolutePath());
		BufferedWriter writer = new BufferedWriter(new FileWriter(communityCSV));
		String header = "CommunityID, ";
		// Need to know when burglary data was written out for each community, find the community with the most entries.
		// This one will definitely have the first burglaries stored because once it has a single burglary, it will 
		// be returned in every SQL query (because total num burglaries will be greater than 0) so most entries.
		int largestNumEntries = 0;
		CommunityInfo com = null; // The community with the most entries
		for (CommunityInfo ci:communities.values()) {
			if (ci.burglariesTime.size() > largestNumEntries) {
				largestNumEntries = ci.burglariesTime.size(); 
				com = ci;
			}
		} // for communities
		// Use that community to generate headers and to remember the times that burglary information was written
		List<Integer> burgTimes = new ArrayList<Integer>();
		for (Integer time:com.burglariesTime) {
			header += ("Burgs"+time+", ");
			burgTimes.add(time);
		}
		header = header.substring(0, header.length()-1); // delete the last comma and space (two characters)
		header += "\n";
		writer.write(this.cleanString(header));

		// Header has been written, now write number of burglaries in each community
		for (Integer cID:communities.keySet()) {
			String line = ""+cID+", "; // the building id starts the line
			CommunityInfo community = communities.get(cID);
			for (Integer time:burgTimes) {
				if (community.burglariesTime.contains(time)) {
					// The community has stored burglary information at the given time, write the num burglaries
					line+=community.burglaries.get(community.burglariesTime.indexOf(time))+", ";
				}
				else {
					line += "0, "; // No burglaries have happened at this time for the community
				}
			} // for burglary times
			line = line.substring(0, line.length()-1); // delete the last comma and space (two characters)
			line += "\n";
			writer.write(line);
		} // for communities
		writer.close();	
	}

	/** Write burglary data to a CSV file. Will show, for each burglar, where they committed their 
	 * burglaries and the times. Will also write a single file with all burglar
	 * @throws IOException */
	private void writeBurglaryData(Map<Integer, BurglaryInfo> burglars, int modelid) throws IOException {
		// The file header
		String header = "BurglarID, time, burgXCoord, burgYCoord, BuildingID, HomeAdr, WorkAdr, SocialAdr\n";
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.resultsDir+"/burglars"+modelid+".csv")));
		writer.write(header);
		for (BurglaryInfo bi:burglars.values()) {
			for (int i=0; i<bi.burglaryTimes.size(); i++) {
				writer.write(bi.id+", "+bi.burglaryTimes.get(i)+", "+bi.xCoords.get(i)+", "+bi.yCoords.get(i)+", "+
						bi.buildings.get(i)+", "+bi.homeID+", "+bi.workID+", "+bi.socialID+"\n");
			}

		} // for BurglarInfo
		writer.close();
	}


	/* ****************** MISC FUNCTIONS / CLASSES ******************/

	/** removes/repaaces characters that might confuse a GIS (e.g. replace '-' with '_'). */
	private String cleanString(String input) {
		return input.replaceAll("-", "_");
	}

	/** Connect to the database */
	private void connect() throws ClassNotFoundException, SQLException {
		Class.forName("oracle.jdbc.driver.OracleDriver");
		this.connection = DriverManager.getConnection(
				"jdbc:oracle:thin:@(DESCRIPTION=(LOAD_BALANCE=yes)(FAILOVER=ON) (ADDRESS=(PROTOCOL=tcp)(HOST=db1-vip.ngs.rl.ac.uk)(PORT=1521))(ADDRESS=(PROTOCOL=tcp)(HOST=db2-vip.ngs.rl.ac.uk)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=ngs11.esc.rl.ac.uk)(FAILOVER_MODE=(TYPE=SESSION)(METHOD=BASIC))))",
				"ngsdb0050", 		// username
		"n1cksp4ssw0rd"); 	// password
		this.statement = this.connection.createStatement();
	}

	public static void main(String[] args) {
		if (args.length<1) {
			System.err.println("Please give one or more command line argument: the ID(s) of the model(s) to extract " +
				"results from.");
		}
		else {
			String badArgument = ""; // Used to report command-line errors
			try {
				List<Integer> ids = new ArrayList<Integer>(args.length); // Array of model ids
				for (int i=0; i<args.length; i++) {
					badArgument = args[i]; // So if next line throws Exception we know which argument is bad
					ids.add(Integer.parseInt(args[i]));
					
				}
				System.out.println("Will extract results from models with ids "+ids.toString());
				new ResultsExtractor(ids);
			} catch (NumberFormatException e) {
				System.err.println("Could not convert the command line argument '"+badArgument+"' "+
				"into a number. Not continuing.");
			}
		}
	}
}

/** Class to store all the information about buildings */
class BuildingInfo {
	int id;							// The building's id
	List<Double> burglaries;		// The total number of burglaries committed at different times
	List<Integer> burglariesTime;	// The corresponding time a burglary was comitted
	List<Double> security;			// Store security as well.
	List<Integer> securityTime;		// The corresponding time a burglary was comitted

	public BuildingInfo (int id) {
		this.id = id;
		this.burglaries = new ArrayList<Double>();
		this.burglariesTime = new ArrayList<Integer>();
		this.security = new ArrayList<Double>();
		this.securityTime = new ArrayList<Integer>();
	}

}

/** Class to store all the information about communities */
class CommunityInfo {
	int id;							// The community's id
	List<Double> burglaries;		// The total number of burglaries committed at different times
	List<Integer> burglariesTime;	// The corresponding time a burglary was comitted

	public CommunityInfo (int id) {
		this.id = id;
		this.burglaries = new ArrayList<Double>();
		this.burglariesTime = new ArrayList<Integer>();
	}

}

/** Class to store all the information about burglars */
class BurglaryInfo {
	int id;							// The burglars's id
	int homeID;						// The buildingID of the agent's home
	int socialID;					// The buildingID of the place where the agent socialises
	int workID;						// The buildingID of the place where the agent works
	List<Integer> burglaryTimes;	// The times that each burglary was comitted
	List<Double> xCoords, yCoords;	// x and y locations of the burglaries
	List<Integer> buildings;		// The burgled buildings 

	public BurglaryInfo(int id, int homeID, int socialID, int workID) {
		this.id = id;
		this.homeID = homeID;
		this.socialID = socialID;
		this.workID = workID;
		this.burglaryTimes = new ArrayList<Integer>();
		this.xCoords = new ArrayList<Double>(); this.yCoords = new ArrayList<Double>();
		this.buildings = new ArrayList<Integer>();
	}

	public void addBurglary(int buildingID, double xcoord, double ycoord, int time) {
		this.buildings.add(buildingID);
		this.xCoords.add(xcoord);
		this.yCoords.add(ycoord);
		this.burglaryTimes.add(time);
	}
}

/** 
 * Class used to hold all results for a particular model. Consists of lists of attribute maps which are
 * populated each time ResultsExtractor gets model results (simpler than using multi-dimensional
 * lists). Can also be used to generate an 'average' result.
 * @author Nick Malleson
 */
class ResultsCollection {
	
	List<Map<Integer, BuildingInfo>> buildingsResults;
	List<Map<Integer, CommunityInfo>> communitiesResults;
	List<Map<Integer, BurglaryInfo>> burglarsResults;
	
	public ResultsCollection() {
		this.buildingsResults = new ArrayList<Map<Integer, BuildingInfo>>();
		this.communitiesResults = new ArrayList<Map<Integer, CommunityInfo>>();
		this.burglarsResults = new ArrayList<Map<Integer, BurglaryInfo>>();
	}

	/**
	 * Create average results data, populating the given hash tables. Just shows average number of burglaries
	 * by the end of the model, not at different times. Also, doesn't show security values.
	 * @param avgBuildings The average information about buildings .
	 * @param avgCommunities The average information about communities.
	 * @param avgBurglars The average information about burglars.
	 */
	public void getAverageResults(Map<Integer, BuildingInfo> avgBuildings,
			Map<Integer, CommunityInfo> avgCommunities,
			Map<Integer, BurglaryInfo> avgBurglars) {
		if (avgBuildings==null || avgCommunities == null || avgBurglars == null) {
			System.err.println("ResultsExtractor.getAverageResults() error: one of the inputs is null, " +
					"cannot continue");
			return;
		}
//		// Get average number of burglaries in each building at different times
//		List<Integer> times = new ArrayList<Integer>(); // The times to get number of burglaries
//		for (int i=0; i<ResultsExtractor.maxIter; i+=ResultsExtractor.iterationIncrement) {
//			times.add(i);
//		}
//		times.add(ResultsExtractor.maxIter); // Also add final iteration
		
		// Now calculate the average num burglaries for each building over all results
		for (Map<Integer, BuildingInfo> buildingResult:this.buildingsResults) {
			for (BuildingInfo bi:buildingResult.values()) {
				if (!avgBuildings.containsKey(bi.id)) { // First time found this building
					BuildingInfo newBI = new BuildingInfo(bi.id);
					// Add the number of burglaries at the end of the simulation (last time available)
					newBI.burglaries.add(bi.burglaries.get(bi.burglaries.size()-1));
					// Also add these to stop output functions crashing
					newBI.burglariesTime.add(-1); newBI.security.add(-1.0); newBI.securityTime.add(-1);
					avgBuildings.put(bi.id, newBI);
//					System.out.println("New building with burglaries: "+newBI.burglaries.get(0));
				}
				else { //Already stored some info for this building, add to its total burglaries
					BuildingInfo newBI = avgBuildings.get(bi.id);
					// Index 0 because only storing final values, not worrying about values over time
					double burgs = newBI.burglaries.get(0);
					newBI.burglaries.set(0, burgs+bi.burglaries.get(bi.burglaries.size()-1));
//					System.out.println("Existing building with burglaries: "+burgs+" -> "+newBI.burglaries.get(0));
				}
			} // for buildings
		} // for building results
		// Have total number of burglaries, go through and calculate averages
		for (BuildingInfo bi:avgBuildings.values()) {
			double burgs = bi.burglaries.get(0);
			// Cacl average by dividing by number of results (will be stored as an integer)
			bi.burglaries.set(0, (double) burgs / (double) this.buildingsResults.size());
		}
		
		// Now calculate average burglaries in each community (basically same as for buildings, I should make
		// a generic function to do both...)
		for (Map<Integer, CommunityInfo> communityResult:this.communitiesResults) {
			for (CommunityInfo ci:communityResult.values()) {
				if (!avgCommunities.containsKey(ci.id)) { // First time found this building
					CommunityInfo newCI = new CommunityInfo(ci.id);
					// Add the number of burglaries at the end of the simulation (last time available)
					newCI.burglaries.add(ci.burglaries.get(ci.burglaries.size()-1));
					newCI.burglariesTime.add(-1); // Also add this to stop output functions crashing
					avgCommunities.put(ci.id, newCI);
//					System.out.println("New community: "+newCI.id+ " with burgs: "+newCI.burglaries.get(0));
				}
				else { //Already stored some info for this building, add to its total burglaries
					CommunityInfo newCI = avgCommunities.get(ci.id);
					// Index 0 because only storing final values, not worrying about values over time
					double burgs = newCI.burglaries.get(0);
					newCI.burglaries.set(0, burgs+ci.burglaries.get(ci.burglaries.size()-1));
//					System.out.println("Existing community: "+newCI.id+ " with prev burgs: "+burgs+" and now: "+newCI.burglaries.get(0));
				}
			} // for buildings
		} // for building results
		// Have total number of burglaries, go through and calculate averages
		for (CommunityInfo ci:avgCommunities.values()) {
			double burgs = ci.burglaries.get(0);
			// Cacl average by dividing by number of results (will be stored as an integer)
			ci.burglaries.set(0, (double) burgs / (double) this.communitiesResults.size());
//			System.out.println("Averaging for community "+ci.id+" with "+burgs+" burglaries over "+this.communitiesResults.size()+" models: "+ci.burglaries.get(0));
		}

		// TODO calculate average burglar values
		
	}

	/** 
	 * Add some results to this collection.
	 * 
	 * @param buildings The list of buildings.
	 * @param communities The list of communities
	 * @param burglars The list of burglars
	 */
	public void addResults(Map<Integer, BuildingInfo> buildings,
			Map<Integer, CommunityInfo> communities,
			Map<Integer, BurglaryInfo> burglars) {
		this.buildingsResults.add(buildings);
		this.communitiesResults.add(communities);
		this.burglarsResults.add(burglars);		
	}
}