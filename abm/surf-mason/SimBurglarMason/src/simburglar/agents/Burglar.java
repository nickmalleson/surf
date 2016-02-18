/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simburglar.agents;

import org.apache.log4j.Logger;
import sim.engine.SimState;
import sim.util.geo.GeomPlanarGraphEdge;
import sim.util.geo.MasonGeometry;
import simburglar.SimBurglar;
import simburglar.exceptions.RoutingException;

/**
 *
 * @author Nick Malleson
 */
public class Burglar extends Agent {

    private static final Logger LOG = Logger.getLogger(Burglar.class);
    protected boolean goingHome = true; // Whether the agent is on the way home or not

    public Burglar(SimBurglar state) {

        super(state);
    }

    public Burglar(SimBurglar state, MasonGeometry home) {
        super(state, home);
    }

    @Override
    public void step(SimState s) {
        SimBurglar state = (SimBurglar) s;
        try {

            // Need a new route
            if (this.destination == null || this.atDestination) {

                if (goingHome) { // Have arrived at home, now go to a randomly chosen house
                    this.destination = Agent.getRamdomBuilding(state);
                    goingHome = false;
//                    LOG.info(this.toString()+" has reached home. Going somewhere else");
                }
                else { // Have reached destination, go home
                    this.destination = this.home;
                    goingHome = true;
//                    LOG.info(this.toString()+" has reached destination. Going home");
                }

                this.atDestination = false;
                this.path = Agent.findNewAStarPath(state, this);
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

}
