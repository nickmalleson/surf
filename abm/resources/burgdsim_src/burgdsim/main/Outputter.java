package burgdsim.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Controls what will/wont be written out during a simulation run. Used instead of System.out.print
 * because it would be easier to direct all output to a file, for example.
 * 
 * @author Nick Malleson
 *
 */
public class Outputter {

	private static Outputter instance = new Outputter();

	public enum DEBUG_TYPES {ROUTING, GENERAL, AWARENESS_SPACE, BURGLARY, DATA_ACCESS, CACHES, SCENARIOS, INIT, ITERATIONS};	// All the different categories for debugging information
	private static boolean debugGeneral = false;
	private static boolean debugRouting = false;
	//	private static boolean debugPECS = true;
	public static boolean debugAwarenessSpace = false; // This is public because in Burglar.java there are some debugging loops which are only necessary if debugging the awareness space
	private static boolean debugBurglary = false;
	private static boolean debugDataAccess = false;
	private static boolean debugCaches = false;
	private static boolean debugScenarios = false;
	private static boolean debugInit = false;
	private static boolean debugIterations = true; // Print num iterations, burglary and mem every 100 iterations
	
	// List of all files we're writing to, indexed by the filename (String)
	private static Map<String, Writer> csvFiles = new Hashtable<String, Writer>();

	private static boolean errorReporting = true; // Whether or not to report errors 

	// Print debugging information. The type tells us what the debugging information is about so it is possible
	// to turn it on or off depending on which part of the programm we're debugging
	public static void debug (String in, DEBUG_TYPES type) {
		if (type.equals(DEBUG_TYPES.ROUTING) && debugRouting) {
			output(in);
		}
		//			else if (type.equals(DEBUG_TYPES.PECS) && debugPECS) {
		//				System.out.print(in);
		//			}
		else if (type.equals(DEBUG_TYPES.GENERAL) && debugGeneral) {
			output(in);
		}
		else if (type.equals(DEBUG_TYPES.AWARENESS_SPACE) && debugAwarenessSpace) {
			output(in);
		}
		else if (type.equals(DEBUG_TYPES.BURGLARY) && debugBurglary) {
			output(in);
		}
		else if (type.equals(DEBUG_TYPES.DATA_ACCESS) && debugDataAccess) {
			output(in);
		}
		else if (type.equals(DEBUG_TYPES.CACHES) && debugCaches) {
			output(in);
		}
		else if (type.equals(DEBUG_TYPES.SCENARIOS) && debugScenarios) {
			output(in);
		}
		else if (type.equals(DEBUG_TYPES.INIT) && debugInit) {
			output(in);
		}
		else if (type.equals(DEBUG_TYPES.ITERATIONS) && debugIterations) {
			output(in);
		}
		
	}
	public static void debugln (String in, DEBUG_TYPES type) {
		if (type.equals(DEBUG_TYPES.ROUTING) && debugRouting) {
			outputln(in);
		}
		//			else if (type.equals(DEBUG_TYPES.PECS) && debugPECS) {
		//				System.out.println(in);
		//			}
		else if (type.equals(DEBUG_TYPES.GENERAL) && debugGeneral) {
			outputln(in);
		}
		else if (type.equals(DEBUG_TYPES.AWARENESS_SPACE) && debugAwarenessSpace) {
			outputln(in);
		}
		else if (type.equals(DEBUG_TYPES.BURGLARY) && debugBurglary) {
			outputln(in);
		}
		else if (type.equals(DEBUG_TYPES.DATA_ACCESS) && debugDataAccess) {
			outputln(in);
		}
		else if (type.equals(DEBUG_TYPES.CACHES) && debugCaches) {
			outputln(in);
		}
		else if (type.equals(DEBUG_TYPES.SCENARIOS) && debugScenarios) {
			outputln(in);
		}
		else if (type.equals(DEBUG_TYPES.INIT) && debugInit) {
			outputln(in);
		}
		else if (type.equals(DEBUG_TYPES.ITERATIONS) && debugIterations) {
			outputln(in);
		}
	}	
	public static void describe (String in) {
		output(in);
	}
	public static void describeln (String in) {
		outputln(in);
	}
	
	/** Will print errors. If errors are being redirected they will be sent to standard output along with
	 * all model messages, otherwise they are sent to standard error. This is ignored if not on NGS. */
	public static void errorln(String in) {
		if (Outputter.errorReporting) {
			if (GlobalVars.ON_NGS == 0) {
				outputErrorln(in);
			}
			else if (GlobalVars.REDIRECT_ERRORS == 1) {
				outputln("ERROR:"+in);
			}
			else {
				outputErrorln(in);
			}
		}

	}
	
	public static void errorln(StackTraceElement[] array) {
		if (Outputter.errorReporting) {
			if (GlobalVars.REDIRECT_ERRORS == 1) {
				outputln("ERROR:** Stack trace begin**");
				for (StackTraceElement e:array)
					outputln("ERROR:"+e.toString());
				outputln("ERROR:** Stack trace end **");

			}
			else {
				outputErrorln("** Stack trace begin**");
				for (StackTraceElement e:array)
					outputErrorln(e.toString());
				outputErrorln("** Stack trace end **");
			}
		}
	}
	
	/* THESE FUNCTIONS ACTUALLY HANDLE OUTPUTING. If GlobalVars.logger isn't null then an MPJ slave is
	 * running this simulation and wants the output to go to a log file. */

	private static void outputErrorln(String in) {
		if (GlobalVars.logger!=null){
			GlobalVars.logger.log(in);
		}
		else {
			System.err.println(in);
		}
	}
	
	private static void output(String in) {
		if (GlobalVars.logger!= null) {
			GlobalVars.logger.log(in);
		}
		else {
			System.out.print(in);
		}
	}
	private static void outputln(String in) {
		if (GlobalVars.logger!=null) {
			GlobalVars.logger.log(in);
		}
		else {
			System.out.println(in);
		}
	}



	public static Outputter getInstance() {
		return instance;
	}

	/**
	 * Write values to a CSV file.
	 * @param filename The name of a file to write to (e.g. "simple_burglar_behaviour.csv")
	 * @param vals A List<String, String> of variable names (for the header) and values to write
	 * @throws IOException If there is a problem writing to the file
	 */
	public static void describeCSV(String filename, List<String> headers, List<String> values) throws IOException {

		try {
			// If this file hasn't been created yet, create it
			if (!Outputter.csvFiles.keySet().contains((filename))) {
				if (!(headers.size()==values.size()))
					Outputter.errorln("WARNING: Outputter: describeCSV: different number of headers and values.");
				Writer w = new BufferedWriter(new FileWriter(new File(filename)));
				Outputter.csvFiles.put(filename, w);
				// Write a file header as well
				String header = "";
				for (int i=0; i<headers.size(); i++) {
					if (i==headers.size()-1) // add a new line (no comma) after last value
						header = header+headers.get(i)+"\n";
					else
						header = header+headers.get(i)+",";
				}
				w.write(header);
			}
			Writer csvWriter = csvFiles.get(filename); // The writer associated with this filename
			String output = ""; // Build up each line from the values passed
			for (int i=0; i<values.size(); i++) {
				if (i==values.size()-1) // add a new line (no comma) after last value
					output = output+values.get(i)+"\n";
				else
					output = output+values.get(i)+",";
			}
			csvWriter.write(output);
		} catch (IOException e) {
			Outputter.errorln("Outputter: describeCSV(). I/O error writing to file.");
			errorln(e.getStackTrace());
			throw e;
		}
	}

	/**
	 * Close all the files we were writing to.
	 * @throws IOException If there was a problem closing files.
	 */
	public static void closeFiles() throws IOException {
		try {
			for (Writer w:Outputter.csvFiles.values())
				w.close();
		} catch (IOException e) {
			Outputter.errorln("Outputter: closeFiles(). I/O error closing files.");
			errorln(e.getStackTrace());
			throw e;
		}
	}

	/**
	 * Turn all debugging output on or off
	 * @param on If true everything will be output, otherwise output no debugging info.
	 */
	public static void debugOn(boolean on) {
		if (!on) {
			debugGeneral = false;
			debugRouting = false;
			debugAwarenessSpace = false;
			debugBurglary = false;
			debugDataAccess = false;
			debugCaches = false;
			debugScenarios = false;
			debugInit = false;
		}
		else {
			debugGeneral = true;
			debugRouting = true;
			debugAwarenessSpace = true;
			debugBurglary = true;
			debugDataAccess = true;
			debugCaches = true;
			debugScenarios = true;
			debugInit = true;
		}
	}

	/** 
	 * Turn error reporting on or off
	 * 
	 * @param b
	 */
	public static void errorOn(boolean b) {
		Outputter.errorReporting = b;

	}
}
