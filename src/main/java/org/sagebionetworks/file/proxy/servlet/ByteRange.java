package org.sagebionetworks.file.proxy.servlet;

/**
 * Byte range request.
 *
 */
public class ByteRange {

	long firstBytePosition;
	long lastBytePosition;
	
	public ByteRange(long firstBytePosition, long lastBytePosition) {
		super();
		this.firstBytePosition = firstBytePosition;
		this.lastBytePosition = lastBytePosition;
	}

	public long getFirstBytePosition() {
		return firstBytePosition;
	}

	public long getLastBytePosition() {
		return lastBytePosition;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (int) (firstBytePosition ^ (firstBytePosition >>> 32));
		result = prime * result
				+ (int) (lastBytePosition ^ (lastBytePosition >>> 32));
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
		ByteRange other = (ByteRange) obj;
		if (firstBytePosition != other.firstBytePosition)
			return false;
		if (lastBytePosition != other.lastBytePosition)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ByteRange [firstBytePosition=" + firstBytePosition
				+ ", lastBytePosition=" + lastBytePosition + "]";
	}
	
}
