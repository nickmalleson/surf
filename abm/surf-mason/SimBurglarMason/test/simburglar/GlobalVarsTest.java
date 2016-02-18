package simburglar;


import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.log4j.Logger;

public class GlobalVarsTest extends TestCase {

    private GlobalVars sone = null, stwo = null;
    private static Logger logger = Logger.getRootLogger();

    public GlobalVarsTest(String name) {
        super(name);
    }

    public void setUp() {
        logger.info("getting singleton...");
        sone = GlobalVars.getInstance();
        logger.info("...got singleton: " + sone);
        logger.info("getting singleton...");
        stwo = GlobalVars.getInstance();
        logger.info("...got singleton: " + stwo);
    }

    public void testUnique() {
        logger.info("checking singletons for equality");
        Assert.assertEquals(true, sone == stwo);
        
        // Also check one of the variables
        Assert.assertEquals(sone.properties, stwo.properties);
        Assert.assertEquals(sone.hashCode(), stwo.hashCode());
    }
    

}