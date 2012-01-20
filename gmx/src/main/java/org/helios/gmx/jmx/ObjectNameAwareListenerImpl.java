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

import java.io.Serializable;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

/**
 * <p>Title: ObjectNameAwareListenerImpl</p>
 * <p>Description: A wrapper for a notification listener to make it a {@link ObjectNameAwareListener}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.jmx.ObjectNameAwareListenerImpl</code></p>
 */

public class ObjectNameAwareListenerImpl implements ObjectNameAwareListener, Serializable {
	/**  */
	private static final long serialVersionUID = -2914275830630229368L;
	/** The wrapped listener */
	protected final NotificationListener listener;
	/** The ObjectName the listener is registered on */
	protected final ObjectName objectName;
	
	
	
	/**
	 * Creates a new ObjectNameAwareListenerImpl
	 * @param listener The wrapped listener
	 * @param objectName The ObjectName the listener is registered on
	 */
	public ObjectNameAwareListenerImpl(NotificationListener listener, ObjectName objectName) {
		this.listener = listener;
		this.objectName = objectName;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
	@Override
	public void handleNotification(Notification notification, Object handback) {
		listener.handleNotification(notification, handback);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.gmx.jmx.ObjectNameAwareListener#getObjectName()
	 */
	@Override
	public ObjectName getObjectName() {
		return objectName;
	}
	


}
