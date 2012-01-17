import javax.management.ObjectName;

import java.lang.management.ManagementFactory;
import java.util.Random;

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
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
Same Host, Remote VM Groovy Test Cases
Whitehead
Jan 16, 2012
*/

class GmxSameHostRemoteVMTestCase extends GroovyTestCase {
	/** The pid of the current VM */
	def pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
	
	
	/** Tracks the test name */
	@Rule
	def testName= new TestName();
	
	/** Instance logger */
	def LOG = Logger.getLogger(getClass());
	
	static {
		BasicConfigurator.configure();
	   }
	
	@Before
	void setUp() {
		String methodName = getMethodName();	
		LOG.debug("\n\t******\n\t Test [" + getClass().getSimpleName() + "." + methodName + "]\n\t******");
	}
	
	 String jmxUrl(int port) {
		return "service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi";
	}
	
	
    public void testSimpleJVMProcess() throws Exception {
    	def port = 18900;
    	def gmx = null;
    	def jvmProcess = null;
    	try {
	    	jvmProcess = JVMLauncher.newJVMLauncher().timeout(5000).basicPortJmx(port).start();
    		def remotePid = jvmProcess.getProcessId();
	    	gmx = Gmx.remote(jmxUrl(port));
	    	def remoteRuntimeName = gmx.mbean(ManagementFactory.RUNTIME_MXBEAN_NAME).Name;
	    	def remoteRuntimePid = remoteRuntimeName.split("@")[0];	    	
	    	assert remotePid.equals(remoteRuntimePid);
	    	assert gmx.getJvmName().equals(remoteRuntimeName);	    	
	    	if(gmx!=null) try { gmx.close(); gmx = null; } catch (Exception e) {}
	    	def exitCode = jvmProcess.stop();
	    	jvmProcess = null;
	    	assert 0==exitCode;
    	} finally {
    		if(gmx!=null) try { gmx.close(); } catch (Exception e) {}
    		if(jvmProcess!=null) try { jvmProcess.destroy(); } catch (Exception e) {}    		
    	}
    }
    
	
	
    public void testRemoteClosureForMBeanCountAndDomains() throws Exception {
    	def port = 18900;
    	def gmx = null;
    	def jvmProcess = null;
    	try {
	    	jvmProcess = JVMLauncher.newJVMLauncher().timeout(120000).basicPortJmx(port).start();
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
    		if(jvmProcess!=null) try { jvmProcess.destroy(); } catch (Exception e) {}    		
    	}
    }
    

}

