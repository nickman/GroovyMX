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
package org.helios.gmx.classloading;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.helios.gmx.util.FreePortFinder;



/**
 * <p>Title: ReverseClassLoader</p>
 * <p>Description: An http server to provide remote class loading from a remote JVM.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.classloading.ReverseClassLoader</code></p>
 */
public class ReverseClassLoader extends AbstractHandler {
	/** The Jetty Server instance */
	protected Server server;
	/** The assigned listening port */
	protected int port = -1;
	/** The assigned listening binding interface */
	protected String bindInterface = null;
	/** The instance statistics */
	protected StatisticsHandler stats = new StatisticsHandler();
	/** The NIO Connector */
	protected SelectChannelConnector connector = null;
	/** The server thread pool */
	protected ExecutorThreadPool threadPool = null;
	
	/** The server's thread group */
	private static final ThreadGroup threadGroup = new ThreadGroup("ReverseClassLoaderThreadGroup");
	/** The server's thread name serial number */
	private static final AtomicLong threadSerial = new AtomicLong(0L);
	

	/** The singleton instance */
	private static volatile ReverseClassLoader instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/**
	 * Acquires the ReverseClassLoader instance, starting the server if it is not running.
	 * @return the ReverseClassLoader instance
	 */
	public static ReverseClassLoader getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new ReverseClassLoader();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Private ctor.
	 * Starts the server.
	 */
	private ReverseClassLoader() {	
		server = new Server();
		threadPool = new ExecutorThreadPool(Executors.newCachedThreadPool(new ThreadFactory(){
			public Thread newThread(Runnable r) {
				Thread t = new Thread(threadGroup, r, "ReverseClassLoader#" + threadSerial.incrementAndGet());
				t.setDaemon(true);
				return t;
			}
		}));
		server.setThreadPool(threadPool);
		try {
			MBeanContainer container = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
			container.setDomain(getClass().getPackage().getName());
			container.addBean(stats);			
			container.addBean(server);
			container.addBean(threadPool);	
			server.addBean(container);
		} catch (Exception e) {
			log("Warning: Failed to register stats MBean for ReverseClassLoader. Continuing.");
			e.printStackTrace(System.err);
		}
		startServer();
	}
	
	/**
	 * Starts the server.
	 */
	private void startServer() {
		try {
			bindInterface = FreePortFinder.hostName();
			port = FreePortFinder.getNextFreePort(bindInterface);
			connector = new SelectChannelConnector();
			connector.setPort(port);
			connector.setMaxIdleTime(30000);
			server.addConnector(connector);
			stats.setHandler(this);
			server.setHandler(stats);
			server.start();
			log("Started HTTP Server on [" + bindInterface + ":" + port + "]");
			server.join();			
		} catch (Exception e) {
			throw new RuntimeException("Failed to start Jetty HTTP Server on [" + bindInterface + ":" + port + "]", e);
		}
	}
	
	public static void main(String[] args) {
		new ReverseClassLoader().startServer();
	}
	
	/**
	 * Stops the server and nulls out the singleton instance.
	 */
	public void stopServer() {
		try { server.stop(); } catch (Exception e) {};
		instance=null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.eclipse.jetty.server.Handler#handle(java.lang.String, org.eclipse.jetty.server.Request, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		
		String className = target.replace("/classloader/", "").replace(".class", "").replace('/', '.');
		
		byte[] classBytes = null;
		InputStream is = null;
		try {
			Class<?> clazz = Class.forName(className);					
			is = clazz.getClassLoader().getResourceAsStream(target.replace("/classloader/", ""));
			ByteArrayOutputStream baos = new ByteArrayOutputStream(is.available());
			byte[] buffer = new byte[8096];
			int bytesRead = -1;
			while((bytesRead=is.read(buffer))!=-1) {
				baos.write(buffer, 0, bytesRead);
			}
			baos.flush();
			is.close();
			classBytes = baos.toByteArray();
			
		} catch (Exception e) {
			throw new RuntimeException("Failed to load class [" + className + "]", e);
		} finally {
			if(is!=null) try { is.close(); } catch (Exception e) {};
		}
        response.setContentType("application/octet-stream");
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentLength(classBytes.length);
        baseRequest.setHandled(true);
        OutputStream os = response.getOutputStream();
        os.write(classBytes);
        os.flush();        
        //log("Served Class [" + className + "] in [" + classBytes.length + "] Bytes" );
	}
	
	protected void log(Object msg) {
		System.out.println(msg);
	}
	
	
}

/**
	Groovy Code Demonstrating Remote MBeanServer MBean Install
	===========================================================

	import org.helios.gmx.*;
	import org.helios.gmx.util.*;
	import javax.management.*;
	import javax.management.loading.MLet;
	
	gmx = Gmx.remote("service:jmx:rmi://NE-WK-NWHI-01.CPEX.com:8002/jndi/rmi://NE-WK-NWHI-01.CPEX.com:8003/jmxrmi");
	//mlet = new MLet([] as URL[], true) ;
	
	on = new ObjectName("org.helios.gmx:service=ReverseClassLoader");
	on2 = new ObjectName("org.helios.gmx:service=MBeanServer");
	
	try { gmx.unregisterMBean(on); } catch (e) {}
	try { gmx.unregisterMBean(on2); } catch (e) {}
	urls = new URL[1];
	urls[0] = new URL("http://localhost:49156/classloader/");
	gmx.createMBean(MLet.class.getName(),  on, [urls, true] as Object[], [urls.getClass().getName(), boolean.class.getName()] as String[]);
	gmx.createMBean("org.helios.gmx.jmx.remote.RemotableMBeanServer",  on2, on);
	
*/
