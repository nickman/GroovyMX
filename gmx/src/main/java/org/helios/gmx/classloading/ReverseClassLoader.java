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

import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.nio.channels.ServerSocketChannel;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;

import javax.management.MBeanServerInvocationHandler;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.loading.MLet;
import javax.management.loading.PrivateMLet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.groovy.runtime.GeneratedClosure;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.helios.gmx.Gmx;
import org.helios.gmx.jmx.remote.RemotableMBeanServer;
import org.helios.gmx.util.FreePortFinder;
import org.helios.gmx.util.JMXHelper;
import org.helios.vm.agent.AgentInstrumentationMBean;
import org.helios.vm.agent.LocalAgentInstaller;



/**
 * <p>Title: ReverseClassLoader</p>
 * <p>Description: An http server to provide remote class loading from a remote JVM.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.classloading.ReverseClassLoader</code></p>
 */
public class ReverseClassLoader extends AbstractHandler implements ClassFileTransformer  {
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
	/** Indicates if the class loader is serving a jar or individual classes */
	protected final boolean jarClassLoader; 
	/** The byte array of the jar is a jar class loader is being used */
	protected byte[] jarContent = null;
	/** The gzipped byte array of the jar is a jar class loader is being used for remote classloaders reporting gzip support */
	protected byte[] gzJarContent = null;
	/** The URL of the code source for this class */
	protected final URL codeSourceUrl = getClass().getProtectionDomain().getCodeSource().getLocation();
	/** The stats MBeanContainer */
	protected MBeanContainer container;
	/** A set of dynamically created class loaders that can load classes that may be requested by a remote class loader */
	protected final Map<ClassLoader, ClassLoader> classLoaders = new WeakHashMap<ClassLoader, ClassLoader>();
	/** The AgentInstrumentation MBean that provides byte code for dynamically generated closures */
	protected final AgentInstrumentationMBean agentInstrumentation;
	/** A map of closure bytecode byte arrays keyed by the class resource name */
	protected final ByteCodeRepository byteCodeRepo = new ByteCodeRepository();
	
	
	
	
	/** The http classloading URI prefix */
	public static final String HTTP_URI_PREFIX = "/classloader/";
	/** The http classloading URI suffix for jar loading */
	public static final String HTTP_URI_JAR_SUFFIX = "gmx.jar";
	
	/** The server's thread group */
	private static final ThreadGroup threadGroup = new ThreadGroup("ReverseClassLoaderThreadGroup");
	/** The server's thread name serial number */
	private static final AtomicLong threadSerial = new AtomicLong(0L);
	
	
	

	/** The singleton instance */
	private static volatile ReverseClassLoader instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader, java.lang.String, java.lang.Class, java.security.ProtectionDomain, byte[])
	 */
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		if(className.toLowerCase().contains("closure")) {
			System.out.println("Transform on Closure Class [" + className + "]");
		}
		byte[] bytecode = classfileBuffer;
		if(loader instanceof GroovyClassLoader) {
			System.out.println("Transform on Groovy Class Load [" + className + "]");
			System.out.println("Stored [" + bytecode.length + "] Bytes for class [" + className + "]");
		}
		if(classBeingRedefined!=null) {
			System.out.println("Redefine on Closure Class [" + className + "] with ClassLoader [" + loader + "]:[" + loader.getClass().getName() + "]");
			if(GeneratedClosure.class.isAssignableFrom(classBeingRedefined) || (loader instanceof GroovyClassLoader.InnerLoader)) {
				System.out.println("Transform on Groovy Class Transform [" + className + "]");
				System.out.println("Stored [" + bytecode.length + "] Bytes for class [" + className + "]");
			}
		}
		return bytecode;
	}
	
	
	/**
	 * Returns the URL array that remote class loaders should use to retrieve classes from this class loader.
	 * If this class was loaded from a jar, the URL will refrence the jar and the standard classpath, otherwise, will reference the standard classpath.
	 * Referencing the jar is more efficient since remotes can load the entire jar in one call, but the URI for individual classes is 
	 * supported for development environments where these classes may not be packaged into a jar yet.
	 * @return a classloading URL
	 */
	public URL[] getHttpCodeBaseURL() {
		List<URL> urls = new ArrayList<URL>();
		try {					
			if(jarClassLoader) {
				urls.add(new URL(new StringBuilder("http://").append(bindInterface).append(":").append(port).append(HTTP_URI_PREFIX).append(HTTP_URI_JAR_SUFFIX).toString()));
			}
			urls.add(new URL(new StringBuilder("http://").append(bindInterface).append(":").append(port).append(HTTP_URI_PREFIX).toString()));
			return urls.toArray(new URL[urls.size()]);
		} catch (Exception e) {
			throw new RuntimeException("Failed to build HttpCodeBaseURL", e);
		}
	}
	
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
	 * Indexes the class name of the passed closure class
	 * @param closure The closure to register
	 */
	@SuppressWarnings("rawtypes")
	public void registerClosure(Closure<?> closure) {
		if(closure==null) throw new IllegalArgumentException("The passed closure was null", new Throwable());
		Class<? extends Closure> closureClass = closure.getClass();
		if(GeneratedClosure.class.isAssignableFrom(closureClass)) {
			
			byte[] bytecode = byteCodeRepo.getByteCode(closureClass);
			if(bytecode!=null) {
				return;
				//throw new RuntimeException("Failed to get bytecode for GeneratedClosure class [" + closureClass.getName() + "]", new Throwable());
			}
			Set<Class<?>> nestedClasses = new HashSet<Class<?>>(Arrays.asList(closureClass.getDeclaredClasses()));
			Collections.addAll(nestedClasses, closureClass.getClasses());
			for(Class<?> clazz: nestedClasses) {
				if(GeneratedClosure.class.isAssignableFrom(clazz)) {
					bytecode = byteCodeRepo.getByteCode(clazz);
					if(bytecode==null) {
						throw new RuntimeException("Failed to get bytecode for GeneratedClosure class [" + closureClass.getName() + "]", new Throwable());
					}										
				}
			}
		} else {
			System.out.println("Not a generated closure [" + closure.getClass().getName() + "]");
		}
	}
	
	/**
	 * Returns the bytecode of the class matching the passed resource class name
	 * @param className the resource class name
	 * @return the bytecode of the named class.
	 */
	protected byte[] getClassBytes(String className) {
		if(className==null) throw new IllegalArgumentException("The passed class name was null", new Throwable());
		byte[] bytecode = byteCodeRepo.getByteCode(className);
		if(bytecode!=null) return bytecode;
		InputStream is = null;
		List<ClassLoader> loaders = null;
		synchronized(classLoaders) {
			loaders = new ArrayList<ClassLoader>(classLoaders.size());
			loaders.add(getClass().getClassLoader());
			loaders.addAll(classLoaders.keySet());
		}
		for(ClassLoader cl: loaders) {
			try {				
				is = cl.getResourceAsStream(className);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				if(is!=null) {
					byte[] buffer = new byte[8096];
					int bytesRead = -1;
					while((bytesRead=is.read(buffer))!=-1) {
						baos.write(buffer, 0, bytesRead);
					}									
				} else {
					Class<?> clazz = Class.forName(className.replace(".class", "").replace('/', '.'), true, cl);
					ObjectOutputStream oos = new ObjectOutputStream(baos);
					oos.writeObject(clazz);
					oos.flush();
				}
				baos.flush();
				return baos.toByteArray();
			} catch (Exception e) {				
			} finally {
				if(is!=null) try { is.close(); } catch (Exception e) {}
			}
		}
		//throw new RuntimeException("Failed to load class [" + className + "]", new Throwable().fillInStackTrace());
		return null;
	}
	
	
	/**
	 * Private ctor.
	 * Starts the server.
	 */
	private ReverseClassLoader() {		
		LocalAgentInstaller.getInstrumentation();
		classLoaders.put(getClass().getClassLoader(), getClass().getClassLoader());
		agentInstrumentation = MBeanServerInvocationHandler.newProxyInstance(ManagementFactory.getPlatformMBeanServer(), AgentInstrumentationMBean.AGENT_INSTR_ON, AgentInstrumentationMBean.class, false);
		agentInstrumentation.addTransformer(this, true);
		jarClassLoader = codeSourceUrl.toString().toLowerCase().endsWith(".jar");
		if(jarClassLoader) {
			loadJarBytes(codeSourceUrl);
			log("Loaded Gmx jar bytes.\n\tStandard:" + jarContent.length + "\n\tGZipped:" + gzJarContent.length);
		}
		server = new Server();
		threadPool = new ExecutorThreadPool(Executors.newCachedThreadPool(new ThreadFactory(){
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(threadGroup, r, "ReverseClassLoader#" + threadSerial.incrementAndGet());
				t.setDaemon(true);
				return t;
			}
		}));
		server.setThreadPool(threadPool);
		try {			
			container = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
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
	 * Loads the Gmx jar into a byte array for serving to remote class loaders
	 * @param jarUrl The URL of the jar resource.
	 */
	protected void loadJarBytes(URL jarUrl) {
		if(jarUrl==null) throw new IllegalArgumentException("The passed jarUrl was null", new Throwable());
		ByteArrayOutputStream baos = null;
		BufferedInputStream bis = null;
		InputStream is = null;	
		GZIPOutputStream gzipOut = null;
		try {
			is = jarUrl.openStream();
			bis = new BufferedInputStream(is);
			baos = new ByteArrayOutputStream(is.available());
			byte[] buffer = new byte[8092];
			int bytesRead = -1;
			while((bytesRead=bis.read(buffer))!=-1) {
				baos.write(buffer, 0, bytesRead);
			}
			baos.flush();
			jarContent = baos.toByteArray();
			baos.close();
			baos = new ByteArrayOutputStream(jarContent.length);
			gzipOut = new GZIPOutputStream(baos, jarContent.length);
			gzipOut.write(jarContent);
			gzipOut.flush();
			gzipOut.finish();
			baos.flush();
			gzJarContent = baos.toByteArray();
			gzipOut.close();
			baos.close();
		} catch (Exception e) {
			throw new RuntimeException("Failed to load bytes for jar [" + jarUrl + "]", e);
		} finally {
			if(bis!=null) try { bis.close(); } catch (Exception e) {}
			if(is!=null) try { is.close(); } catch (Exception e) {}			
		}
	}
	
	/**
	 * Starts the server.
	 */
	private void startServer() {
		try {
			bindInterface = FreePortFinder.hostName();
			//port = FreePortFinder.getNextFreePort(bindInterface);
			connector = new SelectChannelConnector();
			connector.setPort(0);
			connector.setMaxIdleTime(30000);
			server.addConnector(connector);
			stats.setHandler(this);			
			server.setHandler(stats);
			server.start();
			port = connector.getPort();
			ServerSocketChannel channel = (ServerSocketChannel)connector.getConnection();
			port = channel.socket().getLocalPort();
			log("Started HTTP Server on [" + bindInterface + ":" + port + "]");
			container.addBean(connector);
		} catch (Exception e) {
			throw new RuntimeException("Failed to start Jetty HTTP Server on [" + bindInterface + ":" + port + "]", e);
		}
	}
	
	public static void main(String[] args) {
		new ReverseClassLoader().startServer();
		
		/*
			public static void log(Object msg) {
				System.out.println(msg);
			}
			
			public static void main(String[] args) {
				log("Closure Test");
				LocalAgentInstaller.getInstrumentation(3000);
				GroovyShell shell = new GroovyShell();
				String scriptTxt = "def clozure = { println 'Hello World'; }; return clozure;";
				Script script = shell.parse(scriptTxt);
				Closure<?> clozure = (Closure<?>)script.run();
				log("Acquired Clozure [" + clozure.getClass().getName() + "]");
				AgentInstrumentationMBean remotifier = MBeanServerInvocationHandler.newProxyInstance(ManagementFactory.getPlatformMBeanServer(), AgentInstrumentation.AGENT_INSTR_ON, AgentInstrumentationMBean.class, false);
				
				byte[] clozureBytes = remotifier.getByteCode(clozure.getClass());
				if(clozureBytes!=null) {
					log("Clozure Bytes:" + clozureBytes.length + " bytes");
				} else {
					log("Failed to get Closure Bytes");
				}
				
			}		
		 */
		
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
		log("Request Target [" + target + "] \n\tfrom [" + request.getRemoteAddr() + ":" + request.getRemotePort() + "]");
		boolean jarRequest = (target.equals(HTTP_URI_PREFIX + HTTP_URI_JAR_SUFFIX));
		byte[] classBytes = null;
		if(!jarRequest) {
			classBytes = getClassBytes(target.replace(HTTP_URI_PREFIX, "").replace(".class", ""));
		}
		OutputStream os = response.getOutputStream();
		GZIPOutputStream gzipOut = null;
		String encodings = request.getHeader("Accept-Encoding");
		boolean gzipAgent = false;
		if (encodings != null && encodings.indexOf("gzip") != -1) {
			// Go with GZIP
			gzipAgent = true;
		    response.setHeader("Content-Encoding", "gzip");
		    if(!jarRequest) {
		    	gzipOut = new GZIPOutputStream(os, classBytes.length);
		    }
		}
        response.setContentType("application/octet-stream");
        response.setStatus(HttpServletResponse.SC_OK);
        if(jarRequest) {
        	if(gzipAgent) {
        		os.write(gzJarContent);
        	} else {
        		os.write(jarContent);
        	}
        } else {
        	response.setContentLength(classBytes.length);
        	if(gzipOut!=null) {
        		gzipOut.write(classBytes);
        		gzipOut.flush();
        		gzipOut.finish();        		
        	} else {
        		os.write(classBytes);        		
        	}        	
        }
        os.flush();        
        baseRequest.setHandled(true);
	}
	
	protected void log(Object msg) {
		System.out.println(msg);
	}
	
	/**
	 * Installs the remotable MBeanServer MBean in the MBeanServer as a private Met associated with the passd Gmx
	 * @param gmx The Gmx connected to the target MBeanServer in which to install the remotable MBeanServer MBean 
	 */
	public void installRemotableMBeanServer(Gmx gmx) {
		installRemotableMBeanServer(gmx, true);
	}
	
	
	/**
	 * Installs the remotable MBeanServer MBean in the MBeanServer associated with the passd Gmx
	 * @param gmx The Gmx connected to the target MBeanServer in which to install the remotable MBeanServer MBean
	 * @param privateClassLoader If true, the classloading MBean initially registered will be private, otherwise it will be public. 
	 */
	public void installRemotableMBeanServer(Gmx gmx, boolean privateClassLoader) {		
		ObjectName classLoaderOn = JMXHelper.objectName(String.format(Gmx.REMOTE_MBEANS_ON_PREFIX, "ReverseClassLoader", bindInterface, port));
		ObjectName mbeanServerOn = JMXHelper.objectName(String.format(Gmx.REMOTE_MBEANS_ON_PREFIX, "RemotableMBeanServer", bindInterface, port));
		if(!gmx.getMBeanServerConnection().isRegistered(classLoaderOn)) {
			String className = privateClassLoader ? PrivateMLet.class.getName() : MLet.class.getName(); 
			gmx.getMBeanServerConnection().createMBean(className, classLoaderOn, new Object[]{getHttpCodeBaseURL(), true}, new String[]{URL[].class.getName(), boolean.class.getName()});			
		}
		if(!gmx.getMBeanServerConnection().isRegistered(mbeanServerOn)) {
			gmx.getMBeanServerConnection().createMBean(RemotableMBeanServer.class.getName(), mbeanServerOn, classLoaderOn);
		}
		gmx.installedRemote(classLoaderOn, mbeanServerOn);
	}




}

/**
 * 
 * Needs:
 * register remotables with Gmx when installed
 * set sysprop on install on target server so agents can quickly determine if agent is installed
 * unregister on ??
 * 
 * 
 * 
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
