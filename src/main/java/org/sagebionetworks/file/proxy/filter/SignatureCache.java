package org.sagebionetworks.file.proxy.filter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.sagebionetworks.common.util.Clock;

import com.google.inject.Singleton;

/**
 * This cache is used to track valid pre-signed signatures. The signatures will
 * remain in the cache until they expire. Each call to
 * {@link #containsWithRefresh(String)} will refresh the expiration of the
 * signature if it has not already expired.
 * 
 */
@Singleton
public class SignatureCache {

	private Map<String, Long> cache;
	private long signatureTimeoutMS;
	private Clock clock;

	public SignatureCache(long signatureTimeoutMS, Clock clock) {
		super();
		this.cache = new HashMap<String, Long>();
		this.clock = clock;
		this.signatureTimeoutMS = signatureTimeoutMS;
	}

	/**
	 * Contains check that will refresh the timeout of the signature if it
	 * exists.
	 * 
	 * This method is synchronized.
	 * 
	 * @param signature
	 * @return
	 */
	public synchronized boolean containsWithRefresh(String signature) {
		// Remove expired values before checking for the passed signature.
		removeExpired();
		// now check for the remaining.
		if (cache.containsKey(signature)) {
			// refresh the cache value
			long expires = clock.currentTimeMillis() + signatureTimeoutMS;
			cache.put(signature, expires);
			return true;
		}
		return false;
	}

	/**
	 * Add new signature to the cache. This method is synchronized.
	 * 
	 * @param signature
	 */
	public synchronized void putSignature(String signature) {
		removeExpired();
		long expires = clock.currentTimeMillis() + signatureTimeoutMS;
		this.cache.put(signature, expires);
	}

	/**
	 * Remove all expired signatures.
	 */
	private void removeExpired() {
		long now = clock.currentTimeMillis();
		Iterator<Entry<String, Long>> it =this.cache.entrySet().iterator();
		while(it.hasNext()){
			Entry<String, Long> entry = it.next();
			long expires = entry.getValue();
			if (now > expires) {
				it.remove();
			}
		}
	}

}
