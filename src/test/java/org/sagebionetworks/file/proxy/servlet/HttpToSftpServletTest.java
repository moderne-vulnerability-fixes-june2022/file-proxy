package org.sagebionetworks.file.proxy.servlet;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.sagebionetworks.file.proxy.servlet.HttpToSftpServlet.*;

import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.file.proxy.NotFoundException;
import org.sagebionetworks.file.proxy.sftp.ConnectionHandler;
import org.sagebionetworks.file.proxy.sftp.SftpConnection;
import org.sagebionetworks.file.proxy.sftp.SftpConnectionManager;

public class HttpToSftpServletTest {
	
	@Mock
	HttpServletRequest mockRequest;
	@Mock
	HttpServletResponse mockResponse;
	@Mock
	SftpConnectionManager mockConnectionManager;
	@Mock
	ServletOutputStream mockStream;
	@Mock
	SftpConnection mockConnection;
	@Mock
	ConnectionHandler mockConnectionHandler;
	
	String fileName;
	String contentType;
	String contentTypeEncoded;
	long contentSize;
	String contentMD5Hex;
	String contentMD5Base64;
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
		contentSize = 9876L;
		contentType = "text/html; charset=utf-8";
		contentTypeEncoded = URLEncoder.encode(contentType, "UTF-8");
		contentMD5Hex = "c295c08ccfd979130729592bf936b85f";
		contentMD5Base64 = Base64.encodeBase64String(Hex.decodeHex(contentMD5Hex.toCharArray()));
		
		StringBuilder query = new StringBuilder();
		query.append(KEY_FILE_NAME).append("=").append(fileName);
		query.append("&").append(KEY_CONTENT_SIZE).append("=").append(contentSize);
		query.append("&").append(KEY_CONTENT_TYPE).append("=").append(contentTypeEncoded);
		query.append("&").append(KEY_CONTENT_MD5).append("=").append(contentMD5Hex);
		when(mockRequest.getQueryString()).thenReturn(query.toString());
		
		when(mockResponse.getOutputStream()).thenReturn(mockStream);
		
		// Setup a mock connection when connect is called.
		doAnswer(new Answer<Void>(){
			@Override
			public Void answer(InvocationOnMock invocation)
					throws Throwable {
				// Connect with a mock connection.
				ConnectionHandler handler = (ConnectionHandler) invocation.getArguments()[0];
				// forward the mock connection to the handler.
				handler.execute(mockConnection);
				return null;
			}}).when(mockConnectionManager).connect(any(ConnectionHandler.class));
		
		servlet = new HttpToSftpServlet(mockConnectionManager);
		
		// return the content size
		when(mockConnection.getFileSize(anyString())).thenReturn(contentSize);
	}
	
	@Test
	public void testPrepareResponse() throws MalformedURLException, NotFoundException{
		// call under test
		String path = HttpToSftpServlet.prepareResponse(mockRequest, mockResponse, mockConnection);
		assertEquals("/pathStart/pathEnd",path);
		
		// four headers should be added
		verify(mockResponse).setHeader(HEADER_CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_PATTERN, fileName));
		verify(mockResponse).setHeader(HEADER_CONTENT_TYPE, contentType);
		verify(mockResponse).setHeader(HEADER_CONTENT_LENGTH, ""+contentSize);
		verify(mockResponse).setHeader(HEADER_CONTENT_MD5, contentMD5Base64);
	}
	
	@Test
	public void testPrepareResponseWithPrefix() throws MalformedURLException, NotFoundException{
		// The 'prefix' should be ignored.
		StringBuffer urlBuffer = new StringBuffer();
		urlBuffer.append("http://host.org/prfix/sftp/pathStart/pathEnd");
		// call under test
		String path = HttpToSftpServlet.prepareResponse(mockRequest, mockResponse, mockConnection);
		assertEquals("/pathStart/pathEnd",path);
	}
	
	@Test
	public void testPrepareResponseWithSftpInPath() throws MalformedURLException, NotFoundException{
		// The 'prefix' should be ignored.
		StringBuffer urlBuffer = new StringBuffer();
		urlBuffer.append("http://host.org/sftp/pathStart/sftp/pathEnd");
		when(mockRequest.getRequestURL()).thenReturn(urlBuffer);
		// call under test
		String path = HttpToSftpServlet.prepareResponse(mockRequest, mockResponse, mockConnection);
		assertEquals("/pathStart/sftp/pathEnd",path);
	}
	
	@Test
	public void testPrepareResponseNoName() throws MalformedURLException, NotFoundException{
		
		StringBuilder query = new StringBuilder();
		query.append(KEY_CONTENT_SIZE).append("=").append(contentSize);
		query.append("&").append(KEY_CONTENT_TYPE).append("=").append(contentTypeEncoded);
		query.append("&").append(KEY_CONTENT_MD5).append("=").append(contentMD5Hex);
		when(mockRequest.getQueryString()).thenReturn(query.toString());
		
		// call under test
		HttpToSftpServlet.prepareResponse(mockRequest, mockResponse, mockConnection);
		
		// four headers should be added
		verify(mockResponse, never()).setHeader(HEADER_CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_PATTERN, fileName));
		verify(mockResponse).setHeader(HEADER_CONTENT_TYPE, contentType);
		verify(mockResponse).setHeader(HEADER_CONTENT_LENGTH, ""+contentSize);
		verify(mockResponse).setHeader(HEADER_CONTENT_MD5, contentMD5Base64);
	}
	
	@Test
	public void testPrepareResponseNoContentType() throws MalformedURLException, NotFoundException{
		
		StringBuilder query = new StringBuilder();
		query.append(KEY_FILE_NAME).append("=").append(fileName);
		query.append("&").append(KEY_CONTENT_SIZE).append("=").append(contentSize);
		query.append("&").append(KEY_CONTENT_MD5).append("=").append(contentMD5Hex);
		when(mockRequest.getQueryString()).thenReturn(query.toString());
		
		// call under test
		HttpToSftpServlet.prepareResponse(mockRequest, mockResponse, mockConnection);
		
		// four headers should be added
		verify(mockResponse).setHeader(HEADER_CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_PATTERN, fileName));
		verify(mockResponse, never()).setHeader(HEADER_CONTENT_TYPE, contentType);
		verify(mockResponse).setHeader(HEADER_CONTENT_LENGTH, ""+contentSize);
		verify(mockResponse).setHeader(HEADER_CONTENT_MD5, contentMD5Base64);
	}
	
	@Test
	public void testPrepareResponseNoMD5() throws MalformedURLException, NotFoundException{
		
		StringBuilder query = new StringBuilder();
		query.append(KEY_FILE_NAME).append("=").append(fileName);
		query.append("&").append(KEY_CONTENT_SIZE).append("=").append(contentSize);
		query.append("&").append(KEY_CONTENT_TYPE).append("=").append(contentTypeEncoded);
		when(mockRequest.getQueryString()).thenReturn(query.toString());
		
		// call under test
		HttpToSftpServlet.prepareResponse(mockRequest, mockResponse, mockConnection);
		
		// four headers should be added
		verify(mockResponse).setHeader(HEADER_CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_PATTERN, fileName));
		verify(mockResponse).setHeader(HEADER_CONTENT_TYPE, contentType);
		verify(mockResponse).setHeader(HEADER_CONTENT_LENGTH, ""+contentSize);
		verify(mockResponse, never()).setHeader(HEADER_CONTENT_MD5, contentMD5Base64);
	}
	
	@Test
	public void testDoConnectionUnknownException() throws Exception{
		// setup a not found
		String error = "some error";
		doThrow(new RuntimeException(error)).when(mockConnectionManager).connect(any(ConnectionHandler.class));
		// call under test
		servlet.doConnection(mockRequest, mockResponse, mockConnectionHandler);
		// should result in a 500
		verify(mockResponse).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error);		
	}
	
	@Test
	public void testDoConnectionNotFound() throws Exception{
		// setup a not found
		String path = "/file/does/not/exist";
		doThrow(new NotFoundException(path)).when(mockConnectionManager).connect(any(ConnectionHandler.class));
		// call under test
		servlet.doConnection(mockRequest, mockResponse, mockConnectionHandler);
		// should result in a 404
		verify(mockResponse).sendError(HttpServletResponse.SC_NOT_FOUND, path);		
	}
	
	@Test
	public void testDoGetHappy() throws Exception {
		//call under test
		servlet.doGet(mockRequest, mockResponse);
		// All of the headers should be added.
		verify(mockResponse).setHeader(HEADER_CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_PATTERN, fileName));
		verify(mockResponse).setHeader(HEADER_CONTENT_TYPE, contentType);
		verify(mockResponse).setHeader(HEADER_CONTENT_LENGTH, ""+contentSize);
		verify(mockResponse).setHeader(HEADER_CONTENT_MD5, contentMD5Base64);
		
		String expectedPath = "/pathStart/pathEnd";
		verify(mockConnection).getFile(eq(expectedPath), any(OutputStream.class));
	}
	
	@Test
	public void testDoHeadHappy() throws Exception {
		//call under test
		servlet.doHead(mockRequest, mockResponse);
		// All of the headers should be added.
		verify(mockResponse).setHeader(HEADER_CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_PATTERN, fileName));
		verify(mockResponse).setHeader(HEADER_CONTENT_TYPE, contentType);
		verify(mockResponse).setHeader(HEADER_CONTENT_LENGTH, ""+contentSize);
		verify(mockResponse).setHeader(HEADER_CONTENT_MD5, contentMD5Base64);
		// The file should not be read with a head.
		verify(mockConnection, never()).getFile(anyString(), any(OutputStream.class));
	}

	@Test
	public void testIsGZIPRequest(){
		// by default this is not a GZIP request
		assertFalse(HttpToSftpServlet.isGZIPRequest(mockRequest));
		// setup a GZIP request
		when(mockRequest.getHeader(HEADER_ACCEPT_ENCODING)).thenReturn("gzip, deflate");
		assertTrue(HttpToSftpServlet.isGZIPRequest(mockRequest));
	}
	
	@Test
	public void testPrepareResponseGZIP() throws MalformedURLException, NotFoundException{
		// setup a GZIP request
		when(mockRequest.getHeader(HEADER_ACCEPT_ENCODING)).thenReturn("gzip, deflate");
		// call under test
		String path = HttpToSftpServlet.prepareResponse(mockRequest, mockResponse, mockConnection);
		assertEquals("/pathStart/pathEnd",path);
		// gzip content encoding should be used.
		verify(mockResponse).setHeader(HEADER_CONTENT_ENCODING, GZIP);
	}
	
	@Test
	public void testDoGetGZIP() throws Exception {
		// setup a GZIP request
		when(mockRequest.getHeader(HEADER_ACCEPT_ENCODING)).thenReturn("gzip, deflate");
		//call under test
		servlet.doGet(mockRequest, mockResponse);
		// All of the headers should be added.
		verify(mockResponse).setHeader(HEADER_CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_PATTERN, fileName));
		verify(mockResponse).setHeader(HEADER_CONTENT_TYPE, contentType);
		verify(mockResponse).setHeader(HEADER_CONTENT_LENGTH, ""+contentSize);
		verify(mockResponse).setHeader(HEADER_CONTENT_MD5, contentMD5Base64);
		verify(mockResponse).setHeader(HEADER_CONTENT_ENCODING, GZIP);
		
		String expectedPath = "/pathStart/pathEnd";
		// The file should be written to a GZIP
		verify(mockConnection).getFile(eq(expectedPath), any(GZIPOutputStream.class));
	}
}
