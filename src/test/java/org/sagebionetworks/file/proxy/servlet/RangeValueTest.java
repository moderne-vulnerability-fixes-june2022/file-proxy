package org.sagebionetworks.file.proxy.servlet;

import static org.junit.Assert.*;

import org.junit.Test;

public class RangeValueTest {
	
	@Test
	public void testHappy(){
		//call under test.
		RangeValue range = new RangeValue("bytes=100-201");
		assertEquals(new Long(100), range.getFirstBytePosition());
		assertEquals(new Long(201), range.getLastBytePosition());
		assertEquals(RangeUnits.bytes, range.getRangeUnits());
		assertEquals("bytes=100-201", range.toString());
	}
	
	@Test
	public void testHappyWhiteSpace(){
		// call under test.
		RangeValue range = new RangeValue("bytes = 100 - 201 ");
		assertEquals(new Long(100), range.getFirstBytePosition());
		assertEquals(new Long(201), range.getLastBytePosition());
		assertEquals(RangeUnits.bytes, range.getRangeUnits());
		assertEquals("bytes=100-201", range.toString());
	}
	
	@Test
	public void testRangeNoLast(){
		// call under test
		RangeValue range = new RangeValue("bytes=100-");
		assertEquals(new Long(100), range.getFirstBytePosition());
		assertEquals(null, range.getLastBytePosition());
		assertEquals(RangeUnits.bytes, range.getRangeUnits());
		assertEquals("bytes=100-", range.toString());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUnknownUnits(){
		// call under test.
		RangeValue range = new RangeValue("unknown=100-201");
		assertEquals(new Long(100), range.getFirstBytePosition());
		assertEquals(new Long(201), range.getLastBytePosition());
		assertEquals(RangeUnits.bytes, range.getRangeUnits());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testRangeFirstLargerThanLast(){
		// call under test.
		RangeValue range = new RangeValue("unknown=101-100");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testRangeFirstSameAsLast(){
		// call under test.
		RangeValue range = new RangeValue("unknown=101-101");
	}

}
