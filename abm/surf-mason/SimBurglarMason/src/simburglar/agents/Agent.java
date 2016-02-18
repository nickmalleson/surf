/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simburglar.agents;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import com.vividsolutions.jts.planargraph.Node;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.geo.GeomPlanarGraphDirectedEdge;
import sim.util.geo.GeomPlanarGraphEdge;
import sim.util.geo.MasonGeometry;
import sim.util.geo.PointMoveTo;
import simburglar.GISFunctions;
import simburglar.GlobalVars;
import simburglar.SimBurglar;
import simburglar.exceptions.RoutingException;

/**
 * A basic agent that can be used as a template for other, more complicated
 * agents. The only thing this agent will do is walk randomly from house to
 * house.
 *
 * @author Nick Malleson
 */
public class Agent implements Steppable, Serializable {

    private static int uniqueID = 0;
    protected int id = ++Agent.uniqueID;
    private static final long serialVersionUID = -1113018274619047013L;
    private static final Logger LOG = Logger.getLogger(Agent.class);
    // point that denotes agent's position
    protected MasonGeometry location;
    // agent's home location
    protected MasonGeometry home;
    // The base speed of the agent (currently the same for all agents and set from a model parameter).
    protected double moveRate;
    private static double baseMoveRate; // The class-level move rate
    // Used by agent to walk along line segment; assigned in setNewRoute()
    protected LengthIndexedLine segment = null;
    protected double startIndex = 0.0; // start position of current line
    protected double endIndex = 0.0; // end position of current line
    protected double currentIndex = 0.0; // current location along line
    protected MasonGeometry destination = null; // The point that the agent is heading towards
    protected boolean atDestination = false;
    // A list of roads that neeed to be followed to get to the destination
    List<GeomPlanarGraphDirectedEdge> path = new ArrayList<GeomPlanarGraphDirectedEdge>();
    // useful for graph
    int indexOnPath = 0;
    int pathDirection = 1;
    int linkDirection = 1;

    static {

        Agent.baseMoveRate = Double.parseDouble(GlobalVars.getProperty("BaseMoveRate"));
        LOG.debug("Setting base move rate for all agents to "+Agent.baseMoveRate);

    }

    /**
     * Create a burglar with a randomly chosen starting position.
     *
     * Will find a random building and use the constructor
     * <code>Burglar(state, building)</code>
     *
     * @param state
     */
    public Agent(SimBurglar state) {

        // Find a home for the burglar (randomly selected building)
        this(state, Agent.getRamdomBuilding(state));
    }

    public Agent(SimBurglar state, MasonGeometry home) {

        this.home = home;

        // Set their move rate
        this.moveRate = Agent.baseMoveRate;

        // Make their current location their home

        this.location = new MasonGeometry(this.home.getGeometry().getCentroid());

        location.isMovable = true;

        // Now set up attributes for this agent

//            location.addStringAttribute("TYPE", "STUDENT");
//
//            int age = (int) (20.0 + 2.0 * state.random.nextGaussian());
//
//            location.addIntegerAttribute("AGE", age);
    }

    @Override
    public void step(SimState s) {
        SimBurglar state = (SimBurglar) s;
        try {


            if (this.destination == null || this.atDestination) { // Only occurs at the beginning of simulation
                this.destination = Agent.getRamdomBuilding(state);
                this.atDestination = false;
                this.path = Agent.findNewAStarPath(state, this);
//                LOG.info("Reached destination, now travelling to " + this.destination.toString());

                // Have created a new path, set up the edge so the agent knows how to get around.
                GeomPlanarGraphEdge edge = (GeomPlanarGraphEdge) path.get(0).getEdge();
                Agent.setupEdge(edge, this);
                // Move the agent onto the start of the path and then stop.
                Agent.moveToCoordinate(segment.extractPoint(currentIndex), this);
                return;
            }


            // check that we've been placed on an Edge
            // (It is ok for segment to be null in the cases where destination and origin are the same).
            assert !(segment == null && this.path.isEmpty()) :
                    "Segment empty and paths null for agent " + this.toString() + " whose home is "
                    + getHomeID(state) + " and destination " + state.buildingIDs.inverse().get(this.destination);
            
            this.moveAlongPath();


        }
        catch (RoutingException ex) {
            LOG.error("Error routing agent " + this.id + ". Exitting.", ex);
            state.finish();
        }
    }
    
    protected void moveAlongPath() {
                    // move along the current segment
            currentIndex += moveRate * linkDirection;

            // check to see if the progress has taken the current index beyond its goal
            // given the direction of movement. If so, proceed to the next edge
            if (linkDirection == 1 && currentIndex > endIndex) {
                this.atDestination = transitionToNextEdge(currentIndex - endIndex);
            }
            else if (linkDirection == -1 && currentIndex < startIndex) {
                this.atDestination = transitionToNextEdge(startIndex - currentIndex);
            }
            Coordinate currentPos = segment.extractPoint(currentIndex);
            moveToCoordinate(currentPos, this);
    }

    /**
     * Plots a path between the Agent's home Node and its work Node
     *
     * @param state The SimState
     * @param location The current location of the agent.
     * @param destination The destination
     * @param burglar The burglar who is needs the path.
     */
    protected static List<GeomPlanarGraphDirectedEdge> findNewAStarPath(
            SimBurglar state, Agent burglar) throws RoutingException {


        MasonGeometry location = burglar.location, destination = burglar.destination;

        /* First, find the nearest junction to our current position. */

        Node currentJunction = state.network.findNode(location.getGeometry().getCoordinate());

        // Not exactly on a junction, find the nearest and move onto it
        if (currentJunction == null) {
            MasonGeometry nearestJunction = GISFunctions.findNearestObject(location, state.junctions, state);
            currentJunction = state.network.findNode(nearestJunction.getGeometry().getCoordinate());
            // XXXX when should move agent onto the junction?
//            this.moveTo(currentJunction.getCoordinate());
        }
//        assert currentJunction != null : "Could not find a junction for agent " + this.id + " at " + location;

        /* Now find the junction that is closest to the destination */

        Node destinationJunction = state.network.findNode(destination.getGeometry().getCoordinate());
        if (destinationJunction == null) {
            MasonGeometry nearestJunction = GISFunctions.findNearestObject(destination, state.junctions, state);
            destinationJunction = state.network.findNode(nearestJunction.getGeometry().getCoordinate());
        }
        assert destinationJunction != null : String.format("Could not find a junction for the destination %s for agent %s", destination, burglar.toString());

        if (currentJunction == destinationJunction) {
            // Bit of a hack, just add a single edge to the path, otherwise the algorithm breaks later.
            // This will make the agent do some odd routing but only happens rarely.
            LOG.warn("Current and destination junctions are same for agent " + burglar.toString());
            final GeomPlanarGraphDirectedEdge e = (GeomPlanarGraphDirectedEdge) currentJunction.getOutEdges().getEdges().get(0);
            return new ArrayList<GeomPlanarGraphDirectedEdge>() {
                {
                    add(e);
                }

            };
        }

//        assert currentJunction != destinationJunction : "Current and destination junctions are the same";


        // find the appropriate A* path between them
        AStar pathfinder = new AStar();
        List<GeomPlanarGraphDirectedEdge> paths = pathfinder.astarPath(currentJunction, destinationJunction);

        // if the path works, lay it in
        if (paths != null && paths.size() > 0) {
            return paths;
        }
        else {
            if (paths == null) {
                // Not sure why this could happen!
                throw new RoutingException("Internal error: Could not find a path. Path is null");
            }
            else {
                throw new RoutingException("Agent " + burglar.toString() + "(home " + burglar.getHomeID(state)
                        + ", destination " + state.buildingIDs.inverse().get(destination) + ")"
                        + " got an empty path between junctions " + currentJunction + " and " + destinationJunction
                        + ". Probably the network is disconnected.");
            }

        }
    }

    /**
     * Sets the Agent up to proceed along an Edge
     *
     * @param edge the GeomPlanarGraphEdge to traverse next
     * @param burg The burglar who the edge is being set up for
     *
     */
    protected static void setupEdge(GeomPlanarGraphEdge edge, Agent b) {

        // set up the new segment and index info
        LineString line = edge.getLine();
        b.segment = new LengthIndexedLine(line);
        b.startIndex = b.segment.getStartIndex();
        b.endIndex = b.segment.getEndIndex();

        // check to ensure that Agent is moving in the right direction
        double distanceToStart = line.getStartPoint().distance(b.location.geometry),
                distanceToEnd = line.getEndPoint().distance(b.location.geometry);
        if (distanceToStart <= distanceToEnd) { // closer to start
            b.currentIndex = b.startIndex;
            b.linkDirection = 1;
        }
        else if (distanceToEnd < distanceToStart) { // closer to end
            b.currentIndex = b.endIndex;
            b.linkDirection = -1;
        }

    }

    /**
     * Transition to the next edge in the path
     *
     * @param residualMove the amount of distance the agent can still travel
     * this turn
     */
    private boolean transitionToNextEdge(double residualMove) {

        // Move the agent to their position before starting the transition
//                        Coordinate currentPos = segment.extractPoint(endIndex);
//                moveTo(currentPos, this);

        // update the counter for where the index on the path is
        indexOnPath += pathDirection;

        // check to make sure the Agent has not reached the end
        // of the path already
        if ((pathDirection > 0 && indexOnPath >= path.size())
                || (pathDirection < 0 && indexOnPath < 0))// depends on where you're going!
        {
            // Have reached the junction nearest to the destination. Move onto the final destination
            moveToCoordinate(this.destination.getGeometry().getCoordinate(), this);
            // Reset everything
            startIndex = 0.0;
            endIndex = 0.0;
            currentIndex = 0.0;
            destination = null;
            path.clear();
            indexOnPath = 0;
            return true;
        }

        // move to the next edge in the path
        GeomPlanarGraphEdge edge =
                (GeomPlanarGraphEdge) path.get(indexOnPath).getEdge();
        setupEdge(edge, this);


        double speed = residualMove * linkDirection;
        currentIndex += speed;

        // check to see if the progress has taken the current index beyond its goal
        // given the direction of movement. If so, proceed to the next edge
        if (linkDirection == 1 && currentIndex > endIndex) {
            return transitionToNextEdge(currentIndex - endIndex);
        }
        else if (linkDirection == -1 && currentIndex < startIndex) {
            return transitionToNextEdge(startIndex - currentIndex);
        }
        
        // If here then don't need to transition onto another edge and haven't
        // reached the destination yet.
        return false;
        
    }

    // Convenience for moving a point. Don't want to create this object each iteration.
    private PointMoveTo pmt = new PointMoveTo();

    /**
     * Move the agent to the given coordinates
     *
     * @param c
     * @param b
     */
    protected static void moveToCoordinate(Coordinate c, Agent b) {
        b.pmt.setCoordinate(c);
        b.location.getGeometry().apply(b.pmt);
        b.getGeometry().geometry.geometryChanged();

    }

    /**
     * Get the agent's current location.
     *
     * @return geometry representing agent location
     */
    public MasonGeometry getGeometry() {
        return location;
    }

    /**
     * Get the agent's home location.
     */
    public MasonGeometry getHome() {
        return this.home;
    }

    /**
     * Convenience to get the building id of this agent's home. Same as:
     * <code>state.buildingIDs.inverse().get(this.home)
     * <code>
     *
     * @return
     */
    public int getHomeID(SimBurglar state) {
        return state.buildingIDs.inverse().get(this.home);
    }

    @Override
    public String toString() {
        return "Burglar " + this.id;
    }

    protected static MasonGeometry getRamdomBuilding(SimBurglar state) {
        return (MasonGeometry) state.buildings.getGeometries().get(state.random.nextInt(state.buildings.getGeometries().size()));
    }

}
