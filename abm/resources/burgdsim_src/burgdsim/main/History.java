package burgdsim.main;

import java.util.ArrayList;
import java.util.List;

import com.panayotis.gnuplot.JavaPlot;
import com.panayotis.gnuplot.plot.AbstractPlot;
import com.panayotis.gnuplot.style.PlotStyle;
import com.panayotis.gnuplot.style.Style;


import burgdsim.burglars.Burglar;
import burgdsim.pecs.StateVariable;

/**
 * The History class is designed to store the entire history of the simulation. It collects information about
 * the agents and the environment and can output information in different formats.
 * @author Nick Malleson
 *
 */
public class History {

	private static boolean firstrun = true;
	private static List<List<List<String[]>>> burglarHist ; // a multi-dimensional list of burglars, iterations and "name", "value" pairs of variables

	private static List<Burglar> burglars;
	
	private static final boolean SINGLE_GRAPH = false; // Draw all graphs in one window?
	
	public History() {
		// Reset any static variables in case they have already been used by a previous simulation
		if (burglarHist != null) {
			burglarHist.clear();
			burglarHist = null;
		}
		if (burglars != null) {
			burglars.clear();
			burglars = null;
		}
		firstrun = true;
	}

	/**
	 * The step method collects all the required information at every iteration.
	 */
	public void step() {
		if (firstrun) { // Need to initialise the arrays
			burglarHist = new ArrayList<List<List<String[]>>>();
			burglars = new ArrayList<Burglar>();
			// Store every burglar in the list
			for (Burglar b:GlobalVars.BURGLAR_ENVIRONMENT.getAllObjects(Burglar.class)) {
				burglars.add(b);
				burglarHist.add(new ArrayList<List<String[]>>());
			}
			firstrun = false;
		}
		// Add each burglar's information for this iteration.
		Burglar b;
		for (int i=0; i<burglars.size(); i++) {
			b = burglars.get(i);
			List<String[]> variables = new ArrayList<String[]>();
			// Add the burglar's wealth, action guiding motive and current action.
			variables.add(new String[] {"CurrentAction", b.getCurrentAction().toString()});
			variables.add(new String[] {"ActionGuidingMotive", b.getActionGuidingMotive().getName()});
			variables.add(new String[] {"Wealth", String.valueOf(b.getWealth())});
			// Add all the values of their state variables and associated motives
			for (StateVariable s:b.getStateVariables()) { // Add state variables and motives
				variables.add(new String[]{s.getName(), String.valueOf(s.getValue())});
				variables.add(new String[]{s.getMotive().getName(), String.valueOf(s.getMotive().getIntensity())});
			}
			
			// Store all the variables for this iteration
			burglarHist.get(i).add(variables);
		}

	}

	/**
	 * Scheduled to be called at the end of the simulation
	 */
	public void end() {
		if (ContextCreator.isTrue(GlobalVars.NON_BURGLARY_SENSITIVITY_TEST_PARAMETERS.PRINT_HISTORY_TABLE)) {
			printEntireHistoryCSV();	
		}
		
//		printEntireHistory();
		
		// This will draw graphs automatically
		System.out.println("History.end() not drawing graphs.");
//		for (Burglar b:GlobalVars.BURGLAR_ENVIRONMENT.getAllObjects(Burglar.class)) {
//			drawAgentGraphs(b);
//		}
	}

	public static void printEntireHistory() {
		Burglar b;
		Outputter.describeln("Printing burglar history:");
		Outputter.describeln(burglarHist.size()+","+burglarHist.get(0).size()+","+burglarHist.get(0).get(0).size());
		for (int i=0; i<burglars.size(); i++) {
			b = burglars.get(i);
			Outputter.describeln("History for burglar "+b.getName());
			List<List<String[]>> iter = burglarHist.get(i);
			for (int j=0; j<iter.size(); j++) {
				Outputter.describeln("\tIteration: "+j);
				List<String[]> variables = iter.get(j);
				for (int k=0; k<variables.size(); k++) {
					Outputter.describeln("\t\t"+variables.get(k)[0]+", "+variables.get(k)[1]);
				} // k
			} // j
			Outputter.describeln("");
		} // i
	}
	
	/* Same as printEntireHistory() but prints data in 'csv' format */
	public static void printEntireHistoryCSV() {
		Burglar b;
		System.out.println("Printing burglar history:");
		System.out.println(burglarHist.size()+","+burglarHist.get(0).size()+","+burglarHist.get(0).get(0).size());
		for (int i=0; i<burglars.size(); i++) {
			b = burglars.get(i);
			System.out.println("History for burglar "+b.getName());
			// Do the column headers:
			System.out.print("Iteration, days, time, ");
			for (int t=0; t<burglarHist.get(i).get(0).size(); t++) {
//			for (String title:burglarHist.get(i).get(0).get(0)) {
				// Go through the first iteration, printing column headers
				System.out.print(burglarHist.get(i).get(0).get(t)[0]+",");
			}
			System.out.println();
			List<List<String[]>> iter = burglarHist.get(i); // The history for this burglar
			for (int j=0; j<iter.size(); j++) { // for every iteration
				System.out.print(j+", "+(j/((double)GlobalVars.ITER_PER_DAY))+", "+
						ContextCreator.convertToRealTime(j, GlobalVars.ITER_PER_DAY)+","); // iteration, days, time
				List<String[]> variables = iter.get(j);
				for (int k=0; k<variables.size(); k++) {
//					System.out.print(variables.get(k)[0]+", "+variables.get(k)[1]+", ");
					System.out.print(variables.get(k)[1]+", ");
				} // k
				System.out.println();
			} // j
			System.out.println();
		} // i
	}

	public void drawAgentGraphs(Burglar b) {
		// Find the history for the burglar
		boolean foundBurglar = false;
		List<List<String[]>> bHist = new ArrayList<List<String[]>>();
		for (int i=0; i<burglars.size();i++) {
			if (burglars.get(i).equals(b)) {
				bHist = burglarHist.get(i);
				foundBurglar = true;
			}
		}
		if (!foundBurglar) {
			Outputter.errorln("Error. History: drawAgentGraphs: couldn't find history for burglar "+b.getName());
			return;
		}
		// Now go through the history and draw a graph of all the variables (except those that are strings, e.g. the current action)

			// store point[][] arrays for each variable we want to plot (one for each plot)
			ArrayList<double[][]> pointsArray = new ArrayList<double[][]>();
			ArrayList<String> plotDesc = new ArrayList<String>(); // A description of the data for each plot
			ArrayList<String> actionArray = new ArrayList<String>(); // Store the agent's current action
			for (int i=0; i<bHist.size(); i++) { // Go through each iteration 
				for (int j=0; j<bHist.get(i).size(); j++) { // Go through the number of variables stored in this iteration
					if (i==0) { 
						double[][] points = new double[GlobalVars.getIteration()][2]; 
						pointsArray.add(points);
						plotDesc.add(bHist.get(0).get(j)[0]); // Add the description of this variable
					}
					try {
						pointsArray.get(j)[i][1]=Double.parseDouble(bHist.get(i).get(j)[1]); // the variable (y)
						pointsArray.get(j)[i][0]=i; // the iteration number (x)
					} catch (NumberFormatException e) {
						// This is caught if we hit a string variable (e.g. current action), add the current action to the plot
						actionArray.add(bHist.get(i).get(j)[1]);  // could check that bHist.get(i).get(j)[1]=="currentAction" in case there are > 1 string variables 
					}
				}
			}
			// Now create the charts
//			JavaPlot.getDebugger().setLevel(Debug.VERBOSE);
			JavaPlot p = new JavaPlot();
			for (int i=0; i<pointsArray.size(); i++) {
				if (!SINGLE_GRAPH) p = new JavaPlot();
				
		        p.newGraph();
				p.addPlot(pointsArray.get(i));
				p.setKey(JavaPlot.Key.TOP_RIGHT);
				//p.setKey(JavaPlot.Key.OFF);
				if (!SINGLE_GRAPH) p.setTitle(plotDesc.get(i));
				PlotStyle s = ((AbstractPlot)p.getPlots().get(0)).getPlotStyle();
				s.setStyle(Style.LINESPOINTS);
				//((AbstractPlot)p.getPlots().get(0)).set("title", plotDesc.get(i));
				
//				// Go through the actionArray and annotate the plot where the action has changed
//				String currAction = "";
//				p.set("parametric","");
//				p.set("yrange","[0,1]");
//				//((AbstractPlot)p.getPlots().get(p.getPlots().size()-1)).set("yrange","[0,1]");
//				for (int j=0; j<actionArray.size(); j++) {
//					if (!currAction.equals(actionArray.get(j))) {
//						currAction = actionArray.get(j);
//						p.addPlot(((Integer)j).toString()+",t");
//							
//					}
//				}
				if (!SINGLE_GRAPH) p.plot();
			} // for pointsArray
		if (SINGLE_GRAPH) p.plot();
 
//		catch (Exception e) {
//			System.err.println(e.getMessage());
//			e.printStackTrace();
//			
//		}
						
	}
	
	public List<List<List<String[]>>> getBurglarHist() {
		return History.burglarHist;
	}

	

}

///**
//* Each burglar has a different history object which stores all their information at each iteration.
//* <p>
//* Burglar info is stored as a List of Hashtables where the list is indexed on the iteration number, and the hashtable
//* stores variable values where the key is the name/description of the variable (e.g. SleepM or CurrentAction). The actual
//* variable value can be of any type.
//* 
//* @author Nick Malleson
//*/
//class BurglarHistory <T> {

////private Burglar burglar; // The burglar who this history is about
//private List<Hashtable<String, T>> historyList;

//public BurglarHistory() {
////this.burglar = burglar;
//this.historyList = new ArrayList<Hashtable<String, T>>();
//}

///**
//* Add some variables from an iteration.
//* 
//* @param iterationInfo The variables as a hashtable, indexed by the varibale name/description.
//*/
//public void addIteration(Hashtable<String, T> iterationInfo) {
//this.historyList.add(iterationInfo);
//}

//public Hashtable<String, T> getIterationInfo(int iteration) {
//return this.historyList.get(iteration);
//}

//public List<Hashtable<String, T>> getAllInfo() {
//return this.historyList;
//}

//}
