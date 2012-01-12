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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

/**
 * <p>Title: URLHelper</p>
 * <p>Description: Static utility methods for URL handling</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.util.URLHelper</code></p>
 */
public class URLHelper {
	
	/**
	 * Creates a URL from the passed string
	 * @param url The url strin
	 * @return a URL
	 */
	public static URL url(CharSequence url) {
		if(url==null) throw new IllegalArgumentException("The passed url was null", new Throwable());
		try {
			return new URL(url.toString());
		} catch (Exception e) {
			throw new RuntimeException("Failed to create URL from [" + url + "]");
		}
	}
	
	/**
	 * Reads the byte stream from a URL using a default transfer buffer size
	 * @param url The URL to read from
	 * @return the read byte array
	 */
	public static byte[] getBytesFromURL(URL url) {
		return getBytesFromURL(url, 8092);
	}
	
	
	/**
	 * Reads the byte stream from a URL
	 * @param url The URL to read from
	 * @param bufferSize The size of the transfer buffer
	 * @return the read byte array
	 */
	public static byte[] getBytesFromURL(URL url, int bufferSize) {
		InputStream is = null;
		try {
			is = url.openStream();
			ByteArrayOutputStream baos = new ByteArrayOutputStream(is.available());
			byte[] buffer = new byte[bufferSize];
			int bytesRead = -1;
			while((bytesRead = is.read(buffer))!=-1) {
				baos.write(buffer, 0, bytesRead);
			}
			baos.flush();
			return baos.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException("Failed to read from URL [" + url + "]", e);
		} finally {
			if(is!=null) try { is.close(); } catch (Exception e) {}
		}
	}	
}
