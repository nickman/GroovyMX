import org.helios.gmx.*;
import org.helios.gmx.util.*;
import groovy.util.GroovyTestCase;
import java.lang.management.*;
import org.junit.*;
import org.junit.rules.*;
import org.apache.log4j.*;


class GmxLocalTestCase extends GroovyTestCase {
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
	
	
    void testGroovyLocalDomain()  {
    	def localDomain = ManagementFactory.getPlatformMBeanServer().getDefaultDomain();
    	def gmx = Gmx.newInstance();
    	println gmx;
    	assert gmx.defaultDomain == localDomain;        
    }
    
    void testGroovyLocalMetaMBeanAttribute() {
    	def runtimeName = ManagementFactory.getRuntimeMXBean().getName();
    	def gmx = Gmx.newInstance();
    	println gmx;
    	def runtimeBean = gmx.mbean(ManagementFactory.RUNTIME_MXBEAN_NAME);
    	assert runtimeBean.Name == runtimeName;
    }
    
    void testGroovyLocalMetaMBeanCompositeAttribute() {
    	def gmx = Gmx.newInstance();
    	println gmx;
    	def on = gmx.mbean("java.lang:type=MemoryPool,name=*").objectName;
    	assert on!=null;
    	def initMem = ManagementFactory.getPlatformMBeanServer().getAttribute(on, "Usage").get("init");    	    	
    	def initMem2 = -99;
    	gmx.mbean(on, {
    		initMem2 = it.Usage.init;
    	});    	
    	assert initMem2 == initMem;
    }
    
    
    void testGroovyLocalMetaMBeanOperation() {
    	long tId = Thread.currentThread().getId();
    	def tInfo = ManagementFactory.getThreadMXBean().getThreadInfo(tId);
    	def gmx = Gmx.newInstance();
    	println gmx;
    	def threadingBean = gmx.mbean(ManagementFactory.THREAD_MXBEAN_NAME);
    	def tInfo2 = ThreadInfo.from(threadingBean.getThreadInfo(tId));
    	assert tId == tInfo.getThreadId();
    	assert tInfo.getThreadId() == tInfo2.getThreadId();
    }
    
    void testGroovyLocalMetaMBeanOperationWithArrayType() {
    	long tId = Thread.currentThread().getId();
    	def tInfo = ManagementFactory.getThreadMXBean().getThreadInfo(tId);
    	def gmx = Gmx.newInstance();
    	println gmx;
    	def threadingBean = gmx.mbean(ManagementFactory.THREAD_MXBEAN_NAME);
    	def tInfo2 = ThreadInfo.from(threadingBean.getThreadInfo([tId] as long[])[0]);
    	assert tInfo.getThreadId() == tInfo2.getThreadId();    	
    }
    
    
    void testGroovyAttachedLocalDomain()  {
    	def localDomain = ManagementFactory.getPlatformMBeanServer().getDefaultDomain();
    	def gmx = Gmx.attachInstance(pid);
    	println gmx;
    	assert gmx.defaultDomain == localDomain;        
    }
    
    
    void testAttachedLocalDomain()  {
    	def localDomain = ManagementFactory.getPlatformMBeanServer().getDefaultDomain();    	
    	Gmx gmx = Gmx.attachInstance(pid);
    	assert localDomain == gmx.defaultDomain;
    	assert localDomain == gmx.serverDomain;
    }
    
    void testAttachAll() {
    	Gmx.attachInstances(true, {
    		println it;
    	});
    }
    
    
}


