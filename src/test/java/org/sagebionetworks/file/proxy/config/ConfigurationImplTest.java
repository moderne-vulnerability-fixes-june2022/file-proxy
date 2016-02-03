package org.sagebionetworks.file.proxy.config;

import static org.junit.Assert.*;

import org.junit.Test;

public class ConfigurationImplTest {
	
	
	@Test
	public void testGetProperty(){
		String key = "foo";
		String value = "bar";
		System.setProperty(key, value);
		ConfigurationImpl config = new ConfigurationImpl();
		// call under test
		String fetched = config.getProperty(key);
		assertEquals(value, fetched);
	}

	@Test (expected=IllegalStateException.class)
	public void testGetPropertyEmpty(){
		String key = "foo";
		String value = "";
		System.setProperty(key, value);
		ConfigurationImpl config = new ConfigurationImpl();
		// call under test
		config.getProperty(key);
	}
	
	@Test (expected=IllegalStateException.class)
	public void testGetPropertyMissing(){
		ConfigurationImpl config = new ConfigurationImpl();
		// call under test
		config.getProperty("does not exist");
	}
}
