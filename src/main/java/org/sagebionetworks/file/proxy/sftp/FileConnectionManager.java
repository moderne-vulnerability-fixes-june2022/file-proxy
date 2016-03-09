package org.sagebionetworks.file.proxy.sftp;


/**
 * Abstraction for file connection management.
 *
 */
public interface FileConnectionManager {

	/**
	 * Make an file connection and provide it to the passed handler.
	 * The passed connection is only valid for the duration of this call.
	 * 
	 * @param handler
	 * @throws Exception
	 */
	public void connect(ConnectionHandler handler) throws Exception;
}
