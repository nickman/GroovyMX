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

import groovy.lang.Closure;

import java.io.Serializable;

import javax.management.Notification;
import javax.management.ObjectName;

/**
 * <p>Title: ClosureWrappingNotificationListener</p>
 * <p>Description: A {@link javax.management.NotificationListener} that wraps and delegates to a Groovy {@link groovy.lang.Closure}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.jmx.ClosureWrappingNotificationListener</code></p>
 */
public class ClosureWrappingNotificationListener implements Serializable, ObjectNameAwareListener {
	/**  */
	private static final long serialVersionUID = 8086336838224760998L;
	/** The wrapped closure */
	protected final Closure<?> closure;
	/** Additional closure arguments */
	protected final Object[] arguments;
	/** Indicates if the notification expects a handback */
	protected final boolean expectHandback;
	
	
	/** The ObjectName this listener was registered on */
	protected final ObjectName objectName;
	
	
	
	
	/**
	 * Creates a new ClosureWrappingNotificationListener
	 * @param expectHandback Indicates if the notification expects a handback
	 * @param objectName The ObjectName this listener was registered on
	 * @param closure The wrapped closure
	 * @param arguments Additional closure arguments
	 */
	public ClosureWrappingNotificationListener(boolean expectHandback, ObjectName objectName, Closure<?> closure, Object...arguments) {
		this.expectHandback = expectHandback;
		this.closure = closure;
		this.arguments = arguments;
		this.objectName = objectName;		
	}
	
	/**
	 * Creates a new ClosureWrappingNotificationListener that expects a handback
	 * @param objectName The ObjectName this listener was registered on
	 * @param closure The wrapped closure
	 * @param arguments Additional closure arguments
	 */
	public ClosureWrappingNotificationListener(ObjectName objectName, Closure<?> closure, Object...arguments) {
		this(true, objectName, closure, arguments);
	}



	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
	@Override
	public void handleNotification(Notification notification, Object handback) {
		int index = expectHandback ? 2 : 1;
		Object[] args = new Object[index + (arguments==null ? 0 : arguments.length)];
		args[0] = notification;
		if(expectHandback) args[1] = handback;
		if(arguments!=null) {
			System.arraycopy(arguments, 0, args, index, arguments.length);
		}
		try {
			closure.call(args);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}



	/**
	 * Returns the ObjectName this listener was registered on 
	 * @return the ObjectName this listener was registered on 
	 */
	@Override
	public ObjectName getObjectName() {
		return objectName;
	}

	/**
	 * Returns the wrapped closure
	 * @return the closure
	 */
	public Closure<?> getClosure() {
		return closure;
	}

	/**
	 * Returns the wrapped closure's arguments
	 * @return the arguments
	 */
	public Object[] getArguments() {
		return arguments;
	}

	/**
	 * Indicates if this listener expects a non-null handback
	 * @return true if this listener expects a non-null handback, false otherwise
	 */
	public boolean isExpectHandback() {
		return expectHandback;
	}
	
	

}
