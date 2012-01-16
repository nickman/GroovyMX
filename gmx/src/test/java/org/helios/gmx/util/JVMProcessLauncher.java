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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;


/**
 * <p>Title: JVMProcessLauncher</p>
 * <p>Description: Utility to launch new managed java processes.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.util.JVMProcessLauncher</code></p>
 */

public class JVMProcessLauncher {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("Java Home:" + System.getProperty("java.home"));
		log("Classpath:" + System.getProperty("java.class.path"));
		log("OS:" + System.getProperty("os.name"));
		String os = System.getProperty("os.name").toLowerCase();
		String binPath = System.getProperty("java.home") + File.separator + "bin" + File.separator;
		String jexec = null;
		if(os.equals("windows")) {
			jexec = binPath + "javaw.exe";
		} else {
			jexec = binPath + "java";
		}
		File jexecFile = new File(jexec);
		if(!jexecFile.exists()) {
			throw new RuntimeException("The java executable [" + jexecFile + "] could not be found");
		}
		ProcessBuilder pb = new ProcessBuilder(jexec, "-cp", System.getProperty("java.class.path"), LaunchedJVMMain.class.getName(), "10000");
		Process p = null;
		try {
			p = pb.start();
			String pid = getPID(p.getInputStream());
			log("JVM Started PID:" + pid);
			startStreamReader(System.err, p.getErrorStream());
			startStreamReader(System.out, p.getInputStream());
			stopTestJVM(p.getOutputStream(), 5000);
			int returnCode = p.waitFor();
			log("Return Code:" + returnCode);
		} catch (Exception e) {
			throw new RuntimeException("JVM Process Listen Failed", e);
		}


		// java.class.path
		
	}
	
	public static String getPID(InputStream is) throws IOException {
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader bis = new BufferedReader(isr);
		return bis.readLine();
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	private static void stopTestJVM(final OutputStream process, final long timeout) {
		Thread t = new Thread() {
			public void run() {
				try { Thread.currentThread().join(timeout); } catch (Exception e) {}
				try {
					process.write("STOP\n".getBytes());
					process.flush();
				} catch (Exception e) {}
			}
		};
		t.setDaemon(true);
		t.start();
		
	}
	
	private static void startStreamReader(final OutputStream os, final InputStream is) {
		
		Thread t = new Thread() {
			public void run() {
				byte[] buffer = new byte[8092];
				while(true) {
					int bytesRead = -1;
					try {
						bytesRead = is.read(buffer);
						if(bytesRead == -1) break;
						os.write(buffer, 0, bytesRead);
					} catch (Exception e) {
						
					}
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}

}
