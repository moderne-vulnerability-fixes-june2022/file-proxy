package org.sagebionetworks.file.proxy.sftp;

import java.io.OutputStream;

import org.sagebionetworks.file.proxy.NotFoundException;

/**
 * Abstraction of all operations that can be performed on a SFTP connection.
 * 
 */
public interface SftpConnection {

	/**
	 * Execute a SFTP get and write the file to the passed output stream.
	 * 
	 * @param path
	 * @param stream
	 */
	void getFile(String path, OutputStream stream) throws NotFoundException;
	
	/**
	 * 
	 * @param path
	 * @param stream
	 * @param limit
	 * @param offset
	 * @return
	 * @throws NotFoundException
	 */
	boolean getFileRange(String path, OutputStream stream, long limit, long offset) throws NotFoundException;

	/**
	 * Get the size of a given file.
	 * @param path
	 * @return
	 */
	long getFileSize(String path) throws NotFoundException;
}
