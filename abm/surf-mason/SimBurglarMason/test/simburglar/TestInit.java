/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simburglar;

import java.lang.reflect.Method;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Class provides a single method (<code>setupModel</code>) that creates and
 * returns a <code>SimBurglar</code> model which can be used for testing. Will
 * ensure only one of these is created, otherwise each test will try to create
 * a new one.
 * @author Nick Malleson
 */
public class TestInit {

    private static SimBurglar state = null;

    @Test
    public static SimBurglar setupModel() {

        if (TestInit.state == null) {
            try {
            // Call the SimBurglar preInitialise method to read properties files etc.
            // This is private so use reflection
            Method m = SimBurglar.class.getDeclaredMethod("preInitialise", (Class<?>[]) null);
            m.setAccessible(true); //if security settings allow this
            m.invoke(null, (Object[]) null); //use null if the method is static
//        SimBurglar.class.getMethod("preInitialise", (Class<?>) null).invoke(null, (Object) null);

            // Make sure we use the correct data source - some GIS details will be hard coded
            GlobalVars.getInstance().properties.setProperty("DataDir", GIS_FIELDS.CHOSEN_DATA.DATA_NAME);

            state = new SimBurglar(System.currentTimeMillis());
            state.start();
            }
            catch (Exception ex) {
                fail("Got an exception when trying to initialise a model for testing.");
            }

        }
        return state;

    }

}
