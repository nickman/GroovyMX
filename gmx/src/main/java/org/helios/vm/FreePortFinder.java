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
package org.helios.vm;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>Title: FreePortFinder</p>
 * <p>Description: Utility class to find a free port</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.vm.FreePortFinder</code></p>
 */
public class FreePortFinder {
	/** The starting range to look for ports */
	public static final int startRange = 49152;
	/** The ending range to look for ports */
	public static final int endRange = 65535;
	/** The most recently attempted port test */
	private static final AtomicInteger lastTested = new AtomicInteger(startRange-1);
	
	
	/**
	 * Returns the next port number to test, cycling through {@link FreePortFinder#startRange} to {@link FreePortFinder#endRange}. 
	 * @return the next port number to test
	 */
	protected synchronized static int nextPortToTest() {
		int nextPort = lastTested.incrementAndGet();
		if(nextPort > endRange) {
			lastTested.set(startRange-1);
			nextPort = lastTested.incrementAndGet();
		}
		return nextPort;
	}
	
	/**
	 * Determines the next free port
	 * @return The next free port number or {@code -1} if one could not be allocated.
	 */
	public static int getNextFreePort() {
		return getNextFreePort(null);
	}
	
	
	/**
	 * Determines the next free port
	 * @param bindingAddress An optional binding address
	 * @return The next free port number or {@code -1} if one could not be allocated.
	 */
	public static int getNextFreePort(String bindingAddress) {
		int port = nextPortToTest();
		final int overflow = port;
		do {
			if(isPortFree(port, bindingAddress)) {
				return port;
			}
			port = nextPortToTest();
		} while(port!=overflow);
		return -1;
	}
	
	/**
	 * Tests a socket to determine if the port is free
	 * @param port The port to test
	 * @return true if the port is available, false otherwise
	 */
	protected static boolean isPortFree(int port) {
		return isPortFree(port, null);
	}
	
	
	/**
	 * Tests a socket to determine if the port is free
	 * @param port The port to test
	 * @param bindingAddress An optional binding address
	 * @return true if the port is available, false otherwise
	 */
	protected static boolean isPortFree(int port, String bindingAddress) {
		ServerSocket so = null;
		try {
			so = new ServerSocket();
			so.setReuseAddress(true);
			InetSocketAddress socketAddress = (bindingAddress==null ? new InetSocketAddress(port) : new InetSocketAddress(bindingAddress, port));
			so.bind(socketAddress, 1);
			return so.isBound();
		} catch (Exception e) {
			return false;
		} finally {
			if(so!=null) try { so.close(); } catch (Exception e) {}
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		for(int i = 0; i < 10; i++) {
			System.out.println("Next Free Port:" + getNextFreePort());
		}

	}

}
