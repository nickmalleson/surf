package burgdsim.main;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;


import burgdsim.pecs.*;

import com.panayotis.gnuplot.JavaPlot;
import com.panayotis.gnuplot.dataset.ArrayDataSet;


/**
 * Draws graphs of how a motive changes over time assuming the value of the variable remains constant.
 * 
 * @author Nick Malleson
 *
 */
public class Grapher {

	public static void main(String args[]) {
		
        Hashtable<Integer, Double> times = SocialV.getMotiveLookupTable();
        Set<Integer> keys = times.keySet();
        Iterator<Integer> keyIt = keys.iterator();
		double[][] values = new double[times.size()][2];
		for (int i=0; i<keys.size(); i++) {
			int key = keyIt.next();
			values[i] = new double[]{(double)key, (Double)times.get(key)};
		}
		
        JavaPlot p = new JavaPlot();
        p.addPlot(new ArrayDataSet(values));
		p.plot();
			        
		
	}
	
}
