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
}


