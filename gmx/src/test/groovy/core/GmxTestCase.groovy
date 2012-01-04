import org.helios.gmx.*;
import groovy.util.GroovyTestCase;
import java.lang.management.*;
import org.junit.*;
import org.junit.rules.*;


class GmxLocalTestCase extends GroovyTestCase {

    void testLocalDomain()  {
    	def localDomain = ManagementFactory.getPlatformMBeanServer().getDefaultDomain();
    	def gmx = Gmx.newInstance();
    	assert gmx.defaultDomain == localDomain;        
    }
    
    void testLocalMetaMBeanAttribute() {
    	def runtimeName = ManagementFactory.getRuntimeMXBean().getName();
    	def gmx = Gmx.newInstance();
    	def runtimeBean = gmx.mbean(ManagementFactory.RUNTIME_MXBEAN_NAME);
    	assert runtimeBean.Name == runtimeName;
    }
    
    void testLocalMetaMBeanOperation() {
    	long tId = Thread.currentThread().getId();
    	def tInfo = ManagementFactory.getThreadMXBean().getThreadInfo(tId);
    	def gmx = Gmx.newInstance();
    	def threadingBean = gmx.mbean(ManagementFactory.THREAD_MXBEAN_NAME);
    	ThreadInfo tInfo2 = ThreadInfo.from(threadingBean.getThreadInfo(tId));
    }
    
}


