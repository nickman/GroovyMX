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


/**
 * <p>Title: DeferredClass</p>
 * <p>Description: A class definition containing the classloader, classname and class bytes which is used
 * when the class transformer needs to register a targetted class in the {@link ByteCodeRepository} but
 * cannot create an actual class reference.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.classloading.DeferredClass</code></p>
 */
public class DeferredClass {
	/** The classloader for the deferred class */
	private final ClassLoader classLoader;
	/** The classname of the deferred class */
	private final String className;
	/** The byte code of the deferred class */
	private final byte[] bytecode;
	

	/**
	 * Creates a new DeferredClass
	 * @param classLoader The classloader for the deferred class
	 * @param className The classname of the deferred class
	 * @param bytecode The byte code of the deferred class
	 */
	public static DeferredClass newInstance(ClassLoader classLoader, String className, byte[] bytecode) {
		return new DeferredClass(classLoader, className, bytecode);
	}
	
	/**
	 * Creates a new DeferredClass
	 * @param classLoader The classloader for the deferred class
	 * @param className The classname of the deferred class
	 * @param bytecode The byte code of the deferred class
	 */
	private DeferredClass(ClassLoader classLoader, String className, byte[] bytecode) {
		this.classLoader = classLoader;
		this.className = className;
		this.bytecode = bytecode;
	}

	/**
	 * Returns classloader for the deferred class
	 * @return the classLoader
	 */
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	/**
	 * Returns the class name
	 * @return the className
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Returns the class byte code
	 * @return the bytecode
	 */
	public byte[] getBytecode() {
		return bytecode;
	}

	/**
	 * Constructs a <code>String</code> with all attributes in <code>name:value</code> format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder();    
	    retValue.append("DeferredClass [")
		    .append(TAB).append("classLoader:").append(this.classLoader)
		    .append(TAB).append("className:").append(this.className)
		    .append(TAB).append("bytecode:").append(this.bytecode.length)
	    	.append("\n]");    
	    return retValue.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((className == null) ? 0 : className.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;		
		if (getClass() != obj.getClass())
			return false;
		DeferredClass other = (DeferredClass) obj;
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		return true;
	}
	
	
	
}
