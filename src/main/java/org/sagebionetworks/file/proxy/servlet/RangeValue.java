package org.sagebionetworks.file.proxy.servlet;

/**
 * Parser for HTTP Range header value.
 *
 */
public class RangeValue {
	
	public static String UNITS_DELIMITER = "=";
	public static String POSITION_DELIMITER = "-";
	
	RangeUnits rangeUnits;
	Long firstBytePosition;
	Long lastBytePosition;
	
	/**
	 * Parse a range string.
	 * @param rangeString
	 */
	public RangeValue(String rangeString){
		if(rangeString == null){
			throw new IllegalArgumentException("rangeString cannot be null");
		}
		String[] split = rangeString.split(UNITS_DELIMITER);
		if(split.length != 2){
			throw new IllegalArgumentException("Unable to read Range: "+rangeString);
		}
		try {
			rangeUnits = RangeUnits.valueOf(split[0].trim());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Unknown range units: "+rangeString);
		}
		// split on the position
		split = split[1].split(POSITION_DELIMITER);
		firstBytePosition = Long.parseLong(split[0].trim());
		if(split.length > 1){
			lastBytePosition = Long.parseLong(split[1].trim());
			if(firstBytePosition >= lastBytePosition){
				throw new IllegalArgumentException("First bytes position must be smaller than the last byte position");
			}
		}
	}

	public RangeUnits getRangeUnits() {
		return rangeUnits;
	}

	public Long getFirstBytePosition() {
		return firstBytePosition;
	}

	public Long getLastBytePosition() {
		return lastBytePosition;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((firstBytePosition == null) ? 0 : firstBytePosition
						.hashCode());
		result = prime
				* result
				+ ((lastBytePosition == null) ? 0 : lastBytePosition.hashCode());
		result = prime * result
				+ ((rangeUnits == null) ? 0 : rangeUnits.hashCode());
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
		RangeValue other = (RangeValue) obj;
		if (firstBytePosition == null) {
			if (other.firstBytePosition != null)
				return false;
		} else if (!firstBytePosition.equals(other.firstBytePosition))
			return false;
		if (lastBytePosition == null) {
			if (other.lastBytePosition != null)
				return false;
		} else if (!lastBytePosition.equals(other.lastBytePosition))
			return false;
		if (rangeUnits != other.rangeUnits)
			return false;
		return true;
	}
	
	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder();
		builder.append(rangeUnits.name());
		builder.append(UNITS_DELIMITER);
		builder.append(firstBytePosition);
		builder.append(POSITION_DELIMITER);
		if(lastBytePosition != null){
			builder.append(lastBytePosition);
		}
		return builder.toString();
	}
	
}
