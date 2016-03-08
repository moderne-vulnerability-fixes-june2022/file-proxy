package org.sagebionetworks.file.proxy;

import org.sagebionetworks.file.proxy.config.Configuration;
import org.sagebionetworks.file.proxy.sftp.ConnectionHandler;
import org.sagebionetworks.file.proxy.sftp.FileConnectionManager;

import com.google.inject.Inject;

/**
 * Connection manager for local files.
 *
 */
public class LocalConnectionManager implements FileConnectionManager {
	
	final Configuration config;


	@Inject
	public LocalConnectionManager(Configuration config) {
		this.config = config;
	}


	@Override
	public void connect(ConnectionHandler handler) throws Exception {
		// path prefix is used to resolve relative path requests to actual files.
		String pathPrefix = config.getLocalPathPrefix();
		LocalFileConnection connection = new LocalFileConnection(pathPrefix);
		handler.execute(connection);
	}

}
