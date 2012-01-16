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
package org.helios.gmx.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;


/**
 * <p>Title: LaunchedJVMMain</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.util.LaunchedJVMMain</code></p>
 */

public class LaunchedJVMMain {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args)  {
		long start = System.currentTimeMillis();
		long timeout = Long.parseLong(args[0]);
		System.out.println(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
		System.out.println("Starting Test JVM. Timeout:" + timeout + " ms.");
		final Thread mainThread = Thread.currentThread();
		final Thread streamThread = new Thread("Main") {
			public void run() {
				InputStreamReader inreader = null;
				BufferedReader breader =  null;
				try {
					inreader = new InputStreamReader(System.in);
					breader = new BufferedReader(inreader);
					while(!(breader.readLine()).equals("STOP")) {
						
					}
					System.out.println("Received Stop Signal. Exiting...");
					mainThread.interrupt();
					System.exit(0); // OK
				} catch (Exception e) {
					e.printStackTrace(System.err);
					System.exit(1);   // Stream error
				} finally {
					try { breader.close(); } catch (Exception e) {}
					try { inreader.close(); } catch (Exception e) {}
				}
			}
		};
		streamThread.setDaemon(false);
		streamThread.start();
		try { 
			mainThread.join(timeout);
			System.out.println("Test JVM timed out");
			streamThread.interrupt();
			System.exit(2);  // Main error
		} catch (InterruptedException ie) {
			
		}

	}

}
