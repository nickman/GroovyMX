import org.helios.gmx.*;
import java.lang.management.*;

Gmx gmx = Gmx.remote("service:jmx:rmi://NE-WK-NWHI-01.CPEX.com:8002/jndi/rmi://NE-WK-NWHI-01.CPEX.com:8003/jmxrmi");
try {
	@Lazy clozure = {
		def cntr = 0;
		def mxBean = ManagementFactory.getThreadMXBean();
		mxBean.getThreadInfo(mxBean.getAllThreadIds()).each() {
			cntr += it.getBlockedCount();
		};
		return cntr;
	}
	def blockCount = gmx.exec(clozure);
	println "Block Count:$blockCount"
} finally {
	if(gmx!=null) try { gmx.close(); } catch (e) {}
}
