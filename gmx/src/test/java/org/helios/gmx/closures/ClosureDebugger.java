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
package org.helios.gmx.closures;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

import org.helios.gmx.Gmx;
import org.helios.gmx.classloading.ReverseClassLoader;
import org.helios.vm.VirtualMachine;
import org.helios.vm.VirtualMachineBootstrap;
import org.helios.vm.VirtualMachineDescriptor;
import org.helios.vm.agent.LocalAgentInstaller;

/**
 * <p>Title: ClosureDebugger</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.closures.ClosureDebugger</code></p>
 */
public class ClosureDebugger {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LocalAgentInstaller.getInstrumentation();
		ReverseClassLoader.getInstance();
		
		GroovyShell shell = new GroovyShell();
		Script script = shell.parse("def clozure = { println 'Hello World'; def a = ['foo']; a.each(){println it;}; };  " + 
				"bindings.setProperty('classInfo', clozure.metaClass.theCachedClass.classInfo);" +  
				"bindings.setProperty('classLoader', clozure.metaClass.theCachedClass.classInfo.artifactClassLoader);" +
				"bindings.setProperty('clozure', clozure);");
		log("Parsed");
		Binding binding = new Binding();
		binding.setProperty("bindings", binding);
		script.setBinding(binding);
		script.run();
		log("Run");
		Closure clozure = (Closure)binding.getVariables().get("clozure");
		clozure.call();
		log("Getting Byte Code");
		byte[] bytecode = ReverseClassLoader.getInstance().getByteCodeFromResource(clozure.getClass().getName().replace('.', '/') + ".class");
		log("Byte Code:" + bytecode);
		binding = null;
		script = null;
		clozure = null;
		shell = null;
		System.gc();
		VirtualMachineBootstrap.getInstance();
		Gmx gmx = null;
		for(VirtualMachineDescriptor vmd: VirtualMachine.list()) {
			if(vmd.displayName().toLowerCase().contains("org.jboss.main")) {
			//if(vmd.displayName().toLowerCase().contains("jconsole")) {
//			if(vmd.displayName().toLowerCase().contains(".h2.")) {
				gmx = Gmx.attachInstance(vmd.id());
				log("Connected" + gmx);
			}
		}		
		gmx.installRemote(true);
		try { Thread.currentThread().join(150000); } catch (Exception e) {}
		gmx.close();
	}
	
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

}
