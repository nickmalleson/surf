/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simburglar.agents;

import com.vividsolutions.jts.geom.LineString;
import java.lang.reflect.Method;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import sim.engine.SimState;
import sim.field.geo.GeomVectorField;
import sim.util.geo.MasonGeometry;
import simburglar.FIELDS;
import simburglar.GlobalVars;
import simburglar.SimBurglar;
import simburglar.exceptions.RoutingException;

// These are to use is() equalTo(), etc
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import simburglar.GIS_FIELDS;
import simburglar.TestInit;


/**
 *
 * @author Nick Malleson
 */
public class BurglarTest {
    
           
    
    static SimBurglar state;
    
    static Logger LOG = Logger.getLogger(BurglarTest.class);
    
    static Agent burglar;
    
    public BurglarTest() {
    }
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        
        BurglarTest.state = TestInit.setupModel();
        
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        
        // Remove all agents and add a single a one who can be configured just as we want.
        state.agents.clear();
        
        // Find the home we want (a particular building).

        MasonGeometry home = state.buildingIDs.get(GIS_FIELDS.CHOSEN_DATA.HOME_ID);
        
        burglar = new Agent(state, home);

        // Make their current location their home

        state.agents.addGeometry(burglar.getGeometry());
        
        LOG.info("Created a single agent at house with ID "+GIS_FIELDS.CHOSEN_DATA.HOME_ID+": "+burglar.toString());

    }
    
    @After
    public void tearDown() {
    }


  
    

    /**
     * Test of findNewPath method, of class Burglar.
     */
    @Test
    public void testFindNewPath() throws RoutingException {
        
        MasonGeometry destBuilding = state.buildingIDs.get(GIS_FIELDS.CHOSEN_DATA.BUILDING_A);
        
//        burglar.findNewPath(state, destBuilding);
        
    }



//    /**
//     * Test of setNewRoute method, of class Burglar.
//     */
//    @Test
//    public void testSetNewRoute() {
//        System.out.println("setNewRoute");
//        LineString line = null;
//        boolean start = false;
//        Burglar instance = null;
//        instance.setNewRoute(line, start);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of step method, of class Burglar.
//     */
//    @Test
//    public void testStep() {
//        System.out.println("step");
//        SimState state = null;
//        Burglar instance = null;
//        instance.step(state);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of move method, of class Burglar.
//     */
//    @Test
//    public void testMove() throws Exception {
//        System.out.println("move");
//        SimBurglar geoTest = null;
//        Burglar instance = null;
//        instance.move(geoTest);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
    
}
