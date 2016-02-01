package org.sagebionetworks.file.proxy.module;


import org.sagebionetworks.file.proxy.config.Configuration;
import org.sagebionetworks.file.proxy.config.ConfigurationImpl;
import org.sagebionetworks.file.proxy.filter.PreSignedUrlFilter;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class ApplicationModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(Configuration.class).to(ConfigurationImpl.class);
		
		// Filters must be singletons.
		bind(PreSignedUrlFilter.class).in(Singleton.class);
	}

}
