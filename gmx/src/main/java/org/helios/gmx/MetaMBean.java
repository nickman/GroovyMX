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
import groovy.lang.MetaClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

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
	protected final Map<String, Set<OperationSignature>> operations = new HashMap<String, Set<OperationSignature>>();
	
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
				Set<OperationSignature> opSigs = operations.get(minfo.getName());
				if(opSigs==null) {
					opSigs = new HashSet<OperationSignature>();
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
		// TODO Auto-generated method stub
		return null;
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
		return null;
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
	public Object invokeMethod(String name, Object args) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see groovy.lang.GroovyObject#setMetaClass(groovy.lang.MetaClass)
	 */
	@Override
	public void setMetaClass(MetaClass metaClass) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see groovy.lang.GroovyObject#setProperty(java.lang.String, java.lang.Object)
	 */
	@Override
	public void setProperty(String propertyName, Object newValue) {
		// TODO Auto-generated method stub

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
	public static class OperationSignature {
		/** The classes represented in the sgnature */
		protected final Class<?>[] signature;
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
			StringBuilder b = new StringBuilder(getClass().getName());
			if(infos!=null) {
				for(MBeanParameterInfo pinfo: infos) {
					String className = pinfo.getType();
					if(Primitive.isPrimitiveName(className)) {
						sig.add(Primitive.getPrimitive(className).getPclazz());
					} else {
						Class<?> clazz = null;
						try {
							clazz = Class.forName(className);							
						} catch (Exception e) {
							clazz = UnknownClass.class;
						}
						b.append(clazz.getName());
						sig.add(clazz);
					}
				}
			}
			signature = sig.toArray(new Class[sig.size()]);
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
	}

}