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

import java.io.File;
import java.lang.reflect.Field;

/**
 * <p>Title: JVMLauncher</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.util.jvmcontrol.JVMLauncher</code></p>
 */

public class JVMLauncher {

	/** The process builder */
	private final ProcessBuilder processBuilder;
	/** The classpath */
	protected final StringBuilder classpath = new StringBuilder();
	/** Indicates if this JVM is running in Windows */
	public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
	/** This JVM's java executable */
	public static final String THIS_JAVA_EXEC = System.getProperty("java.home") + File.separator + "bin" + File.separator + (IS_WINDOWS ? "javaw.exe" : "java");
	
	/**
	 * Creates a new JVMLauncher
	 * @param commands An optional array of commands to append to the process builder
	 * @return a new JVMLauncher
	 */
	public static JVMLauncher newJVMLauncher(String...commands) {
		JVMLauncher launcher = new JVMLauncher();
		if(commands!=null) {
			for(String command: commands) {
				if(command==null || command.trim().length()<1) continue;
				launcher.processBuilder.command(command.trim());
			}
		}
		return launcher;
	}
	
	/**
	 * Starts the JVM and returns a {@link LaunchedJVMProcess} that wraps the process 
	 * @return a {@link LaunchedJVMProcess} that wraps the process
	 */
	public LaunchedJVMProcess start() {
		Process process = null;
		try {
			process = processBuilder.start();
			Field f = process.getClass().getDeclaredField("pid");
			f.setAccessible(true);
			int pid = f.getInt(process);
			return LaunchedJVMProcess.newInstance("" + pid, process);
		} catch (Exception e) {
			if(process!=null) {
				process.destroy();
			}
			throw new RuntimeException("Failed to start process " + processBuilder.command().toString(), e);
		}
	}
	
	
	
	
	/**
	 * Creates a new JVMLauncher
	 */
	private JVMLauncher() {
		processBuilder = new ProcessBuilder(THIS_JAVA_EXEC);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
