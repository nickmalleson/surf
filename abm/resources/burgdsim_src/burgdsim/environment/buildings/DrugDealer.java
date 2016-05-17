package burgdsim.environment.buildings;

import burgdsim.environment.Coord;

public class DrugDealer extends Building {
	
	public DrugDealer(Coord coord) {
		super(coord);
//		this.id = DrugDealer.UniqueID++;
	}
//	
//	public DrugDealer(Coord coord, int id, int type) {
//		super(coord, id,type);
//	}
//	
	public DrugDealer(Building b) {
		super(b);
	}
	

	@Override
	public String toString() {
		return "DrugDealer "+this.id;
	}
	
	

}
