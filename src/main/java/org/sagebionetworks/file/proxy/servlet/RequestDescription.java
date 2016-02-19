package org.sagebionetworks.file.proxy.servlet;

/**
 * Description of a file download request.
 *
 */
public class RequestDescription {
	
	long fileSize = -1;
	String path = null;
	boolean isGZIP = false;
	ByteRange range = null;
	
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
	public boolean isGZIP() {
		return isGZIP;
	}
	public void setIsGZIP(boolean useGZIP) {
		this.isGZIP = useGZIP;
	}
	public ByteRange getRange() {
		return range;
	}
	public void setRange(ByteRange range) {
		this.range = range;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (fileSize ^ (fileSize >>> 32));
		result = prime * result + (isGZIP ? 1231 : 1237);
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((range == null) ? 0 : range.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RequestDescription other = (RequestDescription) obj;
		if (fileSize != other.fileSize)
			return false;
		if (isGZIP != other.isGZIP)
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (range == null) {
			if (other.range != null)
				return false;
		} else if (!range.equals(other.range))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "RequestDescription [fileSize=" + fileSize + ", path=" + path
				+ ", isGZIP=" + isGZIP + ", range=" + range + "]";
	}

}
