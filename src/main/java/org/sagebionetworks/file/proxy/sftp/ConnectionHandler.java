package org.sagebionetworks.file.proxy.sftp;

import java.io.IOException;

import org.sagebionetworks.file.proxy.NotFoundException;

/**
 * An abstraction for interacting with an file connection.

 *
 */
public interface ConnectionHandler {
	
	/**
	 * Perform all operation on a given connection.
	 * The connection will be unconditionally closed when this method returns.
	 * 
	 * @param connection
	 * @throws IOException 
	 */
	public void execute(FileConnection connection) throws NotFoundException, Exception;

}
