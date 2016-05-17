package burgdsim.main;

import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.ShortestPath;

public class MyShortestPath<T> extends ShortestPath<T> {
	
	static int unique = 0;
	int id = 0;

	public MyShortestPath(Network<T> net, T source, String desc) {
		super(net, source);
		this.desc = desc;
		this.id = unique++;
		
		System.out.println("Creating "+id);
		// TODO Auto-generated constructor stub
	}
	
	public MyShortestPath(Network<T> net, String desc) {
		super(net);
		this.desc = desc;
		this.id = unique++;
		System.out.println("Creating "+id);
		// TODO Auto-generated constructor stub
	}
	String desc;
	/* (non-Javadoc)
	 * @see repast.simphony.space.graph.ShortestPath#finalize()
	 */
	@Override
	public void finalize() {
		System.out.println("Finalising shortest path "+id);
		// TODO Auto-generated method stub
		super.finalize();
	}



	
	
}
