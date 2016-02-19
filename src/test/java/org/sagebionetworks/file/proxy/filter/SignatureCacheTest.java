package org.sagebionetworks.file.proxy.filter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.common.util.Clock;

public class SignatureCacheTest {
	
	@Mock
	Clock mockClock;
	
	long expiresMS;
	SignatureCache cache;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		// Setup the clock to return a new value each call
		when(mockClock.currentTimeMillis()).thenReturn(1L, 2L,3L,4L,5L,6L,7L,8L,9L,10L,11L,12L);
		expiresMS = 2;
		cache = new SignatureCache(expiresMS, mockClock);
	}
	
	@Test
	public void testcontainsWithRefreshNotFound(){
		assertFalse(cache.containsWithRefresh("noSuchThing"));
	}
	
	@Test
	public void testCachehHappy(){
		String signature = "signature";
		// add to the cache
		cache.putSignature(signature);
		// should be in the cache
		assertTrue(cache.containsWithRefresh(signature));
		// each check should refresh the cache
		assertTrue(cache.containsWithRefresh(signature));
		assertTrue(cache.containsWithRefresh(signature));
		// simulate 4 milliseconds passing
		mockClock.currentTimeMillis();
		mockClock.currentTimeMillis();
		mockClock.currentTimeMillis();
		mockClock.currentTimeMillis();
		// The value should no longer be in the cache
		assertFalse(cache.containsWithRefresh(signature));
	}
	
	

}
