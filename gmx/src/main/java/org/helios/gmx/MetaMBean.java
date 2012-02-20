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
package org.helios.gmx;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.GroovyShell;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import groovy.lang.Script;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ObjectName;

import org.helios.gmx.jmx.ObjectNameAwareListener;
import org.helios.gmx.util.JMXHelper;
import org.helios.gmx.util.Primitive;

/**
 * <p>Title: MetaMBean</p>
 * <p>Description: A meta object that represents an MBean.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.MetaMBean</code></p>
 */
public class MetaMBean implements GroovyObject {
	
	/** The JMX ObjectName of the MBean */
	protected final ObjectName objectName;
	/** The Gmx reference that created this MetaMBean */
	protected final Gmx gmx;
	/** A reference to the MBean's MBeanInfo */
	protected final AtomicReference<MBeanInfo> mbeanInfo = new AtomicReference<MBeanInfo>(null);
	/** A set of attribute names */
	protected final Set<String> attributeNames = new CopyOnWriteArraySet<String>();
	/** A map of operations keyed by operation name with a set of all signatures for that name as the value */
	protected final Map<String, TreeSet<OperationSignature>> operations = new HashMap<String, TreeSet<OperationSignature>>();
	/** The instance MetaClass */
	protected MetaClass metaClass;
	/** The dynamically generated script text used to invoke mbean operations */
	protected final AtomicReference<String> invokerScriptText = new AtomicReference<String>(null);
	/** The dynamically generated script used to invoke mbean operations */
	protected Script invokerScript = null;
	
	
	
	
	/**
	 * Creates a new MetaMBean
	 * @param objectName The JMX ObjectName
	 * @param gmx The Gmx instance that created this MetaMBean 
	 * @return a MetaMBean
	 */
	public static MetaMBean newInstance(ObjectName objectName, Gmx gmx) {
		if(objectName==null) throw new IllegalArgumentException("The passed ObjectName was null", new Throwable());		
		return new MetaMBean(objectName, gmx);
	}
	
	/**
	 * Creates a new MetaMBean
	 * @param objectName The JMX ObjectName
	 * @param gmx The Gmx instance that created this MetaMBean
	 * @return a MetaMBean
	 */
	public static MetaMBean newInstance(CharSequence objectName, Gmx gmx) {
		return newInstance(JMXHelper.objectName(objectName), gmx);
	}
	
	
	
	/**
	 * Creates a new MetaMBean
	 * @param objectName The JMX ObjectName of the MBean
	 * @param gmx The Gmx instance that created this MetaMBean
	 */
	private MetaMBean(ObjectName objectName, Gmx gmx) {
		this.objectName = objectName;
		this.gmx = gmx;
		try {
			mbeanInfo.set(gmx.mbeanServerConnection.getMBeanInfo(objectName));
			for(MBeanAttributeInfo minfo: mbeanInfo.get().getAttributes()) {
				if(minfo.isReadable()) {
					attributeNames.add(minfo.getName());
				}
			}
			final StringBuilder scriptBuffer = new StringBuilder();
			final Set<String> imports = new HashSet<String>();		
			for(MBeanOperationInfo minfo: mbeanInfo.get().getOperations()) {
				final StringBuilder params = new StringBuilder();
				final StringBuilder signature = new StringBuilder("[");
				final StringBuilder values = new StringBuilder("[");					
				addImport(minfo.getReturnType(), imports);
				scriptBuffer.append("public Object ").append(minfo.getName()).append("(");				
				MBeanParameterInfo[] pinfos = minfo.getSignature();
				if(pinfos!=null && pinfos.length>0) {
					int cnt = 0;
					for(MBeanParameterInfo pinfo: pinfos) {
						addImport(pinfo.getType(), imports);
						params.append(renderTypeName(pinfo.getType())).append(" p").append(cnt).append(",");
						signature.append("'").append(pinfo.getType()).append("',");
						values.append("p").append(cnt).append(",");
						cnt++;
					}
					signature.deleteCharAt(signature.length()-1);
					values.deleteCharAt(values.length()-1);
					params.deleteCharAt(params.length()-1);
				}
				
				
				params.append(") {");
				signature.append("] as String[]);");
				values.append("] as Object[],");
				scriptBuffer.append(params);
				scriptBuffer.append("\n\treturn server.invoke(objectName, '").append(minfo.getName()).append("', ");
				scriptBuffer.append(values).append(signature).append("\n}\n");
			}
			scriptBuffer.insert(0, renderImports(imports));
			System.out.println(scriptBuffer.toString());
			invokerScriptText.set(scriptBuffer.toString());
			invokerScript = new GroovyShell().parse(scriptBuffer.toString());
			Binding binding = new Binding();
			binding.setProperty("server", gmx.getMBeanServerConnection());
			binding.setProperty("objectName", objectName);
			invokerScript.setBinding(binding);
		} catch (Exception e) {			
			throw new RuntimeException("Failed to acquire MBeanInfo for MBean [" + objectName + "]", e);
		}	
	}
	
	protected String renderImports(final Set<String> imports) {
		StringBuilder b = new StringBuilder();
		for(String s: imports) {
			b.append(s);
		}
		return b.toString();
	}
	
	protected void addImport(String className, final Set<String> imports) {
		try {
			imports.add("import " + Class.forName(className).getPackage().getName() + ".*;\n");
		} catch (Exception e) {}
	}
	
	protected String renderTypeName(String name) throws Exception {
		if(Primitive.isPrimitiveName(name)) {
			return Primitive.getPrimitive(name).getPclazz().getName();
		}
		Class<?> clazz = Class.forName(name);
		if(clazz.isArray()) {
			StringBuilder b = new StringBuilder();
			Class<?> arrClass = clazz;
			do {
				b.append("[]");
				arrClass = arrClass.getComponentType();
			} while(arrClass.isArray());
			b.insert(0, arrClass.getName());
			return b.toString();
		} else {
			return name;
		}
		
	}

	/**
	 * {@inheritDoc}
	 * @see groovy.lang.GroovyObject#getMetaClass()
	 */
	@Override
	public MetaClass getMetaClass() {
        if (metaClass == null) {
            metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(MetaMBean.class);
        }
        return metaClass;
	}
	
	/**
	 * {@inheritDoc}
	 * @see groovy.lang.GroovyObject#getProperty(java.lang.String)
	 */
	@Override
	public Object getProperty(String propertyName) {
		if(attributeNames.contains(propertyName)) {
			return gmx.mbeanServerConnection.getAttribute(objectName, propertyName);			
		}
		return getMetaClass().getProperty(this, propertyName);
	}
	
	

	/**
	 * {@inheritDoc}
	 * @see groovy.lang.GroovyObject#invokeMethod(java.lang.String, java.lang.Object)
	 */
	@Override
	public Object invokeMethod(String name, Object arg) {
		return invokerScript.invokeMethod(name, arg);
	}
	
	
	/**
	 * Generates an array of classes from an array of objects.
	 * @param args The object array
	 * @return an array of classes
	 */
	protected Class<?>[] argsToSignagture(Object...args) {
		Class<?>[] signature = new Class[args.length];
		for(int i = 0; i < args.length; i++) {
			signature[i] = args[i]==null ? null : args[i].getClass();
		}
		return signature;
	}
	
	/**
	 * Finds all operation signatures with an operation name matching the passed name and having the same number of arguments
	 * @param name The op name
	 * @param argCount The argument count
	 * @return a [possibly empty] set of matching operation signatures
	 */
	protected Set<OperationSignature> getOpSigs(String name, int argCount) {
		if(!operations.containsKey(name)) {
			throw new RuntimeException("Operation name [" + name + "] was not found", new Throwable());
		}
		Set<OperationSignature> set = new HashSet<OperationSignature>();
		TreeSet<OperationSignature> opSigs = operations.get(name);
		for(OperationSignature os: opSigs) {
			if(os.getArgCount()==argCount) {
				set.add(os);
			} else {
				if(os.getArgCount()>argCount) {
					break;
				}
			}
		}
		return set;
	}

	/**
	 * {@inheritDoc}
	 * @see groovy.lang.GroovyObject#setMetaClass(groovy.lang.MetaClass)
	 */
	@Override
	public void setMetaClass(MetaClass metaClass) {
		this.metaClass = metaClass;

	}

	/**
	 * {@inheritDoc}
	 * @see groovy.lang.GroovyObject#setProperty(java.lang.String, java.lang.Object)
	 */
	@Override
	public void setProperty(String propertyName, Object newValue) {
		if(attributeNames.contains(propertyName)) {
			gmx.mbeanServerConnection.setAttribute(objectName, new Attribute(propertyName, newValue));
		}
		getMetaClass().setProperty(this, propertyName, newValue);
	}
	

	
	/**
	 * <p>Title: UnknownClass</p>
	 * <p>Description: Synthetic class to represent a unknown class that could not be classloaded</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.gmx.MetaMBean.UnknownClass</code></p>
	 */
	public static class UnknownClass {
		public boolean equals(Object obj) {
			return (obj instanceof Class<?>); 
		}
	}
	
	/**
	 * <p>Title: OperationSignature</p>
	 * <p>Description: A container class to provide detailed signature matching of provided arguments to operation signatures.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.gmx.MetaMBean.OperationSignature</code></p>
	 */
	public static class OperationSignature implements Comparable<OperationSignature> {
		/** The classes represented in the sgnature */
		protected final Class<?>[] signature;
		/** The classes represented in the sgnature */
		protected final String[] strSignature;
		/** The op name */
		protected final String opName;
		
		
		/**
		 * Creates a new OperationSignature
		 * @param info The MBean's MBeanOperationInfo
		 * @return an OperationSignature for the passed MBeanOperationInfo 
		 */
		public static OperationSignature newInstance(MBeanOperationInfo info) {
			if(info==null) throw new IllegalArgumentException("The passed info was null", new Throwable());
			return new OperationSignature(info.getName(), info.getSignature());
		}
		
		/**
		 * Returns the number of operation parameters
		 * @return the number of operation parameters
		 */
		public int getArgCount() {
			return strSignature.length;
		}
		
		/**
		 * Creates a new OperationSignature
		 * @param opName The operation name
		 * @param infos An array of the MBean operation info signatures
		 */
		private OperationSignature(String opName, MBeanParameterInfo...infos) {	
			this.opName = opName;
			List<Class<?>> sig = new ArrayList<Class<?>>(infos==null ? 0 : infos.length);	
			List<String> strSig = new ArrayList<String>(infos==null ? 0 : infos.length);
			if(infos!=null) {
				for(MBeanParameterInfo pinfo: infos) {
					String className = pinfo.getType();
					strSig.add(className);					
					if(Primitive.isPrimitiveName(className)) {
						sig.add(Primitive.getPrimitive(className).getPclazz());
					} else {
						Class<?> clazz = null;
						try {
							clazz = Class.forName(className);							
						} catch (Exception e) {
							clazz = UnknownClass.class;
						}						
						sig.add(clazz);
					}
				}
			}
			signature = sig.toArray(new Class[sig.size()]);
			strSignature = strSig.toArray(new String[strSig.size()]);
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return Arrays.hashCode(strSignature);
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {			
			if(obj instanceof OperationSignature) return Arrays.deepEquals(strSignature, ((OperationSignature)obj).strSignature);
			return false;			
		}

		/**
		 * Returns the operation signature
		 * @return the signature
		 */
		public Class<?>[] getSignature() {
			return signature.clone();
		}

		/**
		 * Returns the operation signature as a string array
		 * @return the stringified signature
		 */
		public String[] getStrSignature() {
			return strSignature;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(OperationSignature os) {
			Integer thisArgCount = signature.length;
			Integer thatArgCount = os.signature.length;
			if(thisArgCount.intValue()==thatArgCount.intValue()) {
				return (this.hashCode()<os.hashCode() ? -1 : (this.hashCode()==os.hashCode() ? 0 : 1));
			}
			return (thisArgCount<thatArgCount ? -1 : (thisArgCount==thatArgCount ? 0 : 1));
		}

		/**
		 * Constructs a <code>String</code> with key attributes in name = value format.
		 * @return a <code>String</code> representation of this object.
		 */
		@Override
		public String toString() {
		    final String TAB = "\n\t";
		    StringBuilder retValue = new StringBuilder("OperationSignature [")
		    	.append(TAB).append("Name = ").append(opName)
		    	.append(TAB).append("Arg Count = ").append(signature.length)
		        .append(TAB).append("strSignature = ").append(Arrays.toString(this.strSignature))
		        .append(TAB).append("hashCode = ").append(this.hashCode())
		        .append("\n]");    
		    return retValue.toString();
		}

		/**
		 * @return the opName
		 */
		public String getOpName() {
			return opName;
		}
	}
	
	/**
	 * Registers a notification listener with the MBeanServer on the MBean represented by this MetaMBean
	 * @param listener A closure that will passed the notification and handback.
	 * @param filter A closure that will be passed the notification to determine if it should be filtered or not. If null, no filtering will be performed before handling notifications.
	 * @param handback The object to be passed back to the listener closure. Can be null (so long as the notification is not expecting it....)
	 * @param closureArgs Optional arguments to the listener closure
	 * @return The wrapped listener that can be used to unregister the listener
	 */
	public ObjectNameAwareListener addListener(Closure<Void> listener, Closure<Boolean> filter, Object handback, Object...closureArgs ) {
		if(gmx.isRemote()) {
			if(!gmx.isRemoted()) {
				gmx.installRemote();
			}			
		}		
		return gmx.addListener(objectName.toString(), listener, filter, handback, closureArgs);				
	}
	
	/**
	 * Registers a notification listener with the MBeanServer on the MBean represented by this MetaMBean
	 * @param listener A closure that will passed the notification and handback.
	 * @param closureArgs Optional arguments to the listener closure
	 * @return The wrapped listener that can be used to unregister the listener
	 */
	public ObjectNameAwareListener addListener(Closure<Void> listener, Object...closureArgs) {
		return addListener(listener, null, null, closureArgs);
	}
	
//	/**
//	 * Registers a notification listener with the MBeanServer on the MBean represented by this MetaMBean
//	 * @param listener A closure that will passed the notification and handback.
//	 * @return The wrapped listener that can be used to unregister the listener
//	 */
//	public ObjectNameAwareListener addListener(NotificationListener listener) {
//		return gmx.addListener()
//		
//	}
	

	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	@Override
	public String toString() {
	    return new StringBuilder("[")
	    	.append(objectName).append("]@")
	    	.append(gmx.jvmName==null ? gmx.serverDomain : gmx.jvmName)
	    	.toString();
	}

	/**
	 * The JMX ObjectName of the MBean
	 * @return the objectName
	 */
	public ObjectName getObjectName() {
		return objectName;
	}

	/**
	 * The MBean's MBeanInfo
	 * @return the mbeanInfo
	 */
	public MBeanInfo getMbeanInfo() {
		return mbeanInfo.get();
	}

}
