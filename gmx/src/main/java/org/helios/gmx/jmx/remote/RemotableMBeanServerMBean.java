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
package org.helios.gmx.jmx.remote;

import java.net.URL;

import groovy.lang.Closure;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;

/**
 * <p>Title: RemotableMBeanServerMBean</p>
 * <p>Description: MBean interface for {@link RemotableMBeanServer}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.jmx.remote.RemotableMBeanServerMBean</code></p>
 */
public interface RemotableMBeanServerMBean extends MBeanServer, MBeanRegistration {
	
	/**
	 * Invokes the closure extracted from the passed byte array and returns the result
	 * @param closureBytes The closure serialized as a byte array
	 * @param arguments optional arguments
	 * @return the return value of the closure
	 */
	public Object invokeClosure(byte[] closureBytes, Object arguments);
	
	/**
	 * Invokes the submitted script passing in the MBeanServer as a binding and returning the script's return value.
	 * @param script The script to execute.
	 * @param args Arguments to the script
	 * @return the script's return value
	 */
	public Object invokeScript(String script, Object...args);
	
	/**
	 * Invokes the passed closure and returns the result
	 * @param closure The closure
	 * @param arguments optional arguments
	 * @return the return value of the closure
	 */
	public Object invokeClosure(Closure<?> closure, Object arguments);	
	
	/**
	 * The URL of the reverse class loader
	 * @return the reverseClassLoadURL
	 */
	public URL getReverseClassLoadURL();

	/**
	 * This MBean's class loader name 
	 * @return the classLoader
	 */
	public String getClassLoader();

	/**
	 * The reverse class loader host
	 * @return the reverseClassLoadHost
	 */
	public String getReverseClassLoadHost();

	/**
	 * The reverse class loader port
	 * @return the reverseClassLoadPort
	 */
	public int getReverseClassLoadPort();
	
	/**
	 * Returns the JMX domain names of all located MBeanServers in this JVM
	 * @return the JMX domain names of all located MBeanServers in this JVM
	 */
	public String[] getMBeanServerDomains();
	
	/**
	 * Sets the reverse class loader URL
	 * @param reverseClassLoadURL The URL of the reverse class loader
	 */
	public void setReverseClassLoadURL(URL reverseClassLoadURL);
	
	
}
