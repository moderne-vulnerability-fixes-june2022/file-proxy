package org.sagebionetworks.file.proxy.sftp;

import java.io.OutputStream;

import org.sagebionetworks.file.proxy.NotFoundException;

/**
 * Abstraction of all operations that can be performed on a file connection.
 * 
 */
public interface FileConnection {

	/**
	 * Execute a SFTP get and write the file to the passed output stream.
	 * 
	 * @param path
	 * @param stream
	 */
	void getFile(String path, OutputStream stream) throws NotFoundException;
	
	/**
	 * Request a range of bytes from the given file.
	 * 
	 * @param path
	 * @param stream
	 * @param startByteIndex The index of the start of the byte range to be read.
	 * @param endByteIndex The index of the end of the byte range to be read.
	 * @return True if the end of the file was reached with this read.
	 * 
	 * @throws NotFoundException
	 */
	boolean getFileRange(String path, OutputStream stream, long startByteIndex, long endByteIndex) throws NotFoundException;

	/**
	 * Get the size of a given file.
	 * @param path
	 * @return
	 */
	long getFileSize(String path) throws NotFoundException;
}
