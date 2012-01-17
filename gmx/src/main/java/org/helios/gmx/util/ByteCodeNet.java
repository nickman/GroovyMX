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
package org.helios.gmx.util;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

import org.helios.gmx.classloading.ByteCodeRepository;

/**
 * <p>Title: ByteCodeNet</p>
 * <p>Description: A class file transformer used to trap the bytecode of a target class</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.util.ByteCodeNet</code></p>
 */

public class ByteCodeNet implements ClassFileTransformer {
	/** The class to get the byte code for */
	private final String targetClassName;
	/** The captured bytecode */
	private final ThreadLocal<Map<String, byte[]>> byteCodeMap = new ThreadLocal<Map<String, byte[]>>() {
		/**
		 * {@inheritDoc}
		 * @see java.lang.ThreadLocal#initialValue()
		 */
		@Override
		protected Map<String, byte[]> initialValue() {
			return null;
		}
	};
	/** The instrumentation instance */
	private static volatile Instrumentation instrumentation = null;

	/**
	 * Creates a new ByteCodeNet
	 * @param targetClass The class to get the byte code for
	 * @return the bytecode of the class
	 */
	public static Map<String, byte[]> getClassBytes(Class<?> targetClass) {
		if(instrumentation==null) {
			instrumentation = ByteCodeRepository.getInstance().getInstrumentation();
		}
		if(targetClass==null) throw new IllegalArgumentException("The passed class was null", new Throwable());
		ByteCodeNet bcn = new ByteCodeNet(targetClass.getName().replace('.', '/'));
		bcn.byteCodeMap.set(new HashMap<String, byte[]>());
		try {
			if(!instrumentation.isModifiableClass(targetClass)) {
				throw new RuntimeException("The class [" + targetClass.getName() + "] is not modifiable", new Throwable());
			}
			instrumentation.addTransformer(bcn, true);
			try {
				instrumentation.retransformClasses(targetClass);
			} catch (Exception e) {
				throw new RuntimeException("Failed to retransform class [" + targetClass.getName() + "]", e); 
			}
			Map<String, byte[]> map = bcn.byteCodeMap.get();
			return map;
		} finally {
			bcn.byteCodeMap.remove();
			instrumentation.removeTransformer(bcn);
		}
	}
	
	/**
	 * Creates a new ByteCodeNet
	 * @param targetClassName The class to get the byte code for
	 */
	private ByteCodeNet(String targetClassName) {
		super();
		this.targetClassName = targetClassName;
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader, java.lang.String, java.lang.Class, java.security.ProtectionDomain, byte[])
	 */
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> redefineClass, ProtectionDomain protectionDomain, byte[] classBytes) throws IllegalClassFormatException {
		Map<String, byte[]> map = byteCodeMap.get();
		if(map!=null) {			
			map.put(className, classBytes);
			System.out.println("Added class [" + className + "]");
		}
		return classBytes;
	}

}
