package burgdsim.scenario;

import java.util.ArrayList;
import java.util.List;

import burgdsim.main.Outputter;

public class NumericParameter<T extends Number> extends Parameter<T> {

	private static final long serialVersionUID = 1L;

	public NumericParameter(String name, T value, T initVal, T finalValue, T increment) {
		super(name, value, initVal, finalValue, increment);
	}

	public NumericParameter(String name, T value) {
		super(name, value);
	}

	/**
	 * Expand this dynamic Parameter, creating a new static Parameter for every possible value. If this
	 * NumericParamter is not dynamic (only has one possible value) then the list will be size 1.
	 * @return
	 */
	public List<Parameter<?>> expand() {
		
		List<Parameter<?>> newParams = new ArrayList<Parameter<?>>(); 
		// Need to work out whether the new paramter should be a double or an int.

		if (this.value instanceof Integer) {
			int value = this.initVal.intValue();
			int increment = this.increment.intValue();
			int fin = this.finalValue.intValue();
//			while (value<this.finalValue.intValue()) {
			while (value<fin) {
				Parameter<?> p = new NumericParameter<Integer>(this.name, value);
				newParams.add(p);
				value+=increment;
			}
			// Add the final value
			Parameter<?> p = new NumericParameter<Integer>(this.name, fin);
			newParams.add(p);
		}
		else if (this.value instanceof Double) {
			double value = this.initVal.doubleValue();
			double increment = this.increment.doubleValue();
			double fin = this.finalValue.doubleValue();			
//			while (value<this.finalValue.intValue()) {
			while (value<fin) {
				Parameter<?> p = new NumericParameter<Double>(this.name, value);
				newParams.add(p);
				value+=increment;
			}
			Parameter<?> p = new NumericParameter<Double>(this.name, fin);
			newParams.add(p);		}
		else {
			Outputter.errorln("NumericParameter.expand() error, have only implemented Integer or Double " +
					"NumericParameters, not: "+this.value.getClass().toString());
		}
		
		return newParams;
	}
}
