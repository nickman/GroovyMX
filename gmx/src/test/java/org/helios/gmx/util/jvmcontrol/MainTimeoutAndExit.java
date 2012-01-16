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
package org.helios.gmx.util.jvmcontrol;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;

/**
 * <p>Title: MainTimeoutAndExit</p>
 * <p>Description: A test supporting JVM launched main class that does nothing except hold the main thread for a timeout period or until the JVM exits.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.util.jvmcontrol.MainTimeoutAndExit</code></p>
 */
public class MainTimeoutAndExit {

	/**
	 * @param args
	 */
	public static void main(String[] args)  {	
		if(args.length<1) {
			throw new IllegalArgumentException("MainTimeoutAndExit requires at least 1 argument: timeout", new Throwable());
		}
		long timeout = Long.parseLong(args[0]);
		final String subMainClass;
		final String[] subMainClassArgs;
		if(args.length>1) {
			subMainClass = args[1];
			if(args.length>2) {
				subMainClassArgs = new String[args.length-2];
				for(int i = 2; i < args.length; i++) {
					subMainClassArgs[i-2] = args[i];
				}
			} else {
				subMainClassArgs = new String[0];
			}
			System.out.println("SubMainClass:" + subMainClass);
		} else {
			subMainClass = null;
			subMainClassArgs = null;
		}
		System.out.println("MainTimeoutAndExit PID:" + ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
		System.out.println("Starting Test JVM. Timeout:" + timeout + " ms.");
		final Thread mainThread = Thread.currentThread();
		final Thread streamThread = new Thread("Main") {
			public void run() {
				InputStreamReader inreader = null;
				BufferedReader breader =  null;
				try {
					inreader = new InputStreamReader(System.in);
					breader = new BufferedReader(inreader);
					String line = null;
					while(true) {
						line = breader.readLine();
						System.out.println("breader [" + line + "]");
						if("STOP".equals(line)) break;
					}
					System.out.println("Received Stop Signal. Exiting...");
					System.out.flush();
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
		final Thread subMainThread = new Thread("SubMain") {
			public void run() {
				try {
					Class<?> subMainClazz = Class.forName(subMainClass);
					Method main = subMainClazz.getDeclaredMethod("main", String[].class);
					main.invoke(null, (Object[])subMainClassArgs);
				} catch (Exception e) {
					e.printStackTrace(System.err);
					System.exit(3);  // SubMain error
				}
			}
		};
		subMainThread.setDaemon(true);
		if(subMainClass!=null) {
			subMainThread.start();
		}
		streamThread.setDaemon(false);
		streamThread.start();
		try { 
			if(timeout==0) {
				mainThread.join();
			} else {
				mainThread.join(timeout);
			}			
			System.out.println("Test JVM timed out");
			streamThread.interrupt();
			System.exit(2);  // Main error
		} catch (InterruptedException ie) {
			
		}

	}

}
