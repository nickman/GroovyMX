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
import javax.management.NotificationListener;

/**
 * <p>Title: ClosureWrappingNotificationListener</p>
 * <p>Description: A {@link javax.management.NotificationListener} that wraps and delegates to a Groovy {@link groovy.lang.Closure}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.jmx.ClosureWrappingNotificationListener</code></p>
 */
public class ClosureWrappingNotificationListener implements Serializable, NotificationListener {
	/**  */
	private static final long serialVersionUID = 8086336838224760998L;
	/** The wrapped closure */
	protected final Closure<?> closure;
	/** Additional closure arguments */
	protected final Object[] arguments;
	
	
	
	/**
	 * Creates a new ClosureWrappingNotificationListener
	 * @param closure The wrapped closure
	 * @param arguments Additional closure arguments
	 */
	public ClosureWrappingNotificationListener(Closure<?> closure, Object...arguments) {
		this.closure = closure;
		this.arguments = arguments;
	}



	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
	@Override
	public void handleNotification(Notification notification, Object handback) {
		// TODO Auto-generated method stub
		
	}

}
