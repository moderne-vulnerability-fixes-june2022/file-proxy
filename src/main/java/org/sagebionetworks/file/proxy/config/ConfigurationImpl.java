package org.sagebionetworks.file.proxy.config;

import java.util.Properties;

public class ConfigurationImpl implements Configuration {
	
	
	Properties properties;
	
	public ConfigurationImpl(){
		// Load all of the properties
		// Now override the configuration with the system properties.
		for(String key: System.getProperties().stringPropertyNames()){
			properties.put(key, System.getProperties().get(key));
		}
	}

	@Override
	public String getUrlSignerSecretKey() {
		// TODO Auto-generated method stub
		return null;
	}

}
