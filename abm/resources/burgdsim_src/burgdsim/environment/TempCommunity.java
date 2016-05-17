package burgdsim.environment;

/** A temporary class to used in BurglarFactory to read the number of burglars in each community using the 
 * ShapefileLoader.
 * @author Nick Malleson
 *
 */
public class TempCommunity {
	private int id;
	private int BglrC1, BglrC2, BglrC3, BglrC4, BglrC5;
	
	public TempCommunity() {}
	
	public int getId() { 
		return id; 
	}
	public void setId(int id) { 
		this.id = id;
	}
	public int getBglrC1() { 
		return BglrC1; 
	}
	public void setBglrC1(int bglrC1) { 
		this.BglrC1 = bglrC1; 
	}
	public int getBglrC2() { 
		return BglrC2; 
	}
	public void setBglrC2(int bglrC2) { 
		this.BglrC2 = bglrC2; 
	}
	public int getBglrC3() { 
		return BglrC3; 
	}
	public void setBglrC3(int bglrC3) { 
		this.BglrC3 = bglrC3; 
	}
	public int getBglrC4() { 
		return BglrC4; 
	}
	public void setBglrC4(int bglrC4) {
		this.BglrC4 = bglrC4; 
	}
	public int getBglrC5() { 
		return BglrC5; 
	}
	public void setBglrC5(int bglrC5) {
		this.BglrC5 = bglrC5; 
	}

}