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

import java.lang.reflect.Field;

/**
 * <p>Title: ClosureDehydrator</p>
 * <p>Description: Dehydrates a closure instance for serialization</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.util.ClosureDehydrator</code></p>
 */
public class ClosureDehydrator {
	/** The reflective for {@link groovy.lang.Closure#delegate} */
	protected final Field delegateField;
	/** The reflective for {@link groovy.lang.Closure#owner} */
	protected final Field ownerField;
	/** The reflective for {@link groovy.lang.Closure#thisObject} */
	protected final Field thisObjectField;
	/** The closure class */
	protected final Class<?> clozureClazz;
	
	/** The closure class name */
	public static final String CLOSURE_CLASS_NAME = "groovy.lang.Closure";
	
	/**
	 * Creates a new ClosureDehydrator class
	 */
	public ClosureDehydrator() {
		try { 
			clozureClazz = Class.forName(CLOSURE_CLASS_NAME); 
		} catch (Exception e) {
			throw new RuntimeException("Failed to load Closure Class [" + CLOSURE_CLASS_NAME + "]", e);
		}
		delegateField = getField(clozureClazz, "delegate");
		ownerField = getField(clozureClazz, "owner");
		thisObjectField = getField(clozureClazz, "thisObject");
	}
	
	/**
	 * If the passed object is a closure, it will be dehydrated by setting the referenced fields to null
	 * @param clozure The clozure to dehydrate. Ignored if null or not a closure.
	 */
	public void dehydrate(Object clozure) {
		if(clozure != null && clozureClazz.isAssignableFrom(clozure.getClass())) {
			try { delegateField.set(clozure, null); } catch (Exception e) {}
			try { ownerField.set(clozure, null); } catch (Exception e) {}
			try { thisObjectField.set(clozure, null); } catch (Exception e) {}
		}
	}
	
	public static Field getField(Class<?> clazz, String fieldName) {
		try {
			Field f = clazz.getDeclaredField(fieldName);
			f.setAccessible(true);
			return f;
		} catch (Exception e) {
			return null;
		}
	}
	
}
