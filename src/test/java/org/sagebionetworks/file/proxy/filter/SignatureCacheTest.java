package org.sagebionetworks.file.proxy.filter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;

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
		when(mockClock.currentTimeMillis()).thenReturn(1L, 2L,3L,4L,5L,6L,7L,8L,9L,10L,11L,12L,13L,15L,16L,17L,18L,19L,20L,21L,22L,23L,24L,25L);
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
	
	@Test
	public void testPLFM3760(){
		int count = 10;
		List<String> signatures = new LinkedList<String>();
		for(int i=0; i<count; i++){
			String signature = "sig"+i;
			cache.putSignature(signature);
			signatures.add(signature);
		}
		
		// simulate 4 milliseconds passing
		mockClock.currentTimeMillis();
		mockClock.currentTimeMillis();
		mockClock.currentTimeMillis();
		mockClock.currentTimeMillis();
		// all signatures should now be missing
		for(String signature: signatures){
			assertFalse(cache.containsWithRefresh(signature));
		}
	}
	
	

}
