package burgdsim.environment.buildings;

import burgdsim.environment.Coord;

public class Workplace extends Building {
	
	public Workplace(Coord coord) {
		super(coord);
	}
	
	public Workplace(Building b) {
		super(b);
	}

	@Override
	public String toString() {
		return "Workplace "+this.id;
	}
	
	

}
