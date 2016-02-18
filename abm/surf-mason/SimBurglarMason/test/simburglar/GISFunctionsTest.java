/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simburglar;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import sim.field.geo.GeomVectorField;
import sim.util.geo.MasonGeometry;
import simburglar.exceptions.RoutingException;

// These are to use is() equalTo(), etc
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import simburglar.agents.Agent;
import simburglar.agents.BurglarTest;


/**
 *
 * @author Nick Malleson
 */
public class GISFunctionsTest {
    
    static Logger LOG = Logger.getLogger(GISFunctionsTest.class);
    static SimBurglar state;
    
    public GISFunctionsTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        
        // Set up a model (or use the existing one if it has already been set
        // up by another test).
        GISFunctionsTest.state = TestInit.setupModel(); 
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

  /**
     * Tests that the nearest house to an agent is their home (obviously)
     */
    @Test
    public void testFindNearestObject() throws RoutingException, Exception {
        
        // Remove all agents and add a single a one who can be configured just as we want.
        state.agents.clear();
        
        // Find the home we want (a particular building).

        MasonGeometry home = state.buildingIDs.get(GIS_FIELDS.CHOSEN_DATA.HOME_ID);
        
        Agent burglar = new Agent(state, home);

        // Make their current location their home

        state.agents.addGeometry(burglar.getGeometry());
        
        LOG.info("Created a single agent at house with ID "+GIS_FIELDS.CHOSEN_DATA.HOME_ID+": "+burglar.toString());
        
        // Find the nearest house to the agent's home location
        
        LOG.info("findNearestObject"); 
        
        LOG.info("\tLooking for nearest house from agent's location"); 
       
        MasonGeometry centre = burglar.getGeometry();
        MasonGeometry result = GISFunctions.findNearestObject(centre, state.buildings, state);
        Integer resultID = result.getIntegerAttribute(FIELDS.BUILDINGS_ID.toString());
        // Agent hasn't moved so the above search should have found the agent's home.
        assertEquals(burglar.getHome(), result);
        // Also check IDs
        assertEquals(String.format("Expected to find home ID '%s' but got '%s'", GIS_FIELDS.CHOSEN_DATA.HOME_ID, resultID),
                GIS_FIELDS.CHOSEN_DATA.HOME_ID, resultID);
        
   
        // Now find the nearest object to another house (search for Building B around building A).
        LOG.info(String.format("\tLooking for nearest object from house %s to house %s", 
                GIS_FIELDS.CHOSEN_DATA.BUILDING_A.toString(), GIS_FIELDS.CHOSEN_DATA.BUILDING_B.toString()));
        
        MasonGeometry buildingA = null, buildingB = null, mg;
        Integer buildingAID = GIS_FIELDS.CHOSEN_DATA.BUILDING_A;
        Integer buildingBID = GIS_FIELDS.CHOSEN_DATA.BUILDING_B;
        for (Object o:state.buildings.getGeometries()) {
            mg = (MasonGeometry) o;
            if (mg.getIntegerAttribute(FIELDS.BUILDINGS_ID.toString()).equals(buildingAID)) {
                buildingA = mg;
            }
            else if (mg.getIntegerAttribute(FIELDS.BUILDINGS_ID.toString()).equals(buildingBID)) {
                buildingB = mg;
            }
        }
        assertNotNull(buildingA);
        assertNotNull(buildingB);
        
        result = GISFunctions.findNearestObject(buildingA, state.buildings, state); // this should be building B!
        assertEquals(buildingB, result);
        // Also check ID's are the same
        resultID = result.getIntegerAttribute(FIELDS.BUILDINGS_ID.toString());        
        assertThat(String.format("From building %s I expected to find the closest is building '%s' but got '%s'", buildingAID, buildingBID, resultID),
                buildingAID, not(equalTo(resultID)));
        
        // Check I've not done something stupid
        assertNotSame(buildingA, buildingB);
        assertThat(buildingA, not(equalTo(buildingB)));
        assertThat(buildingA.getIntegerAttribute(FIELDS.BUILDINGS_ID.toString()), not(equalTo(resultID)));
        assertNotSame(buildingA, result);
        assertThat(buildingA, not(equalTo(result)));
    }
}
