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

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.helios.gmx.util.DequeuedWeakReferenceValueMap;

/**
 * <p>Title: ByteCodeRepository</p>
 * <p>Description: Manages an indexed repository of byte code for dynamically generated classes.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.classloading.ByteCodeRepository</code></p>
 */

public class ByteCodeRepository {
	/** A map of byte code keyed by the class the bytecode represents */
	protected final Map<Class<?>, byte[]> classToByteCode = Collections.synchronizedMap(new WeakHashMap<Class<?>, byte[]>());
	/** A map of Classes keyed by the class name */
	protected final DequeuedWeakReferenceValueMap<String, Class<?>> nameToClass = new DequeuedWeakReferenceValueMap<String, Class<?>>(); 
	/** A map of Classes keyed by the class resource name */
	protected final DequeuedWeakReferenceValueMap<String, Class<?>> resourceNameToClass = new DequeuedWeakReferenceValueMap<String, Class<?>>(); 
	
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
	 * Returns the bytecode for the named class
	 * @param className The class name
	 * @return the class bytecode or null if it was not found.
	 */
	public byte[] getByteCode(String className) {
		Class<?> clazz = nameToClass.get(className);
		if(clazz!=null) {
			return classToByteCode.get(clazz);
		}
		return null;
	}
	
	/**
	 * Returns the bytecode for the named class using the class resource name
	 * @param className The class resource name
	 * @return the class bytecode or null if it was not found.
	 */
	public byte[] getByteCodeFromResource(String className) {
		Class<?> clazz = resourceNameToClass.get(className);
		if(clazz!=null) {
			return classToByteCode.get(clazz);
		}
		return null;
	}
	
	
	/**
	 * Returns the bytecode for the passed class
	 * @param clazz The class 
	 * @return the class bytecode or null if it was not found.
	 */
	public byte[] getByteCode(Class<?> clazz) {
		return classToByteCode.get(clazz);
	}
	
	
}
