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
package org.helios.vm.agent;

import groovy.lang.GroovyClassLoader;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;

import org.codehaus.groovy.runtime.GeneratedClosure;

/**
 * <p>Title: ByteCodeCollectingClassFileTransformer</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.vm.agent.ByteCodeCollectingClassFileTransformer</code></p>
 */

public class ByteCodeCollectingClassFileTransformer implements ClassFileTransformer {
	/** A map of bytecode byte arrays keyed by the class */
	private final WeakHashMap<Class<?>, byte[]> byteCode = new WeakHashMap<Class<?>, byte[]>();
	/** A map of bytecode byte arrays keyed by the class name */
	private final WeakHashMap<String, byte[]> byteCodeByName = new WeakHashMap<String, byte[]>(); 
	
	/** The notification broadcaster fired when targetted classes are loaded */
	private final NotificationBroadcasterSupport broadcaster;
	
	/** Notification sequence number */
	private static final AtomicLong sequence = new AtomicLong(0L);
	
	/**
	 * Creates a new ByteCodeCollectingClassFileTransformer
	 * @param broadcaster The notification broadcaster fired when targetted classes are loaded
	 */
	public ByteCodeCollectingClassFileTransformer(NotificationBroadcasterSupport broadcaster) {
		super();
		this.broadcaster = broadcaster;
	}

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
			System.out.println("Transform on Groovy Class [" + className + "]");
			Notification n = new Notification("Load", AgentInstrumentation.AGENT_INSTR_ON, sequence.incrementAndGet());
			Map<String, Object> classInfo = new HashMap<String, Object>();
			classInfo.put("ClassLoader", loader);
			classInfo.put("ClassName", className);
			classInfo.put("ClassBytes", classfileBuffer);
			n.setUserData(classInfo);
			byteCodeByName.put(className, bytecode);
			System.out.println("Stored [" + bytecode.length + "] Bytes for class [" + className + "]");
			
			broadcaster.sendNotification(n);
		}
		if(classBeingRedefined!=null) {
			System.out.println("Redefine on Closure Class [" + className + "] with ClassLoader [" + loader + "]:[" + loader.getClass().getName() + "]");
			if(GeneratedClosure.class.isAssignableFrom(classBeingRedefined) || (loader instanceof GroovyClassLoader.InnerLoader)) {
				System.out.println("Transform on Groovy Class [" + className + "]");
				Notification n = new Notification("Redefine", AgentInstrumentation.AGENT_INSTR_ON, sequence.incrementAndGet());
				Map<String, Object> classInfo = new HashMap<String, Object>();
				classInfo.put("Class", classBeingRedefined);
				classInfo.put("ClassName", className);
				classInfo.put("ClassBytes", classfileBuffer);
				n.setUserData(classInfo);
				byteCode.put(classBeingRedefined, bytecode);
				System.out.println("Stored [" + bytecode.length + "] Bytes for class [" + className + "]");
				broadcaster.sendNotification(n);				
			}
		}
		return bytecode;
	}
	
	/**
	 * Retrieves the byte code array for the passed class
	 * @param clazz The class to get the byte code for
	 * @return A byte array or null if the byte code was not found.
	 */
	public byte[] getByteCode(Class<?> clazz) {
		if(clazz==null) throw new IllegalArgumentException("The passed class was null", new Throwable());
		byte[] bytecode = null;
		synchronized(byteCode) {
			bytecode =  byteCode.get(clazz);
		}
		if(bytecode==null) {
			bytecode = byteCodeByName.get(clazz.getName());
		}
		return bytecode;
	}	

}
