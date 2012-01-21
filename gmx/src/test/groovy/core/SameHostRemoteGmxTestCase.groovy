import javax.management.ObjectName;
import javax.management.Notification;

import java.lang.management.ManagementFactory;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.helios.gmx.Gmx;
import org.helios.gmx.util.ClosureCompiler;
import org.helios.gmx.util.JMXHelper;
import org.helios.gmx.util.jvmcontrol.JVMLauncher;
import org.helios.gmx.util.jvmcontrol.LaunchedJVMProcess;
import org.junit.Assert;
import org.junit.Before;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.helios.gmx.notifications.NotificationTriggerService;
/**
Same Host, Remote VM Groovy Test Cases
Whitehead
Jan 16, 2012
*/

class GmxSameHostRemoteVMTestCase extends GroovyTestCase {
	/** The pid of the current VM */
	def pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
	
	/** The test JVM these tests will remote against */
	static def jvmProcess = null;
	/** The port that the management agent will listen on in the test JVM */
	static def port = 18901;
	/** The number of test methods in this script */
	def testCount = countTestCases();
	/** The number of test methods executed */
	def testsExecuted = 0;
	/** A random instance */
	def random = new Random(System.nanoTime());

	/** Tracks the test name */
	@Rule
	def testName= new TestName();
	
	/** Instance logger */
	def LOG = Logger.getLogger(getClass());
	
	static {
		BasicConfigurator.configure();
	}

	@After
	void afterTest() {
		if(testsExecuted==testCount) {
			if(jvmProcess!=null) {
				try { jvmProcess.destroy(); } catch (e) {};
			}
		}
	}
	
	@Before
	void setUp() {		
		String methodName = getMethodName();
		if(testsExecuted<1) {
			if(jvmProcess==null) {
				jvmProcess = JVMLauncher.newJVMLauncher().timeout(120000).basicPortJmx(port).shutdownHook().start();
			}
		}
		testsExecuted++;
		LOG.debug("\n\t******\n\t Test [" + getClass().getSimpleName() + "." + methodName + "]\n\t******");
	}
	
	 String jmxUrl(int port) {
		return "service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi";
	}
	
	@Test
    public void testSimpleJVMProcess() throws Exception {
    	def gmx = null;
    	try {
    		def remotePid = jvmProcess.getProcessId();
	    	gmx = Gmx.remote(jmxUrl(port));
	    	def remoteRuntimeName = gmx.mbean(ManagementFactory.RUNTIME_MXBEAN_NAME).Name;
	    	def remoteRuntimePid = remoteRuntimeName.split("@")[0];	    	
	    	assert remotePid.equals(remoteRuntimePid);
	    	assert gmx.getJvmName().equals(remoteRuntimeName);	    	
	    	if(gmx!=null) try { gmx.close(); gmx = null; } catch (Exception e) {}
    	} finally {
    		if(gmx!=null) try { gmx.close(); } catch (Exception e) {}
    	}
    }
    
	
	@Test(timeout=5000L)
    public void testRemoteClosureForMBeanCountAndDomains() throws Exception {
    	def gmx = null;
    	try {
	    	gmx = Gmx.remote(jmxUrl(port));
	    	def remoteDomains = gmx.exec({ return it.getDomains();});
			def domains = gmx.getDomains();
			Assert.assertArrayEquals("Domain array", domains, remoteDomains);
			def remoteMBeanCount = gmx.exec({ return it.getMBeanCount();});
			def mbeanCount = gmx.getMBeanCount();
			Assert.assertEquals("MBean Count", mbeanCount, remoteMBeanCount);
			remoteMBeanCount = gmx.exec({it, name, query -> return it.queryNames(name, query).size();}, null, null);
			Assert.assertEquals("The Remote MBeanCount", mbeanCount, remoteMBeanCount);
			def gcWildcard = JMXHelper.objectName("java.lang:type=GarbageCollector,*");
			def gcMbeanCount = gmx.queryNames(gcWildcard, null).size();
			def gcRemoteMbeanCount = gmx.exec({it, name, query -> return it.queryNames(name, query).size();}, gcWildcard, null);
			Assert.assertEquals("The GC MBeanCount", gcMbeanCount, gcRemoteMbeanCount);
    	} finally {
    		if(gmx!=null) try { gmx.close(); } catch (Exception e) {}
    	}
    }
	
	/**
	 * Tests a simple notification listener registration with an expected (but null) handback
	 * @throws Exception thrown on any error
	 */
	@Test(timeout=5000L)
    public void testRemoteNotificationListenerExpectedNullHandback() throws Exception {    	
    	def gmx = null;    	
		def latch = new CountDownLatch(1);
    	try {	    	
	    	gmx = Gmx.remote(jmxUrl(port));
	    	gmx.installRemote();
	    	def objectName = NotificationTriggerService.register(gmx.mbeanServer);
	    	def tns = gmx.mbean(objectName);
	    	def userData = null;
			def handback = "foo";
	    	def userDataReference = System.nanoTime();
	    	def listener  = gmx.addListener(objectName, {n, h ->
	    		userData = n.getUserData();
				handback = h;
				latch.countDown();
	    	}, null, null);
			Assert.assertTrue("The handback expected of the listener", listener.isExpectHandback());
	    	Assert.assertEquals("The number of notification listeners registered", 1, tns.ListenerCount);
	    	tns.sendMeANotification(userDataReference);
			latch.await(5000, TimeUnit.MILLISECONDS);
	    	Assert.assertEquals("The user data in the received notification", userDataReference, userData);
			Assert.assertNull("The handback", handback);
	    	
    	} finally {
    		if(gmx!=null) try { gmx.close(); } catch (Exception e) {}
    	}
    }
	
	/**
	* Tests a simple notification listener registration with an expected (and non null) handback
	* @throws Exception thrown on any error
	*/
   @Test(timeout=5000L)
   public void testRemoteNotificationListenerExpectedNonNullHandback() throws Exception {
	   def gmx = null;
	   def latch = new CountDownLatch(1);
	   try {
		   gmx = Gmx.remote(jmxUrl(port));
		   gmx.installRemote();
		   def objectName = NotificationTriggerService.register(gmx.mbeanServer);
		   def tns = gmx.mbean(objectName);
		   def userData = null;
		   def handback = "foo";
		   def userDataReference = System.nanoTime();
		   def listener  = gmx.addListener(objectName, {n, h ->
			   userData = n.getUserData();
			   handback = h;
			   latch.countDown();
		   }, null, "bar");
		   Assert.assertTrue("The handback expected of the listener", listener.isExpectHandback());
		   Assert.assertEquals("The number of notification listeners registered", 1, tns.ListenerCount);
		   tns.sendMeANotification(userDataReference);
		   latch.await(5000, TimeUnit.MILLISECONDS);
		   Assert.assertEquals("The user data in the received notification", userDataReference, userData);
		   Assert.assertEquals("The handback", "bar", handback);
		   
	   } finally {
		   if(gmx!=null) try { gmx.close(); } catch (Exception e) {}
	   }
   }

    
   /**
	* Tests a simple notification listener registration with an non-expected handback
	* @throws Exception thrown on any error
	*/
   @Test(timeout=5000L)
   public void testRemoteNotificationListenerNonExpectedHandback() throws Exception {
	   def gmx = null;
	   def latch = new CountDownLatch(1);
	   try {
		   gmx = Gmx.remote(jmxUrl(port));
		   gmx.installRemote();
		   def objectName = NotificationTriggerService.register(gmx.mbeanServer);
		   def tns = gmx.mbean(objectName);
		   def userData = null;
		   def userDataReference = System.nanoTime();
		   def listener = gmx.addListener(objectName, {
			   userData = it.getUserData();
			   latch.countDown();
		   });
	   	   Assert.assertFalse("The handback expected of the listener", listener.isExpectHandback());
		   Assert.assertEquals("The number of notification listeners registered", 1, tns.ListenerCount);
		   tns.sendMeANotification(userDataReference);
		   latch.await(5000, TimeUnit.MILLISECONDS);
		   Assert.assertEquals("The user data in the received notification", userDataReference, userData);
		   
	   } finally {
		   if(gmx!=null) try { gmx.close(); } catch (Exception e) {}
	   }
   }
   
   /**
   * Tests a simple notification listener registration with closure arguments
   * @throws Exception thrown on any error
   */
  @Test(timeout=5000L)
  public void testRemoteNotificationListenerWithClosureArgumentsNoHandback() throws Exception {
	  def gmx = null;
	  def latch = new CountDownLatch(1);
	  try {
		  gmx = Gmx.remote(jmxUrl(port));
		  gmx.installRemote();
		  def objectName = NotificationTriggerService.register(gmx.mbeanServer);
		  def tns = gmx.mbean(objectName);
		  def userData = null;
		  def numA = random.nextLong();
		  def numB = random.nextLong();
		  def numC = random.nextLong();
		  def result = 1L;
		  def notification = null;
		  def userDataReference = System.nanoTime();
		  def listener = gmx.addListener(objectName, {notif, a, b, c ->
			  notification = notif;
			  result = (a + b + c);
			  latch.countDown();
		  }, numA, numB, numC);
		  Assert.assertFalse("The handback expected of the listener", listener.isExpectHandback());
		  Assert.assertEquals("The number of notification listeners registered", 1, tns.ListenerCount);
		  tns.sendMeANotification(userDataReference);
		  latch.await(5000, TimeUnit.MILLISECONDS);
		  Assert.assertEquals("The sum of numbers calculated", (numA + numB + numC), result);
		  Assert.assertEquals("The notification", Notification.class, notification.getClass());
		  
	  } finally {
		  if(gmx!=null) try { gmx.close(); } catch (Exception e) {}
	  }
  }

  /**
  * Tests a simple notification listener registration with closure arguments and a handback
  * @throws Exception thrown on any error
  */
 @Test(timeout=5000L)
 public void testRemoteNotificationListenerWithClosureArgumentsWithHandback() throws Exception {
	 def gmx = null;
	 def latch = new CountDownLatch(1);
	 try {
		 gmx = Gmx.remote(jmxUrl(port));
		 gmx.installRemote();
		 def objectName = NotificationTriggerService.register(gmx.mbeanServer);
		 def tns = gmx.mbean(objectName);
		 def handback = null;
		 def numA = random.nextLong();
		 def numB = random.nextLong();
		 def numC = random.nextLong();
		 def result = 1L;
		 def notification = null;
		 def handbackValue = System.nanoTime();
		 def listener = gmx.addListener(objectName, {notif, h, a, b, c ->
			 notification = notif;
			 handback = h;
			 result = (a + b + c);
			 latch.countDown();
		 }, null, handbackValue, numA, numB, numC);
		 Assert.assertTrue("The handback expected of the listener", listener.isExpectHandback());
		 Assert.assertEquals("The number of notification listeners registered", 1, tns.ListenerCount);
		 tns.sendMeANotification("foo");
		 latch.await(5000, TimeUnit.MILLISECONDS);
		 Assert.assertEquals("The sum of numbers calculated", (numA + numB + numC), result);
		 Assert.assertEquals("The notification", Notification.class, notification.getClass());
		 Assert.assertEquals("The notification user data", "foo", notification.getUserData());
		 Assert.assertEquals("The handback", handbackValue, handback);
		 
	 } finally {
		 if(gmx!=null) try { gmx.close(); } catch (Exception e) {}
	 }
 }

   /**
   * Tests a closure listener registration with an invalid number of supplied arguments
   * @throws Exception thrown on any error
   */
  @Test
  public void testRemoteNotificationListenerInvalidSuppliedArgCount() throws Exception {
	  def gmx = Gmx.newInstance();
	  def exception = null;
	  def objectName = JMXHelper.objectName("foo:name=bar");
	  try {
		  gmx.addListener(objectName, {}, "foo");
	  } catch (e) {
	  	exception = e;		
	  } finally {
		  if(gmx!=null) try { gmx.close(); } catch (Exception e) {}
	  }
	  Assert.assertNotNull("The exception", exception);
	  println exception.getMessage();
	  Assert.assertEquals("The exception error message", exception.getMessage(), String.format(Gmx.INVALID_ARG_COUNT_TEMPLATE, 1, 1, 0));
  }



}

