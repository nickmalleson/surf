package burgdsim.environment.buildings;

import burgdsim.environment.Coord;

public class Social extends Building {
	
	public Social(Coord coord) {
		super(coord);
	}

	public Social(Building b) {
		super(b);
	}


	@Override
	public String toString() {
		return "Social "+this.id;
	}
	
	

}
