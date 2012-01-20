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
package org.helios.gmx.mbeansupport;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;


/**
 * <p>Title: BaseSimpleService</p>
 * <p>Description: A base class for test MBean implementations.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.mbeansupport.BaseSimpleService</code></p>
 */
public class BaseSimpleService extends NotificationBroadcasterSupport implements BaseSimpleServiceMBean {
	/**  */
	private static final long serialVersionUID = -1502910907281398108L;
	/** The MBeanServer where this bean is registered */
	protected transient MBeanServer server = null;
	/** The ObjectName under which this bean is registered */
	protected ObjectName objectName = null;
	/** A sequence generator for emitted notifications */
	protected final AtomicLong sequenceFactory = new AtomicLong(0L);
	/** The count of subscribed listeners */
	protected final AtomicInteger listenerCount = new AtomicInteger(0);
	
	/** The default notification type */
	protected String notificationType = getClass().getPackage().getName();
	
	/**
	 * Returns the next notification sequence
	 * @return the next notification sequence
	 */
	protected long nextSequence() {
		return sequenceFactory.incrementAndGet();
	}
	
	/**
	 * Creates and sends a new notification
	 * @param message The message to attach
	 * @param userData The user data to attach
	 */
	protected void sendNotification(String message, Object userData) {
		this.sendNotification(newNotification(message, userData));
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcasterSupport#addNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
		super.addNotificationListener(listener, filter, handback);
		listenerCount.incrementAndGet();
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcasterSupport#removeNotificationListener(javax.management.NotificationListener)
	 */
	public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
		super.removeNotificationListener(listener);
		listenerCount.decrementAndGet();
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcasterSupport#removeNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
		super.removeNotificationListener(listener, filter, handback);
		listenerCount.decrementAndGet();
	}

	/**
	 * Returns the number of registerd listeners
	 * @return the number of registerd listeners
	 */
	public int getListenerCount() {
		return listenerCount.get();
	}
	
	
	/**
	 * Creates and sends a new default notification
	 */
	protected void sendNotification() {
		this.sendNotification(newNotification());
	}
	
	
	
	/**
	 * Returns a new notification
	 * @param message The message to attach
	 * @param userData The user data to attach
	 * @return a new notification
	 */
	protected Notification newNotification(String message, Object userData) {
		Notification n = message==null ?
				new Notification(notificationType, objectName, nextSequence(), System.currentTimeMillis()) :
				new Notification(notificationType, objectName, nextSequence(), System.currentTimeMillis(), message);
		if(userData!=null) {
			n.setUserData(userData);
		}
		return n;
	}
	
	/**
	 * Returns a new default notification
	 * @return a new notification
	 */
	protected Notification newNotification() {
		return newNotification(null, null);
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#preRegister(javax.management.MBeanServer, javax.management.ObjectName)
	 */
	@Override
	public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
		this.server = server;
		objectName = name;
		return name;
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#postRegister(java.lang.Boolean)
	 */
	@Override
	public void postRegister(Boolean registrationDone) {
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#preDeregister()
	 */
	@Override
	public void preDeregister() throws Exception {
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#postDeregister()
	 */
	@Override
	public void postDeregister() {	
	}

}
