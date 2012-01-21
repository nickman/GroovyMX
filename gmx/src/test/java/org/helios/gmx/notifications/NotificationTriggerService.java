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
package org.helios.gmx.notifications;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.helios.gmx.mbeansupport.BaseSimpleService;
import org.helios.gmx.util.JMXHelper;

/**
 * <p>Title: NotificationTriggerService</p>
 * <p>Description: An MBean service that sends notifications to test listeners.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.notifications.NotificationTriggerService</code></p>
 */
public class NotificationTriggerService extends BaseSimpleService implements NotificationTriggerServiceMBean {
	/**  */
	private static final long serialVersionUID = -2046438869815498554L;


	/**
	 * Sends a JMX notification with the passed value as the user data.
	 * @param userData The user data to attach to the notification.
	 */
	public void sendMeANotification(Object userData) {
		sendNotification("Notification from [" + objectName + "]", userData);		
	}
	

	
	/**
	 * Registers an instance of this bean
	 * @param server The server to register in
	 * @return the ObjectName generated for the bean
	 */
	public static ObjectName register(MBeanServer server) {
		NotificationTriggerService tns = new NotificationTriggerService();
		ObjectName on = JMXHelper.getName(tns);
		try {
			server.registerMBean(tns, on);
			return on;
		} catch (Exception e) {
			throw new RuntimeException("Failed to register NotificationTriggerService with object name [" + on + "]", e);
		}
	}
}
