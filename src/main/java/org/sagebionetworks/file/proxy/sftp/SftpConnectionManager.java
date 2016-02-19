package org.sagebionetworks.file.proxy.sftp;


public interface SftpConnectionManager {

	/**
	 * Make an SFTP connection and provide it to the passed handler.
	 * The passed connection is only valid for the duration of this call.
	 * 
	 * @param handler
	 * @throws Exception
	 */
	public void connect(ConnectionHandler handler) throws Exception;
}
