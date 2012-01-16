import org.helios.gmx.*;
import org.helios.gmx.util.*;
import groovy.util.GroovyTestCase;
import java.lang.management.*;
import org.junit.*;
import org.junit.rules.*;
import org.apache.log4j.*;

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

}

