package burgdsim.environment.buildings;

import burgdsim.environment.Coord;

public class House extends Building {
	
	public House() {
		super();
	}
	
	public House(Coord coord) {
		super(coord);
	}
	
	public House(Building b) {
		super(b);
		this.accessibility = b.accessibility;
		this.visibility = b.visibility;
		this.security = b.security;
		this.const_tv = b.const_tv;
	}
	

	@Override
	public String toString() {
		return "House "+this.id;
	}
	
	

}
