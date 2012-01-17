import org.helios.gmx.*;
import java.lang.management.*;
import org.helios.vm.agent.*;
import org.helios.vm.*;
import org.helios.gmx.classloading.*;
import org.helios.gmx.util.LoggingConfig;

//VirtualMachineBootstrap.findAttachAPI();
//ReverseClassLoader.getInstance();
LoggingConfig.set(ReverseClassLoader.class, true);
LoggingConfig.set(ByteCodeRepository.class, true);

Gmx gmx = null;
VirtualMachine.list().each() {
	if(it.displayName().contains("JConsole")) {
		println "Connecting to JVM ${it.id()}";
		gmx = Gmx.attachInstance(it.id());
	}	
}
if(gmx!=null) {
	try {		
		def blockCount = gmx.exec({
			def cntr = 0;
			def mxBean = ManagementFactory.getThreadMXBean();
			mxBean.getThreadInfo(mxBean.getAllThreadIds()).each() {
				cntr += it.getBlockedCount();
			};
			return cntr;
		});
		println "Block Count:$blockCount"
		Thread.sleep(20000);
	} finally {
		if(gmx!=null) try { gmx.close(); } catch (e) {}
	}
}