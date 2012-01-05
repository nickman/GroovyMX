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

import java.io.ObjectInputStream;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.loading.ClassLoaderRepository;

/**
 * <p>Title: RuntimeMBeanServer</p>
 * <p>Description: A wrapper around an {@link javax.management.MBeanServer} that converts all checked exceptions to runtime exceptions.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.jmx.RuntimeMBeanServer</code></p>
 */
public class RuntimeMBeanServer extends RuntimeMBeanServerConnection implements MBeanServer {
	/** A reference to the delegate MBeanServer */
	protected final MBeanServer innerServer;
	/**
	 * Creates a new RuntimeMBeanServer
	 * @param innerConnection The inner MBeanServer delegate
	 */
	protected RuntimeMBeanServer(MBeanServer innerConnection) {
		super(innerConnection);
		innerServer = innerConnection;
	}
	
	/**
	 * Returns a RuntimeMBeanServer wrapper for the passed inner connection
	 * @param innerServer The inner MBeanServer delegate to wrap
	 * @return a RuntimeMBeanServer wrapper for the passed inner server
	 */
	public static RuntimeMBeanServer getInstance(MBeanServer innerServer) {
		return new RuntimeMBeanServer(innerServer);
	}	

    /**
     * <p>De-serializes a byte array in the context of a given MBean
     * class loader.  The class loader is found by loading the class
     * <code>className</code> through the {@link
     * javax.management.loading.ClassLoaderRepository Class Loader
     * Repository}.  The resultant class's class loader is the one to
     * use.
     *
     * @param className The name of the class whose class loader should be
     * used for the de-serialization.
     * @param data The byte array to be de-sererialized.
     *
     * @return  The de-serialized object stream.
     *
     * @deprecated Use {@link #getClassLoaderRepository} to obtain the
     * class loader repository and use it to deserialize.
     */
    @Deprecated
    @Override
    public ObjectInputStream deserialize(String className, byte[] data) {
    	try {
    		return innerServer.deserialize(className, data);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [deserialize(String className, byte[] data)]", e);    		
    	}    	
    }


   
    /**
     * <p>De-serializes a byte array in the context of a given MBean
     * class loader.  The class loader is the one that loaded the
     * class with name "className".  The name of the class loader to
     * be used for loading the specified class is specified.  If null,
     * the MBean Server's class loader will be used.</p>
     *
     * @param className The name of the class whose class loader should be
     * used for the de-serialization.
     * @param data The byte array to be de-sererialized.
     * @param loaderName The name of the class loader to be used for
     * loading the specified class.  If null, the MBean Server's class
     * loader will be used.
     *
     * @return  The de-serialized object stream.
     *
     * @deprecated Use {@link #getClassLoader getClassLoader} to obtain
     * the class loader for deserialization.
     */
    @Deprecated
    @Override
    public ObjectInputStream deserialize(String className, ObjectName loaderName, byte[] data) {
    	try {
    		return innerServer.deserialize(className, loaderName, data);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [deserialize(String className, ObjectName loaderName, byte[] data)]", e);    		
    	}    	    	
    }

    /**
     * <p>De-serializes a byte array in the context of the class loader 
     * of an MBean.</p>
     *
     * @param name The name of the MBean whose class loader should be
     * used for the de-serialization.
     * @param data The byte array to be de-sererialized.
     *
     * @return The de-serialized object stream.
     *
     * @deprecated Use {@link #getClassLoaderFor getClassLoaderFor} to
     * obtain the appropriate class loader for deserialization.
     */
    @Deprecated
    @Override
    public ObjectInputStream deserialize(ObjectName name, byte[] data) {
    	try {
    		return innerServer.deserialize(name, data);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [deserialize(ObjectName name, byte[] data)]", e);    		
    	}    	    	    	
    }
    

    /**
     * <p>Return the named {@link java.lang.ClassLoader}.</p>
     *
     * @param loaderName The ObjectName of the ClassLoader.  May be
     * null, in which case the MBean server's own ClassLoader is
     * returned.
     *
     * @return The named ClassLoader.  If <var>l</var> is the actual
     * ClassLoader with that name, and <var>r</var> is the returned
     * value, then either:
     *
     * <ul>
     * <li><var>r</var> is identical to <var>l</var>; or
     * <li>the result of <var>r</var>{@link
     * ClassLoader#loadClass(String) .loadClass(<var>s</var>)} is the
     * same as <var>l</var>{@link ClassLoader#loadClass(String)
     * .loadClass(<var>s</var>)} for any string <var>s</var>.
     * </ul>
     *
     * What this means is that the ClassLoader may be wrapped in
     * another ClassLoader for security or other reasons.
     *
     */
    public ClassLoader getClassLoader(ObjectName loaderName) {
    	try {
    		return innerServer.getClassLoader(loaderName);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [getClassLoader(ObjectName loaderName)]", e);    		
    	}    	    	    	    	
    }
	

    /**
     * <p>Return the ClassLoaderRepository for this MBeanServer.
     * @return The ClassLoaderRepository for this MBeanServer.
     *
     */
    public ClassLoaderRepository getClassLoaderRepository() {
    	return innerServer.getClassLoaderRepository();
    }
    
    /**
     * <p>Return the {@link java.lang.ClassLoader} that was used for
     * loading the class of the named MBean.</p>
     *
     * @param mbeanName The ObjectName of the MBean.
     *
     * @return The ClassLoader used for that MBean.  If <var>l</var>
     * is the MBean's actual ClassLoader, and <var>r</var> is the
     * returned value, then either:
     *
     * <ul>
     * <li><var>r</var> is identical to <var>l</var>; or
     * <li>the result of <var>r</var>{@link
     * ClassLoader#loadClass(String) .loadClass(<var>s</var>)} is the
     * same as <var>l</var>{@link ClassLoader#loadClass(String)
     * .loadClass(<var>s</var>)} for any string <var>s</var>.
     * </ul>
     *
     * What this means is that the ClassLoader may be wrapped in
     * another ClassLoader for security or other reasons.
     */
    public ClassLoader getClassLoaderFor(ObjectName mbeanName) {
    	try {
    		return innerServer.getClassLoader(mbeanName);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [getClassLoader(ObjectName mbeanName)]", e);    		
    	}    	    	    	    	    	
    }


    /**
     * Registers a pre-existing object as an MBean with the MBean
     * server. If the object name given is null, the MBean must
     * provide its own name by implementing the {@link
     * javax.management.MBeanRegistration MBeanRegistration} interface
     * and returning the name from the {@link
     * MBeanRegistration#preRegister preRegister} method.
     *
     * @param object The  MBean to be registered as an MBean.	  
     * @param name The object name of the MBean. May be null.
     *
     * @return An <CODE>ObjectInstance</CODE>, containing the
     * <CODE>ObjectName</CODE> and the Java class name of the newly
     * registered MBean.  If the contained <code>ObjectName</code>
     * is <code>n</code>, the contained Java class name is
     * <code>{@link #getMBeanInfo getMBeanInfo(n)}.getClassName()</code>.
     */
    public ObjectInstance registerMBean(Object object, ObjectName name) {
    	try {
    		return innerServer.registerMBean(object, name);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [registerMBean(Object object, ObjectName name)]", e);    		
    	}    	    	    	    	    	    	
    }
    
    /**
     * <p>Instantiates an object using the list of all class loaders
     * registered in the MBean server's {@link
     * javax.management.loading.ClassLoaderRepository Class Loader
     * Repository}.  The object's class should have a public
     * constructor.  This method returns a reference to the newly
     * created object.	The newly created object is not registered in
     * the MBean server.</p>
     *
     * <p>This method is equivalent to {@link
     * #instantiate(String,Object[],String[])
     * instantiate(className, (Object[]) null, (String[]) null)}.</p>
     *
     * @param className The class name of the object to be instantiated.    
     *
     * @return The newly instantiated object.	 
     */
    public Object instantiate(String className) {
    	try {
    		return innerServer.instantiate(className);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [instantiate(String className)]", e);    		
    	}    	    	    	    	    	    	    	
    }



    /**
     * <p>Instantiates an object using the class Loader specified by its
     * <CODE>ObjectName</CODE>.	 If the loader name is null, the
     * ClassLoader that loaded the MBean Server will be used.  The
     * object's class should have a public constructor.	 This method
     * returns a reference to the newly created object.	 The newly
     * created object is not registered in the MBean server.</p>
     *
     * <p>This method is equivalent to {@link
     * #instantiate(String,ObjectName,Object[],String[])
     * instantiate(className, loaderName, (Object[]) null, (String[])
     * null)}.</p>
     *
     * @param className The class name of the MBean to be instantiated.	   
     * @param loaderName The object name of the class loader to be used.
     *
     * @return The newly instantiated object.	 
     *
     */
    public Object instantiate(String className, ObjectName loaderName) {
    	try {
    		return innerServer.instantiate(className, loaderName);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [instantiate(String className, ObjectName loaderName)]", e);    		
    	}    	    	    	    	    	    	    	
    	
    }

    /**
     * <p>Instantiates an object using the list of all class loaders
     * registered in the MBean server {@link
     * javax.management.loading.ClassLoaderRepository Class Loader
     * Repository}.  The object's class should have a public
     * constructor.  The call returns a reference to the newly created
     * object.	The newly created object is not registered in the
     * MBean server.</p>
     *
     * @param className The class name of the object to be instantiated.
     * @param params An array containing the parameters of the
     * constructor to be invoked.
     * @param signature An array containing the signature of the
     * constructor to be invoked.
     *
     * @return The newly instantiated object.	 
     */	   
    public Object instantiate(String className, Object params[], String signature[]) {
    	try {
    		return innerServer.instantiate(className, params, signature);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [instantiate(String className, Object params[], String signature[])]", e);    		
    	}    	    	    	    	    	    	
    	
    }

    /**
     * <p>Instantiates an object. The class loader to be used is
     * identified by its object name. If the object name of the loader
     * is null, the ClassLoader that loaded the MBean server will be
     * used.  The object's class should have a public constructor.
     * The call returns a reference to the newly created object.  The
     * newly created object is not registered in the MBean server.</p>
     *
     * @param className The class name of the object to be instantiated.
     * @param params An array containing the parameters of the
     * constructor to be invoked.
     * @param signature An array containing the signature of the
     * constructor to be invoked.
     * @param loaderName The object name of the class loader to be used.
     *
     * @return The newly instantiated object.	 
     *
     */	   
    public Object instantiate(String className, ObjectName loaderName,Object params[], String signature[]) {
    	try {
    		return innerServer.instantiate(className, loaderName, params, signature);    		
    	} catch (Exception e) {
    		throw new RuntimeMBeanServerException("Failed to invoke [instantiate(String className, ObjectName loaderName,Object params[], String signature[])]", e);    		
    	}    	    	    	    	    	    	    	
    }
    

}
