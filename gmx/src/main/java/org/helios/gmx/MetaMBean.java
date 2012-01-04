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

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

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

}
