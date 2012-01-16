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
package org.helios.gmx.jmx.remote;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import javax.management.MBeanServerFactory;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.loading.ClassLoaderRepository;

/**
 * <p>Title: RemotableMBeanServer</p>
 * <p>Description: An MBean that provides remote MBeanServer operations.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.jmx.remote.RemotableMBeanServer</code></p>
 */
public class RemotableMBeanServer implements RemotableMBeanServerMBean, Serializable {
	/**  */
	private static final long serialVersionUID = 5409466004786907167L;
	/** The registration injected MBeanServer */
	protected transient MBeanServer server = null;
	/** The ObjectName this MBean is registered as */
	protected ObjectName objectName = null;
	/** The URL of the reverse class loader */
	protected URL reverseClassLoadURL;
	/** This MBean's class loader */
	protected final ClassLoader classLoader;	
	/** The reverse class loader host */
	protected String reverseClassLoadHost;
	/** The reverse class loader port */
	protected int reverseClassLoadPort;
	/** Invocation Context Classloader */
	protected ClassLoader invocationContextClassLoader = null;
	
	
	
	/**
	 * Creates a new RemotableMBeanServer
	 * @param reverseClassLoadURL The URL of the reverse class loader
	 */
	public RemotableMBeanServer(URL reverseClassLoadURL) {
		this.reverseClassLoadURL = reverseClassLoadURL;
		classLoader = getClass().getClassLoader();
		reverseClassLoadHost = this.reverseClassLoadURL.getHost(); 
		reverseClassLoadPort = this.reverseClassLoadURL.getPort();
		invocationContextClassLoader = new URLClassLoader(new URL[]{this.reverseClassLoadURL}, classLoader);
	}
	
	/**
	 * Creates a new RemotableMBeanServer
	 */
	public RemotableMBeanServer() {
		classLoader = getClass().getClassLoader();
	}
	
	/**
	 * Sets the reverse class loader URL
	 * @param reverseClassLoadURL The URL of the reverse class loader
	 */
	@Override
	public void setReverseClassLoadURL(URL reverseClassLoadURL) {
		this.reverseClassLoadURL = reverseClassLoadURL;
		reverseClassLoadHost = this.reverseClassLoadURL.getHost(); 
		reverseClassLoadPort = this.reverseClassLoadURL.getPort();
		invocationContextClassLoader = new URLClassLoader(new URL[]{this.reverseClassLoadURL}, classLoader);
	}

	/**
	 * Invokes the closure extracted from the passed byte array and returns the result
	 * @param closureBytes The closure serialized as a byte array
	 * @param arguments optional arguments
	 * @return the return value of the closure
	 */
	public Object invokeClosure(byte[] closureBytes, Object...arguments) {
		System.out.println("\n\tExtracting Closure from byte array:" + closureBytes.length + " Bytes\n");
		Closure<?> closure = null;
		ClassLoader current = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(invocationContextClassLoader);
			ByteArrayInputStream bais = new ByteArrayInputStream(closureBytes);
			ObjectInputStream ois = new ObjectInputStream(bais);
			closure = (Closure<?>)ois.readObject();
			return invokeClosure(closure, arguments);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw new RuntimeException("Failed to extract closure form byte array", e);
		} finally {
			Thread.currentThread().setContextClassLoader(current);
		}
	}
	
	/**
	 * Invokes the passed closure and returns the result
	 * @param closure The closure
	 * @param arguments optional arguments
	 * @return the return value of the closure
	 */
	public Object invokeClosure(Closure<?> closure, Object[] arguments) {
		ClassLoader current = Thread.currentThread().getContextClassLoader();		
		try {
			Thread.currentThread().setContextClassLoader(invocationContextClassLoader);
			int argsSize = (arguments==null ? 0 : arguments.length);
			Object[] args = new Object[argsSize+1];
			args[0] = server;
			for(int i = 0; i < argsSize; i++) {
				args[i+1] = arguments[i];
			}
			Object val = closure.call(args);
			return val;
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw new RuntimeException("Failed to invoke closure", e);
		} finally {
			Thread.currentThread().setContextClassLoader(current);
		}
	}
	
	/**
	 * Invokes the submitted script passing in the MBeanServer as a binding and returning the script's return value.
	 * @param script The script to execute.
	 * @param args Arguments to the script
	 * @return the script's return value
	 */
	public Object invokeScript(String script, Object...args) {
		try {
			Map<String, Object> binds = new HashMap<String, Object>(1);
			binds.put("server", server);
			binds.put("arguments", args);
			GroovyShell shell = new GroovyShell(new Binding(binds));
			return shell.evaluate(script);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw new RuntimeException("Failed to invoke script", e);
		}
	}
	
	/**
	 * The URL of the reverse class loader
	 * @return the reverseClassLoadURL
	 */
	public URL getReverseClassLoadURL() {
		return reverseClassLoadURL;
	}

	/**
	 * This MBean's class loader name 
	 * @return the classLoader
	 */
	public String getClassLoader() {
		return classLoader.toString();
	}

	/**
	 * The reverse class loader host
	 * @return the reverseClassLoadHost
	 */
	public String getReverseClassLoadHost() {
		return reverseClassLoadHost;
	}

	/**
	 * The reverse class loader port
	 * @return the reverseClassLoadPort
	 */
	public int getReverseClassLoadPort() {
		return reverseClassLoadPort;
	}
	
	
	/**
	 * Returns the JMX domain names of all located MBeanServers in this JVM
	 * @return the JMX domain names of all located MBeanServers in this JVM
	 */
	public String[] getMBeanServerDomains() {
		try {
			Set<String> domains = new HashSet<String>();
			for(MBeanServer server: MBeanServerFactory.findMBeanServer(null)) {
				domains.add(server.getDefaultDomain()==null ? "null" : server.getDefaultDomain());
			}
			return domains.toArray(new String[domains.size()]);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return new String[]{};
		}
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#postDeregister()
	 */
	@Override
	public void postDeregister() {
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#postRegister(java.lang.Boolean)
	 */
	@Override
	public void postRegister(Boolean registrationDone) {
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#preDeregister()
	 */
	@Override
	public void preDeregister() throws Exception {
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#preRegister(javax.management.MBeanServer, javax.management.ObjectName)
	 */
	@Override
	public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
		this.server = server;
		objectName = name;		
		return name;
	}	

	/**
	 * @param name
	 * @param listener
	 * @param filter
	 * @param handback
	 * @throws InstanceNotFoundException
	 * @see javax.management.MBeanServer#addNotificationListener(javax.management.ObjectName, javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	public void addNotificationListener(ObjectName name,
			NotificationListener listener, NotificationFilter filter,
			Object handback) throws InstanceNotFoundException {
		server.addNotificationListener(name, listener, filter, handback);
	}

	/**
	 * @param name
	 * @param listener
	 * @param filter
	 * @param handback
	 * @throws InstanceNotFoundException
	 * @see javax.management.MBeanServer#addNotificationListener(javax.management.ObjectName, javax.management.ObjectName, javax.management.NotificationFilter, java.lang.Object)
	 */
	public void addNotificationListener(ObjectName name, ObjectName listener,
			NotificationFilter filter, Object handback)
			throws InstanceNotFoundException {
		server.addNotificationListener(name, listener, filter, handback);
	}

	/**
	 * @param className
	 * @param name
	 * @param params
	 * @param signature
	 * @return
	 * @throws ReflectionException
	 * @throws InstanceAlreadyExistsException
	 * @throws MBeanRegistrationException
	 * @throws MBeanException
	 * @throws NotCompliantMBeanException
	 * @see javax.management.MBeanServer#createMBean(java.lang.String, javax.management.ObjectName, java.lang.Object[], java.lang.String[])
	 */
	public ObjectInstance createMBean(String className, ObjectName name,
			Object[] params, String[] signature) throws ReflectionException,
			InstanceAlreadyExistsException, MBeanRegistrationException,
			MBeanException, NotCompliantMBeanException {
		return server.createMBean(className, name, params, signature);
	}

	/**
	 * @param className
	 * @param name
	 * @param loaderName
	 * @param params
	 * @param signature
	 * @return
	 * @throws ReflectionException
	 * @throws InstanceAlreadyExistsException
	 * @throws MBeanRegistrationException
	 * @throws MBeanException
	 * @throws NotCompliantMBeanException
	 * @throws InstanceNotFoundException
	 * @see javax.management.MBeanServer#createMBean(java.lang.String, javax.management.ObjectName, javax.management.ObjectName, java.lang.Object[], java.lang.String[])
	 */
	public ObjectInstance createMBean(String className, ObjectName name,
			ObjectName loaderName, Object[] params, String[] signature)
			throws ReflectionException, InstanceAlreadyExistsException,
			MBeanRegistrationException, MBeanException,
			NotCompliantMBeanException, InstanceNotFoundException {
		return server.createMBean(className, name, loaderName, params,
				signature);
	}

	/**
	 * @param className
	 * @param name
	 * @param loaderName
	 * @return
	 * @throws ReflectionException
	 * @throws InstanceAlreadyExistsException
	 * @throws MBeanRegistrationException
	 * @throws MBeanException
	 * @throws NotCompliantMBeanException
	 * @throws InstanceNotFoundException
	 * @see javax.management.MBeanServer#createMBean(java.lang.String, javax.management.ObjectName, javax.management.ObjectName)
	 */
	public ObjectInstance createMBean(String className, ObjectName name,
			ObjectName loaderName) throws ReflectionException,
			InstanceAlreadyExistsException, MBeanRegistrationException,
			MBeanException, NotCompliantMBeanException,
			InstanceNotFoundException {
		return server.createMBean(className, name, loaderName);
	}

	/**
	 * @param className
	 * @param name
	 * @return
	 * @throws ReflectionException
	 * @throws InstanceAlreadyExistsException
	 * @throws MBeanRegistrationException
	 * @throws MBeanException
	 * @throws NotCompliantMBeanException
	 * @see javax.management.MBeanServer#createMBean(java.lang.String, javax.management.ObjectName)
	 */
	public ObjectInstance createMBean(String className, ObjectName name)
			throws ReflectionException, InstanceAlreadyExistsException,
			MBeanRegistrationException, MBeanException,
			NotCompliantMBeanException {
		return server.createMBean(className, name);
	}

	/**
	 * @param name
	 * @param data
	 * @return
	 * @throws InstanceNotFoundException
	 * @throws OperationsException
	 * @deprecated
	 * @see javax.management.MBeanServer#deserialize(javax.management.ObjectName, byte[])
	 */
	public ObjectInputStream deserialize(ObjectName name, byte[] data)
			throws InstanceNotFoundException, OperationsException {
		return server.deserialize(name, data);
	}

	/**
	 * @param className
	 * @param data
	 * @return
	 * @throws OperationsException
	 * @throws ReflectionException
	 * @deprecated
	 * @see javax.management.MBeanServer#deserialize(java.lang.String, byte[])
	 */
	public ObjectInputStream deserialize(String className, byte[] data)
			throws OperationsException, ReflectionException {
		return server.deserialize(className, data);
	}

	/**
	 * @param className
	 * @param loaderName
	 * @param data
	 * @return
	 * @throws InstanceNotFoundException
	 * @throws OperationsException
	 * @throws ReflectionException
	 * @deprecated
	 * @see javax.management.MBeanServer#deserialize(java.lang.String, javax.management.ObjectName, byte[])
	 */
	public ObjectInputStream deserialize(String className,
			ObjectName loaderName, byte[] data)
			throws InstanceNotFoundException, OperationsException,
			ReflectionException {
		return server.deserialize(className, loaderName, data);
	}

	/**
	 * @param name
	 * @param attribute
	 * @return
	 * @throws MBeanException
	 * @throws AttributeNotFoundException
	 * @throws InstanceNotFoundException
	 * @throws ReflectionException
	 * @see javax.management.MBeanServer#getAttribute(javax.management.ObjectName, java.lang.String)
	 */
	public Object getAttribute(ObjectName name, String attribute)
			throws MBeanException, AttributeNotFoundException,
			InstanceNotFoundException, ReflectionException {
		return server.getAttribute(name, attribute);
	}

	/**
	 * @param name
	 * @param attributes
	 * @return
	 * @throws InstanceNotFoundException
	 * @throws ReflectionException
	 * @see javax.management.MBeanServer#getAttributes(javax.management.ObjectName, java.lang.String[])
	 */
	public AttributeList getAttributes(ObjectName name, String[] attributes)
			throws InstanceNotFoundException, ReflectionException {
		return server.getAttributes(name, attributes);
	}

	/**
	 * @param loaderName
	 * @return
	 * @throws InstanceNotFoundException
	 * @see javax.management.MBeanServer#getClassLoader(javax.management.ObjectName)
	 */
	public ClassLoader getClassLoader(ObjectName loaderName)
			throws InstanceNotFoundException {
		return server.getClassLoader(loaderName);
	}

	/**
	 * @param mbeanName
	 * @return
	 * @throws InstanceNotFoundException
	 * @see javax.management.MBeanServer#getClassLoaderFor(javax.management.ObjectName)
	 */
	public ClassLoader getClassLoaderFor(ObjectName mbeanName)
			throws InstanceNotFoundException {
		return server.getClassLoaderFor(mbeanName);
	}

	/**
	 * @return
	 * @see javax.management.MBeanServer#getClassLoaderRepository()
	 */
	public ClassLoaderRepository getClassLoaderRepository() {
		return server.getClassLoaderRepository();
	}

	/**
	 * @return
	 * @see javax.management.MBeanServer#getDefaultDomain()
	 */
	public String getDefaultDomain() {
		return server.getDefaultDomain();
	}

	/**
	 * @return
	 * @see javax.management.MBeanServer#getDomains()
	 */
	public String[] getDomains() {
		return server.getDomains();
	}

	/**
	 * @return
	 * @see javax.management.MBeanServer#getMBeanCount()
	 */
	public Integer getMBeanCount() {
		return server.getMBeanCount();
	}

	/**
	 * @param name
	 * @return
	 * @throws InstanceNotFoundException
	 * @throws IntrospectionException
	 * @throws ReflectionException
	 * @see javax.management.MBeanServer#getMBeanInfo(javax.management.ObjectName)
	 */
	public MBeanInfo getMBeanInfo(ObjectName name)
			throws InstanceNotFoundException, IntrospectionException,
			ReflectionException {
		return server.getMBeanInfo(name);
	}

	/**
	 * @param name
	 * @return
	 * @throws InstanceNotFoundException
	 * @see javax.management.MBeanServer#getObjectInstance(javax.management.ObjectName)
	 */
	public ObjectInstance getObjectInstance(ObjectName name)
			throws InstanceNotFoundException {
		return server.getObjectInstance(name);
	}

	/**
	 * @param className
	 * @param params
	 * @param signature
	 * @return
	 * @throws ReflectionException
	 * @throws MBeanException
	 * @see javax.management.MBeanServer#instantiate(java.lang.String, java.lang.Object[], java.lang.String[])
	 */
	public Object instantiate(String className, Object[] params,
			String[] signature) throws ReflectionException, MBeanException {
		return server.instantiate(className, params, signature);
	}

	/**
	 * @param className
	 * @param loaderName
	 * @param params
	 * @param signature
	 * @return
	 * @throws ReflectionException
	 * @throws MBeanException
	 * @throws InstanceNotFoundException
	 * @see javax.management.MBeanServer#instantiate(java.lang.String, javax.management.ObjectName, java.lang.Object[], java.lang.String[])
	 */
	public Object instantiate(String className, ObjectName loaderName,
			Object[] params, String[] signature) throws ReflectionException,
			MBeanException, InstanceNotFoundException {
		return server.instantiate(className, loaderName, params, signature);
	}

	/**
	 * @param className
	 * @param loaderName
	 * @return
	 * @throws ReflectionException
	 * @throws MBeanException
	 * @throws InstanceNotFoundException
	 * @see javax.management.MBeanServer#instantiate(java.lang.String, javax.management.ObjectName)
	 */
	public Object instantiate(String className, ObjectName loaderName)
			throws ReflectionException, MBeanException,
			InstanceNotFoundException {
		return server.instantiate(className, loaderName);
	}

	/**
	 * @param className
	 * @return
	 * @throws ReflectionException
	 * @throws MBeanException
	 * @see javax.management.MBeanServer#instantiate(java.lang.String)
	 */
	public Object instantiate(String className) throws ReflectionException,
			MBeanException {
		return server.instantiate(className);
	}

	/**
	 * @param name
	 * @param operationName
	 * @param params
	 * @param signature
	 * @return
	 * @throws InstanceNotFoundException
	 * @throws MBeanException
	 * @throws ReflectionException
	 * @see javax.management.MBeanServer#invoke(javax.management.ObjectName, java.lang.String, java.lang.Object[], java.lang.String[])
	 */
	public Object invoke(ObjectName name, String operationName,
			Object[] params, String[] signature)
			throws InstanceNotFoundException, MBeanException,
			ReflectionException {
		return server.invoke(name, operationName, params, signature);
	}

	/**
	 * @param name
	 * @param className
	 * @return
	 * @throws InstanceNotFoundException
	 * @see javax.management.MBeanServer#isInstanceOf(javax.management.ObjectName, java.lang.String)
	 */
	public boolean isInstanceOf(ObjectName name, String className)
			throws InstanceNotFoundException {
		return server.isInstanceOf(name, className);
	}

	/**
	 * @param name
	 * @return
	 * @see javax.management.MBeanServer#isRegistered(javax.management.ObjectName)
	 */
	public boolean isRegistered(ObjectName name) {
		return server.isRegistered(name);
	}

	/**
	 * @param name
	 * @param query
	 * @return
	 * @see javax.management.MBeanServer#queryMBeans(javax.management.ObjectName, javax.management.QueryExp)
	 */
	public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
		return server.queryMBeans(name, query);
	}

	/**
	 * @param name
	 * @param query
	 * @return
	 * @see javax.management.MBeanServer#queryNames(javax.management.ObjectName, javax.management.QueryExp)
	 */
	public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
		return server.queryNames(name, query);
	}

	/**
	 * @param object
	 * @param name
	 * @return
	 * @throws InstanceAlreadyExistsException
	 * @throws MBeanRegistrationException
	 * @throws NotCompliantMBeanException
	 * @see javax.management.MBeanServer#registerMBean(java.lang.Object, javax.management.ObjectName)
	 */
	public ObjectInstance registerMBean(Object object, ObjectName name)
			throws InstanceAlreadyExistsException, MBeanRegistrationException,
			NotCompliantMBeanException {
		return server.registerMBean(object, name);
	}

	/**
	 * @param name
	 * @param listener
	 * @param filter
	 * @param handback
	 * @throws InstanceNotFoundException
	 * @throws ListenerNotFoundException
	 * @see javax.management.MBeanServer#removeNotificationListener(javax.management.ObjectName, javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	public void removeNotificationListener(ObjectName name,
			NotificationListener listener, NotificationFilter filter,
			Object handback) throws InstanceNotFoundException,
			ListenerNotFoundException {
		server.removeNotificationListener(name, listener, filter, handback);
	}

	/**
	 * @param name
	 * @param listener
	 * @throws InstanceNotFoundException
	 * @throws ListenerNotFoundException
	 * @see javax.management.MBeanServer#removeNotificationListener(javax.management.ObjectName, javax.management.NotificationListener)
	 */
	public void removeNotificationListener(ObjectName name,
			NotificationListener listener) throws InstanceNotFoundException,
			ListenerNotFoundException {
		server.removeNotificationListener(name, listener);
	}

	/**
	 * @param name
	 * @param listener
	 * @param filter
	 * @param handback
	 * @throws InstanceNotFoundException
	 * @throws ListenerNotFoundException
	 * @see javax.management.MBeanServer#removeNotificationListener(javax.management.ObjectName, javax.management.ObjectName, javax.management.NotificationFilter, java.lang.Object)
	 */
	public void removeNotificationListener(ObjectName name,
			ObjectName listener, NotificationFilter filter, Object handback)
			throws InstanceNotFoundException, ListenerNotFoundException {
		server.removeNotificationListener(name, listener, filter, handback);
	}

	/**
	 * @param name
	 * @param listener
	 * @throws InstanceNotFoundException
	 * @throws ListenerNotFoundException
	 * @see javax.management.MBeanServer#removeNotificationListener(javax.management.ObjectName, javax.management.ObjectName)
	 */
	public void removeNotificationListener(ObjectName name, ObjectName listener)
			throws InstanceNotFoundException, ListenerNotFoundException {
		server.removeNotificationListener(name, listener);
	}

	/**
	 * @param name
	 * @param attribute
	 * @throws InstanceNotFoundException
	 * @throws AttributeNotFoundException
	 * @throws InvalidAttributeValueException
	 * @throws MBeanException
	 * @throws ReflectionException
	 * @see javax.management.MBeanServer#setAttribute(javax.management.ObjectName, javax.management.Attribute)
	 */
	public void setAttribute(ObjectName name, Attribute attribute)
			throws InstanceNotFoundException, AttributeNotFoundException,
			InvalidAttributeValueException, MBeanException, ReflectionException {
		server.setAttribute(name, attribute);
	}

	/**
	 * @param name
	 * @param attributes
	 * @return
	 * @throws InstanceNotFoundException
	 * @throws ReflectionException
	 * @see javax.management.MBeanServer#setAttributes(javax.management.ObjectName, javax.management.AttributeList)
	 */
	public AttributeList setAttributes(ObjectName name, AttributeList attributes)
			throws InstanceNotFoundException, ReflectionException {
		return server.setAttributes(name, attributes);
	}

	/**
	 * @param name
	 * @throws InstanceNotFoundException
	 * @throws MBeanRegistrationException
	 * @see javax.management.MBeanServer#unregisterMBean(javax.management.ObjectName)
	 */
	public void unregisterMBean(ObjectName name)
			throws InstanceNotFoundException, MBeanRegistrationException {
		server.unregisterMBean(name);
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
