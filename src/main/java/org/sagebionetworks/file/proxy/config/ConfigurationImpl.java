package org.sagebionetworks.file.proxy.config;

import java.util.Properties;

public class ConfigurationImpl implements Configuration {
	
	
	Properties properties;
	
	public ConfigurationImpl(){
		properties = new Properties();
		// Load all of the properties
		// Now override the configuration with the system properties.
		for(String key: System.getProperties().stringPropertyNames()){
			properties.put(key, System.getProperties().get(key));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.file.proxy.config.Configuration#getUrlSignerSecretKey()
	 */
	@Override
	public String getUrlSignerSecretKey() {
		return getProperty("org.sagebionetworks.url.signer.secret.key");
	}
	
	
	/**
	 * Get a property value given its key.
	 * @param key
	 * @return
	 * @throws IllegalStateException if the property cannot be found or the value is empty.
	 */
	private String getProperty(String key){
		String value = properties.getProperty(key);
		if(value == null){
			throw new IllegalStateException("Unable to find configuration property: "+key);
		}
		value = value.trim();
		if("".equals(value)){
			throw new IllegalStateException("Value for configuration property: "+key+" is empty.");
		}
		return value;
	}

}
