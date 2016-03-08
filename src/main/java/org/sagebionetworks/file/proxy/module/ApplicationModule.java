package org.sagebionetworks.file.proxy.module;


import org.sagebionetworks.common.util.ClockImpl;
import org.sagebionetworks.file.proxy.config.Configuration;
import org.sagebionetworks.file.proxy.config.ConfigurationImpl;
import org.sagebionetworks.file.proxy.filter.SignatureCache;
import org.sagebionetworks.file.proxy.sftp.JcraftFactory;
import org.sagebionetworks.file.proxy.sftp.JcraftFactoryImpl;
import org.sagebionetworks.file.proxy.sftp.FileConnectionManager;
import org.sagebionetworks.file.proxy.sftp.SftpConnectionManagerImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class ApplicationModule extends AbstractModule {
	
	public static final long SIGNAGURE_EXPIRES_MS = 1000*60*10;

	@Override
	protected void configure() {
		// Bind interfaces to their implementations
		bind(Configuration.class).to(ConfigurationImpl.class);
		bind(JcraftFactory.class).to(JcraftFactoryImpl.class);
		bind(FileConnectionManager.class).to(SftpConnectionManagerImpl.class);
	}
	
	@Provides
	SignatureCache provideCache(){
		return new 	SignatureCache(SIGNAGURE_EXPIRES_MS, new  ClockImpl());
	}

}
