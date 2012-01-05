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


/**
 * <p>Title: RuntimeMBeanServerException</p>
 * <p>Description: Generalized runtime exception to wrap checked exceptions thrown from MBeanServerConnection operations.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.jmx.RuntimeMBeanServerException</code></p>
 */
public class RuntimeMBeanServerException extends RuntimeException {

	/**  */
	private static final long serialVersionUID = -29552241781597446L;

	/**
	 * Creates a new RuntimeMBeanServerException
	 */
	public RuntimeMBeanServerException() {
		super();
	}

	/**
	 * Creates a new RuntimeMBeanServerException
	 * @param message The exception message
	 */
	public RuntimeMBeanServerException(String message) {
		super(message);
	}

	/**
	 * Creates a new RuntimeMBeanServerException
	 * @param cause The exception cause
	 */
	public RuntimeMBeanServerException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new RuntimeMBeanServerException
	 * @param message The exception message
	 * @param cause The exception cause
	 */
	public RuntimeMBeanServerException(String message, Throwable cause) {
		super(message, cause);
	}

}
