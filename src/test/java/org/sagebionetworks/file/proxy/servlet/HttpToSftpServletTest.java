package org.sagebionetworks.file.proxy.servlet;

import static org.mockito.Mockito.*;
import static org.sagebionetworks.file.proxy.servlet.HttpToSftpServlet.CONTENT_DISPOSITION_PATTERN;
import static org.sagebionetworks.file.proxy.servlet.HttpToSftpServlet.HEADER_CONTENT_DISPOSITION;
import static org.sagebionetworks.file.proxy.servlet.HttpToSftpServlet.HEADER_CONTENT_LENGTH;
import static org.sagebionetworks.file.proxy.servlet.HttpToSftpServlet.HEADER_CONTENT_TYPE;
import static org.sagebionetworks.file.proxy.servlet.HttpToSftpServlet.KEY_CONTENT_SIZE;
import static org.sagebionetworks.file.proxy.servlet.HttpToSftpServlet.KEY_CONTENT_TYPE;
import static org.sagebionetworks.file.proxy.servlet.HttpToSftpServlet.KEY_FILE_NAME;

import java.io.OutputStream;
import java.net.URLEncoder;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.file.proxy.NotFoundException;
import org.sagebionetworks.file.proxy.sftp.SftpManager;

public class HttpToSftpServletTest {
	
	@Mock
	HttpServletRequest mockRequest;
	@Mock
	HttpServletResponse mockResponse;
	@Mock
	SftpManager mockManager;
	@Mock
	ServletOutputStream mockStream;
	
	String fileName;
	String contentType;
	String contentTypeEncoded;
	String contentSize;
	OutputStream out;
	
	HttpToSftpServlet servlet;
	
	@Before
	public void before() throws Exception{
		MockitoAnnotations.initMocks(this);
		StringBuffer urlBuffer = new StringBuffer();
		urlBuffer.append("http://host.org/sftp/pathStart/pathEnd");
		
		when(mockRequest.getRequestURL()).thenReturn(urlBuffer);
		// setup the query string
		fileName = "foo.txt";
		contentSize = "9876";
		contentType = "text/html; charset=utf-8";
		contentTypeEncoded = URLEncoder.encode(contentType, "UTF-8");
		
		StringBuilder query = new StringBuilder();
		query.append(KEY_FILE_NAME).append("=").append(fileName);
		query.append("&").append(KEY_CONTENT_SIZE).append("=").append(contentSize);
		query.append("&").append(KEY_CONTENT_TYPE).append("=").append(contentTypeEncoded);
		when(mockRequest.getQueryString()).thenReturn(query.toString());
		
		when(mockResponse.getOutputStream()).thenReturn(mockStream);
		
		servlet = new HttpToSftpServlet(mockManager);
	}
	
	@Test
	public void testDoGetHappy() throws Exception {
		//call under test
		servlet.doGet(mockRequest, mockResponse);
		// All three headers should be added
		verify(mockResponse).setHeader(HEADER_CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_PATTERN, fileName));
		verify(mockResponse).setHeader(HEADER_CONTENT_LENGTH, "0");
		verify(mockResponse).setHeader(HEADER_CONTENT_TYPE, contentType);
		
		String expectedPath = "/pathStart/pathEnd";
		verify(mockManager).getFile(eq(expectedPath), any(OutputStream.class));
	}
	
	@Test
	public void testDoGetNoFileName() throws Exception{
		
		StringBuilder query = new StringBuilder();
		query.append(KEY_CONTENT_SIZE).append("=").append(contentSize);
		query.append("&").append(KEY_CONTENT_TYPE).append("=").append(contentTypeEncoded);
		when(mockRequest.getQueryString()).thenReturn(query.toString());
		
		//call under test
		servlet.doGet(mockRequest, mockResponse);
		// two headers should be set
		verify(mockResponse, never()).setHeader(HEADER_CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_PATTERN, fileName));
		verify(mockResponse).setHeader(HEADER_CONTENT_TYPE, contentType);
		
		String expectedPath = "/pathStart/pathEnd";
		verify(mockManager).getFile(eq(expectedPath), any(OutputStream.class));
	}
	
	@Test
	public void testDoGetNoContentType() throws Exception{
		StringBuilder query = new StringBuilder();
		query.append(KEY_FILE_NAME).append("=").append(fileName);
		query.append("&").append(KEY_CONTENT_SIZE).append("=").append(contentSize);
		when(mockRequest.getQueryString()).thenReturn(query.toString());
		
		//call under test
		servlet.doGet(mockRequest, mockResponse);
		// two headers should be set
		verify(mockResponse).setHeader(HEADER_CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_PATTERN, fileName));
		verify(mockResponse, never()).setHeader(HEADER_CONTENT_TYPE, contentType);
		
		String expectedPath = "/pathStart/pathEnd";
		verify(mockManager).getFile(eq(expectedPath), any(OutputStream.class));
	}
	
	@Test
	public void testDoGetNoContentSize() throws Exception{
		
		StringBuilder query = new StringBuilder();
		query.append(KEY_FILE_NAME).append("=").append(fileName);
		query.append("&").append(KEY_CONTENT_TYPE).append("=").append(contentTypeEncoded);
		when(mockRequest.getQueryString()).thenReturn(query.toString());
		
		//call under test
		servlet.doGet(mockRequest, mockResponse);
		// two headers should be set
		verify(mockResponse).setHeader(HEADER_CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_PATTERN, fileName));
		verify(mockResponse, never()).setHeader(HEADER_CONTENT_LENGTH, contentSize);
		verify(mockResponse).setHeader(HEADER_CONTENT_TYPE, contentType);
		
		String expectedPath = "/pathStart/pathEnd";
		verify(mockManager).getFile(eq(expectedPath), any(OutputStream.class));
	}
	
	@Test
	public void testDoGetPathWithPrefix() throws Exception {
		StringBuffer urlBuffer = new StringBuffer();
		urlBuffer.append("http://host.org/prfix/sftp/pathStart/pathEnd");
		
		when(mockRequest.getRequestURL()).thenReturn(urlBuffer);
		//call under test
		servlet.doGet(mockRequest, mockResponse);
		// the prefix should not change the path of the file.
		String expectedPath = "/pathStart/pathEnd";
		verify(mockManager).getFile(eq(expectedPath), any(OutputStream.class));
	}
	
	@Test
	public void testDoGetSftpInPath() throws Exception {
		
		StringBuffer urlBuffer = new StringBuffer();
		// In this example the path contains sftp.
		urlBuffer.append("http://host.org/sftp/pathStart/sftp/pathEnd");
		
		when(mockRequest.getRequestURL()).thenReturn(urlBuffer);
		//call under test
		servlet.doGet(mockRequest, mockResponse);
		
		String expectedPath = "/pathStart/sftp/pathEnd";
		verify(mockManager).getFile(eq(expectedPath), any(OutputStream.class));
	}
	
	@Test
	public void testDoGetNotFound() throws Exception {
		// Setup not found
		String path = "/file/does/not/exist";
		doThrow(new NotFoundException(path)).when(mockManager).getFile(any(String.class), any(OutputStream.class));
		//call under test
		servlet.doGet(mockRequest, mockResponse);
		// should result in a 404
		verify(mockResponse).sendError(HttpServletResponse.SC_NOT_FOUND, path);		
	}

}
