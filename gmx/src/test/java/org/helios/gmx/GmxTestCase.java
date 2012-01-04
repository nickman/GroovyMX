package org.helios.gmx;


import java.lang.management.ManagementFactory;



import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Assert;
import org.junit.BeforeClass;
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
public class GmxTestCase {
	/** Tracks the test name */
	@Rule
    public TestName testName= new TestName();

	/** Instance logger */
	protected final Logger LOG = Logger.getLogger(getClass());
	/** This JVM's PID */
	public static final String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
	
	@BeforeClass
	public static void classSetup() {
		BasicConfigurator.configure();
	}
	
	/**
	 * {@inheritDoc}
	 * @see junit.framework.TestCase#setUp()
	 */	
	@Before
	public void setUp() {
		String methodName = testName.getMethodName();
		LOG.debug("\n\t******\n\t Test [" + getClass().getSimpleName() + "." + methodName + "]\n\t******");
	}
	
	


    /**
     * Validates that the default domain name is returned through the property accessor.
     */
    @Test
    public void testLocalDomain()  {
    	String localDomain = ManagementFactory.getPlatformMBeanServer().getDefaultDomain();
    	Gmx gmx = Gmx.newInstance();
    	Assert.assertEquals("The default domain", localDomain, gmx.getProperty("defaultDomain"));        
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
    
    /**
     * Validates that the default domain name is returned through the property accessor for an attached Gmx.
     */
    @Test
    public void testAttachedLocalDomain()  {
    	String localDomain = ManagementFactory.getPlatformMBeanServer().getDefaultDomain();    	
    	Gmx gmx = Gmx.attachInstance(pid);
    	Assert.assertEquals("The default domain", localDomain, gmx.getProperty("defaultDomain"));        
    }
    
    /**
     * Validates that the domain names array is returned through the property accessor for an attached Gmx.
     */
    @Test
    public void testAttachedLocalDomains()  {
    	String[] domains = ManagementFactory.getPlatformMBeanServer().getDomains();
    	Gmx gmx = Gmx.attachInstance(pid);    	
    	Assert.assertArrayEquals("The domains array", domains, (Object[]) gmx.getProperty("domains"));        
    }

    /**
     * Validates that the MBean count is returned through the property accessor for an attached Gmx.
     */
    @Test
    public void testAttachedMBeanCount()  {
    	Integer mbeanCount = ManagementFactory.getPlatformMBeanServer().getMBeanCount();
    	Gmx gmx = Gmx.attachInstance(pid);    	
    	Assert.assertEquals("The MBean Count", mbeanCount, (Integer)gmx.getProperty("MBeanCount"));        
    }
    
    /**
     * Validates that the remote flag is returned through the property accessor for an attached Gmx
     */
    @Test
    public void testAttachedRemote()  {    	
    	Gmx gmx = Gmx.attachInstance(pid);    	
    	Assert.assertTrue("The MBean Remote Flag", (Boolean)gmx.getProperty("remote"));        
    }
    
    
}
