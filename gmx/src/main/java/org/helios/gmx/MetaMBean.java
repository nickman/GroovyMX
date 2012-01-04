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
import java.util.HashMap;
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
				opSigs.add(OperationSignature.newInstance(minfo));
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
		TreeSet<OperationSignature> opSigs = operations.get(name);
		Object[] args = null;
		if(arg.getClass().isArray()) {
			int length = Array.getLength(arg);
			args = new Object[length];
			System.arraycopy(arg, 0, args, 0, length);
		} else {
			args = new Object[]{arg};
		}
		if(opSigs!=null) {
			if(opSigs.size()==1) {
				try {
					return connection.invoke(objectName, name, args, opSigs.iterator().next().getStrSignature());
				} catch (Exception e) {
					throw new RuntimeException("Failed to invoke operation [" + name + "]", e);
				}
			} else {
				Iterator<OperationSignature> opSigIter = opSigs.iterator();
				boolean argCountOverload = false;
				for(; opSigIter.hasNext();) {
					OperationSignature os = opSigIter.next();
					if(os.getSignature().length==args.length) {
						if(!opSigIter.hasNext() || opSigIter.next().getSignature().length!=args.length) {
							try {
								return connection.invoke(objectName, name, args, os.getStrSignature());
							} catch (Exception e) {
								throw new RuntimeException("Failed to invoke operation [" + name + "]", e);
							}													
						} else {
							argCountOverload = true;
							break;
						}
					}					
				}
				if(argCountOverload) {
					throw new UnsupportedOperationException("Overloaded MBean Operations Not Supported Yet. (Coming Soon)", new Throwable());
				} else {
					return getMetaClass().invokeMethod(this, name, args);
				}								
			}
		} else {
			return getMetaClass().invokeMethod(this, name, args);
		}
		
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
	 * Retrieves the named attribute
	 * @param name The attribute name
	 * @return The attribute value
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
		
		/** The hash code of this instance */
		protected final int hashCode;
		
		/**
		 * Creates a new OperationSignature
		 * @param info The MBean's MBeanOperationInfo
		 * @return an OperationSignature for the passed MBeanOperationInfo 
		 */
		public static OperationSignature newInstance(MBeanOperationInfo info) {
			if(info==null) throw new IllegalArgumentException("The passed info was null", new Throwable());
			return new OperationSignature(info.getSignature());
		}
		
		/**
		 * Creates a new OperationSignature
		 * @param infos An array of the MBean operation info signatures
		 */
		private OperationSignature(MBeanParameterInfo...infos) {			
			List<Class<?>> sig = new ArrayList<Class<?>>(infos==null ? 0 : infos.length);	
			List<String> strSig = new ArrayList<String>(infos==null ? 0 : infos.length);
			StringBuilder b = new StringBuilder(getClass().getName());
			if(infos!=null) {
				for(MBeanParameterInfo pinfo: infos) {
					String className = pinfo.getType();
					strSig.add(className);
					b.append(className);
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
			hashCode = b.toString().hashCode();
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return hashCode;
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			if(obj==null) return false;
			if(obj instanceof OperationSignature) {
				return hashCode==obj.hashCode();
			} else {
				return false;
			}
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
			return thisArgCount.compareTo(thatArgCount);
		}
	}

}
