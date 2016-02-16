package org.sagebionetworks.file.proxy.sftp;

import org.sagebionetworks.file.proxy.NotFoundException;

public interface SftpConnectionManager {

	/**
	 * Make an SFTP connection and provide it to the passed handler.
	 * The passed connection is only valid for the duration of this call.
	 * 
	 * @param handler
	 * @throws NotFoundException
	 */
	public void connect(ConnectionHandler handler) throws NotFoundException;
}
