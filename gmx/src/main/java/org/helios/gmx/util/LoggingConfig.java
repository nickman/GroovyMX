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

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Title: LoggingConfig</p>
 * <p>Description: Sketchy logging control because we can't pick a log framework.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.util.LoggingConfig</code></p>
 */

public class LoggingConfig {
	/** The singleton instance */
	protected static volatile LoggingConfig instance = null;
	/** The singleton instance ctor lock */
	protected static final Object lock = new Object();
	/** Logging line separa */
	public static final String EOL = System.getProperty("line.separator");
	/** A map of logging enabled states keyed by package or class name */
	protected final Map<String, Boolean> loggingLevels = new ConcurrentHashMap<String, Boolean>();
	/** A map of gloggers */
	protected final Map<String, GLogger> loggers = new ConcurrentHashMap<String, GLogger>();
	
	/** The global logger used when the passed logger key cannot be resolved */
	protected final GLogger globalLogger;
	/**
	 * Acquires the singleton logging instance
	 * @return the singleton logging instance
	 */
	public static LoggingConfig getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new LoggingConfig();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Sets a logging level for the passed name.
	 * Once the level is set, triggers an update on all registered loggers.
	 * @param name The name of logging namespace to set
	 * @param enabled true to enable, false to disable
	 */
	public synchronized static void set(String name, boolean enabled) {
		if(name==null) return;
		Boolean b = getInstance().loggingLevels.get(name);
		if(b==null && !enabled) return;
		getInstance().loggingLevels.put(name, enabled);
		for(GLogger gl: getInstance().loggers.values()) {
			gl._testEnabled();
		}
	}
	
	/**
	 * Creates a new LoggingConfig
	 */
	private LoggingConfig() {
		globalLogger = new GLogger("*");
	}
	
	/**
	 * Returns a logger for the passed name
	 * @param name The logger name
	 * @return a logger
	 */
	public GLogger getLogger(CharSequence name) {
		if(name==null) return globalLogger;
		String key = name.toString().trim();
		if("*".equals(key)) return globalLogger;
		GLogger logger = loggers.get(key);
		if(logger==null) {
			synchronized(loggers) {
				logger = loggers.get(key);
				if(logger==null) {
					logger = new GLogger(key);
				}
			}
		}
		return logger;
	}
	
	
	/**
	 * Returns a logger for the passed class
	 * @param clazz The class
	 * @return a logger
	 */
	public GLogger getLogger(Class<?> clazz) {
		if(clazz==null) return globalLogger;
		return getLogger(clazz.getName());
	}
	
	
	/**
	 * Formats a stack trace
	 * @param t The throwable
	 * @return A formatted stack trace
	 */
	public static String stackTrace(Throwable t) {
		StringBuilder b = new StringBuilder();
		for(StackTraceElement ste: t.getStackTrace()) {
			b.append(ste.toString()).append(EOL);
		}
		return b.toString();
	}
	
	
	/**
	 * <p>Title: GLogger</p>
	 * <p>Description: A logger handed out that processes acording to the enabled state of the package or class.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.gmx.util.LoggingConfig.GLogger</code></p>
	 */
	public class GLogger {
		/** The GLogger Key */
		protected final String key;
		/** The GLogger parent Key */
		protected final String pkey;
		/** The enabled state */
		protected boolean enabled = false;
		/** The date format */
		protected final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:m:s/S");
		

		/**
		 * Creates a new GLogger
		 * @param key The identity key
		 */
		public GLogger(String key) {
			super();
			this.key = key;
			if(key.contains(".")) {
				StringBuilder b = new StringBuilder(this.key).reverse();
				pkey = b.delete(0, b.indexOf(".")).reverse().toString();
			} else {
				pkey = null;
			}
			_testEnabled();
		}
		
		/**
		 * Determines if this logger is enabled
		 * @return true if the logger is enabled, false otherwise
		 */
		public boolean isEnabled() {
			return enabled;
		}
		
		/**
		 * Sets the enabled state of this logger
		 * @param enabled true to enable, false to disable
		 */
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
			LoggingConfig.set(key, enabled);
		}
		
		/**
		 * Determines if this logger is enabled
		 * @return true if the logger is enabled, false otherwise
		 */
		private boolean _testEnabled() {
			Boolean b = loggingLevels.get(key);
			if(b==null) {
				if(pkey!=null) {
					b = loggingLevels.get(pkey);
				}
			}
			enabled  = b==null ? false : b;
			return enabled;
		}
		
		/**
		 * Logs a formatted value made form the passed objects to the passed print stream
		 * @param stream The print stream to send to
		 * @param objs The objects to format the message from
		 */
		public void log(PrintStream stream, Object...objs) {
			if(!enabled || objs==null || objs.length<1) return;			
			StringBuilder b = new StringBuilder(sdf.format(new Date()));
			b.append("[").append(Thread.currentThread().getName()).append("]");
			int argl = objs.length-1;
			for(int i = 0; i < objs.length; i++) {
				if(objs[i]==null) continue;
				if(i==argl && (objs[i] instanceof Throwable)) {
					b.append(stackTrace((Throwable)objs[i]));
				} else {
					b.append(objs[i].toString());
				}
			}
			stream.print(b.toString());
		}
		
		/**
		 * Logs a formatted value made form the passed objects to System out
		 * @param objs The objects to format the message from
		 */
		public void log(Object...objs) {
			log(System.out, objs);
		}
		
		/**
		 * Logs a formatted value made form the passed objects to System err
		 * @param objs The objects to format the message from
		 */
		public void elog(Object...objs) {
			log(System.err, objs);
		}
		
		
	}
	
	
}
