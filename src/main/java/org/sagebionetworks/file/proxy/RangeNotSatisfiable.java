package org.sagebionetworks.file.proxy;

/**
 * Thrown when a range request cannot be satisfied.
 *
 */
public class RangeNotSatisfiable extends Exception {

	private static final long serialVersionUID = 1L;
	
	long fileSize;

	public RangeNotSatisfiable(String message, long fileSize) {
		super(message);
		this.fileSize = fileSize;
	}

	/**
	 * The full size of the file.
	 * @return
	 */
	public long getFileSize() {
		return fileSize;
	}
	
	
}
