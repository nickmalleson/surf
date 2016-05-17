package burgdsim.main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

/** Used to log messages from MPJ. This will be replaced by Outputter... */
public class Logger {

	private int rank;
	private static Calendar calendar ; // FOr logging time.

	private File logFile ;
	private FileWriter logFileWriter;
	
	private String directory ; // The subdir to store new scenario in.

	public Logger(int id, String directory) {
		this.rank = id;
		this.directory = directory;
	}
	/**
	 * Used to write any output, saves to mpjlog.txt file.
	 * @param s
	 */

	public void log(String s) {
		calendar = new GregorianCalendar();
		String time = twoDigits(calendar.get(Calendar.HOUR_OF_DAY))+":"+
			twoDigits(calendar.get(Calendar.MINUTE))+":"+twoDigits(calendar.get(Calendar.SECOND));
//		System.out.println(time+"- Proc "+this.rank+" logs: "+s);
		try {
			if (logFile==null) {
				logFile = new File("log/"+this.directory+"/mpjlog"+this.rank+".txt");
				if (!logFile.exists()) {
					logFile.createNewFile();
				}
				logFileWriter = new FileWriter(logFile);
			}
			logFileWriter.write(time+"("+GlobalVars.MODEL_ID+"): "+s+"\n");
			System.out.println(s);	// So output goes to standard out as well
			logFileWriter.flush(); 	// So that output written to log immediately
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	/** Makes sure the input has two digits.
	 * 
	 * @param i The input number
	 * @return i if i>9 or "0"+i if i<9
	 */
	private String twoDigits(int i) {
		if (i<9)
			return "0"+i;
		else
			return i+"";
	}
	/**
	 * Convenience function Used to log stack traces, calls log(String) saves to mpjlog.txt file.
	 * @param s
	 * @see log(String s)
	 */
	public void log(StackTraceElement[] s) {
		for (StackTraceElement e:s) {
			log(e.toString());
		}    	
	}

	public void closeLog() {
		if (this.logFileWriter!=null) {
			try {
				this.logFileWriter.close();
				this.logFileWriter=null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

//	public void flush() {
//		if (this.logFileWriter!=null) {
//			try {
//				this.logFileWriter.flush();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//	}
}
