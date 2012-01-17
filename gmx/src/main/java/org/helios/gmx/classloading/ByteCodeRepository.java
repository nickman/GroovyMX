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

import groovy.lang.GroovyClassLoader;
import groovyjarjarasm.asm.ClassReader;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.management.ManagementFactory;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import javax.management.MBeanServerInvocationHandler;

import org.codehaus.groovy.runtime.GeneratedClosure;
import org.helios.gmx.util.DequeuedSoftReferenceValueMap;
import org.helios.gmx.util.LoggingConfig;
import org.helios.gmx.util.LoggingConfig.GLogger;
import org.helios.vm.agent.AgentInstrumentationMBean;
import org.helios.vm.agent.LocalAgentInstaller;

/**
 * <p>Title: ByteCodeRepository</p>
 * <p>Description: Manages an indexed repository of byte code for dynamically generated classes.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.classloading.ByteCodeRepository</code></p>
 */

public class ByteCodeRepository implements ClassFileTransformer {
	/** The singleton instance */
	private static volatile ByteCodeRepository instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** A map of byte code keyed by the class the bytecode represents */
	protected final Map<Class<?>, byte[]> classToByteCode = Collections.synchronizedMap(new WeakHashMap<Class<?>, byte[]>());
	/** A map of Classes keyed by the class name */
	protected final DequeuedSoftReferenceValueMap<String, Class<?>> nameToClass = new DequeuedSoftReferenceValueMap<String, Class<?>>(); 
	/** A map of Classes keyed by the class resource name */
	protected final DequeuedSoftReferenceValueMap<String, Class<?>> resourceNameToClass = new DequeuedSoftReferenceValueMap<String, Class<?>>(); 
	/** A map of {@link DeferredClass}es keyed by the class name */
	protected final DequeuedSoftReferenceValueMap<String, DeferredClass> nameToDeferredClass = new DequeuedSoftReferenceValueMap<String, DeferredClass>();
	
	/** The AgentInstrumentation MBean that provides byte code for dynamically generated closures */
	protected final AgentInstrumentationMBean agentInstrumentation;
	
	/** An instance GLogger */
	protected final GLogger log = LoggingConfig.getInstance().getLogger(getClass());

	
	/** The resource class name of the GeneratedClosure interface */
	public static final String generatedClosureName = GeneratedClosure.class.getName().replace('.', '/');
	
	
	
	/**
	 * Acquires the ByteCodeRepository singleton
	 * @return the ByteCodeRepository singleton
	 */
	public static ByteCodeRepository getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new ByteCodeRepository();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Returns the instrumentation instance
	 * @return the instrumentation instance
	 */
	public Instrumentation getInstrumentation() {
		return agentInstrumentation.getInstrumentation();
	}
	
	/**
	 * Creates a new ByteCodeRepository
	 */
	private ByteCodeRepository() {
		LocalAgentInstaller.getInstrumentation();
		agentInstrumentation = MBeanServerInvocationHandler.newProxyInstance(ManagementFactory.getPlatformMBeanServer(), AgentInstrumentationMBean.AGENT_INSTR_ON, AgentInstrumentationMBean.class, false);
		agentInstrumentation.addTransformer(this, true);		
	}
	
	
	/**
	 * Adds a class to the repository
	 * @param clazz The class to add
	 * @param bytecode The class bytecode
	 */
	public void put(Class<?> clazz, byte[] bytecode) {
		classToByteCode.put(clazz, bytecode);
		nameToClass.put(clazz.getName(), clazz);
		resourceNameToClass.put(clazz.getName().replace('.', '/') + ".class", clazz);
	}
	
	/**
	 * Adds a deferred class to the repository.
	 * This method is called by the class file transformer, but the full class cannot be created at that time.
	 * These parameters are stored until the requester requests the bytecode for a class, at which point we resolve
	 * the class and index it normally.
	 * @param className The class name
	 * @param classLoader The class loader
	 * @param bytecode The class byte code
	 */
	public void put(String className, ClassLoader classLoader, byte[] bytecode) {
		DeferredClass dc = DeferredClass.newInstance(classLoader, className, bytecode);
		nameToDeferredClass.put(className, dc);
	}
	
	/**
	 * Returns the bytecode for the named class
	 * @param className The class name
	 * @return the class bytecode or null if it was not found.
	 */
	public byte[] getByteCode(String className) {
		byte[] bytecode  = null;
		Class<?> clazz = nameToClass.get(className);	
		if(clazz==null) {
			bytecode = getDeferredByteCode(className, null);
		} else {
			bytecode = classToByteCode.get(clazz);
			if(bytecode==null) {
				bytecode = getDeferredByteCode(className, clazz);
			}			
		}
		return bytecode;
	}
	
	/**
	 * Processes a deferred class
	 * @param className The class name to process
	 * @param clazz If available
	 * @return the bytecode or null
	 */
	protected byte[] getDeferredByteCode(String className, Class<?> clazz) {
		byte[] bytecode = null;
		DeferredClass dc = nameToDeferredClass.get(className);
		if(dc==null && clazz != null) {
			try {
				agentInstrumentation.retransformClasses(clazz);
			} catch (UnmodifiableClassException e) {
			}
		}
		if(dc!=null) {
			bytecode = dc.getBytecode();
			synchronized(dc) {
				if(nameToDeferredClass.containsKey(className)) {
					try {
						Class<?> cl = Class.forName(className, true, dc.getClassLoader());
						put(cl, dc.getBytecode());
						nameToDeferredClass.remove(className);
					} catch (Exception e) {}
				}
			}
		}
		return bytecode;
		
	}
	
	/**
	 * Returns the bytecode for the named class using the class resource name
	 * @param className The class resource name
	 * @return the class bytecode or null if it was not found.
	 */
	public byte[] getByteCodeFromResource(String className) {
		byte[] bytecode  = null;
		Class<?> clazz = resourceNameToClass.get(className);
		if(clazz==null) {
			bytecode = getDeferredByteCode(className.replace('/', '.').replace(".class", ""), null);
		} else {
			bytecode = classToByteCode.get(clazz);
		}
		return bytecode;
	}
	
	
	/**
	 * Returns the bytecode for the passed class
	 * @param clazz The class 
	 * @return the class bytecode or null if it was not found.
	 */
	public byte[] getByteCode(Class<?> clazz) {
		byte[] bytecode  = null;
		bytecode = classToByteCode.get(clazz);
		if(bytecode==null) {
			bytecode = getByteCode(clazz.getName());
			if(bytecode==null) {
				try {
					agentInstrumentation.retransformClasses(clazz);
					bytecode = classToByteCode.get(clazz);
				} catch (UnmodifiableClassException e) {
				}
			}
		}
		return bytecode;
	}
	
	/**
	 * Determines if the passed bytecode represents a class that implements {@link GeneratedClosure}.
	 * @param bytecode The byte array
	 * @return true if the bytecode represents a class that implements {@link GeneratedClosure}, false otherwise.
	 */
	protected boolean isGeneratedClosure(byte[] bytecode) {
		try {
			for(String iface: new ClassReader(bytecode).getInterfaces()) {
				if(generatedClosureName.equals(iface)) return true;
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader, java.lang.String, java.lang.Class, java.security.ProtectionDomain, byte[])
	 */
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		byte[] bytecode = classfileBuffer;
		if(classBeingRedefined==null) {
			if(loader instanceof GroovyClassLoader && isGeneratedClosure(bytecode)) {
				put(className, loader, bytecode);
				log.log("Class Load Stored [" , bytecode.length , "] Bytes for deferred class [" , className , "]");
			}
		} else {
			if(GeneratedClosure.class.isAssignableFrom(classBeingRedefined)) {
				put(classBeingRedefined, bytecode);
				log.log("Class Retransform Stored [" , bytecode.length , "] Bytes for deferred class [" , className , "]");				
			}			
		}
		return bytecode;
	}
	
}
