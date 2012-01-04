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

import groovy.util.GroovyTestSuite;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestSuite;

/**
 * <p>Title: GmxGroovyTestSuite</p>
 * <p>Description: Test suite for Groovy based unit tests</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.GmxGroovyTestSuite</code></p>
 */
public class GmxGroovyTestSuite extends TestSuite {
	/** The root location of the groovy unit test source */
	public static final String TEST_ROOT = "./src/test/groovy";
	
	static {
		System.out.println("Inited Groovy Test Suite");
	}
	
	@SuppressWarnings("unchecked")
	public static TestSuite suite() throws Exception {
        TestSuite suite = new TestSuite();
        GroovyTestSuite gsuite = new GroovyTestSuite();
        for(File testFile: findTestFiles()) {
        	suite.addTestSuite(gsuite.compile(testFile.getAbsolutePath()));
        }
        return suite;
    }
	
	public static Set<File> findTestFiles() {
		final Set<File> files = new HashSet<File>();
		File testDir = new File(TEST_ROOT);
		if(!testDir.exists() || !testDir.isDirectory()) {
			throw new RuntimeException("The test root [" + testDir + "] is invalid");
		}
		find(testDir, files, new GroovyTestFileFilter());
		return files;
	}
	
	protected static void find(File dir, final Set<File> files, FileFilter filter) {
		for(File f: dir.listFiles(filter)) {
			if(f.isFile()) {
				files.add(f.getAbsoluteFile());
			} else {
				find(f, files, filter);
			}
		}
	}
	
	public static class GroovyTestFileFilter implements FileFilter {
		public boolean accept(File file) {
			return file.isDirectory() || (!file.isDirectory() && file.getName().toLowerCase().endsWith("testcase.groovy"));
		}
		
	}
}
