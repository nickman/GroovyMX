import org.helios.gmx.*;
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
    	assert gmx.defaultDomain == localDomain;        
    }
    
    void testGroovyLocalMetaMBeanAttribute() {
    	def runtimeName = ManagementFactory.getRuntimeMXBean().getName();
    	def gmx = Gmx.newInstance();
    	def runtimeBean = gmx.mbean(ManagementFactory.RUNTIME_MXBEAN_NAME);
    	assert runtimeBean.Name == runtimeName;
    }
    
    void testGroovyLocalMetaMBeanOperation() {
    	long tId = Thread.currentThread().getId();
    	def tInfo = ManagementFactory.getThreadMXBean().getThreadInfo(tId);
    	def gmx = Gmx.newInstance();
    	def threadingBean = gmx.mbean(ManagementFactory.THREAD_MXBEAN_NAME);
    	ThreadInfo tInfo2 = ThreadInfo.from(threadingBean.getThreadInfo(tId));
    }
    
    void testGroovyAttachedLocalDomain()  {
    	def localDomain = ManagementFactory.getPlatformMBeanServer().getDefaultDomain();
    	def gmx = Gmx.attachInstance(pid);
    	assert gmx.defaultDomain == localDomain;        
    }
    
    
    
}


