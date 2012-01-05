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
package org.helios.gmx.jmx;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;

/**
 * <p>Title: RuntimeMBeanServerConnection</p>
 * <p>Description: A wrapper around an {@link javax.management.MBeanServerConnection} that converts all checked exceptions to runtime exceptions.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.jmx.RuntimeMBeanServerConnection</code></p>
 */
public class RuntimeMBeanServerConnection implements MBeanServerConnection {
	/** The inner MBeanServer delegate */
	protected final MBeanServerConnection innerConnection;	
	
	/**
	 * Returns a RuntimeMBeanServerConnection wrapper for the passed inner connection
	 * @param innerConnection The inner MBeanServer delegate to wrap
	 * @return a RuntimeMBeanServerConnection wrapper for the passed inner connection
	 */
	public static RuntimeMBeanServerConnection getInstance(MBeanServerConnection innerConnection) {
		return new RuntimeMBeanServerConnection(innerConnection);
	}
	
	/**
	 * Creates a new RuntimeMBeanServerConnection 
	 * @param innerConnection The inner MBeanServer delegate
	 */
	protected RuntimeMBeanServerConnection(MBeanServerConnection innerConnection) {
		this.innerConnection = innerConnection;
	}
	
    /**
     * <p>Instantiates and registers an MBean in the MBean server.  The
     * MBean server will use its {@link
     * javax.management.loading.ClassLoaderRepository Default Loader
     * Repository} to load the class of the MBean.  An object name is
     * associated to the MBean.	 If the object name given is null, the
     * MBean must provide its own name by implementing the {@link
     * javax.management.MBeanRegistration MBeanRegistration} interface
     * and returning the name from the {@link
     * MBeanRegistration#preRegister preRegister} method.</p>
     *
     * <p>This method is equivalent to {@link
     * #createMBean(String,ObjectName,Object[],String[])
     * createMBean(className, name, (Object[]) null, (String[])
     * null)}.</p>
     *
     * @param className The class name of the MBean to be instantiated.	   
     * @param name The object name of the MBean. May be null.	 
     *
     * @return An <CODE>ObjectInstance</CODE>, containing the
     * <CODE>ObjectName</CODE> and the Java class name of the newly
     * instantiated MBean.  If the contained <code>ObjectName</code>
     * is <code>n</code>, the contained Java class name is
     * <code>{@link #getMBeanInfo getMBeanInfo(n)}.getClassName()</code>.
     */
    public ObjectInstance createMBean(String className, ObjectName name) {
    	try {
    		return innerConnection.createMBean(className, name);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [createMBean(String className, ObjectName name)]", e);    		
    	}
    }

    /**
     * <p>Instantiates and registers an MBean in the MBean server.  The
     * class loader to be used is identified by its object name. An
     * object name is associated to the MBean. If the object name of
     * the loader is null, the ClassLoader that loaded the MBean
     * server will be used.  If the MBean's object name given is null,
     * the MBean must provide its own name by implementing the {@link
     * javax.management.MBeanRegistration MBeanRegistration} interface
     * and returning the name from the {@link
     * MBeanRegistration#preRegister preRegister} method.</p>
     *
     * <p>This method is equivalent to {@link
     * #createMBean(String,ObjectName,ObjectName,Object[],String[])
     * createMBean(className, name, loaderName, (Object[]) null,
     * (String[]) null)}.</p>
     *
     * @param className The class name of the MBean to be instantiated.	   
     * @param name The object name of the MBean. May be null.	 
     * @param loaderName The object name of the class loader to be used.
     *
     * @return An <CODE>ObjectInstance</CODE>, containing the
     * <CODE>ObjectName</CODE> and the Java class name of the newly
     * instantiated MBean.  If the contained <code>ObjectName</code>
     * is <code>n</code>, the contained Java class name is
     * <code>{@link #getMBeanInfo getMBeanInfo(n)}.getClassName()</code>.
     *
     */
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) {
    	try {
    		return innerConnection.createMBean(className, name, loaderName);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [createMBean(String className, ObjectName name, ObjectName loaderName)]", e);    		
    	}
    }


    /**
     * Instantiates and registers an MBean in the MBean server.  The
     * MBean server will use its {@link
     * javax.management.loading.ClassLoaderRepository Default Loader
     * Repository} to load the class of the MBean.  An object name is
     * associated to the MBean.  If the object name given is null, the
     * MBean must provide its own name by implementing the {@link
     * javax.management.MBeanRegistration MBeanRegistration} interface
     * and returning the name from the {@link
     * MBeanRegistration#preRegister preRegister} method.
     *
     * @param className The class name of the MBean to be instantiated.
     * @param name The object name of the MBean. May be null.
     * @param params An array containing the parameters of the
     * constructor to be invoked.
     * @param signature An array containing the signature of the
     * constructor to be invoked.
     *
     * @return An <CODE>ObjectInstance</CODE>, containing the
     * <CODE>ObjectName</CODE> and the Java class name of the newly
     * instantiated MBean.  If the contained <code>ObjectName</code>
     * is <code>n</code>, the contained Java class name is
     * <code>{@link #getMBeanInfo getMBeanInfo(n)}.getClassName()</code>.
     *
     *
     */
    public ObjectInstance createMBean(String className, ObjectName name, Object params[], String signature[]) {
    	try {
    		return innerConnection.createMBean(className, name, params, signature);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [createMBean(String className, ObjectName name, Object params[], String signature[])]", e);    		
    	}    	
    }

    /**
     * Instantiates and registers an MBean in the MBean server.  The
     * class loader to be used is identified by its object name. An
     * object name is associated to the MBean. If the object name of
     * the loader is not specified, the ClassLoader that loaded the
     * MBean server will be used.  If the MBean object name given is
     * null, the MBean must provide its own name by implementing the
     * {@link javax.management.MBeanRegistration MBeanRegistration}
     * interface and returning the name from the {@link
     * MBeanRegistration#preRegister preRegister} method.
     *
     * @param className The class name of the MBean to be instantiated.
     * @param name The object name of the MBean. May be null.
     * @param params An array containing the parameters of the
     * constructor to be invoked.
     * @param signature An array containing the signature of the
     * constructor to be invoked.
     * @param loaderName The object name of the class loader to be used.
     *
     * @return An <CODE>ObjectInstance</CODE>, containing the
     * <CODE>ObjectName</CODE> and the Java class name of the newly
     * instantiated MBean.  If the contained <code>ObjectName</code>
     * is <code>n</code>, the contained Java class name is
     * <code>{@link #getMBeanInfo getMBeanInfo(n)}.getClassName()</code>.
     *
     */
    public ObjectInstance createMBean(String className, ObjectName name,ObjectName loaderName, Object params[], String signature[]) {
    	try {
    		return innerConnection.createMBean(className, name, loaderName, params, signature);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [createMBean(String className, ObjectName name, ObjectName loaderName, Object params[], String signature[])]", e);    		
    	}    	    	
    }

    /**
     * Unregisters an MBean from the MBean server. The MBean is
     * identified by its object name. Once the method has been
     * invoked, the MBean may no longer be accessed by its object
     * name.
     *
     * @param name The object name of the MBean to be unregistered.
     *
     */
    public void unregisterMBean(ObjectName name) {
    	try {
    		innerConnection.unregisterMBean(name);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [unregisterMBean(ObjectName name)]", e);    		
    	}    	    	
    }

    /**
     * Gets the <CODE>ObjectInstance</CODE> for a given MBean
     * registered with the MBean server.
     *
     * @param name The object name of the MBean.
     *
     * @return The <CODE>ObjectInstance</CODE> associated with the MBean
     * specified by <VAR>name</VAR>.  The contained <code>ObjectName</code>
     * is <code>name</code> and the contained class name is
     * <code>{@link #getMBeanInfo getMBeanInfo(name)}.getClassName()</code>.
     *
     */
    public ObjectInstance getObjectInstance(ObjectName name) {
    	try {
    		return innerConnection.getObjectInstance(name);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [getObjectInstance(ObjectName name)]", e);    		
    	}    	    	
    }
	 

    /**
     * Gets MBeans controlled by the MBean server. This method allows
     * any of the following to be obtained: All MBeans, a set of
     * MBeans specified by pattern matching on the
     * <CODE>ObjectName</CODE> and/or a Query expression, a specific
     * MBean. When the object name is null or no domain and key
     * properties are specified, all objects are to be selected (and
     * filtered if a query is specified). It returns the set of
     * <CODE>ObjectInstance</CODE> objects (containing the
     * <CODE>ObjectName</CODE> and the Java Class name) for the
     * selected MBeans.
     *
     * @param name The object name pattern identifying the MBeans to
     * be retrieved. If null or no domain and key properties are
     * specified, all the MBeans registered will be retrieved.
     * @param query The query expression to be applied for selecting
     * MBeans. If null no query expression will be applied for
     * selecting MBeans.
     *
     * @return A set containing the <CODE>ObjectInstance</CODE>
     * objects for the selected MBeans.  If no MBean satisfies the
     * query an empty list is returned.
     *
     */
    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
    	try {
    		return innerConnection.queryMBeans(name, query);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [queryMBeans(ObjectName name, QueryExp query)]", e);    		
    	}    	    	    	
    }
	    

    /**
     * Gets the names of MBeans controlled by the MBean server. This
     * method enables any of the following to be obtained: The names
     * of all MBeans, the names of a set of MBeans specified by
     * pattern matching on the <CODE>ObjectName</CODE> and/or a Query
     * expression, a specific MBean name (equivalent to testing
     * whether an MBean is registered). When the object name is null
     * or no domain and key properties are specified, all objects are
     * selected (and filtered if a query is specified). It returns the
     * set of ObjectNames for the MBeans selected.
     *
     * @param name The object name pattern identifying the MBean names
     * to be retrieved. If null or no domain and key properties are
     * specified, the name of all registered MBeans will be retrieved.
     * @param query The query expression to be applied for selecting
     * MBeans. If null no query expression will be applied for
     * selecting MBeans.
     *
     * @return A set containing the ObjectNames for the MBeans
     * selected.  If no MBean satisfies the query, an empty list is
     * returned.
     */
    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
    	try {
    		return innerConnection.queryNames(name, query);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [queryNames(ObjectName name, QueryExp query)]", e);    		
    	}    	    	    	    	
    }
	    



    /**
     * Checks whether an MBean, identified by its object name, is
     * already registered with the MBean server.
     *
     * @param name The object name of the MBean to be checked.
     *
     * @return True if the MBean is already registered in the MBean
     * server, false otherwise.
     *
     */
    public boolean isRegistered(ObjectName name) {
    	try {
    		return innerConnection.isRegistered(name);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [isRegistered(ObjectName name)]", e);    		
    	}    	    	    	
    }
	    


    /**
     * Returns the number of MBeans registered in the MBean server.
     *
     * @return the number of MBeans registered.
     */
    public Integer getMBeanCount() {
    	try {
    		return innerConnection.getMBeanCount();    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [getMBeanCount()]", e);    		
    	}    	    	    	    	
    }
	 

    /**
     * Gets the value of a specific attribute of a named MBean. The MBean
     * is identified by its object name.
     *
     * @param name The object name of the MBean from which the
     * attribute is to be retrieved.
     * @param attribute A String specifying the name of the attribute
     * to be retrieved.
     *
     * @return	The value of the retrieved attribute.
     *
     * @see #setAttribute
     */
    public Object getAttribute(ObjectName name, String attribute) {
    	try {
    		return innerConnection.getAttribute(name, attribute);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [getAttribute(ObjectName name, String attribute)]", e);    		
    	}    	    	    	    	    	
    }

    /**
     * Gets the value of a specific attribute of a named MBean. The MBean
     * is identified by its object name.
     *
     * @param name The object name of the MBean from which the
     * attribute is to be retrieved.
     * @param attribute A String specifying the name of the attribute
     * to be retrieved.
     * @param type The expected type of the return result
     *
     * @return	The value of the retrieved attribute.
     *
     * @see #setAttribute
     * @param <T> The expected type of the return result
     */    
    public <T> T getAttribute(ObjectName name, String attribute, Class<T> type) {
    	try {
    		Object result =  innerConnection.getAttribute(name, attribute);
    		return type.cast(result);
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [getAttribute(ObjectName name, String attribute, Class type)]", e);    		
    	}    	    	    	    	    	
    	
    }


    /**
     * Enables the values of several attributes of a named MBean. The MBean
     * is identified by its object name.
     *
     * @param name The object name of the MBean from which the
     * attributes are retrieved.
     * @param attributes A list of the attributes to be retrieved.
     *
     * @return The list of the retrieved attributes.
     *
     * @see #setAttributes
     */
    public AttributeList getAttributes(ObjectName name, String[] attributes) {
    	try {
    		return innerConnection.getAttributes(name, attributes);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [getAttributes(ObjectName name, String[] attributes)]", e);    		
    	}    	    	    	    	    	    	
    }

    /**
     * Sets the value of a specific attribute of a named MBean. The MBean
     * is identified by its object name.
     *
     * @param name The name of the MBean within which the attribute is
     * to be set.
     * @param attribute The identification of the attribute to be set
     * and the value it is to be set to.
     *
     * @see #getAttribute
     */
    public void setAttribute(ObjectName name, Attribute attribute) {
    	try {
    		innerConnection.setAttribute(name, attribute);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [setAttribute(ObjectName name, Attribute attribute)]", e);    		
    	}    	    	    	    	    	    	    	
    }



    /**
     * Sets the values of several attributes of a named MBean. The MBean is
     * identified by its object name.
     *
     * @param name The object name of the MBean within which the
     * attributes are to be set.
     * @param attributes A list of attributes: The identification of
     * the attributes to be set and the values they are to be set to.
     *
     * @return The list of attributes that were set, with their new
     * values.
     *
     * @see #getAttributes
     */
    public AttributeList setAttributes(ObjectName name,AttributeList attributes) {
    	try {
    		return innerConnection.setAttributes(name, attributes);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [setAttributes(ObjectName name,AttributeList attributes)]", e);    		
    	}    	    	    	    	    	    	    	
    }
	

    /**
     * Invokes an operation on an MBean.
     *
     * @param name The object name of the MBean on which the method is
     * to be invoked.
     * @param operationName The name of the operation to be invoked.
     * @param params An array containing the parameters to be set when
     * the operation is invoked
     * @param signature An array containing the signature of the
     * operation. The class objects will be loaded using the same
     * class loader as the one used for loading the MBean on which the
     * operation was invoked.
     *
     * @return The object returned by the operation, which represents
     * the result of invoking the operation on the MBean specified.
     *
     */
    public Object invoke(ObjectName name, String operationName,Object params[], String signature[]) {
    	try {
    		return innerConnection.invoke(name, operationName, params, signature);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [invoke(ObjectName name, String operationName,Object params[], String signature[])]", e);    		
    	}    	    	    	    	    	    	    	    	
    }
    
    /**
     * Invokes an operation on an MBean.
     *
     * @param type The expected return type of the invocation
     * @param name The object name of the MBean on which the method is
     * to be invoked.
     * @param operationName The name of the operation to be invoked.
     * @param params An array containing the parameters to be set when
     * the operation is invoked
     * @param signature An array containing the signature of the
     * operation. The class objects will be loaded using the same
     * class loader as the one used for loading the MBean on which the
     * operation was invoked.
     *
     * @return The object returned by the operation, which represents
     * the result of invoking the operation on the MBean specified.
     *
     * @param <T> The expected return type of the invocation
     */
    public <T> T invoke(Class<T> type, ObjectName name, String operationName,Object params[], String signature[]) {
    	try {
    		Object result =  innerConnection.invoke(name, operationName, params, signature);
    		if(result==null || result instanceof Void ) return null;
    		return type.cast(result);
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [invoke(Class type, ObjectName name, String operationName,Object params[], String signature[])]", e);    		
    	}    	    	    	    	    	    	    	    	
    }    
 

  
    /**
     * Returns the default domain used for naming the MBean.
     * The default domain name is used as the domain part in the ObjectName
     * of MBeans if no domain is specified by the user.
     *
     * @return the default domain.
     */
    public String getDefaultDomain() {
    	try {
    		return innerConnection.getDefaultDomain();    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [getDefaultDomain()]", e);    		
    	}    	    	    	    	    	    	    	    	    	
    }
	    

    /**
     * <p>Returns the list of domains in which any MBean is currently
     * registered.  A string is in the returned array if and only if
     * there is at least one MBean registered with an ObjectName whose
     * {@link ObjectName#getDomain() getDomain()} is equal to that
     * string.  The order of strings within the returned array is
     * not defined.</p>
     *
     * @return the list of domains.
     */
    public String[] getDomains() {
    	try {
    		return innerConnection.getDomains();    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [getDomains()]", e);    		
    	}    	    	    	    	    	    	    	    	    	    	
    }

    /**
     * <p>Adds a listener to a registered MBean.</p>
     *
     * <P> A notification emitted by an MBean will be forwarded by the
     * MBeanServer to the listener.  If the source of the notification
     * is a reference to an MBean object, the MBean server will replace it
     * by that MBean's ObjectName.  Otherwise the source is unchanged.
     *
     * @param name The name of the MBean on which the listener should
     * be added.
     * @param listener The listener object which will handle the
     * notifications emitted by the registered MBean.
     * @param filter The filter object. If filter is null, no
     * filtering will be performed before handling notifications.
     * @param handback The context to be sent to the listener when a
     * notification is emitted.
     *
     * @see #removeNotificationListener(ObjectName, NotificationListener)
     * @see #removeNotificationListener(ObjectName, NotificationListener,
     * NotificationFilter, Object)
     */
    public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) {
    	try {
    		innerConnection.addNotificationListener(name, listener, filter, handback);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback)]", e);    		
    	}    	    	    	    	    	    	    	    	    	    	
    }



    /**
     * <p>Adds a listener to a registered MBean.</p>
     *
     * <p>A notification emitted by an MBean will be forwarded by the
     * MBeanServer to the listener.  If the source of the notification
     * is a reference to an MBean object, the MBean server will
     * replace it by that MBean's ObjectName.  Otherwise the source is
     * unchanged.</p>
     *
     * <p>The listener object that receives notifications is the one
     * that is registered with the given name at the time this method
     * is called.  Even if it is subsequently unregistered, it will
     * continue to receive notifications.</p>
     *
     * @param name The name of the MBean on which the listener should
     * be added.
     * @param listener The object name of the listener which will
     * handle the notifications emitted by the registered MBean.
     * @param filter The filter object. If filter is null, no
     * filtering will be performed before handling notifications.
     * @param handback The context to be sent to the listener when a
     * notification is emitted.
     *
     * @see #removeNotificationListener(ObjectName, ObjectName)
     * @see #removeNotificationListener(ObjectName, ObjectName,
     * NotificationFilter, Object)
     */
    public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) {
    	try {
    		innerConnection.addNotificationListener(name, listener, filter, handback);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)]", e);    		
    	}    	    	    	    	    	    	    	    	    	    	    	
    }



    /**
     * Removes a listener from a registered MBean.
     *
     * <P> If the listener is registered more than once, perhaps with
     * different filters or callbacks, this method will remove all
     * those registrations.
     *
     * @param name The name of the MBean on which the listener should
     * be removed.
     * @param listener The object name of the listener to be removed.
     *
     * @see #addNotificationListener(ObjectName, ObjectName,
     * NotificationFilter, Object)
     */
    public void removeNotificationListener(ObjectName name, ObjectName listener) {
    	try {
    		innerConnection.removeNotificationListener(name, listener);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [removeNotificationListener(ObjectName name, ObjectName listener)]", e);    		
    	}    	    	    	    	    	    	    	    	    	    	    	
    }

    /**
     * <p>Removes a listener from a registered MBean.</p>
     *
     * <p>The MBean must have a listener that exactly matches the
     * given <code>listener</code>, <code>filter</code>, and
     * <code>handback</code> parameters.  If there is more than one
     * such listener, only one is removed.</p>
     *
     * <p>The <code>filter</code> and <code>handback</code> parameters
     * may be null if and only if they are null in a listener to be
     * removed.</p>
     *
     * @param name The name of the MBean on which the listener should
     * be removed.
     * @param listener The object name of the listener to be removed.
     * @param filter The filter that was specified when the listener
     * was added.
     * @param handback The handback that was specified when the
     * listener was added.
     *
     * @see #addNotificationListener(ObjectName, ObjectName,
     * NotificationFilter, Object)
     *
     */
    public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) {
    	try {
    		innerConnection.removeNotificationListener(name, listener, filter, handback);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)]", e);    		
    	}    	    	    	    	    	    	    	    	    	    	    	    	
    }


    /**
     * <p>Removes a listener from a registered MBean.</p>
     *
     * <P> If the listener is registered more than once, perhaps with
     * different filters or callbacks, this method will remove all
     * those registrations.
     *
     * @param name The name of the MBean on which the listener should
     * be removed.
     * @param listener The listener to be removed.
     *
     * @see #addNotificationListener(ObjectName, NotificationListener,
     * NotificationFilter, Object)
     */
    public void removeNotificationListener(ObjectName name, NotificationListener listener) {
    	try {
    		innerConnection.removeNotificationListener(name, listener);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [removeNotificationListener(ObjectName name, NotificationListener listener)]", e);    		
    	}    	    	    	    	    	    	    	    	    	    	    	    	    	
    }

    /**
     * <p>Removes a listener from a registered MBean.</p>
     *
     * <p>The MBean must have a listener that exactly matches the
     * given <code>listener</code>, <code>filter</code>, and
     * <code>handback</code> parameters.  If there is more than one
     * such listener, only one is removed.</p>
     *
     * <p>The <code>filter</code> and <code>handback</code> parameters
     * may be null if and only if they are null in a listener to be
     * removed.</p>
     *
     * @param name The name of the MBean on which the listener should
     * be removed.
     * @param listener The listener to be removed.
     * @param filter The filter that was specified when the listener
     * was added.
     * @param handback The handback that was specified when the
     * listener was added.
     *
     * @see #addNotificationListener(ObjectName, NotificationListener,
     * NotificationFilter, Object)
     */
    public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) {
    	try {
    		innerConnection.removeNotificationListener(name, listener, filter, handback);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)]", e);    		
    	}    	    	    	    	    	    	    	    	    	    	    	
    }

    /**
     * This method discovers the attributes and operations that an
     * MBean exposes for management.
     *
     * @param name The name of the MBean to analyze
     *
     * @return An instance of <CODE>MBeanInfo</CODE> allowing the
     * retrieval of all attributes and operations of this MBean.
     *
     */
    public MBeanInfo getMBeanInfo(ObjectName name) {
    	try {
    		return innerConnection.getMBeanInfo(name);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [getMBeanInfo(ObjectName name)]", e);    		
    	}    	    	    	    	    	    	    	    	    	    	    	    	
    }

 
    /**
     * <p>Returns true if the MBean specified is an instance of the
     * specified class, false otherwise.</p>
     *
     * <p>If <code>name</code> does not name an MBean, this method
     * throws {@link InstanceNotFoundException}.</p>
     *
     * <p>Otherwise, let<br>
     * X be the MBean named by <code>name</code>,<br>
     * L be the ClassLoader of X,<br>
     * N be the class name in X's {@link MBeanInfo}.</p>
     *
     * <p>If N equals <code>className</code>, the result is true.</p>
     *
     * <p>Otherwise, if L successfully loads <code>className</code>
     * and X is an instance of this class, the result is true.
     *
     * <p>Otherwise, if L successfully loads both N and
     * <code>className</code>, and the second class is assignable from
     * the first, the result is true.</p>
     *
     * <p>Otherwise, the result is false.</p>
     * 
     * @param name The <CODE>ObjectName</CODE> of the MBean.
     * @param className The name of the class.
     *
     * @return true if the MBean specified is an instance of the
     * specified class according to the rules above, false otherwise.
     *
     * @see Class#isInstance
     */
    public boolean isInstanceOf(ObjectName name, String className) {
    	try {
    		return innerConnection.isInstanceOf(name, className);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [isInstanceOf(ObjectName name, String className)]", e);    		
    	}    	    	    	    	    	    	    	    	    	    	    	    	    	
    }


}
