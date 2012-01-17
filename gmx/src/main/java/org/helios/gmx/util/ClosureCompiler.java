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

import groovy.lang.Closure;
import groovy.lang.GroovyShell;

import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Title: ClosureCompiler</p>
 * <p>Description: Accepts arrays of strings and compiles them as Groovy script and returns a uniquely named closure.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.util.ClosureCompiler</code></p>
 */
public class ClosureCompiler {
	/** Class name serial number */
	private static final AtomicLong serial = new AtomicLong(0L);
	/** The script compiler */
	private static GroovyShell shell = new GroovyShell();
	
	/**
	 * Compiles the passed script text into a closure
	 * @param scriptFrags The closure script (everythig inside the "{...}"
	 * @return A closure
	 */
	public static Closure<?> compile(String...scriptFrags) {
		String className = "ClosureFactory" + serial.incrementAndGet();
		StringBuilder b = new StringBuilder("public class ");
		b.append(className).append("{  public Object getClosure() { return {");
		for(CharSequence frag: scriptFrags) {
			b.append(frag);
		}
		b.append("}; } }\nreturn new ").append(className).append("().getClosure();");
		return (Closure<?>) shell.evaluate(b.toString());
	}
}
