package burgdsim.main;

import java.io.File;
import java.util.ArrayList;

import mpi.MPI;
import mpi.Request;
import mpi.Status;

import burgdsim.scenario.Scenario;
import burgdsim.scenario.ScenarioGenerator;

/**
 * Used to run models on a grid using MPJ. The BurgdSimRunner class is used to actually run the
 * model, this class is used to control running a number of BurgdSim models over numerous nodes.
 * <p>
 * The "master" process is the one which is designated rank 0 by MPJ. This process delegates models
 * to all other nodes via MPJ message passing.
 * 
 * @see BurgdSimRunner
 * @author Nick Malleson
 */
public class BurgdSimMainMPJ {
		
	/** The number of nodes available for processing */
	private int nodes;
	
	/** The number of processes which will be executed */
	private int jobs = 10;
	
	/** The unique rank of a node (assigned in ascending order from 0)*/
	private int rank;
	
	private final int tag = 50; // Used to identify multiple incoming messages
	
	private Logger log;	// Used for logging stuff from this job
	
	/** Used by other classes to tell a node to terminate the current model (i.e. error has occurrec)*/
	private static boolean stopSim = false;

	
	/* Messages are passed as an array of objects. Element 0 is an integer telling the receiver
	 * something and remaining objects contain data (e.g. the model name a slave should execute). */
	/* Some messages which can be passed around between the master and the slaves */
	/** Indicates that a slave should finish processing, it won't be given any other jobs */
	private static final int EXIT = 1;
	/** Tell a slave that it should run a model (it will also be passed a String name in the Object[]*/
	private static final int RUN_MODEL = 2;
	/** Tell the master the model was run successfully. */
	private static final int MODEL_SUCCESS = 3;
	/** Tell the master that something went wrong with the model */
	private static final int MODEL_FAILURE = 4;
	/** Tell the master there was a problem but not model failure */
	private static final int UNSPECIFIED_PROBLEM = 5;
	
	public BurgdSimMainMPJ(String args[]) {

//        this.log = new Logger(this.rank);
        MPI.Init(args);
        this.nodes = MPI.COMM_WORLD.Size();
        this.rank = MPI.COMM_WORLD.Rank() ;
        if(this.rank == 0) { // This is the master
        	this.master();
        }
        else { // This is a slave
        	this.slave();
        }
        // TODO Not sure if all nodes should call MPI.Finalise() or just master node
        log.log("Finished, closing logs and running MPI.Finalise()");
        log.closeLog();
        MPI.Finalize();	
//        log.log("Finished, closing logs "+(this.rank==0 ? "and running MPI.Finalise()":""));
//        log.closeLog();
//        if (this.rank==0) { // Only master should call MPI.Finalize as slaves will finish at different times.
//            MPI.Finalize();	
//        }
	}
	

	/** The master node sends messages to slaves telling them to run models and collects success / failure */
	private void master() {
//    	this.printClasspath(); // for testing
    	try {
            this.log = new Logger(this.rank, this.findNewLogDir());
    		log.log("Found "+this.nodes+" available nodes to use (including master)");
    		double startTime = System.currentTimeMillis();
    		/* Set up the scenarios which will be run */
    		ScenarioGenerator sg = new ScenarioGenerator();
    		this.jobs = sg.getNumScenarios();

    		// Send messages around in Object arrays (could use same array but easier to understand if two used).
    		int jobsSent = 0; // No. jobs sent, used as an index to get scenarios from the ScenarioGenerator
    		int jobsCompleted = 0; // completed jobs (return a message to master)
    		Object[] output = new Object[2]; // For sent messages
    		Object[] input = new Object[2]; // For received messages
    		// First check that there aren't more processors than jobs, if so then excess processors can exit.
    		if (this.jobs < this.nodes-1) { // nodes-1 because one is reserved for master
    			log.log("More nodes than jobs, will kill some processes");
    			output[0] = EXIT; // Send an exit message to the unrequired nodes
    			output[1] = new String("test");
    			for (int i=jobs+1; i<this.nodes; i++) {
    				log.log("Sending exit message to node "+i+": "+this.translateMessage(i));
    				MPI.COMM_WORLD.Isend(output, 0, output.length, MPI.OBJECT, i, tag) ;
    			}
    			this.nodes = this.jobs+1;
    		}
    		log.log("Have "+this.jobs+" jobs and "+this.nodes+" nodes (including one for master).");
    		Request[] recvRequests = new Request[this.nodes-1]; // Array to store responses from slaves

    		// Send out the first batch of processing
    		for (int i=1; i<this.nodes; i++) { // (not not using jobs+1 because only jobs-1 jobs available for processing, 1 is used for master)  
    			output[0] = RUN_MODEL; // Send message to run a model
    			Scenario s = sg.getScenario(jobsSent); // Also Send the scenario
    			output[1] = s;
//    			log.log("Sending scenario: '"+((Scenario)output[1]).toString()+"' to node "+i+"...");`
    			MPI.COMM_WORLD.Isend(output, 0, output.length, MPI.OBJECT, i, tag);

    			// Listen for responses
    			recvRequests[i-1] = MPI.COMM_WORLD.Irecv(input, 0, input.length, MPI.OBJECT, i, tag ) ; 
    			log.log("Sent '"+output[0].toString()+"' to "+i+//" with scenario: "+((Scenario)output[1]).toString()+
    					"\nNow waiting for 1 minute...");
    			synchronized (this) {
    				this.wait(60000);
    				log.log("...finished waiting");
    			}
    			jobsSent++;
    			// XXXX what happens if a job returns a recieve message here?
    		}
    		log.log("Sent initial batch of messages");

    		// Farm out remaining jobs as others finish
    		Status status = null ; // The status of the master
    		while (jobsCompleted < this.jobs) {
    			status = Request.Waitany( recvRequests ); // Block until a request has completed
    			// If get here then have received a message from one of the dispatched jobs
    			int source = status.source; // The source of the recieved message
    			int message = (Integer) input[0];
    			// TODO Process the message here, could be MODEL_FAILURE
    			log.log("Received message '"+this.translateMessage(message)+"' from: "+
    					source+" (jobs completed/sent: "+jobsCompleted+","+jobsSent+")");
    			jobsCompleted++;
    			if (jobsSent < this.jobs) { // Still have more jobs to dispatch
    				output[0] = RUN_MODEL;
    				output[1] = sg.getScenario(jobsSent);
    				MPI.COMM_WORLD.Isend(output, 0, output.length-1, MPI.OBJECT, source, tag) ;
    				
    				recvRequests[source-1] = MPI.COMM_WORLD.Irecv(input, 0, input.length, MPI.OBJECT, source, tag ) ;
    				jobsSent++;                	
    			} 
    			else { // No more jobs, tell the slave to exit
    				output[0] = EXIT;
    				MPI.COMM_WORLD.Isend(output, 0, output.length, MPI.OBJECT, source, tag) ;
    			}
    		}
    		log.log("All jobs have been completed and slaves told to exit, finishing. " +
    				"Total time was "+((System.currentTimeMillis()-startTime)/3600000)+" decimal hours");
    		
    	} catch (Exception e) {
    		// If catch an error then program flow will return to constructor, call MPI.Finalize() and exit
    		log.log("Master caught an exception, can't continue. Message: '"+e.getMessage()+"'." +
    				" Cause: '"+e.getCause().toString()+"'"+
    				". Logging stack trace.");
    		log.log(e.getStackTrace());
    		// Tell all slaves to exit
    		Object[] output = new Object[2];
    		output[0] = EXIT;
    		for (int i=1; i<this.nodes; i++) {	
				MPI.COMM_WORLD.Isend(output, 0, output.length, MPI.OBJECT, i, tag) ;
    		}
    		MPI.Finalize();
    		System.exit(1);
    	}
	}
	
	/** Slave nodes run models. The wait for a message from the master which tells them the name of the model
	 * they are to run and they return either success or failure when they have finished */
    private void slave() {
        Object[] input = new Object[2]; // For receiving messages
        Object[] output = new Object[2]; // For sending messages back
    	boolean finished = false; // Will keep waiting to run models until told to stop by the master
//    	int count = 0;	// A counter to keep track of how many models this process has created
    	while (!finished) { 
    		try {
    			// Wait for messages from the master (blocking receive)
    			MPI.COMM_WORLD.Recv(input, 0, input.length, MPI.OBJECT, 0, tag) ;
    			// Create the logger for the slave, done here to ensure master has created subdirectory for logs
    			if (this.log == null) {
    		        this.log = new Logger(this.rank, this.findNewLogDir());
    			}
    			// Have recieved a message, process it:
    			int message = ((Integer)input[0]);
    			log.log("Received message: '"+this.translateMessage(message)+"'");
    			
    			if (message==EXIT) { // The message from the master is to quit
    				log.log("Master sent EXIT message so exiting...");
    				finished = true;
    			}
    			
    			else if (message==RUN_MODEL ){
    				Scenario s = (Scenario) input[1]; // Master should have included a scenario with the message
    				log.log("Message is to run a model");
    				
    				// Tell the scenario to use this logger to output data
    				s.setLogger(log);
    				
    				// Run a model using the scenario
    				boolean success = this.runModel(s);
    				
    				// Finished working, send result (or at least an acknowledgement) to master
    				log.log("Finished model, telling master. Current memory usage: (total,free,max), "+
    						Runtime.getRuntime().totalMemory()+", "+
    						Runtime.getRuntime().freeMemory()+", "+
    						Runtime.getRuntime().maxMemory());
    				int msg = ( success ? MODEL_SUCCESS : MODEL_FAILURE );
    				output[0] = msg;
    				MPI.COMM_WORLD.Send( output, 0, output.length, MPI.OBJECT, 0, tag );
    				log.log("Sent response '"+this.translateMessage(msg)+"' to master.");
    			} // else if RUN_MODEL
    			else { // Didn't understand the master's message
    				log.log("Unrecognised message from master: "+this.translateMessage(message)+"("+message+")");
    				output[0] = UNSPECIFIED_PROBLEM;
    				MPI.COMM_WORLD.Send( output, 0, output.length, MPI.OBJECT, 0, tag );
    			}
    		} catch (Exception e) { // Caught an exception running model, send error to master
    			log.log("MPIMain.slave() Caught an exception: "+e.getClass()+", "+e.getMessage());
    			log.log(e.getStackTrace());
				output[0] = MODEL_FAILURE;
				// TODO Send specific info about the problem ? 
				MPI.COMM_WORLD.Isend( output, 0, output.length, MPI.OBJECT, 0, tag );
    		} // try/catch
    	} // while !finished
    }

    
//    private void printClasspath() {
//		System.out.println("***************** PRINT THE CLASSPATH ****************");
//        //Get the System Classloader
//        ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();
//        //Get the URLs
//        URL[] urls = ((URLClassLoader)sysClassLoader).getURLs();
//        for(int i=0; i< urls.length; i++)
//            System.out.println(urls[i].getFile());
//        System.out.println("*************************** ***************************");
//    }

	public static void main (String[] args) {
		new BurgdSimMainMPJ(args);
	}
	
	/**
	 * Run the model represented by the given scenario
	 * @param s The scenario which defines the model.
	 * @return True if the model completed successfully, false otherwise.
	 * @throws Exception
	 */
	private boolean runModel(Scenario s) throws Exception {
		boolean success = true; // Assume model succeeded
		log.log("*********************** STARTING NEW SIM *********************");
		BurgdSimRunner runner = new BurgdSimRunner();
		File file = new File("burgdsim.rs"); // the scenario dir
		runner.load(file);	// load the repast scenario (contexts, projections etc)
		s.execute(); 		// This will set the GlobalVars
		log.log("Running Scenario: "+s.toString());

//		//		for(int i=0; i<numRuns; i++){		
		
		runner.runInitialize();  	// ContextCreator.build() called here.
		log.log("*********************** INITIALISED *********************");
		// For some reason this next line doesn't cause sim to terminate properly, the "end" functions are called
		// but sim keeps running and breaks. So manually check if tickCount>endTime in while loop
		//			RunEnvironment.getInstance().endAt(endTime);
		log.log("BurgdSimMainMPJ: will end runs at "+GlobalVars.RUN_TIME+" iterations");
		double endTime = GlobalVars.RUN_TIME;
		
		/* RUN THE SIMULATION */				
//		while (runner.getActionCount() > 0){  // loop until last action is left
//			double ticks = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
//			if (runner.getModelActionCount() == 0 || ticks > endTime) {
//				runner.setFinishing(true);
//			}
//			runner.step();  // execute all scheduled actions at next tick
//			if (ticks % 500 == 0) {
//				System.out.println(ticks+", ");
//			}
//
//		}
		// Use a tick counter to determine when to stop sim rather than checking how many actions remain
		double ticks = 0;
		while (ticks <= endTime){  // loop until last action is left
			if (BurgdSimMainMPJ.stopSim) { // Another class has set this, probably because an error has occurred
				ticks=endTime;
				success = false;
				log.log("BurgdSimMainMPJ has been told to stop, terminating this run.");
				BurgdSimMainMPJ.stopSim = false; // reset the boolean ready for the next run
				// TODO SHould report to master that an error occurred, at the moment it will report success
			}
			if (ticks == endTime) {
				log.log("BurgdSimMain, last tick, setting finishing");
				runner.setFinishing(true);
			}
			runner.step();  // execute all scheduled actions at next tick
			if (ticks % 1000 == 0) {
				log.log(ticks+", ");
			}
			ticks++;
		}
		log.log("\n***********************STOPPING CURRENT SIM*********************");
		runner.stop();          // execute any actions scheduled at run end
		runner.cleanUpRun();
	//} // for numRuns 
		log.log("*********************** FINISHING RUN *********************");
		runner.cleanUpBatch();    // run after all runs complete
		return success;
	}

	/** Logs are stored in subdirectories (under the log/ parent dir). Each mpj run is given a new, ascending
	 * order, integer subdirectory, so that if a few mpj jobs are started at once the logs will not interfere with
	 * each other. This function finds an appropriate name for the subdirectory for this run. E.g. if this is the
	 * fourth job executed the log will be in /log/3/*.
	 * <p> Because each node will have its own instance of this class, static variables can't be used to make sure
	 * all nodes from the same run put their logs in the same directory. Instead, directories are found as follows:
	 * <ul>
	 * <li>If this node is a master the first thing it does is create a new sub-directory, one integer higher
	 * than any others</li>
	 * <li>If this node is a slave it finds the highest integer directory and uses that one to store logs. Slaves
	 * don't create their logger until they have received their first message from the master, which ensures that
	 * the master will have created the appropriate directory.</li>
	 * </ul> */
	private String findNewLogDir() {
		// Find the highest numbered directory
		ArrayList<Integer> dirNumbers = new ArrayList<Integer>(); 
		for (String s:new File("log/").list()) { // Iterate over all directories in log/
			File f = new File("log/"+s);
			if (f.isDirectory()) {
				try {
					// See if the directory name is a number
					Integer i = Integer.parseInt(f.getName());
					dirNumbers.add(i);
				} catch (NumberFormatException e){ } // Directory isn't a number
			}
		}
//		File newDir = ""; // The new directory to store logs in.
		Integer highest = 0;
		for (Integer i:dirNumbers) {
			if (i>highest) {
				highest = i;
			}
		}
		// Have found current highest directory]
		if (this.rank==0) { // this node is the master, create the new sub-directory.
			String dir = new Integer(highest+1).toString();
			new File("log/"+dir).mkdir();
			return dir;
		}
		else { // this node is a slave, use the highest dir (this will just have been created by master)
			String dir = new Integer(highest).toString();
			return dir;			
		}
	}


	/** Can be called by other classes to tell the current node to stop executing. */
	public static void stopSim() {
		BurgdSimMainMPJ.stopSim = true;		
	}
	
	/** Translate a numerical message (passed between master and slaves) into textual meaning */
	private String translateMessage(int message) {
		if (message==EXIT)
			return "EXIT";
		else if (message==RUN_MODEL)
			return "RUN_MODEL";
		else if (message==MODEL_SUCCESS)
			return "MODEL_SUCCESS";
		else if (message==MODEL_FAILURE )
			return "MODEL_FAILURE";
		else if (message == UNSPECIFIED_PROBLEM)
			return "UNSPECIFIED_PROBLEM";
		else
			return "UNTRANSLATABLE MESSAGE";
	}
}
