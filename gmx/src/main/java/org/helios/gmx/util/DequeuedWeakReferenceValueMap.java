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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Title: DequeuedWeakReferenceValueMap</p>
 * <p>Description: A weak reference wrapped map that has the reference queue cleared and actively processed by a thread</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.gmx.util.DequeuedWeakReferenceValueMap</code></p>
 * @param <K> The type of the key
 * @param <V> The value of the key wrapped in a weak reference
 */

public class DequeuedWeakReferenceValueMap<K, V> {
	/** The inner map of weak reference values */
	private final Map<K, KeyAwareWeakReferenceValue> referenceMap = new ConcurrentHashMap<K, KeyAwareWeakReferenceValue>();
	
	/** The reference queue that will be processed by the queue processor thread */
	private static final ReferenceQueue<?> REF_Q = new ReferenceQueue<Object>();
	/** The reference queue processing thread runnable */
	private static final Runnable REF_Q_RUNNABLE = new Runnable() {		
		@Override
		public void run() {
			Object enqueued = null;
			while(true) {
				try {
					enqueued = REF_Q.remove();
					if(enqueued!=null && (enqueued instanceof Runnable)) {
						try { ((Runnable)enqueued).run(); } catch (Exception e) {}
					}
				} catch (InterruptedException e) {
				}
			}
		}
	};
	/** The reference queue processing thread */
	private static final Thread REF_Q_PROCESSOR = new Thread(REF_Q_RUNNABLE, DequeuedWeakReferenceValueMap.class.getSimpleName() + " ClearingThread");

	static {
		REF_Q_PROCESSOR.setDaemon(true);
		REF_Q_PROCESSOR.start();
	}
	
	/**
	 * Puts a new value into the map
	 * @param key The map key
	 * @param value The map value
	 * @param runnables Optional additional runnables to execute on reference enqueue
	 * @return The old value that was bound under the key, or null if there was none.
	 */
	@SuppressWarnings("unchecked")
	public V put(K key, V value, Runnable...runnables) {
		KeyAwareWeakReferenceValue oldRef =  referenceMap.put(key, new KeyAwareWeakReferenceValue(key, value, (ReferenceQueue<? super V>) REF_Q, runnables));
		if(oldRef!=null) {
			return oldRef.get();
		}
		return null;
	}
	
	/**
	 * Returns the value in the map keyed by the passed key
	 * @param key The key
	 * @return The value or null if not found
	 */
	public V get(K key) {
		KeyAwareWeakReferenceValue ref =  referenceMap.get(key);
		if(ref!=null) {
			return ref.get();
		}
		return null;
	}
	
	/**
	 * Indicates if the passed key is present in the map and the value has not been enqueued 
	 * @param key The key to check for
	 * @return true if the key is present in the map, false otherwise
	 */
	public boolean containsKey(K key) {
		KeyAwareWeakReferenceValue ref =  referenceMap.get(key);
		return ref!=null && !ref.isEnqueued();
	}
	
	/**
	 * Removes the value keyed by the passed key from the map
	 * @param key The key to remove
	 * @return Ther removed value or null
	 */
	public V remove(K key) {
		KeyAwareWeakReferenceValue ref = referenceMap.remove(key);
		if(ref!=null) {
			return ref.get();
		}
		return null;
	}
	
	
	/**
	 * <p>Title: KeyAwareWeakReferenceValue</p>
	 * <p>Description: An extension of {@link WekReference} that retains the associated key so it can be cleared from the map it is registered in.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.gmx.util.DequeuedWeakReferenceValueMap.KeyAwareWeakReferenceValue</code></p>
	 * @param K The type of the key
	 * @param V The value of the key wrapped in a weak reference
	 */
	public class KeyAwareWeakReferenceValue extends WeakReference<V> implements Runnable {
		/** The key used to clear the map entry in {@link DequeuedWeakReferenceValueMap#referenceMap} */
		private final K key;
		/** Runnable overrides */
		private final Runnable[] runnables;
		
		/**
		 * Creates a new KeyAwareWeakReferenceValue
		 * @param key The key associated with the weakly referenced value
		 * @param value The value to be held as a weak reference
		 * @param refQueue The reference queue
		 */
		public KeyAwareWeakReferenceValue(K key, V value, ReferenceQueue<? super V> refQueue, Runnable...runnables) {
			super(value, refQueue);
			this.key =key;
			this.runnables = (runnables==null || runnables.length<1) ? null : runnables;
		}
		
		/**
		 * {@inheritDoc}
		 * <p>Runnable called by the reference queue processor to clear the entry in {@link DequeuedWeakReferenceValueMap#referenceMap}
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			referenceMap.remove(key);			
			if(runnables!=null) {
				for(Runnable r: runnables) {
					r.run();
				}
			}
		}
		
	}
}
