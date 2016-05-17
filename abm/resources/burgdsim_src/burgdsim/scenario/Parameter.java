package burgdsim.scenario;

import java.io.Serializable;

public abstract class Parameter<T> implements Serializable  {

	private static final long serialVersionUID = 1L;
	protected String name;
	protected T value;		// The current (or default) value
	protected T initVal;		// Initial value (for dynnamic Parameters)
	protected T finalValue;	// Final value (for dynnamic Parameters)
	protected T increment;	// Increment amount (for dynnamic Parameters)
	private boolean dynamic; // Whether or not the parameter will be sensitivity tested.

	/**
	 * Create a parameter which will not be used in a batch run, it only has a single value.
	 * @param name
	 * @param value
	 */
	public Parameter(String name, T value) {
		this.name = name;
		this.value = value;
		this.dynamic = false;
	}
	
	/**
	 * Create a new parameter which can be used in a batch run.
	 * @param name
	 * @param value
	 * @param initVal
	 * @param finalValue
	 * @param increment
	 */
	public Parameter(String name, T value, T initVal, T finalValue, T increment) {
		this.name = name;
		this.value = value;
		this.initVal = initVal;
		this.finalValue = finalValue;
		this.increment = increment;
		this.dynamic = true;
	}

	public String getName() {
		return name;
	}
	
	public T getValue() {
		return value;
	}

//	public T getInitVal() {
//		return initVal;
//	}
//
//	public T getFinalValue() {
//		return finalValue;
//	}
//
//	public T getIncrement() {
//		return increment;
//	}
	public boolean isDynamic() {
		return this.dynamic;
	}
	
	@Override
	public String toString() {
		return "Parameter "+this.name+(this.dynamic ? " (dynamic): " :": ")+this.value.toString();
	}
	
	/**
	 * Return true if this Paramters are the same as those of the given object.
	 */
	@Override
	public boolean equals(Object obj) {
//		System.out.print("Parameter.equals() comparing: "+obj.toString()+" to "+this.toString()+": ");
		if (!(obj instanceof Parameter)) {
			System.out.println("false");
			return false;
		}
		Parameter<?> p = (Parameter<?>) obj;
		if (	p.name.equals(this.name) && 
				p.value.equals(this.value) && 
				p.initVal.equals(this.initVal) && 
				p.increment.equals(this.increment) &&
				p.finalValue.equals(this.finalValue) ) {
//			System.out.println("true");
			return true;
		}
		else {
			System.out.println("false");
			return false;	
		}
		
	}
	
	/**
	 * Return true if this Parameter has the same name as the given parameter. This can be used to compare
	 * a dynamic Parameter to the static Parameters that are created when it is expanded.
	 *  are the same as those of the given object.
	 */
	public boolean same(Parameter<?> p) {
//		System.out.print("Parameter.same() comparing: "+p.toString()+" to "+this.toString()+": ");
		if (p.getName().equals(this.getName())) {
//			System.out.println("true");
			return true;
		}
			
		else {
//			System.out.println("false");
			return false;
		}
	}
}
