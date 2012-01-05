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

import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.helios.gmx.util.JMXHelper;
import org.helios.gmx.util.Primitive;

/**
 * <p>Title: MetaMBean</p>
 * <p>Description: A meta object that represents an MBean.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.MetaMBean</code></p>
 */
public class MetaMBean implements GroovyObject {
	
	/** The JMX ObjectName of the MBean */
	protected final ObjectName objectName;
	/** The connection to the MBeanServer where the MBean is registered */
	protected final MBeanServerConnection connection;
	/** A reference to the MBean's MBeanInfo */
	protected final AtomicReference<MBeanInfo> mbeanInfo = new AtomicReference<MBeanInfo>(null);
	/** A set of attribute names */
	protected final Set<String> attributeNames = new CopyOnWriteArraySet<String>();
	/** A map of operations keyed by operation name with a set of all signatures for that name as the value */
	protected final Map<String, TreeSet<OperationSignature>> operations = new HashMap<String, TreeSet<OperationSignature>>();
	/** The instance MetaClass */
	protected MetaClass metaClass;
	
	
	/**
	 * Creates a new MetaMBean
	 * @param objectName The JMX ObjectName
	 * @param connection The JMX MBeanServerConnection
	 * @return a MetaMBean
	 */
	public static MetaMBean newInstance(ObjectName objectName, MBeanServerConnection connection) {
		if(objectName==null) throw new IllegalArgumentException("The passed ObjectName was null", new Throwable());
		if(connection==null) throw new IllegalArgumentException("The passed MBeanServerConnection was null", new Throwable());
		return new MetaMBean(objectName, connection);
	}
	
	/**
	 * Creates a new MetaMBean
	 * @param objectName The JMX ObjectName
	 * @param connection The JMX MBeanServerConnection
	 * @return a MetaMBean
	 */
	public static MetaMBean newInstance(CharSequence objectName, MBeanServerConnection connection) {
		return newInstance(JMXHelper.objectName(objectName), connection);
	}
	
	
	
	/**
	 * Creates a new MetaMBean
	 * @param objectName The JMX ObjectName of the MBean
	 * @param connection The connection to the MBeanServer where the MBean is registered
	 */
	private MetaMBean(ObjectName objectName, MBeanServerConnection connection) {
		this.objectName = objectName;
		this.connection = connection;
		try {
			mbeanInfo.set(this.connection.getMBeanInfo(objectName));
			for(MBeanAttributeInfo minfo: mbeanInfo.get().getAttributes()) {
				if(minfo.isReadable()) {
					attributeNames.add(minfo.getName());
				}
			}
			for(MBeanOperationInfo minfo: mbeanInfo.get().getOperations()) {
				TreeSet<OperationSignature> opSigs = operations.get(minfo.getName());
				if(opSigs==null) {
					opSigs = new TreeSet<OperationSignature>();
					operations.put(minfo.getName(), opSigs);
				}
				OperationSignature newOp = OperationSignature.newInstance(minfo); 
				if(opSigs.contains(newOp)) {
					System.err.println("WARN: Overwriten Op:" + newOp);					
				}
				opSigs.add(newOp);		
				System.out.println("OpSig Count:" + opSigs.size());
				System.out.println("");
			}			
			
		} catch (Exception e) {			
			throw new RuntimeException("Failed to acquire MBeanInfo for MBean [" + objectName + "]", e);
		}	
	}

	/**
	 * {@inheritDoc}
	 * @see groovy.lang.GroovyObject#getMetaClass()
	 */
	@Override
	public MetaClass getMetaClass() {
        if (metaClass == null) {
            metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(MetaMBean.class);
        }
        return metaClass;
	}

	/**
	 * {@inheritDoc}
	 * @see groovy.lang.GroovyObject#getProperty(java.lang.String)
	 */
	@Override
	public Object getProperty(String propertyName) {
		if(attributeNames.contains(propertyName)) {
			return _getAttribute(propertyName);
		}
		return getMetaClass().getProperty(this, propertyName);
	}
	
	
	/**
	 * Retrieves the named attribute
	 * @param name The attribute name
	 * @return The attribute value
	 */
	protected Object _getAttribute(String name) {
		try {
			return connection.getAttribute(objectName, name);
		} catch (Exception e) {
			throw new RuntimeException("Failed to get attribute value [" + name + "] from MBean [" + objectName + "]", e);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see groovy.lang.GroovyObject#invokeMethod(java.lang.String, java.lang.Object)
	 */
	@Override
	public Object invokeMethod(String name, Object arg) {
		
		Object[] args = null;
		if(arg.getClass().isArray()) {
			int length = Array.getLength(arg);
			args = new Object[length];
			System.arraycopy(arg, 0, args, 0, length);
		} else {
			args = new Object[]{arg};
		}
		Set<OperationSignature> opSigs = getOpSigs(name, args.length);
		
		if(!opSigs.isEmpty()) {
			if(opSigs.size()==1) {
				try {
					return connection.invoke(objectName, name, args, opSigs.iterator().next().getStrSignature());
				} catch (Exception e) {
					throw new RuntimeException("Failed to invoke operation [" + name + "]", e);
				}
			}
			// Attempt to match one of the overloads
			OperationSignature os = matchOpSig(opSigs.toArray(new OperationSignature[opSigs.size()]), args);
			try {
				return connection.invoke(objectName, name, args, os.getStrSignature());
			} catch (Exception e) {
				throw new RuntimeException("Failed to invoke operation [" + name + "]", e);
			}													
		}
		return getMetaClass().invokeMethod(this, name, args);		
	}
	
	/**
	 * Attempts to match the passed argument array against one of the OperationSignatures in the passed OperationSignature array 
	 * @param opSigs An array of OperationSignatures that have the same number of arguments
	 * @param args The array of arguments to find a matching OperationSignature for
	 * @return A matching OperationSignature or null if one was not found. If more than one match, a RuntimeException will be thrown.
	 */
	protected OperationSignature matchOpSig(OperationSignature[] opSigs, Object...args) {
		Set<OperationSignature> matches = new HashSet<OperationSignature>();
		Class<?>[] signature = argsToSignagture(args);
		for(OperationSignature os: opSigs) {
			Class<?>[] opSig = os.getSignature();
			boolean match = true;
			for(int i = 0; i < signature.length; i++) {
				Class<?> opType = opSig[i];
				Class<?> argType = signature[i];
				if(argType==null) continue;
				if(opType.equals(UnknownClass.class)) continue;
				if(opType.isAssignableFrom(argType)) continue;
				if(Primitive.isValidAssignment(opType, argType)) continue;
				if((opType.isArray() && !argType.isArray()) || (!opType.isArray() && argType.isArray())) {
					match = false;
					break;
				}
				if(opType.isArray() && argType.isArray()) {
					if(Array.getLength(opType) != Array.getLength(argType)) {
						match = false;
						break;
					}
					if(opType.getComponentType().isAssignableFrom(argType.getComponentType())) continue;
					if(Primitive.isValidAssignment(opType.getComponentType(), argType.getComponentType())) continue;
				}
				
			}
			if(match) matches.add(os);

		}
		
		if(matches.isEmpty() || matches.size()>1) throw new RuntimeException("Matched multiple OperationSignatures for the passed argument array:" + matches.toString(), new Throwable());
		return matches.iterator().next();
		
	}
	
	/**
	 * Generates an array of classes from an array of objects.
	 * @param args The object array
	 * @return an array of classes
	 */
	protected Class<?>[] argsToSignagture(Object...args) {
		Class<?>[] signature = new Class[args.length];
		for(int i = 0; i < args.length; i++) {
			signature[i] = args[i]==null ? null : args[i].getClass();
		}
		return signature;
	}
	
	/**
	 * Finds all operation signatures with an operation name matching the passed name and having the same number of arguments
	 * @param name The op name
	 * @param argCount The argument count
	 * @return a [possibly empty] set of matching operation signatures
	 */
	protected Set<OperationSignature> getOpSigs(String name, int argCount) {
		Set<OperationSignature> set = new HashSet<OperationSignature>();
		TreeSet<OperationSignature> opSigs = operations.get(name);
		for(OperationSignature os: opSigs) {
			if(os.getArgCount()==argCount) {
				set.add(os);
			} else {
				if(os.getArgCount()>argCount) {
					break;
				}
			}
		}
		return set;
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
		if(attributeNames.contains(propertyName)) {
			_setAttribute(propertyName, newValue);
		}
		getMetaClass().setProperty(this, propertyName, newValue);
	}
	
	/**
	 * Sets the named attribute
	 * @param name The attribute name
	 * @param value The attribute value
	 */
	protected void _setAttribute(String name, Object value) {
		try {
			connection.setAttribute(objectName, new Attribute(name, value));			
		} catch (Exception e) {
			throw new RuntimeException("Failed to set attribute value [" + name + "] from MBean [" + objectName + "]", e);
		}
	}
	
	/**
	 * <p>Title: UnknownClass</p>
	 * <p>Description: Synthetic class to represent a unknown class that could not be classloaded</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.gmx.MetaMBean.UnknownClass</code></p>
	 */
	public static class UnknownClass {
		public boolean equals(Object obj) {
			return (obj instanceof Class<?>); 
		}
	}
	
	/**
	 * <p>Title: OperationSignature</p>
	 * <p>Description: A container class to provide detailed signature matching of provided arguments to operation signatures.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.gmx.MetaMBean.OperationSignature</code></p>
	 */
	public static class OperationSignature implements Comparable<OperationSignature> {
		/** The classes represented in the sgnature */
		protected final Class<?>[] signature;
		/** The classes represented in the sgnature */
		protected final String[] strSignature;
		/** The op name */
		protected final String opName;
		
		
		/**
		 * Creates a new OperationSignature
		 * @param info The MBean's MBeanOperationInfo
		 * @return an OperationSignature for the passed MBeanOperationInfo 
		 */
		public static OperationSignature newInstance(MBeanOperationInfo info) {
			if(info==null) throw new IllegalArgumentException("The passed info was null", new Throwable());
			return new OperationSignature(info.getName(), info.getSignature());
		}
		
		/**
		 * Returns the number of operation parameters
		 * @return the number of operation parameters
		 */
		public int getArgCount() {
			return strSignature.length;
		}
		
		/**
		 * Creates a new OperationSignature
		 * @param opName The operation name
		 * @param infos An array of the MBean operation info signatures
		 */
		private OperationSignature(String opName, MBeanParameterInfo...infos) {	
			this.opName = opName;
			List<Class<?>> sig = new ArrayList<Class<?>>(infos==null ? 0 : infos.length);	
			List<String> strSig = new ArrayList<String>(infos==null ? 0 : infos.length);
			if(infos!=null) {
				for(MBeanParameterInfo pinfo: infos) {
					String className = pinfo.getType();
					strSig.add(className);					
					if(Primitive.isPrimitiveName(className)) {
						sig.add(Primitive.getPrimitive(className).getPclazz());
					} else {
						Class<?> clazz = null;
						try {
							clazz = Class.forName(className);							
						} catch (Exception e) {
							clazz = UnknownClass.class;
						}						
						sig.add(clazz);
					}
				}
			}
			signature = sig.toArray(new Class[sig.size()]);
			strSignature = strSig.toArray(new String[strSig.size()]);
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return Arrays.hashCode(strSignature);
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {			
			if(obj instanceof OperationSignature) return Arrays.deepEquals(strSignature, ((OperationSignature)obj).strSignature);
			return false;			
		}

		/**
		 * Returns the operation signature
		 * @return the signature
		 */
		public Class<?>[] getSignature() {
			return signature.clone();
		}

		/**
		 * Returns the operation signature as a string array
		 * @return the stringified signature
		 */
		public String[] getStrSignature() {
			return strSignature;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(OperationSignature os) {
			Integer thisArgCount = signature.length;
			Integer thatArgCount = os.signature.length;
			if(thisArgCount.intValue()==thatArgCount.intValue()) {
				return (this.hashCode()<os.hashCode() ? -1 : (this.hashCode()==os.hashCode() ? 0 : 1));
			}
			return (thisArgCount<thatArgCount ? -1 : (thisArgCount==thatArgCount ? 0 : 1));
		}

		/**
		 * Constructs a <code>String</code> with key attributes in name = value format.
		 * @return a <code>String</code> representation of this object.
		 */
		@Override
		public String toString() {
		    final String TAB = "\n\t";
		    StringBuilder retValue = new StringBuilder("OperationSignature [")
		    	.append(TAB).append("Name = ").append(opName)
		    	.append(TAB).append("Arg Count = ").append(signature.length)
		        .append(TAB).append("strSignature = ").append(Arrays.toString(this.strSignature))
		        .append(TAB).append("hashCode = ").append(this.hashCode())
		        .append("\n]");    
		    return retValue.toString();
		}

		/**
		 * @return the opName
		 */
		public String getOpName() {
			return opName;
		}
	}

}
