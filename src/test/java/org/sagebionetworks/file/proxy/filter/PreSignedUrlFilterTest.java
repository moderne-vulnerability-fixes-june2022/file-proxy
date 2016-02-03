package org.sagebionetworks.file.proxy.filter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Date;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.file.proxy.config.Configuration;
import org.sagebionetworks.url.HttpMethod;
import org.sagebionetworks.url.UrlSignerUtils;

public class PreSignedUrlFilterTest {
	
	@Mock
	Configuration mockConfig;
	@Mock
	HttpServletRequest mockRequest;
	@Mock
	HttpServletResponse mockResponse;
	@Mock
	FilterChain mockFilterChain;
	ByteArrayOutputStream outStream;
	
	HttpMethod method;
	Date expiration;
	String credentials;
	String hostPath;
	String queryString;
	String unsignedUrl;
	URL signedUrl;
	
	PreSignedUrlFilter filter;

	@Before
	public void before() throws IOException{
		MockitoAnnotations.initMocks(this);

		method = HttpMethod.GET;
		credentials = "Super secret key to sign URLs";
		hostPath = "http://host.org/path/child";
		queryString = "foo=bar&a=one";
		unsignedUrl = hostPath+"?"+queryString;
		expiration = new Date(System.currentTimeMillis()+(300*1000));
		// create a valid pre-signed URL
		signedUrl = UrlSignerUtils.generatePreSignedURL(method, unsignedUrl, expiration, credentials);
		
		when(mockConfig.getUrlSignerSecretKey()).thenReturn(credentials);
		
		// Setup request
		when(mockRequest.getMethod()).thenReturn(method.toString());
		StringBuffer urlBuffer = new StringBuffer(hostPath);
		when(mockRequest.getRequestURL()).thenReturn(urlBuffer);
		when(mockRequest.getQueryString()).thenReturn(signedUrl.getQuery());
		
		// setup response
		outStream = new ByteArrayOutputStream();
		when(mockResponse.getWriter()).thenReturn(new PrintWriter(outStream));
		
		// Filter under test.
		filter = new PreSignedUrlFilter(mockConfig);
	}
	
	@Test
	public void testDoFilterHappy() throws IOException, ServletException{
		// call under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		// success means doChain()
		verify(mockFilterChain).doFilter(mockRequest, mockResponse);
		
	}
	
	@Test
	public void testDoFilterExpired() throws IOException, ServletException{
		// setup an expiration in the past
		expiration = new Date(456L);
		signedUrl = UrlSignerUtils.generatePreSignedURL(method, unsignedUrl, expiration, credentials);
		when(mockRequest.getQueryString()).thenReturn(signedUrl.getQuery());
		// call under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		verify(mockFilterChain, never()).doFilter(mockRequest, mockResponse);
		verify(mockResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		verify(mockResponse).getWriter();
		String message = new String(outStream.toByteArray(), "UTF-8");
		assertEquals(UrlSignerUtils.MSG_URL_EXPIRED, message);
	}
	
	@Test
	public void testDoFilterNoMatch() throws IOException, ServletException{
		// setup an expiration in the past
		signedUrl = UrlSignerUtils.generatePreSignedURL(method, unsignedUrl, expiration, credentials);
		String queryString = signedUrl.getQuery();
		// return a sub-set of the query string
		when(mockRequest.getQueryString()).thenReturn(queryString.substring(1, queryString.length()));
		// call under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		verify(mockFilterChain, never()).doFilter(mockRequest, mockResponse);
		verify(mockResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		verify(mockResponse).getWriter();
		String message = new String(outStream.toByteArray(), "UTF-8");
		assertEquals(UrlSignerUtils.MSG_SIGNATURE_DOES_NOT_MATCH, message);
	}

}
