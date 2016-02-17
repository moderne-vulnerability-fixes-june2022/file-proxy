package org.sagebionetworks.file.proxy.module;


import org.sagebionetworks.file.proxy.config.Configuration;
import org.sagebionetworks.file.proxy.config.ConfigurationImpl;
import org.sagebionetworks.file.proxy.sftp.JcraftFactory;
import org.sagebionetworks.file.proxy.sftp.JcraftFactoryImpl;
import org.sagebionetworks.file.proxy.sftp.SftpConnectionManager;
import org.sagebionetworks.file.proxy.sftp.SftpConnectionManagerImpl;

import com.google.inject.AbstractModule;

public class ApplicationModule extends AbstractModule {

	@Override
	protected void configure() {
		// Bind interfaces to their implementations
		bind(Configuration.class).to(ConfigurationImpl.class);
		bind(JcraftFactory.class).to(JcraftFactoryImpl.class);
		bind(SftpConnectionManager.class).to(SftpConnectionManagerImpl.class);
	}

}
