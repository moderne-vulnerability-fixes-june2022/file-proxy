package org.sagebionetworks.file.proxy.servlet;

/**
 * Description of a file download request.
 *
 */
public class RequestDescription {
	
	long fileSize;
	String path;
	boolean useGZIP;
	ByteRange range;
	
	public long getFileSize() {
		return fileSize;
	}
	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public boolean isUseGZIP() {
		return useGZIP;
	}
	public void setUseGZIP(boolean useGZIP) {
		this.useGZIP = useGZIP;
	}
	public ByteRange getRange() {
		return range;
	}
	public void setRange(ByteRange range) {
		this.range = range;
	}

}
