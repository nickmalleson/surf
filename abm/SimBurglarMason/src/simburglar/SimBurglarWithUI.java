/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simburglar;

import com.vividsolutions.jts.io.ParseException;
import java.awt.Color;
import java.awt.Paint;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import javax.swing.JFrame;
import org.apache.log4j.Logger;
import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.SimplePortrayal2D;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.portrayal.simple.LabelledPortrayal2D;
import sim.util.geo.MasonGeometry;

/**
 *
 * @author Nick Malleson
 */
public class SimBurglarWithUI extends GUIState {

    private static Logger LOG = Logger.getLogger(SimBurglarWithUI.class);
    private Display2D display;
    private JFrame displayFrame;
    // Portrayals are used for visualising objects in fields
    private GeomVectorFieldPortrayal walkwaysPortrayal = new GeomVectorFieldPortrayal();
    private GeomVectorFieldPortrayal buildingPortrayal = new GeomVectorFieldPortrayal();
    private GeomVectorFieldPortrayal agentPortrayal = new GeomVectorFieldPortrayal();

    public SimBurglarWithUI(SimState state) {
        super(state);
    }

    public SimBurglarWithUI() throws Exception {
        super(new SimBurglar(System.currentTimeMillis()));
    }

    @Override
    public void init(Controller controller) {
        super.init(controller);

        display = new Display2D(SimBurglar.WIDTH, SimBurglar.HEIGHT, this);

        display.attach(walkwaysPortrayal, "Walkways", true);
        display.attach(buildingPortrayal, "Buildings", true);
//        display.attach(roadsPortrayal, "Roads", true);
        display.attach(agentPortrayal, "Agents", true);

        displayFrame = display.createFrame();
        controller.registerFrame(displayFrame);
        displayFrame.setVisible(true);
    }

    @Override
    public void start() {
        super.start();
        setupPortrayals();
    }

    private void setupPortrayals() {
        SimBurglar world = (SimBurglar) state;

        walkwaysPortrayal.setField(world.roads);
        walkwaysPortrayal.setPortrayalForAll(new GeomPortrayal(Color.BLACK, true));

        buildingPortrayal.setField(world.buildings);
        BuildingLabelPortrayal b = new BuildingLabelPortrayal(new GeomPortrayal(Color.DARK_GRAY, true), Color.BLUE);
        buildingPortrayal.setPortrayalForAll(b);

        agentPortrayal.setField(world.agents);
        agentPortrayal.setPortrayalForAll(new GeomPortrayal(Color.RED, 15.0, true));

        display.reset();
        display.setBackdrop(Color.WHITE);

        display.repaint();
    }

    public static void main(String[] args) throws Exception {


        try {
            SimBurglar.preInitialise();
        }
        catch (Exception e) {
            LOG.error("Exception thrown during pre-initialisation", e);
            return;
        }

        SimBurglarWithUI worldGUI = null;

        try {
            worldGUI = new SimBurglarWithUI();

        }
        catch (ParseException ex) {
            LOG.error("Error initialising model.", ex);
        }
        catch (MalformedURLException ex) {
            LOG.error("Error initialising model.", ex);
        }
        catch (FileNotFoundException ex) {
            LOG.error("Error initialising model.", ex);
        }

        try {
            Console console = new Console(worldGUI);
            console.setVisible(true);
        }
        catch (Exception e) {
            LOG.error("Error caught while running model.", e);
        }
    } // main

}

class BuildingLabelPortrayal extends LabelledPortrayal2D {

    private static final long serialVersionUID = 1L;

    public BuildingLabelPortrayal(SimplePortrayal2D child, Paint paint) {
        super(child, null, paint, true);
    }

    @Override
    public String getLabel(Object object, DrawInfo2D info) {
        if (object instanceof MasonGeometry) {
            MasonGeometry mg = (MasonGeometry) object;

            return mg.getStringAttribute("NAME");
        }

        return "No Name";
    }

}
