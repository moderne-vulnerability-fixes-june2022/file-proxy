package org.sagebionetworks.file.proxy.config;

public interface Configuration {
	
	/**
	 * The secret key used to sign all pre-signed URLs.
	 * 
	 * @return
	 */
	public String getUrlSignerSecretKey();

}
