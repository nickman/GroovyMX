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

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: Primitive</p>
 * <p>Description: A helper enum to work with primitives and their upconverts.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.util.Primitive</code></p>
 */

public enum Primitive {
	/** Primitive descriptor for Byte */
	BYTE(Byte.class, byte.class),
	/** Primitive descriptor for Boolean */
	BOOLEAN(Boolean.class, boolean.class),
	/** Primitive descriptor for Short */
	SHORT(Short.class, short.class),
	/** Primitive descriptor for Integer */
	INT(Integer.class, int.class),
	/** Primitive descriptor for Character */
	CHAR(Character.class, char.class),
	/** Primitive descriptor for Long */
	LONG(Long.class, long.class),
	/** Primitive descriptor for Float */
	FLOAT(Float.class, float.class),
	/** Primitive descriptor for Double */
	DOUBLE(Double.class, double.class),
	/** Primitive descriptor for Void */
	VOID(Void.class, void.class);
	
	/** The number of entries in the enum */
	public static final int count = Primitive.values().length;	
	/** A map of Primitives keyed by the primitive class name */
	private static final Map<String, Primitive> PNAME_TO_P = new HashMap<String, Primitive>(count);
	/** A map of Primitives keyed by the primitive class */
	private static final Map<Class<?>, Primitive> PCLASS_TO_P = new HashMap<Class<?>, Primitive>(count); 	
	/** A map of Primitives keyed by the upconvert class name */
	private static final Map<String, Primitive> UNAME_TO_P = new HashMap<String, Primitive>(count); 
	/** A map of Primitives keyed by the upconvert class */
	private static final Map<Class<?>, Primitive> UCLASS_TO_P = new HashMap<Class<?>, Primitive>(count); 
	
	static {
		for(Primitive p: Primitive.values()) {
			PNAME_TO_P.put(p.pclazz.getName(), p);
			PCLASS_TO_P.put(p.pclazz, p);
			UNAME_TO_P.put(p.uclazz.getName(), p);
			UCLASS_TO_P.put(p.uclazz, p);
		}
	}
	
	/**
	 * Determines assignment compatibility for the passed types
	 * @param to The assignment to class
	 * @param from The assignment from class
	 * @return true if {@code from} and be assigned to {@code to}, false otherwise.
	 */
	public static boolean isValidAssignment(Class<?> to, Class<?> from) {
		if(to==null || from==null) return false;
		Primitive toP = PCLASS_TO_P.get(to);
		Primitive fromP = PCLASS_TO_P.get(from);
		if(toP==null && fromP==null) {
			return to.isAssignableFrom(from);
		}
		if(toP==null || fromP==null) {
			return false;
		}
		return toP.equals(fromP);
		
	}
	
	/**
	 * Creates a new Primitive 
	 * @param uclazz the upconvert class for the primitive
	 * @param pclazz the primitive class for the primitive
	 */
	private Primitive(Class<?> uclazz, Class<?> pclazz) {
		this.uclazz = uclazz;
		this.pclazz = pclazz;
	}
	
	/** The primitive's upconvert class */
	private final Class<?> uclazz;
	/** The primitive's primitive class */
	private final Class<?> pclazz;
	
	/**
	 * Determines if the passed object is an upconvert for a primitive type
	 * @param obj The object to test
	 * @return true if the passed object type is an upconvert, false if not.
	 */
	public static boolean isPrimitive(Object obj) {
		if(obj==null) return false;
		return PNAME_TO_P.containsKey(obj.getClass());
	}
	
	/**
	 * Determines if the passed class is an upconvert for a primitive type
	 * @param clazz The class to test
	 * @return true if the passed type is an upconvert, false if not.
	 */
	public static boolean isPrimitive(Class<?> clazz) {
		if(clazz==null) return false;
		return PCLASS_TO_P.containsKey(clazz); 
	}
	
	/**
	 * Determines if the passed class can be represented as a primitive or a class
	 * @param type the type to test
	 * @return true if the passed class can be represented as a primitive or a class, 
	 * false if the passed class has no primitive counterpart or was null 
	 */
	public static boolean isDual(Class<?> type) {
		if(type==null) return false;
		return isPrimitive(type) || UCLASS_TO_P.containsKey(type);
	}
	
	/**
	 * Determines if the passed object's class can be represented as a primitive or a class
	 * @param obj The object whose type will be tested
	 * @return true if the passed object's class can be represented as a primitive or a class, 
	 * false if the passed class has no primitive counterpart or was null 
	 */
	public static boolean isDual(Object obj) {
		if(obj==null) return false;
		Class<?> type = obj.getClass();
		return isPrimitive(type) || UCLASS_TO_P.containsKey(type);
	}
	
	/**
	 * If the passed type is a primitive or autoboxable, returns the pair array:<ol>
	 * 	<li>The primitive</li>
	 *  <li>The autoboxable</li>
	 * </ol>
	 * @param type The primitive or autoboxable
	 * @return A class array pair or null if the passed type is null or not a primitive or autoboxable 
	 */
	public static Class<?>[] getAutoBoxPair(Class<?> type) {
		if(type==null || !isDual(type)) {
			return null;
		}
		Class<?>[] types = new Class[2];
		if(isPrimitive(type)) {
			types[0] = type;
			types[1] = up(type);
		} else {
			types[0] = primitive(type);
			types[1] = type;			
		}
		return types;
	}
	
	
	/**
	 * Returns the upper class for the passed primitive class. 
	 * If not a primitive, returns the passed class
	 * @param clazz The class to return the upper for
	 * @return The upper of the passed primitive class or the upper class that was passed.
	 */
	public static Class<?> up(Class<?> clazz) {
		if(clazz==null) throw new IllegalArgumentException("Passed class was null", new Throwable());
		if(isPrimitive(clazz)) {
			return get(clazz).uclazz;
		} else {
			return clazz;
		}
	}
	
	/**
	 * Returns the primitive class for the passed upper class. 
	 * If not an upper class, returns the passed primitive
	 * @param clazz The class to return the primitive for
	 * @return The primitive of the passed upper class or the primitive class that was passed.
	 */
	public static Class<?> primitive(Class<?> clazz) {
		if(clazz==null) throw new IllegalArgumentException("Passed class was null", new Throwable());
		if(isPrimitive(clazz)) {
			return clazz;			
		} else {
			return get(clazz).pclazz;
		}
	}
	
	
	/**
	 * Determines if the passed class name is a primitive type
	 * @param name The class name to test
	 * @return true if the passed type is a primitive, false if not.
	 */
	public static boolean isPrimitiveName(String name) {
		if(name==null) return false;
		return PNAME_TO_P.containsKey(name); 
	}
	
	/**
	 * Returns the Primitive enum for the passed name
	 * @param name The primitive class name
	 * @return A primitive or null
	 */
	public static Primitive getPrimitive(String name) {
		return PNAME_TO_P.get(name);
	}
	
	/**
	 * Returns the Primitive enum for the passed object.
	 * Throws a runtime exception if the passed object is not a primitive upconvert.
	 * @param obj The object to get the Primitive enum for 
	 * @return the Primitive or null if the passed object was null.
	 */
	public static Primitive get(Object obj) {
		if(obj==null) return null;
		if(!isPrimitive(obj)) throw new RuntimeException("The type [" + obj.getClass().getName() + "] is not primitive or an upconvert");
		return UCLASS_TO_P.get(obj.getClass());
	}
	
	/**
	 * Returns the Primitive enum for the passed class.
	 * Throws a runtime exception if the passed object is not a primitive or upconvert.
	 * @param clazz The class to get the Primitive enum for 
	 * @return the Primitive or null if the passed class was null.
	 */
	public static Primitive get(Class<?> clazz) {
		if(clazz==null) return null;
		//if(!isPrimitive(clazz)) throw new RuntimeException("The type [" + clazz.getName() + "] is not primitive or an upconvert");
		Primitive p = UCLASS_TO_P.get(clazz);
		if(p==null) {
			p = PCLASS_TO_P.get(clazz);
		}
		if(p==null) {
			throw new RuntimeException("Failed to match the type [" + clazz.getName() + "] which we thought was a primitive or an upconvert");
		}
		return p;
	}
	
	
	/**
	 * Renders the primitive info as a string
	 * @return a string describing the primitive
	 */
	public String toString() {
		StringBuilder b = new StringBuilder(this.name());
		b.append("\n\tPrimitive Class:").append(pclazz.getName());
		b.append("\n\tUpConvert Class:").append(uclazz.getName());
		return b.toString();
	}
	

	
	
	public static void log(Object message) {
		System.out.println(message);
	}
	
	/**
	 * Returns the primitive's upconvert class
	 * @return the primitive's upconvert class
	 */
	public Class<?> getUclazz() {
		return uclazz;
	}
	/**
	 * Returns the primitive's upconvert class
	 * @return the primitive's upconvert class
	 */
	public Class<?> getPclazz() {
		return pclazz;
	}
}

