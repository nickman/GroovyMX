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
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.helios.gmx.util.ByteCodeNet;

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
	/** The main class */
	protected volatile String mainClass = null;
	/** The main class arguments */
	protected volatile String[] mainArgs = null;
	/** The configured Debug option */
	protected volatile String debugOption = null;
	/** The configured JavaAgent option */
	protected final List<String> javaAgent = new ArrayList<String>();
	/** The launched JVM timeout */
	protected long timeout = -1; 
	
	/** The classpath */
	protected final StringBuilder classpath = new StringBuilder();
	/** The system properties */
	protected final List<String> sysProps = new ArrayList<String>();
	
	/** Indicates if this JVM is running in Windows */
	public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
	/** This JVM's java executable */
	public static final String THIS_JAVA_EXEC = System.getProperty("java.home") + File.separator + "bin" + File.separator + (IS_WINDOWS ? "javaw.exe" : "java");
	/** The Debug option template */
	public static String DEBUG_OPTION = "-Xrunjdwp:transport=dt_socket,address=%s,server=%s,suspend=%s";
	/** The eol character for this platform */
	public static final String EOL = System.getProperty("line.separator", "\n");
	/** The temp directory for this environment */
	public static final File TMP_DIR = new File(System.getProperty("java.io.tmpdir"));
	/** The runtime name */
	public static final String JVM_NAME = ManagementFactory.getRuntimeMXBean().getName();
	
	
	/**
	 * Creates a new JVMLauncher
	 * @return a new JVMLauncher
	 */
	public static JVMLauncher newJVMLauncher() {
		JVMLauncher launcher = new JVMLauncher();
		return launcher;
	}
	
	/**
	 * Enables the debug agent on the JVM
	 * @param port The port to listen on
	 * @param server Indicates if the debug agent should listen as a server 
	 * @param suspend Indicates if the JVM should suspend until a debugger connects
	 * @return the JVM launcher
	 */
	public JVMLauncher debug(int port, boolean server, boolean suspend) {
		debugOption = String.format(DEBUG_OPTION, port, server ? "y" : "n", suspend ? "y" : "n");
		return this;
	}
	
	/**
	 * Sets up a super basic JMX listener using sysprops.
	 * Remoting is enabled, authentication and ssl are disabled. 
	 * @param port The port to listen on
	 * @return this launcher.
	 */
	public JVMLauncher basicPortJmx(int port) {
		Properties p = new Properties();
		p.put("com.sun.management.jmxremote", "");
		p.put("com.sun.management.jmxremote.authenticate", "false");
		p.put("com.sun.management.jmxremote.ssl", "false");
		p.put("com.sun.management.jmxremote.port", "" + port);
		appendSysProps(p);
		return this;
		
	}
	
	/**
	 * Configures the JVMLauncher for a Java Agent 
	 * @param agentJar The location of the agent jar
	 * @param agentOptions The agent options
	 * @return the JVM Launcher
	 */
	public JVMLauncher javaAgent(File agentJar, String...agentOptions) {		
		if(agentJar==null) throw new IllegalArgumentException("The passed agent jar was null", new Throwable());
		if(!agentJar.exists()) {
			throw new IllegalArgumentException("The specified agent jar [" + agentJar + "] does not exist", new Throwable());
		}
		StringBuilder b = new StringBuilder("-javaagent:");
		b.append(agentJar.getAbsolutePath());
		if(agentOptions!=null && agentOptions.length>0) {
			b.append(":");
			boolean atLeastOne = false;
			for(String option: agentOptions) {
				b.append(option.trim()).append(",");
				atLeastOne = true;
			}
			if(atLeastOne) {
				b.deleteCharAt(b.length()-1);
			}
		}
		javaAgent.add(b.toString());
		return this;
		
	}
	
	/**
	 * Configures the JVMLauncher for a Java Agent 
	 * @param agentJar The location of the agent jar
	 * @param agentOptions The agent options
	 * @return the JVM Launcher
	 */
	public JVMLauncher javaAgent(String agentJar, String...agentOptions) {
		if(agentJar==null) throw new IllegalArgumentException("The passed agent jar was null", new Throwable());
		return javaAgent(new File(agentJar), agentOptions);
	}
	
	
	
	
	/**
	 * Appends classpaths to the JVM's command line options
	 * @param paths The paths to append to the classpath 
	 * @return this JVMLauncher
	 */
	public JVMLauncher appendClassPath(String...paths) {
		if(paths!=null) {
			for(String path: paths) {
				if(path==null || path.trim().length()<1) continue;
				classpath.append(File.pathSeparator).append(path.trim());
			}
		}
		return this;
	}
	
	/**
	 * Appends system property declarations to the JVM's command line options
	 * @param sysProps system property name and value pairs, <code>=</code> separated 
	 * @return this JVMLauncher
	 */
	public JVMLauncher appendSysProps(String...sysProps) {
		if(sysProps!=null) {
			for(String sysProp: sysProps) {
				if(sysProp==null || sysProp.trim().length()<1) continue;
				this.sysProps.add("-D" + sysProp.trim());
			}
		}
		return this;
	}	
	
	/**
	 * Appends system property declarations to the JVM's command line options
	 * @param sysProps Properties to be appended to the  JVM's command line options as "-D" options
	 * @return this JVMLauncher
	 */
	public JVMLauncher appendSysProps(Properties sysProps) {
		if(sysProps!=null) {
			for(Map.Entry<Object, Object> entry: sysProps.entrySet()) {
				this.sysProps.add("-D" + entry.getKey().toString() + "=" + entry.getValue().toString());
			}
		}
		return this;
	}
	
	/**
	 * Causes the JVM to exit after the configured timeout
	 * @param timeout The timeout period in ms.
	 * @return this launcher
	 */
	public JVMLauncher timeout(long timeout) {
		return timeout(timeout, TimeUnit.MILLISECONDS);
	}
	
	
	/**
	 * Causes the JVM to exit after the configured timeout
	 * @param timeout The timeout period
	 * @param unit The unit of time
	 * @return this launcher
	 */
	public JVMLauncher timeout(long timeout, TimeUnit unit) {
		if(unit==null) unit = TimeUnit.MILLISECONDS;
		this.timeout = TimeUnit.MILLISECONDS.convert(timeout, unit);
		return this;
	}
	
	/**
	 * Starts the JVM and returns a {@link LaunchedJVMProcess} that wraps the process 
	 * @return a {@link LaunchedJVMProcess} that wraps the process
	 */
	public LaunchedJVMProcess start() {
		Process process = null;
		if(timeout==-1 && mainClass==null) {
			timeout(0);
		}
		try {
			if(debugOption!=null) {
				processBuilder.command().add("-Xdebug");
				processBuilder.command().add(debugOption);
			}
			String mainClassDir = writeMainClass();
			classpath.append(File.pathSeparator).append(mainClassDir);
			processBuilder.command().add("-cp");
			processBuilder.command().add(classpath.toString());
			if(!sysProps.isEmpty()) {
				for(String sysProp: sysProps) {
					processBuilder.command().add(sysProp);
				}
			}
			if(timeout!=-1) {
				processBuilder.command().add(MainTimeoutAndExit.class.getName());
				processBuilder.command().add("" + timeout);
			}
			if(mainClass!=null) {
				processBuilder.command().add(mainClass);
				if(mainArgs!=null) {
					for(String arg: mainArgs) {
						if(arg==null) continue;
						processBuilder.command().add(arg);
					}
					
				}
			}
			System.out.println("Starting:\n" + processBuilder.command());
			String pid = null;
			process = processBuilder.start();
			try {				
				pid = getPid(process);
			} catch (Exception e) {
				String errMsg = getError(process);
				process.destroy();
				throw new RuntimeException("Failed to start JVM [" + errMsg + "]", e);
			}
			
			
			return LaunchedJVMProcess.newInstance("" + pid, process, processBuilder.command());
		} catch (Exception e) {
			if(process!=null) {
				process.destroy();
			}
			throw new RuntimeException("Failed to start process " + processBuilder.command().toString(), e);
		}
	}
	
	/**
	 * Returns the PID for the passed process
	 * @param process The process
	 * @return the PID in string form
	 */
	protected String getPid(Process process) {		
		InputStreamReader isr = null;
		BufferedReader br = null;
		try {
			isr = new InputStreamReader(process.getInputStream());
			br = new BufferedReader(isr);
			String line = null;
			while(true) {		
				line = br.readLine();
				if(line.startsWith("MainTimeoutAndExit PID:")) {
					line = line.split(":")[1];
					break;
				}
			}
			return line;
		} catch (Exception e) {			
			throw new RuntimeException("Failed to get PID from process", e);
		} finally {
			if(br!=null) try { br.close(); } catch (Exception e) {}
			if(isr!=null) try { isr.close(); } catch (Exception e) {}
		}
	}
	
	
	/**
	 * Reads the process error stream and returns it as a string.
	 * @param process The process to read the error stream from
	 * @return the error stream content
	 */
	protected String getError(Process process) {		
		InputStreamReader isr = null;
		BufferedReader br = null;
		StringBuilder b = new StringBuilder("JVM Start Error:");
		try {
			isr = new InputStreamReader(process.getErrorStream());
			br = new BufferedReader(isr);
			String line = null;
			while((line = br.readLine())!=null) {		
				b.append("\n").append(line);
			}
			return b.toString();
		} catch (Exception e) {			
			throw new RuntimeException("Failed to read process error stream", e);
		} finally {
			if(br!=null) try { br.close(); } catch (Exception e) {}
			if(isr!=null) try { isr.close(); } catch (Exception e) {}
		}
	}
	
	
	
	
	/**
	 * Creates a new JVMLauncher
	 */
	private JVMLauncher() {
		processBuilder = new ProcessBuilder(THIS_JAVA_EXEC);
	}
	
	/**
	 * Appends a main class and arguments
	 * @param mainClass The main class to run
	 * @param mainArgs The command line arguments to the main class
	 * @return this JVMLauncher
	 */
	public JVMLauncher appendMain(String mainClass, Object...mainArgs) {
		this.mainClass = mainClass;
		List<String> args = new ArrayList<String>();
		if(mainArgs!=null) {
			for(Object arg: mainArgs) {
				if(arg!=null) {
					args.add(arg.toString());
				}
			}
		}
		this.mainArgs = args.toArray(new String[args.size()]);
		return this;		
	}
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//LaunchedJVMProcess jvm = JVMLauncher.newJVMLauncher().timeout(100000).debug(1889, true, true).start();
		LaunchedJVMProcess jvm = JVMLauncher.newJVMLauncher().timeout(100000).start();
		try { Thread.sleep(3000); } catch (Exception e) {} 
		System.out.println(jvm);
		try { jvm.waitFor(); } catch (Exception e) {}
	}
	
	/**
	 * Writes out a temporary file with the MainTimeoutAndExit class
	 * @return the name of the directory with the root of the classpath
	 */
	protected String writeMainClass() {
		StringBuilder b = new StringBuilder();
		File classFile = null;
		FileOutputStream fos = null;
		
		try {
			File jvmDir = new File(TMP_DIR + File.separator + JVM_NAME);
			if(jvmDir.exists()) {
				jvmDir.delete();
			}
			jvmDir.mkdir();
			jvmDir.deleteOnExit();
			b.append(jvmDir.getAbsolutePath());
			for(String pkg: MainTimeoutAndExit.class.getPackage().getName().split("\\.")) {
				b.append(File.separator).append(pkg);
			}
			new File(b.toString()).mkdirs();
			b.append(File.separator).append(MainTimeoutAndExit.class.getSimpleName()).append(".class");
			classFile = new File(b.toString());
			fos = new FileOutputStream(classFile);
			URL codeSource = MainTimeoutAndExit.class.getProtectionDomain().getCodeSource().getLocation();
			System.out.println("MainTimeoutAndExit CodeSource URL [" + codeSource + "]");
			Map<String, byte[]> classBytes = ByteCodeNet.getClassBytes(MainTimeoutAndExit.class);
			//fos.write(classBytes);
			fos.flush();
			fos.close();
			System.out.println("MainTimeoutAndExit class file [" + classFile.getAbsolutePath() + "]");
			return jvmDir.getAbsolutePath();			
		} catch (Exception e) {
			throw new RuntimeException("Failed to write main class", e);
		} finally {
			if(fos!=null) {
				try { fos.flush(); } catch (Exception e) {}
				try { fos.close(); } catch (Exception e) {}
			}
		}
	}

}
