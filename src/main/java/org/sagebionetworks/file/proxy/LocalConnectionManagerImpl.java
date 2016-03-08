package org.sagebionetworks.file.proxy;

import org.sagebionetworks.file.proxy.config.Configuration;
import org.sagebionetworks.file.proxy.sftp.ConnectionHandler;
import org.sagebionetworks.file.proxy.sftp.FileConnectionManager;

import com.google.inject.Inject;

public class LocalConnectionManagerImpl implements FileConnectionManager {
	
	final Configuration config;


	@Inject
	public LocalConnectionManagerImpl(Configuration config) {
		super();
		this.config = config;
	}


	@Override
	public void connect(ConnectionHandler handler) throws Exception {
		// TODO Auto-generated method stub

	}

}
