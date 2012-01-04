package org.helios.gmx;


import java.lang.management.ManagementFactory;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * <p>Title: GmxTestCase</p>
 * <p>Description: Test cases for {@link Gmx}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.gmx.GmxTestCase</code></p>
 */
public class GmxTestCase extends TestCase {
	/** Tracks the test name */
	@Rule
    public TestName testName= new TestName();

	/** Static class logger */
	protected static final Logger LOG = Logger.getLogger(GmxTestCase.class);

	
	/**
	 * {@inheritDoc}
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	@Before
	public void setUp() throws Exception {
		String methodName = testName.getMethodName();
		LOG.debug("\n\t******\n\t Test [" + getClass().getSimpleName() + "." + methodName + "]\n\t ******");
	}
	
	
    /**
     * Create the test case
     * @param testName name of the test case
     */
    public GmxTestCase( String testName ) {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static junit.framework.Test suite() {
        return new TestSuite( GmxTestCase.class );
    }

    /**
     * Validates that the default domain name is returned through the property accessor.
     */
    @Test
    public void testLocalDomain()  {
    	String localDomain = ManagementFactory.getPlatformMBeanServer().getDefaultDomain();
    	Gmx gmx = Gmx.newInstance();
    	assertEquals("The default domain", localDomain, gmx.getProperty("defaultDomain"));        
    }
    
    /**
     * Validates that the domain names array is returned through the property accessor.
     */
    @Test
    public void testLocalDomains()  {
    	String[] domains = ManagementFactory.getPlatformMBeanServer().getDomains();
    	Gmx gmx = Gmx.newInstance();    	
    	Assert.assertArrayEquals("The domains array", domains, (Object[]) gmx.getProperty("domains"));        
    }
    
    /**
     * Validates that the MBean count is returned through the property accessor.
     */
    @Test
    public void testMBeanCount()  {
    	Integer mbeanCount = ManagementFactory.getPlatformMBeanServer().getMBeanCount();
    	Gmx gmx = Gmx.newInstance();    	
    	Assert.assertEquals("The MBean Count", mbeanCount, (Integer)gmx.getProperty("MBeanCount"));        
    }
    
    /**
     * Validates that the remote flag is returned through the property accessor.
     */
    @Test
    public void testRemote()  {    	
    	Gmx gmx = Gmx.newInstance();    	
    	Assert.assertFalse("The MBean Remote Flag", (Boolean)gmx.getProperty("remote"));        
    }
    
    
    
}
