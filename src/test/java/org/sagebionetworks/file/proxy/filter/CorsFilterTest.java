package org.sagebionetworks.file.proxy.filter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CorsFilterTest {
	
	@Mock
	HttpServletRequest mockRequest;
	@Mock
	HttpServletResponse mockResponse;
	@Mock
	FilterChain mockFilterChain;
	
	CorsFilter filter;
	
	@Before
	public void before() throws IOException{
		MockitoAnnotations.initMocks(this);
		filter = new CorsFilter();
	}
	
	@Test
	public void testIsPreFlightRequest(){
		// default request is not a pre-flight check.
		assertFalse(CorsFilter.isPreFlightRequest(mockRequest));
		// pre-flight checks have this header
		when(mockRequest.getHeader(CorsFilter.ACCESS_CONTROL_REQUEST_METHOD)).thenReturn(CorsFilter.OPTIONS);
		// pre-flight check methods are
		when(mockRequest.getMethod()).thenReturn(CorsFilter.OPTIONS);
		assertTrue(CorsFilter.isPreFlightRequest(mockRequest));
	}
	
	@Test
	public void testDoFilterNotPreflightCheck() throws IOException, ServletException{
		// A GET is a non-pre-flight check
		when(mockRequest.getMethod()).thenReturn("GET");
		//call under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		// should just forward to the chain
		verify(mockFilterChain).doFilter(mockRequest, mockResponse);
		// the allows headers should always be added to the response
		verify(mockResponse).addHeader(CorsFilter.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
		
	}
	
	@Test
	public void testDoFilterPreflightCheck() throws IOException, ServletException{
		// pre-flight checks have this header
		when(mockRequest.getHeader(CorsFilter.ACCESS_CONTROL_REQUEST_METHOD)).thenReturn(CorsFilter.OPTIONS);
		// pre-flight check methods are
		when(mockRequest.getMethod()).thenReturn(CorsFilter.OPTIONS);
		//call under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		// pre-flight checks should not go past the filter
		verify(mockFilterChain, never()).doFilter(mockRequest, mockResponse);
		// this header should be added.
		verify(mockResponse).addHeader(CorsFilter.ACCESS_CONTROL_ALLOW_ORIGIN, CorsFilter.WILD_CARD);
		// max age should get added
		verify(mockResponse).addHeader(CorsFilter.ACCESS_CONTROL_MAX_AGE, CorsFilter.MAX_AGE_SECONDS);
	}

}
