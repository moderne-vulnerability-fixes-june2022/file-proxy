package org.sagebionetworks.file.proxy.sftp;

import java.io.OutputStream;

import org.sagebionetworks.file.proxy.NotFoundException;

/**
 * Abstraction for SFTP operations.
 *
 */
public interface SftpManager {

	/**
	 * Execute a SFTP get and write the file to the passed output stream.
	 * 
	 * @param path
	 * @param stream
	 */
	void getFile(String path, OutputStream stream) throws NotFoundException;

}
