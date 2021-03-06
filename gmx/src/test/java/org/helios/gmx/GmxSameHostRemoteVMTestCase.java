/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package org.helios.gmx;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.Random;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
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
 * <p>Title: GmxSameHostRemoteVMTestCase</p>
 * <p>Description: Gmx tests for same host but remote VM connections.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.GmxSameHostRemoteVMTestCase</code></p>
 */
public class GmxSameHostRemoteVMTestCase {
	/** Tracks the test name */
	@Rule
    public TestName testName= new TestName();
	
	public static final Random random = new Random(System.nanoTime());

	/** Instance logger */
	protected final Logger LOG = Logger.getLogger(getClass());
	/** This JVM's PID */
	public static final String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
	
	@BeforeClass
	public static void classSetup() {
		BasicConfigurator.configure();
//		LoggingConfig.set(ReverseClassLoader.class, true);
//		LoggingConfig.set(ByteCodeRepository.class, true);
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
	 * Creates a JMXServiceURL string for the passed port
	 * @param port The listening port
	 * @return a JMXServiceURL string
	 */
	private String jmxUrl(int port) {
		return "service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi";
	}
	

	
    /**
     * Validates that the Runtime name of a launched JVM is the same as the launcher says it is.
     */
    @Test(timeout=20000)    
    public void testSimpleJVMProcess() throws Exception {
    	int port = 18900;
    	Gmx gmx = null;
    	LaunchedJVMProcess jvmProcess = null;
    	try {
	    	jvmProcess = JVMLauncher.newJVMLauncher().timeout(5000).basicPortJmx(port).start();
    		//jvmProcess = JVMLauncher.newJVMLauncher().timeout(50000).basicPortJmx(port).debug(1889, true, true).start();
	    	String remotePid = jvmProcess.getProcessId();
	    	gmx = Gmx.remote(jmxUrl(port));	    	
	    	String remoteRuntimeName = (String)gmx.mbean(ManagementFactory.RUNTIME_MXBEAN_NAME).getProperty("Name");
	    	String remoteRuntimePid = remoteRuntimeName.split("@")[0];	    	
	    	Assert.assertEquals("The remote runtime pid", remotePid, remoteRuntimePid);
	    	Assert.assertEquals("The remote runtime name", gmx.getJvmName(), remoteRuntimeName);
	    	if(gmx!=null) try { gmx.close(); gmx = null; } catch (Exception e) {}
	    	int exitCode = jvmProcess.stop();
	    	jvmProcess = null;
	    	Assert.assertEquals("The JVM process exit code", 0, exitCode);
	    	
    	} finally {
    		if(gmx!=null) try { gmx.close(); } catch (Exception e) {}
    		if(jvmProcess!=null) try { jvmProcess.destroy(); } catch (Exception e) {}    		
    	}
    }

    /**
     * Validates simple remote closure execution against a foreign JVM by comparing MBean count and Domains.
     */
    @Test(timeout=20000)
    public void testRemoteClosureForMBeanCountAndDomains() throws Exception {
    	int port = 18900;
    	Gmx gmx = null;
    	LaunchedJVMProcess jvmProcess = null;
    	try {
	    	jvmProcess = JVMLauncher.newJVMLauncher().timeout(0).basicPortJmx(port).start();	    		    
	    	gmx = Gmx.remote(jmxUrl(port));
	    	String[] remoteDomains = (String[])gmx.exec(ClosureCompiler.compile("return it.getDomains();"));
	    	String[] domains = gmx.getDomains();	    	
	    	Assert.assertArrayEquals("The remote Domain names" , domains, remoteDomains);
	    	Integer remoteMBeanCount = (Integer)gmx.exec(ClosureCompiler.compile("return it.getMBeanCount();"));
	    	Integer mbeanCount = gmx.getMBeanCount();
	    	Assert.assertEquals("The Remote MBeanCount", mbeanCount, remoteMBeanCount);	    	
	    	remoteMBeanCount = (Integer)gmx.exec(ClosureCompiler.compile("return it.queryNames(null, null).size();"));
	    	Assert.assertEquals("The Remote MBeanCount", mbeanCount, remoteMBeanCount);
	    	remoteMBeanCount = (Integer)gmx.exec(ClosureCompiler.compile("it, name, query -> return it.queryNames(name, query).size();"), null, null);
	    	Assert.assertEquals("The Remote MBeanCount", mbeanCount, remoteMBeanCount);
	    	ObjectName gcWildcard = JMXHelper.objectName("java.lang:type=GarbageCollector,*");
	    	Integer gcMbeanCount = gmx.queryNames(gcWildcard, null).size();
	    	Integer gcRemoteMbeanCount = (Integer)gmx.exec(ClosureCompiler.compile("it, name, query -> return it.queryNames(name, query).size();"), gcWildcard, null);
	    	Assert.assertEquals("The GC MBeanCount", gcMbeanCount, gcRemoteMbeanCount);
    	} finally {
    		if(gmx!=null) try { gmx.close(); } catch (Exception e) {}
    		if(jvmProcess!=null) try { jvmProcess.destroy(); } catch (Exception e) {}    		
    	}    	
    }
    
    @Test
    public void testNewMBeanOpInvoker() throws Exception {
    	Gmx gmx = null;
    	try {
    		gmx = Gmx.newInstance();
    		MetaMBean mbean = gmx.mbean(ManagementFactory.THREAD_MXBEAN_NAME);
    		ThreadInfo directThreadInfo = ((ThreadInfo[])ManagementFactory.getThreadMXBean().getThreadInfo(new long[]{Thread.currentThread().getId()}))[0];    		
    		CompositeData[] gmxCompDatas = (CompositeData[]) mbean.invokeMethod("getThreadInfo", new long[]{Thread.currentThread().getId()});
    		ThreadInfo gmxThreadInfo = ThreadInfo.from(gmxCompDatas[0]);
    		Assert.assertEquals("The Thread Name", directThreadInfo.getThreadName(), gmxThreadInfo.getThreadName());
    		Assert.assertEquals("The Thread ID", directThreadInfo.getThreadId(), gmxThreadInfo.getThreadId());
    		Assert.assertEquals("The Thread toString", directThreadInfo.toString(), gmxThreadInfo.toString());
    	} finally {
    		
    	}
    }
    
    

}
