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

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.GroovyShell;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import groovy.lang.Script;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.helios.gmx.classloading.ByteCodeRepository;
import org.helios.gmx.classloading.ReverseClassLoader;
import org.helios.gmx.jmx.ClosureWrappingNotificationFilter;
import org.helios.gmx.jmx.ClosureWrappingNotificationListener;
import org.helios.gmx.jmx.ObjectNameAwareListener;
import org.helios.gmx.jmx.RuntimeMBeanServer;
import org.helios.gmx.jmx.RuntimeMBeanServerConnection;
import org.helios.gmx.util.ClosureDehydrator;
import org.helios.gmx.util.JMXHelper;
import org.helios.vm.VirtualMachine;
import org.helios.vm.VirtualMachineBootstrap;
import org.helios.vm.VirtualMachineDescriptor;

/**
 * <p>Title: Gmx</p>
 * <p>Description: A factory for {@link javax.management.MBeanServerConnection}s and invocation facade.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.Gmx</code></p>
 * TODO:
 * MBeanServer[Connection] by JMXServiceURL, JMXServiceURL String and protocol components. 
 * Auto Reconnect
 * Authentication for remote connections
 * attrs: just values, name/value pairs, set
 */

public class Gmx implements GroovyObject, MBeanServerConnection, NotificationListener {
	
	/** The wrapped MBeanServer connection */
	protected RuntimeMBeanServerConnection mbeanServerConnection;
	/** The wrapped MBeanServer populated if the connection is an MBeanServer */
	protected RuntimeMBeanServer mbeanServer;
	/** The JMXConnector for remote connections */
	protected JMXConnector connector = null;
	/** The JMXConnector's originating service URL */
	protected JMXServiceURL serviceURL =  null;
	/** The JMXConnector environment */
	protected final Map<String, ?> environment = new HashMap<String, Object>();
	/** The JMXConnector's connection Id */
	protected String connectionId = null;
	/** The mbean server default domain */
	protected String serverDomain = null;
	/** The mbean server jvm instance runtime name */
	protected String jvmName = null;
	/** Flag to indicate if this Gmx is connected */
	protected final AtomicBoolean connected = new AtomicBoolean(false);
	/** Flag to indicate if this Gmx is has been remoted */
	protected final AtomicBoolean remoted = new AtomicBoolean(false);
	
	/** The instance MetaClass */
	protected MetaClass metaClass;
	/** The MetaMBean for the remoted class loader on this remote server */
	protected MetaMBean remoteClassLoader = null;
	/** The MetaMBean for the remoted MBeanServer on this remote server */
	protected MetaMBean remotedMBeanServer = null;
	
	/** The closure dehydrator */
	protected final ClosureDehydrator dehydrator = new ClosureDehydrator();
	/** A map of sets of registered JMX notification listeners  */
	protected final Map<ObjectName, Set<ObjectNameAwareListener>> registeredNotificationListeners = new ConcurrentHashMap<ObjectName, Set<ObjectNameAwareListener>>();
	
	
	/** The platform MBeanServer Default Domain Name */
	public static final String PLATFORM_DEFAULT_DOMAIN = ManagementFactory.getPlatformMBeanServer().getDefaultDomain();
	/** The error message template for invalid arguments counts on listener adds */
	public static final String INVALID_ARG_COUNT_TEMPLATE = "Invalid argument count. Closure expects [%s] but additional supplied closure argument count was [%s]. Diff can be 1 or 2 but was [%s]";
	
	/** The standard JMX ObjectName prefix of the remotable MBeanServer MBean */
	public static final String REMOTE_MBEANSERVER_ON_PREFIX = "org.helios.gmx:service=RemotableMBeanServer,domain=%s,host=%s,port=%s";
	/** The standard JMX ObjectName prefix of the remotable MBeanServer MBean */
	public static final String REMOTE_MLET_ON_PREFIX = "org.helios.gmx:service=ReverseClassLoader,host=%s,port=%s";
	
	
	/** A set of Gmx references that will be closed and cleared on shutdown */
	public static final Set<WeakReference<Gmx>> GMX_REFERENCES = new CopyOnWriteArraySet<WeakReference<Gmx>>();
	
	/** This JVM's PID */
	public static final String PID = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
	
	static {
		ByteCodeRepository.getInstance();
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run() {
				//System.out.println("Cleaning up Gmx References...");
				int cnt = 0;
				for(WeakReference<Gmx> ref: GMX_REFERENCES) {
					Gmx gmx = ref.get();
					if(gmx!=null) {
						gmx.close();
						cnt++;
					}
				}
				GMX_REFERENCES.clear();
				//System.out.println("Cleaned up " + cnt + " Gmx References");
			}
		});
	}

	public static void main(String[] args) {
		try {
			ReverseClassLoader.getInstance();
			GroovyShell shell = new GroovyShell();
			Script script = shell.parse(new File(System.getProperty("user.home") + File.separator + "GMXRemotingClosure.groovy"));
			log("Script:" + script);
			VirtualMachineBootstrap.getInstance();
			for(VirtualMachineDescriptor vmd: VirtualMachine.list()) {
				if(vmd.displayName().toLowerCase().contains("jconsole")) {
					String pid = vmd.id();
					log("Connecting to [" + pid + "]");
					script.getBinding().setVariable("args", new String[]{pid});
					//script.getBinding().setVariable("bytecode", ReverseClassLoader.getInstance().);
					script.run();
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	
	/**
	 * Creates a new Gmx
	 * @param mbeanServerConnection The wrapped MBeanServer connection
	 */
	private Gmx(MBeanServerConnection mbeanServerConnection) {
		if(mbeanServerConnection==null) throw new IllegalArgumentException("The passed connection was null", new Throwable());
		this.mbeanServerConnection = RuntimeMBeanServerConnection.getInstance(mbeanServerConnection);
		if(this.mbeanServerConnection instanceof MBeanServer) {
			this.mbeanServer = RuntimeMBeanServer.getInstance((MBeanServer)this.mbeanServerConnection);
			connected.set(true);
		} else {
			this.mbeanServer = null;
		}
		serverDomain = this.mbeanServerConnection.getDefaultDomain();
		try {
			this.jvmName = (String)this.mbeanServerConnection.getAttribute(JMXHelper.objectName(ManagementFactory.RUNTIME_MXBEAN_NAME), "Name");
		} catch (Exception e) {}		
		GMX_REFERENCES.add(new WeakReference<Gmx>(this));
	}
	
	/**
	 * Creates a new Gmx
	 * @param serviceURL The remote JMXServiceURL
	 */
	private Gmx(JMXServiceURL serviceURL, Map<String, ?> environment)  {
		if(serviceURL==null) throw new IllegalArgumentException("The passed serviceURL was null", new Throwable());		
		this.serviceURL = serviceURL;
		try {
			this.connector = JMXConnectorFactory.connect(serviceURL);
			this.mbeanServerConnection = RuntimeMBeanServerConnection.getInstance(connector.getMBeanServerConnection());
			this.connectionId = connector.getConnectionId();
			connected.set(true);
			connector.addConnectionNotificationListener(this, null, this.connectionId);
			if(this.mbeanServerConnection instanceof MBeanServer) {
				this.mbeanServer = RuntimeMBeanServer.getInstance((MBeanServer)this.mbeanServerConnection);
			} else {
				this.mbeanServer = null;
			}			
		} catch (IOException e) {
			throw new RuntimeException("Failed to connect to remote MBeanServer on URL [" + serviceURL + "]");
		}
		serverDomain = this.mbeanServerConnection.getDefaultDomain();
		try {
			this.jvmName = (String)this.mbeanServerConnection.getAttribute(JMXHelper.objectName(ManagementFactory.RUNTIME_MXBEAN_NAME), "Name");
		} catch (Exception e) {}	
		GMX_REFERENCES.add(new WeakReference<Gmx>(this));
		
	}
	
	/**
	 * Creates a new Gmx instance that wraps the local platform MBeanServer
	 * @return a new local platform MBeanServer wrapping Gmx.
	 */
	public static Gmx newInstance() {
		return new Gmx(ManagementFactory.getPlatformMBeanServer());
	}
	
	/**
	 * Creates a new Gmx instance that wraps the passed MBeanServer
	 * @param server The MBeanServer to wrap
	 * @return a new MBeanServer wrapping Gmx.
	 */
	public static Gmx newInstance(MBeanServer server) {
		return new Gmx(server);
	}
	

	/**
	 * Creates a new Gmx instance that wraps a local MBeanServer
	 * @param defaultDomain The default domain name to get the MBeanServer for
	 * @return a new local MBeanServer wrapping Gmx.
	 */
	public static Gmx newInstance(String defaultDomain) {
		if(defaultDomain==null || PLATFORM_DEFAULT_DOMAIN.equals(defaultDomain)) {
			return new Gmx(ManagementFactory.getPlatformMBeanServer());
		}
		for(MBeanServer server: MBeanServerFactory.findMBeanServer(null)) {
			if(server.getDefaultDomain().equals(defaultDomain)) {
				return new Gmx(server);
			}
		}
		throw new IllegalArgumentException("No MBeanServer found for default domain name [" + defaultDomain + "]", new Throwable());
	}
	
	/**
	 * Uses the <a ref="http://docs.oracle.com/javase/6/docs/jdk/api/attach/spec/index.html">VM Attach API</a> to connect to a local VM and acquire an MBeanServerConnection.
	 * @param vmId The target virtual machine identifier, or, usually, the JVM's process id.
	 * @return a remote type Gmx to a local JVM outside this VM.
	 */
	public static Gmx attachInstance(String vmId) {
		VirtualMachineBootstrap.getInstance();
		VirtualMachine vm = VirtualMachine.attach(vmId);
		return new Gmx(vm.getJMXServiceURL(), new HashMap<String, Object>(0));
	}
	
	/**
	 * Creates a new remote Gmx
	 * @param serviceURL The JMXServiceURL to create the Gmx from
	 * @return a remote Gmx
	 */
	public static Gmx remote(JMXServiceURL serviceURL) {
		ReverseClassLoader.getInstance();
		return new Gmx(serviceURL, new HashMap<String, Object>(0));
	}
	
	/**
	 * Creates a new remote Gmx
	 * @param serviceURL The JMXServiceURL to create the Gmx from
	 * @return a remote Gmx
	 */
	public static Gmx remote(CharSequence serviceURL) {
		return new Gmx(JMXHelper.serviceURL(serviceURL), new HashMap<String, Object>(0));
	}	
	
	/**
	 * Locates all JVMs on the local host using the attach API and invokes the passed closure on each.
	 * If a closure is provided, the Gmx will be closed after the closure is invoked and the return value will be null.
	 * Otherwise, the return value will be live Gmx instances.
	 * Attach failures are silently ignored.
	 * @param includeThis If true, includes this JVM (the one running this command)
	 * @param gmxHandler The optional closure to execute on each Gmx created for each discovered JVM. 
	 * @return An array of Gmx instances created for each located and successfully attached JVM.
	 */
	public static Gmx[] attachInstances(boolean includeThis, Closure<Gmx> gmxHandler) {
		Set<Gmx> set = new HashSet<Gmx>();
		for(VirtualMachineDescriptor vmd: VirtualMachine.list()) {
			if(!vmd.id().equals(PID) || (vmd.id().equals(PID) && includeThis)) {
				Gmx gmx = null;
				try {
					gmx = Gmx.attachInstance(vmd.id());
				} catch (Exception e) {
					continue;
				}
				if(gmxHandler!=null) {
					try {
						gmxHandler.call(gmx);
					} finally {
						gmx.close();
					}
				} else {
					try {
						set.add(Gmx.attachInstance(vmd.id()));
					} catch (Exception e) {}
				}				
			}
		}
		return gmxHandler!=null ? null : set.toArray(new Gmx[set.size()]);
	}
	
	/**
	 * Locates all JVMs on the local host (not including this one) using the attach API and invokes the passed closure on each.
	 * Attach failures are silently ignored.
	 * If a closure is provided, the Gmx will be closed after the closure is invoked and the return value will be null.
	 * Otherwise, the return value will be live Gmx instances.
	 * @param gmxHandler The optional closure to execute on each Gmx created for each discovered JVM.
	 * @return An array of Gmx instances created for each located and successfully attached JVM.
	 */
	public static Gmx[] attachInstances(Closure<Gmx> gmxHandler) {
		return attachInstances(false, gmxHandler);
	}
	
	/**
	 * Locates all JVMs on the local host (not including this one) using the attach API and returns a Gmx for each.
	 * Attach failures are silently ignored.
	 * @return An array of Gmx instances created for each located and successfully attached JVM.
	 */
	public static Gmx[] attachInstances() {
		return attachInstances(false, null);
	}
	
	/**
	 * Locates all JVMs on the local host using the attach API and returns a Gmx for each.
	 * Attach failures are silently ignored.
	 * @param includeThis If true, includes this JVM (the one running this command)
	 * @return An array of Gmx instances created for each located and successfully attached JVM.
	 */
	public static Gmx[] attachInstances(boolean includeThis) {
		return attachInstances(includeThis, null);
	}
	
	
	/**
	 * Determines if this Gmx is remote
	 * @return true if this Gmx is remote, false otherwise.
	 */
	public boolean isRemote() {
		return connector!=null;
	}
	

	
	/**
	 * Closes a remote connection.
	 * If this is not a remote Gmx, or the connection is already closed, the command does nothing.
	 */
	public void close() {
		for(Map.Entry<ObjectName, Set<ObjectNameAwareListener>> entry: registeredNotificationListeners.entrySet()) {
			for(ObjectNameAwareListener listener: entry.getValue()) {
				try {
					removeNotificationListener(listener.getObjectName(), listener);
				} catch (Exception e) {}
			}
			entry.getValue().clear();
		}
		registeredNotificationListeners.clear();
		if(remoteClassLoader!=null) {
			try { 
				mbeanServerConnection.unregisterMBean(remoteClassLoader.getObjectName());
				remoteClassLoader=null;
			} catch (Exception e) {}
		}
		if(remotedMBeanServer!=null) {
			try { 
				mbeanServerConnection.unregisterMBean(remotedMBeanServer.getObjectName());
				remotedMBeanServer=null;
			} catch (Exception e) {}
		}		
		if(connector!=null) {			
			try { connector.close(); } catch (Exception e) {}
			connected.set(false);
		}
	}
	
	/**
	 * Indicates if this Gmx is connected.
	 * @return true if this Gmx is live, false if it is disconnected.
	 */
	public boolean isConnected() {
		return connected.get();
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}
	
	// =========================================================================================
	//	Remoting operations
	// =========================================================================================
	
	/**
	 * Installs the remotable MBeanServer on the target MBeanServer
	 * @param privateMlet If true, the MLet will be private, otherwise it will be public.
	 * @return this Gmx
	 */
	public Gmx installRemote(boolean privateMlet) {
		ReverseClassLoader.getInstance().installRemotableMBeanServer(this, privateMlet);
		remoted.set(true);
		return this;
	}
	
	/**
	 * Installs the remotable MBeanServer on the target MBeanServer with a private MLet
	 * @return this Gmx
	 */
	public Gmx installRemote() {
		return installRemote(true);
	}
	
	
	/**
	 * Callback from the reverse class loader providing the ObjectNames of the insalled remotes.
	 * @param remoteClassLoaderOn The JMX ObjectName of the remote classloader
	 * @param remoteMBeanServerOn The JMX ObjectName of the remote MBeanServer
	 */
	public void installedRemote(ObjectName remoteClassLoaderOn, ObjectName remoteMBeanServerOn) {
		remoteClassLoader = mbean(remoteClassLoaderOn);
		remotedMBeanServer = mbean(remoteMBeanServerOn);
		final MetaMBean finalBean = this.remotedMBeanServer;
		this.mbeanServer = RuntimeMBeanServer.getInstance(
		(MBeanServer) Proxy.newProxyInstance(remotedMBeanServer.getClass().getClassLoader(), new Class<?>[]{MBeanServer.class}, new InvocationHandler(){
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				return finalBean.invokeMethod(method.getName(), args);
			}
		}));
		System.out.println("Created remoted MBeanServer proxy [" + this.mbeanServer + "]");
		
	}
	
	/**
	 * Returns the remotable MBeanServer MetaMBean for this Gmx.
	 * @return a MetaMBean
	 */
	public MetaMBean gmxRemote() {
		if(!isRemoted()) {
			installRemote();
		}
		return remotedMBeanServer;
	}
	
	/**
	 * Invokes the passed closure, passing the {@link MBeanServerConnection} as the <code>it</code>.
	 * If the Gmx is remote, the closure will be serialized and executed on the foreign {@link MBeanServer}.
	 * @param <T> The expetced return type of the closure
	 * @param closure The closure to execute
	 * @param args Optional arguments to bind to the closure as an <code>args</code> property.
	 * @return The return value of the closure
	 */
	public <T> T exec(Closure<T> closure, Object...args) {
		if(closure==null) throw new IllegalArgumentException("The passed closure was null", new Throwable());
		if(!isRemote()) {
			return closure.call(mergeArguments(this, args));
		}
		if(!isRemoted()) {
			synchronized(remoted) {
				if(!isRemoted()) {
					installRemote();
				}
			}
		}
		return invokeRemoteClosure(closure, args);
	}
	
	/**
	 * Executes the passed closure forcing local execution.
	 * If the Gmx represents a local {@link MBeanServer}, this call is the same as {@link Gmx#exec(Closure)}.
	 * If the Gmx represents a remote {@link MBeanServerConnection}, this method supresses remoting and invokes the call against the remote {@link MBeanServerConnection} stub. 
	 * @param closure The closure to be executed with this Gmx's {@link MBeanServerConnection} as the first parameter.
	 * @param args The caller supplied arguments to the closure
	 * @return the return value of the closure
	 */
	public <T> T execLocal(Closure<T> closure, Object...args) {
		if(!isRemote()) {
			return exec(closure, args);
		}
		return closure.call(mergeArguments(args, this));
	}
	
	/**
	 * Invokes a closure remotely in the foreign MBeanServer
	 * @param closure The closure to execute
	 * @param arguments The closure arguments
	 * @return The return value of the closure execution.
	 */
	@SuppressWarnings("unchecked")
	public <T> T invokeRemoteClosure(Closure<T> closure, Object...arguments) {
		if(closure==null) throw new IllegalArgumentException("The passed closure was null", new Throwable());
		dehydrator.dehydrate(closure);
		return (T)remotedMBeanServer.invokeMethod("invokeClosure", new Object[]{closure, arguments});
	}
	
	/**
	 * Registers a notification listener with the MBeanServer
	 * @param objectName The JMX ObjectName that represents the MBeans from which to receive notifications
	 * @param listener A closure that will passed the notification and handback.
	 * @param filter A closure that will be passed the notification to determine if it should be filtered or not. If null, no filtering will be performed before handling notifications.
	 * @param handback The object to be passed back to the listener closure. Can be null (so long as the notification is not expecting it....)
	 * @param closureArgs Optional arguments to the listener closure
	 * @return The wrapped listener that can be used to unregister the listener
	 */
	public ObjectNameAwareListener addListener(CharSequence objectName, Closure<Void> listener, Closure<Boolean> filter, Object handback, Object...closureArgs ) {
		return addListener(JMXHelper.objectName(objectName), listener, filter, handback, closureArgs);
	}
	
	/**
	 * Registers a notification listener with the MBeanServer
	 * @param objectName The JMX ObjectName that represents the MBeans from which to receive notifications
	 * @param listener A closure that will passed the notification and handback.
	 * @param filter A closure that will be passed the notification to determine if it should be filtered or not. If null, no filtering will be performed before handling notifications.
	 * @param handback The object to be passed back to the listener closure. Can be null (so long as the notification is not expecting it....)
	 * @param closureArgs Optional arguments to the listener closure
	 * @return The wrapped listener that can be used to unregister the listener
	 */
	public ObjectNameAwareListener addListener(ObjectName objectName, Closure<Void> listener, Closure<Boolean> filter, Object handback, Object...closureArgs ) {
		if(isRemote()) {
			if(!isRemoted()) {
				installRemote();
			}			
		}
		int expectedArgCount = listener.getParameterTypes().length;
		int clozureSuppliedArgCount = closureArgs==null ? 0 : closureArgs.length;
		int notificationSuppliedArgCount = expectedArgCount-clozureSuppliedArgCount;
		if(notificationSuppliedArgCount <1 || notificationSuppliedArgCount >2) {
			throw new IllegalArgumentException(String.format(INVALID_ARG_COUNT_TEMPLATE, expectedArgCount, clozureSuppliedArgCount, notificationSuppliedArgCount));
		} 
		boolean expectsHandback = notificationSuppliedArgCount==2;
		ObjectNameAwareListener onAwareListener = new ClosureWrappingNotificationListener(expectsHandback, objectName, dehydrator.dehydrate(listener), closureArgs);
		_addRegisteredListener(onAwareListener);
		mbeanServerConnection.addNotificationListener(objectName, onAwareListener, new ClosureWrappingNotificationFilter(dehydrator.dehydrate(filter)), handback);
		return onAwareListener;
	}
	
	
	/**
	 * Registers a JMX {@link NotificationListener} with the {@link MBeanServer}
	 * @param objectName The JMX {@link ObjectName} that represents the MBeans from which to receive notifications
	 * @param listener A closure that will passed the notification and handback.
	 * @param closureArgs Optional arguments to the listener closure
	 * @return The wrapped listener that can be used to unregister the listener
	 */
	public ObjectNameAwareListener addListener(CharSequence objectName, Closure<Void> listener, Object...closureArgs) {
		return addListener(objectName, listener, null, null, closureArgs);
	}
	
	/**
	 * Registers a JMX {@link NotificationListener} with the {@link MBeanServer}
	 * @param objectName The JMX {@link ObjectName} that represents the MBeans from which to receive notifications
	 * @param listener A closure that will passed the notification and handback.
	 * @param closureArgs Optional arguments to the listener closure
	 * @return The wrapped listener that can be used to unregister the listener
	 */
	public ObjectNameAwareListener addListener(ObjectName objectName, Closure<Void> listener, Object...closureArgs) {
		return addListener(objectName, listener, null, null, closureArgs);
	}
	
	/**
	 * Removes a registered listener
	 * @param name The ObjectName of the MBean where the listener was registered
	 * @param listener The listener to remove
	 */
	public void removeListener(ObjectName name, NotificationListener listener) {
		mbeanServerConnection.removeNotificationListener(name, listener);
		_removeRegisteredListener(name, listener);
	}
	
	/**
	 * Removes a registered listener
	 * @param listener The object name aware listener to remove
	 */
	public void removeListener(ObjectNameAwareListener listener) {
		mbeanServerConnection.removeNotificationListener(listener.getObjectName(), listener);
		_removeRegisteredListener(listener.getObjectName(), listener);
	}
	
	
	/**
	 * Stores a registered JMX {@link NotificationListener} keyed by {@link ObjectName}.
	 * @param listener The {@link NotificationListener} that was registered
	 */
	protected void _addRegisteredListener(ObjectNameAwareListener listener) {
		Set<ObjectNameAwareListener> listeners = registeredNotificationListeners.get(listener.getObjectName());
		if(listeners==null) {
			synchronized(registeredNotificationListeners) {
				listeners = registeredNotificationListeners.get(listener.getObjectName());
				if(listeners==null) {
					listeners = new CopyOnWriteArraySet<ObjectNameAwareListener>();
					registeredNotificationListeners.put(listener.getObjectName(), listeners);
				}
			}
		}
		listeners.add(listener);
	}
	
	/**
	 * Removes a registered JMX {@link NotificationListener} keyed by {@link ObjectName}.
	 * @param objectName The {@link ObjectName} of the MBean to remove the listener from
	 * @param listener The {@link NotificationListener} that was registered
	 */
	protected void _removeRegisteredListener(ObjectName objectName, NotificationListener listener) {
		Set<ObjectNameAwareListener> listeners = registeredNotificationListeners.get(objectName);
		if(listeners!=null) {
			synchronized(listeners) {
				listeners.remove(listener);
				if(listeners.isEmpty()) {
					listeners.remove(objectName);
				}
			}
		}		
	}
	
	
	// =========================================================================================
	//	MetaMBean operations
	// =========================================================================================
	
	/**
	 * Queries the MBeanServer for MBeans with matching ObjectNames and executes the passed closure on each.
	 * @param objectName The ObjectName to match against
	 * @param beanHandler A closure which will operate on each returned {@link MetaMBean}
	 * @return An array of matching {@link MetaMBean}s.
	 */
	public MetaMBean[] mbeans(ObjectName objectName, Closure<MetaMBean> beanHandler) {
		Set<MetaMBean> metaBeans = new HashSet<MetaMBean>();
		for(ObjectName on: mbeanServerConnection.queryNames(objectName, null)) {
			MetaMBean bean = MetaMBean.newInstance(on, this);
			metaBeans.add(bean);
			if(beanHandler!=null) beanHandler.call(bean);
		}
		return metaBeans.toArray(new MetaMBean[metaBeans.size()]);
	}
	
	/**
	 * Queries the MBeanServer for MBeans with matching ObjectNames and executes the passed closure on the first instance found.
	 * @param objectName The ObjectName to match against
	 * @param beanHandler A closure which will operate on the first returned {@link MetaMBean}
	 * @return The first matched {@link MetaMBean} or null if there was no match. 
	 */
	public MetaMBean mbean(ObjectName objectName, Closure<MetaMBean> beanHandler) {
		Set<ObjectName> matches = mbeanServerConnection.queryNames(objectName, null);
		if(matches.isEmpty()) return null;
		MetaMBean bean = MetaMBean.newInstance(matches.iterator().next(), this);
		if(beanHandler!=null) beanHandler.call(bean);
		return bean;
	}
	
	/**
	 * Queries the MBeanServer for MBeans with matching ObjectNames and executes the passed closure on the first instance found.
	 * @param objectName The ObjectName to match against
	 * @param beanHandler A closure which will operate on the first returned {@link MetaMBean}
	 * @return The first matched {@link MetaMBean} or null if there was no match. 
	 */
	public MetaMBean mbean(CharSequence objectName, Closure<MetaMBean> beanHandler) {
		return mbean(JMXHelper.objectName(objectName), beanHandler);
	}
	
	/**
	 * Queries the MBeanServer for MBeans with matching ObjectNames and returns the first instance found.
	 * @param objectName The ObjectName to match against
	 * @return The first matched {@link MetaMBean} or null if there was no match. 
	 */
	public MetaMBean mbean(CharSequence objectName) {
		return mbean(JMXHelper.objectName(objectName), null);
	}
	
	
	
	/**
	 * Queries the MBeanServer for MBeans with matching ObjectNames and returns the first instance found.
	 * @param objectName The ObjectName to match against
	 * @return The first matched {@link MetaMBean} or null if there was no match. 
	 */
	public MetaMBean mbean(ObjectName objectName) {
		Set<ObjectName> matches = mbeanServerConnection.queryNames(objectName, null);
		if(matches.isEmpty()) return null;
		return MetaMBean.newInstance(matches.iterator().next(), this);
	}

	
	
	/**
	 * Queries the MBeanServer for MBeans with matching ObjectNames
	 * @param objectName The ObjectName to match against
	 * @return An array of matching {@link MetaMBean}s.
	 */
	public MetaMBean[] mbeans(ObjectName objectName) {
		return mbeans(objectName, null);
	}
	
	
	/**
	 * Queries the MBeanServer for MBeans with matching ObjectNames and executes the passed closure on each.
	 * @param objectName The ObjectName to match against
	 * @param beanHandler A closure which will operate on each returned {@link MetaMBean}
	 * @return An array of matching {@link MetaMBean}s.
	 */
	public MetaMBean[] mbeans(CharSequence objectName, Closure<MetaMBean> beanHandler) {
		return mbeans(JMXHelper.objectName(objectName), beanHandler);
	}
	
	/**
	 * Queries the MBeanServer for MBeans with matching ObjectNames
	 * @param objectName The ObjectName to match against
	 * @return An array of matching {@link MetaMBean}s.
	 */
	public MetaMBean[] mbeans(CharSequence objectName) {
		return mbeans(JMXHelper.objectName(objectName), null);
	}
	
	

	
	
	
	// =========================================================================================
	//	GroovyObject implementation
	// =========================================================================================
	
	/**
	 * {@inheritDoc}
	 * @see groovy.lang.GroovyObject#getMetaClass()
	 */
	public MetaClass getMetaClass() {
        if (metaClass == null) {
            metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(Gmx.class);
        }
        return metaClass;
	}

	/**
	 * {@inheritDoc}
	 * @see groovy.lang.GroovyObject#getProperty(java.lang.String)
	 */
	@Override
	public Object getProperty(String propertyName) {
		return getMetaClass().getProperty(this, propertyName);
	}

	/**
	 * {@inheritDoc}
	 * @see groovy.lang.GroovyObject#invokeMethod(java.lang.String, java.lang.Object)
	 */
	@Override
	public Object invokeMethod(String name, Object args) {
		return getMetaClass().invokeMethod(this, name, args);
	}

	/**
	 * {@inheritDoc}
	 * @see groovy.lang.GroovyObject#setMetaClass(groovy.lang.MetaClass)
	 */
	@Override
	public void setMetaClass(MetaClass metaClass) {
		this.metaClass = metaClass;
		
	}

	/**
	 * {@inheritDoc}
	 * @see groovy.lang.GroovyObject#setProperty(java.lang.String, java.lang.Object)
	 */
	@Override
	public void setProperty(String propertyName, Object newValue) {
		getMetaClass().setProperty(this, propertyName, newValue);		
	}
	
	// =========================================================================================
	//	MBeanServerConnection implementation
	// =========================================================================================
	

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#addNotificationListener(javax.management.ObjectName, javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	public void addNotificationListener(ObjectName name,
			NotificationListener listener, NotificationFilter filter,
			Object handback) {
		mbeanServerConnection.addNotificationListener(name, listener, filter,
				handback);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#addNotificationListener(javax.management.ObjectName, javax.management.ObjectName, javax.management.NotificationFilter, java.lang.Object)
	 */
	public void addNotificationListener(ObjectName name, ObjectName listener,
			NotificationFilter filter, Object handback)
			throws InstanceNotFoundException, IOException {
		mbeanServerConnection.addNotificationListener(name, listener, filter,
				handback);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#createMBean(java.lang.String, javax.management.ObjectName, java.lang.Object[], java.lang.String[])
	 */
	public ObjectInstance createMBean(String className, ObjectName name,
			Object[] params, String[] signature) throws ReflectionException,
			InstanceAlreadyExistsException, MBeanRegistrationException,
			MBeanException, NotCompliantMBeanException, IOException {
		return mbeanServerConnection.createMBean(className, name, params,
				signature);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#createMBean(java.lang.String, javax.management.ObjectName, javax.management.ObjectName, java.lang.Object[], java.lang.String[])
	 */
	public ObjectInstance createMBean(String className, ObjectName name,
			ObjectName loaderName, Object[] params, String[] signature)
			throws ReflectionException, InstanceAlreadyExistsException,
			MBeanRegistrationException, MBeanException,
			NotCompliantMBeanException, InstanceNotFoundException, IOException {
		return mbeanServerConnection.createMBean(className, name, loaderName,
				params, signature);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#createMBean(java.lang.String, javax.management.ObjectName, javax.management.ObjectName)
	 */
	public ObjectInstance createMBean(String className, ObjectName name,
			ObjectName loaderName) throws ReflectionException,
			InstanceAlreadyExistsException, MBeanRegistrationException,
			MBeanException, NotCompliantMBeanException,
			InstanceNotFoundException, IOException {
		return mbeanServerConnection.createMBean(className, name, loaderName);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#createMBean(java.lang.String, javax.management.ObjectName)
	 */
	public ObjectInstance createMBean(String className, ObjectName name)
			throws ReflectionException, InstanceAlreadyExistsException,
			MBeanRegistrationException, MBeanException,
			NotCompliantMBeanException, IOException {
		return mbeanServerConnection.createMBean(className, name);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#getAttribute(javax.management.ObjectName, java.lang.String)
	 */
	public Object getAttribute(ObjectName name, String attribute)
			throws MBeanException, AttributeNotFoundException,
			InstanceNotFoundException, ReflectionException, IOException {
		return mbeanServerConnection.getAttribute(name, attribute);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#getAttributes(javax.management.ObjectName, java.lang.String[])
	 */
	public AttributeList getAttributes(ObjectName name, String[] attributes)
			throws InstanceNotFoundException, ReflectionException, IOException {
		return mbeanServerConnection.getAttributes(name, attributes);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#getDefaultDomain()
	 */
	public String getDefaultDomain() throws IOException {
		return mbeanServerConnection.getDefaultDomain();
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#getDomains()
	 */
	public String[] getDomains() throws IOException {
		return mbeanServerConnection.getDomains();
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#getMBeanCount()
	 */
	public Integer getMBeanCount() throws IOException {
		return mbeanServerConnection.getMBeanCount();
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#getMBeanInfo(javax.management.ObjectName)
	 */
	public MBeanInfo getMBeanInfo(ObjectName name)
			throws InstanceNotFoundException, IntrospectionException,
			ReflectionException, IOException {
		return mbeanServerConnection.getMBeanInfo(name);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#getObjectInstance(javax.management.ObjectName)
	 */
	public ObjectInstance getObjectInstance(ObjectName name)
			throws InstanceNotFoundException, IOException {
		return mbeanServerConnection.getObjectInstance(name);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#invoke(javax.management.ObjectName, java.lang.String, java.lang.Object[], java.lang.String[])
	 */
	public Object invoke(ObjectName name, String operationName,
			Object[] params, String[] signature)
			throws InstanceNotFoundException, MBeanException,
			ReflectionException, IOException {
		return mbeanServerConnection.invoke(name, operationName, params,
				signature);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#isInstanceOf(javax.management.ObjectName, java.lang.String)
	 */
	public boolean isInstanceOf(ObjectName name, String className)
			throws InstanceNotFoundException, IOException {
		return mbeanServerConnection.isInstanceOf(name, className);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#isRegistered(javax.management.ObjectName)
	 */
	public boolean isRegistered(ObjectName name) throws IOException {
		return mbeanServerConnection.isRegistered(name);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#queryMBeans(javax.management.ObjectName, javax.management.QueryExp)
	 */
	public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query)
			throws IOException {
		return mbeanServerConnection.queryMBeans(name, query);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#queryNames(javax.management.ObjectName, javax.management.QueryExp)
	 */
	public Set<ObjectName> queryNames(ObjectName name, QueryExp query)
			throws IOException {
		return mbeanServerConnection.queryNames(name, query);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#removeNotificationListener(javax.management.ObjectName, javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	public void removeNotificationListener(ObjectName name,
			NotificationListener listener, NotificationFilter filter,
			Object handback) throws InstanceNotFoundException,
			ListenerNotFoundException, IOException {
		mbeanServerConnection.removeNotificationListener(name, listener,
				filter, handback);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#removeNotificationListener(javax.management.ObjectName, javax.management.NotificationListener)
	 */
	public void removeNotificationListener(ObjectName name,
			NotificationListener listener) throws InstanceNotFoundException,
			ListenerNotFoundException, IOException {
		mbeanServerConnection.removeNotificationListener(name, listener);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#removeNotificationListener(javax.management.ObjectName, javax.management.ObjectName, javax.management.NotificationFilter, java.lang.Object)
	 */
	public void removeNotificationListener(ObjectName name,
			ObjectName listener, NotificationFilter filter, Object handback)
			throws InstanceNotFoundException, ListenerNotFoundException,
			IOException {
		mbeanServerConnection.removeNotificationListener(name, listener,
				filter, handback);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#removeNotificationListener(javax.management.ObjectName, javax.management.ObjectName)
	 */
	public void removeNotificationListener(ObjectName name, ObjectName listener)
			throws InstanceNotFoundException, ListenerNotFoundException,
			IOException {
		mbeanServerConnection.removeNotificationListener(name, listener);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#setAttribute(javax.management.ObjectName, javax.management.Attribute)
	 */
	public void setAttribute(ObjectName name, Attribute attribute)
			throws InstanceNotFoundException, AttributeNotFoundException,
			InvalidAttributeValueException, MBeanException,
			ReflectionException, IOException {
		mbeanServerConnection.setAttribute(name, attribute);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#setAttributes(javax.management.ObjectName, javax.management.AttributeList)
	 */
	public AttributeList setAttributes(ObjectName name, AttributeList attributes)
			throws InstanceNotFoundException, ReflectionException, IOException {
		return mbeanServerConnection.setAttributes(name, attributes);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#unregisterMBean(javax.management.ObjectName)
	 */
	public void unregisterMBean(ObjectName name)
			throws InstanceNotFoundException, MBeanRegistrationException,
			IOException {
		mbeanServerConnection.unregisterMBean(name);
	}

	
	// =========================================================================================
	//	NotificationListener implementation
	// =========================================================================================
	
	/**
	 * Callback when this connection is opened
	 * @param connNot The connection notification
	 */
	public void onConnectionOpened(JMXConnectionNotification connNot) {
		//System.out.println("Connection Opened:" + connNot);
	}
	
	/**
	 * Callback when this connection is closed
	 * @param connNot The connection notification
	 */
	public void onConnectionClosed(JMXConnectionNotification connNot) {
		//System.out.println("Connection Closed:" + connNot);
		//close();
	}

	/**
	 * Callback when this connection fails
	 * @param connNot The connection notification
	 */
	public void onConnectionFailed(JMXConnectionNotification connNot) {
		System.out.println("Connection Failed:" + connNot);
		//close();
	}
	
	/**
	 * Callback when this connection may have lost notifications
	 * @param connNot The connection notification
	 */
	public void onConnectionLostNotifications(JMXConnectionNotification connNot) {
		System.out.println("Connection Lost Notifications:" + connNot);
	}
	
	/**
	 * {@inheritDoc}
	 * <p>Delegates notifications:<ul>
	 * 	<li>{@link JMXConnectionNotification}s: Delegated locally to Gmx</li>
	 * </ul>
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
	public void handleNotification(Notification notification, Object handback) {
		if(notification instanceof JMXConnectionNotification) {
			JMXConnectionNotification connNot = (JMXConnectionNotification)notification;
			String type = connNot.getType();
			if(JMXConnectionNotification.CLOSED.equals(type)) {
				onConnectionClosed(connNot);
			} else if(JMXConnectionNotification.FAILED.equals(type)) {
				onConnectionFailed(connNot);
			} else if(JMXConnectionNotification.OPENED.equals(type)) {
				onConnectionOpened(connNot);
			} else if(JMXConnectionNotification.NOTIFS_LOST.equals(type)) {
				onConnectionLostNotifications(connNot);
			}
		}
	}

	/**
	 * Returns the internal MBeanServer reference
	 * @return the internal MBeanServer reference which may be null if this is a remote
	 * connection and has not been server remoted. 
	 */
	public RuntimeMBeanServer getMBeanServer() {
		return mbeanServer;
	}
	
	/**
	 * Returns the internal MBeanServerConnection reference
	 * @return the internal MBeanServerConnection reference 
	 */
	public RuntimeMBeanServerConnection getMBeanServerConnection() {
		return mbeanServerConnection;
	}
	

	/**
	 * The remote JMX connection
	 * @return the remote JMX connection which may be null if this is not a remote connection
	 */
	public JMXConnector getConnector() {
		return connector;
	}

	/**
	 * The remote JMX connection JMX Service URL
	 * @return the remote JMX connection JMX Service URL which may be null if this is not a remote connection
	 */
	public JMXServiceURL getServiceURL() {
		return serviceURL;
	}

	/**
	 * Returns the configured environment for this connection
	 * @return the configured environment for this connection which may be empty 
	 * because no environment was set or because this is not a remote connection.
	 */
	public Map<String, ?> getEnvironment() {
		return environment;
	}

	/**
	 * Returns the remote connection Id
	 * @return the remote connection Id which may be null if this is not a remote connection
	 */
	public String getConnectionId() {
		return connectionId;
	}

	/**
	 * Returns the MBeanServer default domain.
	 * This value is cached so this is a more efficient call than {@link Gmx#getDefaultDomain()}.
	 * @return the MBeanServer default domain.
	 */
	public String getServerDomain() {
		return serverDomain;
	}

	/**
	 * Returns the JVM Runtime name for the connected JVM
	 * If the connected MBeanServer does not have the {@link java.lang.management.RuntimeMXBean} registered, this will be null.
	 * @return the JVM Runtime name for the connected JVM
	 */
	public String getJvmName() {
		return jvmName;
	}

	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	@Override
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("Gmx [")
	    	.append(TAB).append("DefaultDomain = ").append(this.serverDomain)
	    	.append(TAB).append("JvmName = ").append(this.jvmName)
	    	.append(TAB).append("Remote = ").append(this.isRemote());
	    	if(isRemote()) {
	    		retValue.append(TAB).append("serviceURL = ").append(this.serviceURL)
	    		.append(TAB).append("connectionId = ").append(this.connectionId);
	    	}
	        retValue.append("\n]");    
	    return retValue.toString();
	}
	
	/**
	 * Callback from the ReverseClassLoader when the remotes have been installed
	 * @param remoteClassLoader the MetaMBean for the remoted class loader on this remote server
	 * @param remotedMBeanServer the MetaMBean for the remoted MBeanServer on this remote server
	 */
	protected void installedRemotes(MetaMBean remoteClassLoader, MetaMBean remotedMBeanServer) {
		try {
			this.remoteClassLoader = remoteClassLoader;
			this.remotedMBeanServer = remotedMBeanServer;
			final MetaMBean finalBean = this.remotedMBeanServer; 
			this.mbeanServer = (RuntimeMBeanServer) Proxy.newProxyInstance(remotedMBeanServer.getClass().getClassLoader(), new Class<?>[]{RuntimeMBeanServer.class}, new InvocationHandler(){
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					return finalBean.invokeMethod(method.getName(), args);
				}
			});
			System.out.println("Created remoted MBeanServer proxy [" + this.mbeanServer + "]");
		} catch (Exception e) {
			throw new RuntimeException("Failed to handle callback on remote install", e);
		}
	}
	
	/**
	 * Returns the MetaMBean for the remoted class loader on this remote server
	 * May be null if this has not been installed on the remote
	 * @return the remoteClassLoader MetaMBean
	 */
	public MetaMBean getRemoteClassLoader() {
		return remoteClassLoader;
	}

	/**
	 * Returns the MetaMBean for the remoted MBeanServer on this remote server
	 * May be null if this has not been installed on the remote
	 * @return the remotedMBeanServer MetaMBean
	 */
	public MetaMBean getRemotedMBeanServer() {
		return remotedMBeanServer;
	}

	/**
	 * Flag to indicate if this Gmx is has been remoted
	 * @return true if the Gmx is remoted, false otherwise.
	 */
	public boolean isRemoted() {
		return remoted.get();
	}
	
	/**
	 * Merges internally supplied arguments to a closure with caller supplied arguments into one flat object array.
	 * @param suppliedArgs The caller supplied arguments
	 * @param injectedArgs The internal API supplied arguments
	 * @return an Object array
	 */
	public static Object[] mergeArguments(Object suppliedArgs, Object...injectedArgs) {
		Object[] flattened = null;
		List<Object> fobjs = new ArrayList<Object>();
		if(injectedArgs!=null && injectedArgs.length>0) {
			Collections.addAll(fobjs, injectedArgs);
		}
		if(suppliedArgs!=null) {
			if(Object[].class.isAssignableFrom(suppliedArgs.getClass())) {
				Collections.addAll(fobjs, (Object[])suppliedArgs);
			} else {
				fobjs.add(suppliedArgs);
			}
		}
		flattened = new Object[fobjs.size()];
		return fobjs.toArray(flattened);
	}

}
