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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jetty.util.log.Log;

/**
 * <p>Title: LaunchedJVMProcess</p>
 * <p>Description: Represents a launched JVM process.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.util.jvmcontrol.LaunchedJVMProcess</code></p>
 */

public class LaunchedJVMProcess {
	/** The native OS process ID */
	private final String processId;	
	/** The Java Process */
	private final Process process;
	/** The exit code from the process */
	protected final AtomicInteger exitCode = new AtomicInteger(Integer.MAX_VALUE);
	
	/** A list of the arguments to this process */
	private final List<String> arguments;
	/** The process standard out reader thread */
	private final Thread stdOutThread;
	/** The out queue */
	private final Queue<String> outQueue = new ArrayBlockingQueue<String>(100, false);
	/** The process standard err reader thread */
	private final Thread stdErrThread;
	/** The err queue */
	private final Queue<String> errQueue = new ArrayBlockingQueue<String>(100, false);
	
	/** A map of LaunchedJVMProcesses keyed by the processId */
	private static final Map<String, LaunchedJVMProcess> processes = new ConcurrentHashMap<String, LaunchedJVMProcess>();
	
	/** The thread group that stream reader threads run in */
	private static final ThreadGroup STREAMER_THREAD_GROUP = new ThreadGroup("LaunchedJVMProcessStreamThreads");
	
	/**
	 * Returns an existing LaunchedJVMProcess 
	 * @param processId The process ID of the LaunchedJVMProcess to retrieve
	 * @return The keyed LaunchedJVMProcess or null if one was not found for the passed process ID.
	 */
	static LaunchedJVMProcess getLaunchedJVMProcess(String processId) {
		if(processId==null) throw new IllegalArgumentException("The passed processId was null", new Throwable());
		return processes.get(processId);
	}
	
	/**
	 * Creates a new LaunchedJVMProcess
	 * @param processId The native OS process ID 
	 * @param process The java process
	 * @param arguments A list of the arguments to this JVM launch
	 * @return a new LaunchedJVMProcess or an existing cached process.
	 */
	static LaunchedJVMProcess newInstance(String processId, Process process, List<String> arguments) {
		if(processId==null) throw new IllegalArgumentException("The passed processId was null", new Throwable());
		LaunchedJVMProcess jvmProcess = processes.get(processId);
		if(jvmProcess==null) {
			synchronized(processes) {
				jvmProcess = processes.get(processId);
				if(jvmProcess==null) {
					if(process==null) throw new IllegalArgumentException("The passed process was null", new Throwable());
					jvmProcess = new LaunchedJVMProcess(processId, process, arguments);
				}
			}
		}
		return jvmProcess;
	}
	
	/**
	 * Creates a new LaunchedJVMProcess
	 * @param processId The native OS process ID 
	 * @param process The java process
	 * @param arguments A list of the arguments to this JVM launch
	 */
	private LaunchedJVMProcess(final String processId, final Process process, final List<String> arguments) {
		super();	
		this.processId = processId;
		this.process = process;
		this.arguments = arguments;
		stdOutThread = newStreamReaderThread("Out", this.processId, this.process.getInputStream(), outQueue);
		stdErrThread = newStreamReaderThread("Err", this.processId, this.process.getErrorStream(), errQueue);
		final LaunchedJVMProcess jvmp = this;
		Thread pWatcher = new Thread(STREAMER_THREAD_GROUP, "ProcessWatcher-" + processId) {
			public void run() {
				try {
					exitCode.set(process.waitFor());
					if(stdOutThread.isAlive()) stdOutThread.interrupt();
					if(stdErrThread.isAlive()) stdErrThread.interrupt();
					processes.remove(jvmp);
				} catch (InterruptedException ie) {
					Thread.interrupted();
				}
			}
		};
		pWatcher.setDaemon(true);
		pWatcher.start();
	}

	
	/**
	 * Returns the next line of standard out from the process
	 * @return the next line of standard out from the process or null if one was not available.
	 */
	public String nextOut() {
		return outQueue.poll();
	}
	
	/**
	 * Returns the next line of standard err from the process
	 * @return the next line of standard err from the process or null if one was not available.
	 */
	public String nextErr() {
		return errQueue.poll();
	}
	
	/**
	 * Drains the out queue and returns the content in a string array
	 * @return a string array
	 */
	public String[] drainOut() {
		List<String> drain = new ArrayList<String>(outQueue.size());
		outQueue.removeAll(drain);
		return drain.toArray(new String[drain.size()]);
	}
	
	/**
	 * Drains the err queue and returns the content in a string array
	 * @return a string array
	 */
	public String[] drainErr() {
		List<String> drain = new ArrayList<String>(errQueue.size());
		errQueue.removeAll(drain);
		return drain.toArray(new String[drain.size()]);
	}

	/**
	 * Returns the native OS process ID
	 * @return the processId
	 */
	public String getProcessId() {
		return processId;
	}


	/**
	 * Returns the Java process representing the process
	 * @return the process
	 */
	public Process getProcess() {
		return process;
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((processId == null) ? 0 : processId.hashCode());
		return result;
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LaunchedJVMProcess other = (LaunchedJVMProcess) obj;
		if (processId == null) {
			if (other.processId != null)
				return false;
		} else if (!processId.equals(other.processId))
			return false;
		return true;
	}


	/**
	 * Kills the JVM process
	 * @see java.lang.Process#destroy()
	 */
	public void destroy() {
		try { stdOutThread.interrupt(); } catch (Exception e) {}
		try { stdErrThread.interrupt(); } catch (Exception e) {}
		outQueue.clear();
		errQueue.clear();
		process.destroy();
	}
	
	public int stop() {
		OutputStream os = process.getOutputStream();
		try {
			os.write("STOP\n".getBytes());
			os.flush();
			Thread.sleep(200);
			System.out.println("Pending JVM Output:" + outQueue);
			System.err.println("Pending JVM Errput:" + errQueue);
//			try { stdOutThread.interrupt(); } catch (Exception e) {}
//			try { stdErrThread.interrupt(); } catch (Exception e) {}
			outQueue.clear();
			errQueue.clear();
			int code =  process.waitFor();
			return code;
		} catch (Exception e) {
			throw new RuntimeException("Failed to call stop on JVM", e);
		}
	}


	/**
	 * Returns the exit value for the subprocess.
	 * @return the exit value for the subprocess.
	 * @see java.lang.Process#exitValue()
	 */
	public Integer exitValue() {
		if(isRunning()) {
			return null;
		} else {
			return exitCode.get();
		}		
	}
	
	/**
	 * Determines if the process is still running
	 * @return true if the process is still running, false otherwise
	 */
	public boolean isRunning() {
		return exitCode.get()==Integer.MAX_VALUE;
	}


	/**
	 * Returns the error stream of the subprocess.
	 * @return the error stream of the subprocess.
	 * @see java.lang.Process#getErrorStream()
	 */
	public InputStream getErrorStream() {
		return process.getErrorStream();
	}


	/**
	 * Returns the input stream of the subprocess.
	 * @return the input stream of the subprocess.
	 * @see java.lang.Process#getInputStream()
	 */
	public InputStream getInputStream() {
		return process.getInputStream();
	}


	/**
	 * Returns the output stream of the subprocess.
	 * @return the output stream of the subprocess.
	 * @see java.lang.Process#getOutputStream()
	 */
	public OutputStream getOutputStream() {
		return process.getOutputStream();
	}


	/**
	 *  Causes the current thread to wait, if necessary, until the process represented by this Process object has terminated.
	 * @return the exit value of the process. By convention, 0 indicates normal termination. 
	 * @throws InterruptedException thra string representation of this processown if the waiting thread is interrupted while waiting for the process to complete.
	 * @see java.lang.Process#waitFor()
	 */
	public int waitFor() throws InterruptedException {
		return process.waitFor();
	}
	
	/**
	 * Creates a new stream reader for the process
	 * @param type The type of reader ("Out" or "Err")
	 * @param processId The process Id.
	 * @param is The input stream to read
	 * @param lineQueue The queue to write output to
	 * @return the thread
	 */
	protected static Thread newStreamReaderThread(String type, String processId, final InputStream is, final Queue<String> lineQueue) {
		if(type==null || (!type.equals("Out") && !type.equals("Err"))) throw new IllegalArgumentException("The passed type [" + type + "] was invalid", new Throwable());
		Thread t = new Thread(STREAMER_THREAD_GROUP, processId + "-" + type + "-Reader") {
			InputStreamReader isr = null;
			BufferedReader br = null;
			public void run() {
				while(true) {
					try {
						if(isr==null) isr = new InputStreamReader(is);
						if(br==null) br = new BufferedReader(isr);
						lineQueue.add(br.readLine());
					} catch (Exception e) {
						if(e instanceof InterruptedException) {
							break;
						}
					} finally {
						if(br!=null) try { br.close(); } catch (Exception e){}
						if(isr!=null) try { isr.close(); } catch (Exception e){}
					}
					
				}
			}
		};
		t.setDaemon(true);
		t.start();
		return t;
	}

	/**
	 * Constructs a <code>String</code> with all attributes in <code>name:value</code> format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder();    
	    retValue.append("LaunchedJVMProcess [")
		    .append(TAB).append("processId:").append(this.processId)
		    .append(TAB).append("isRunning:").append(this.isRunning())
		    .append(TAB).append("process:").append(this.process)		    
	    	.append(TAB).append("outQueue [");
	    	for(String line: new ArrayList<String>(outQueue)) {
	    		retValue.append("\n\t\t").append(line);
	    	}
	    	retValue.append("\n\t]")
		    .append(TAB).append("errQueue [");
	    	for(String line: new ArrayList<String>(errQueue)) {
	    		retValue.append("\n\t\t").append(line);
	    	}
	    	retValue.append("\n\t]")	    	
		    .append(TAB).append("arguments [");
	    	for(String arg: arguments) {
	    		retValue.append("\n\t\t").append(arg);
	    	}
	    	retValue.append("\n\t]")
	    	.append("\n]");    
	    return retValue.toString();
	}
}
