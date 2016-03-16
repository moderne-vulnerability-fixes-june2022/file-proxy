package org.sagebionetworks.file.proxy.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.file.proxy.servlet.FileControllerImpl.*;
import static org.sagebionetworks.file.proxy.servlet.FileControllerImpl.CONTENT_DISPOSITION_PATTERN;
import static org.sagebionetworks.file.proxy.servlet.FileControllerImpl.GZIP;
import static org.sagebionetworks.file.proxy.servlet.FileControllerImpl.HEADER_ACCEPT_ENCODING;
import static org.sagebionetworks.file.proxy.servlet.FileControllerImpl.HEADER_ACCEPT_RANGES;
import static org.sagebionetworks.file.proxy.servlet.FileControllerImpl.HEADER_CONTENT_DISPOSITION;
import static org.sagebionetworks.file.proxy.servlet.FileControllerImpl.HEADER_CONTENT_ENCODING;
import static org.sagebionetworks.file.proxy.servlet.FileControllerImpl.HEADER_CONTENT_LENGTH;
import static org.sagebionetworks.file.proxy.servlet.FileControllerImpl.HEADER_CONTENT_MD5;
import static org.sagebionetworks.file.proxy.servlet.FileControllerImpl.HEADER_CONTENT_RANGE;
import static org.sagebionetworks.file.proxy.servlet.FileControllerImpl.HEADER_CONTENT_TYPE;
import static org.sagebionetworks.file.proxy.servlet.FileControllerImpl.HEADER_RANGE;
import static org.sagebionetworks.file.proxy.servlet.FileControllerImpl.KEY_CONTENT_MD5;
import static org.sagebionetworks.file.proxy.servlet.FileControllerImpl.KEY_CONTENT_SIZE;
import static org.sagebionetworks.file.proxy.servlet.FileControllerImpl.KEY_CONTENT_TYPE;
import static org.sagebionetworks.file.proxy.servlet.FileControllerImpl.KEY_FILE_NAME;
import static org.sagebionetworks.file.proxy.servlet.FileControllerImpl.prepareContentHeaders;

import java.io.OutputStream;
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
import org.sagebionetworks.file.proxy.FileConnection;
import org.sagebionetworks.file.proxy.NotFoundException;
import org.sagebionetworks.file.proxy.RangeNotSatisfiable;
import org.sagebionetworks.file.proxy.sftp.ConnectionHandler;
import org.sagebionetworks.file.proxy.sftp.FileConnectionManager;

public class FileControllerTest {
	
	@Mock
	HttpServletRequest mockRequest;
	@Mock
	HttpServletResponse mockResponse;
	@Mock
	FileConnectionManager mockConnectionManager;
	@Mock
	ServletOutputStream mockStream;
	@Mock
	FileConnection mockConnection;
	@Mock
	ConnectionHandler mockConnectionHandler;
	
	String fileName;
	String contentType;
	String contentTypeEncoded;
	long contentSize;
	String contentMD5Hex;
	String contentMD5Base64;
	OutputStream out;
	String pathPrefix;
	
	FileControllerImpl servlet;
	
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
		
		pathPrefix = "/sftp/";
		servlet = new FileControllerImpl(mockConnectionManager, pathPrefix);
		
		// return the content size
		when(mockConnection.getFileSize(anyString())).thenReturn(contentSize);
		when(mockConnection.getLastModifiedDate(anyString())).thenReturn(456000L);
	}
	
	@Test
	public void testPrepareResponse() throws Exception {
		// call under test
		RequestDescription desc = FileControllerImpl.prepareResponse(mockRequest, mockResponse, mockConnection, pathPrefix);
		assertEquals("/pathStart/pathEnd", desc.getPath());
		assertFalse(desc.isGZIP());
		assertEquals(contentSize, desc.getFileSize());
		
		// four headers should be added
		verify(mockResponse).setHeader(HEADER_CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_PATTERN, fileName));
		verify(mockResponse).setHeader(HEADER_CONTENT_TYPE, contentType);
		verify(mockResponse).setHeader(HEADER_CONTENT_LENGTH, ""+contentSize);
		verify(mockResponse).setHeader(HEADER_CONTENT_MD5, contentMD5Base64);
		verify(mockResponse).setHeader(HEADER_E_TAG, contentMD5Hex);
		verify(mockResponse).setHeader(eq(HEADER_DATE), anyString());
		verify(mockResponse).setHeader(HEADER_CONTENT_LOCATION, desc.getPath());
		verify(mockResponse).setHeader(HEADER_LAST_MODIFIED, "Thu, 01 Jan 1970 00:07:36 GMT");
		// also support for byte serving
		verify(mockResponse).setHeader(HEADER_ACCEPT_RANGES, BYTES);
	}
	
	@Test
	public void testPrepareResponseWithPrefix() throws Exception {
		// The 'prefix' should be ignored.
		StringBuffer urlBuffer = new StringBuffer();
		urlBuffer.append("http://host.org/prfix/sftp/pathStart/pathEnd");
		// call under test
		RequestDescription desc = FileControllerImpl.prepareResponse(mockRequest, mockResponse, mockConnection, pathPrefix);
		assertEquals("/pathStart/pathEnd",desc.getPath());
	}
	
	@Test
	public void testPrepareResponseWithSftpInPath() throws Exception {
		// The 'prefix' should be ignored.
		StringBuffer urlBuffer = new StringBuffer();
		urlBuffer.append("http://host.org/sftp/pathStart/sftp/pathEnd");
		when(mockRequest.getRequestURL()).thenReturn(urlBuffer);
		// call under test
		RequestDescription desc = FileControllerImpl.prepareResponse(mockRequest, mockResponse, mockConnection, pathPrefix);
		assertEquals("/pathStart/sftp/pathEnd",desc.getPath());
	}
	
	@Test
	public void testPrepareResponseNoName() throws Exception {
		
		StringBuilder query = new StringBuilder();
		query.append(KEY_CONTENT_SIZE).append("=").append(contentSize);
		query.append("&").append(KEY_CONTENT_TYPE).append("=").append(contentTypeEncoded);
		query.append("&").append(KEY_CONTENT_MD5).append("=").append(contentMD5Hex);
		when(mockRequest.getQueryString()).thenReturn(query.toString());
		
		// call under test
		FileControllerImpl.prepareResponse(mockRequest, mockResponse, mockConnection, pathPrefix);
		
		// four headers should be added
		verify(mockResponse, never()).setHeader(HEADER_CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_PATTERN, fileName));
		verify(mockResponse).setHeader(HEADER_CONTENT_TYPE, contentType);
		verify(mockResponse).setHeader(HEADER_CONTENT_LENGTH, ""+contentSize);
		verify(mockResponse).setHeader(HEADER_CONTENT_MD5, contentMD5Base64);
	}
	
	@Test
	public void testPrepareResponseNoContentType() throws Exception {
		
		StringBuilder query = new StringBuilder();
		query.append(KEY_FILE_NAME).append("=").append(fileName);
		query.append("&").append(KEY_CONTENT_SIZE).append("=").append(contentSize);
		query.append("&").append(KEY_CONTENT_MD5).append("=").append(contentMD5Hex);
		when(mockRequest.getQueryString()).thenReturn(query.toString());
		
		// call under test
		FileControllerImpl.prepareResponse(mockRequest, mockResponse, mockConnection, pathPrefix);
		
		// four headers should be added
		verify(mockResponse).setHeader(HEADER_CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_PATTERN, fileName));
		verify(mockResponse, never()).setHeader(HEADER_CONTENT_TYPE, contentType);
		verify(mockResponse).setHeader(HEADER_CONTENT_LENGTH, ""+contentSize);
		verify(mockResponse).setHeader(HEADER_CONTENT_MD5, contentMD5Base64);
	}
	
	@Test
	public void testPrepareResponseNoMD5() throws Exception {
		
		StringBuilder query = new StringBuilder();
		query.append(KEY_FILE_NAME).append("=").append(fileName);
		query.append("&").append(KEY_CONTENT_SIZE).append("=").append(contentSize);
		query.append("&").append(KEY_CONTENT_TYPE).append("=").append(contentTypeEncoded);
		when(mockRequest.getQueryString()).thenReturn(query.toString());
		
		// call under test
		FileControllerImpl.prepareResponse(mockRequest, mockResponse, mockConnection, pathPrefix);
		
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
	public void testIsGZIPRequestNoAcceptHeader(){
		String contentType = "text/plain";
		long fileSize = FileControllerImpl.MIN_COMPRESSION_FILE_SIZE_BYES+1;
		// call under test
		boolean isGZIP = FileControllerImpl.isGZIPRequest(mockRequest, contentType, fileSize);
		assertFalse(isGZIP);
	}
	
	@Test
	public void testIsGZIPRequest(){
		String contentType = "text/plain";
		long fileSize = FileControllerImpl.MIN_COMPRESSION_FILE_SIZE_BYES+1;
		// setup a GZIP request
		when(mockRequest.getHeader(HEADER_ACCEPT_ENCODING)).thenReturn("gzip, deflate");
		// call under test
		boolean isGZIP = FileControllerImpl.isGZIPRequest(mockRequest, contentType, fileSize);
		assertTrue(isGZIP);
	}
	
	@Test
	public void testIsGZIPRequestBinary(){
		String contentType = "application/octet-stream";
		long fileSize = FileControllerImpl.MIN_COMPRESSION_FILE_SIZE_BYES+1;
		// setup a GZIP request
		when(mockRequest.getHeader(HEADER_ACCEPT_ENCODING)).thenReturn("gzip, deflate");
		// call under test
		boolean isGZIP = FileControllerImpl.isGZIPRequest(mockRequest, contentType, fileSize);
		assertFalse(isGZIP);
	}
	
	@Test
	public void testIsGZIPRequestTooSmall(){
		String contentType = "text/plain";
		long fileSize = FileControllerImpl.MIN_COMPRESSION_FILE_SIZE_BYES-1;
		// setup a GZIP request
		when(mockRequest.getHeader(HEADER_ACCEPT_ENCODING)).thenReturn("gzip, deflate");
		// call under test
		boolean isGZIP = FileControllerImpl.isGZIPRequest(mockRequest, contentType, fileSize);
		assertFalse(isGZIP);
	}
	
	@Test
	public void testPrepareResponseGZIP() throws Exception {
		// setup a GZIP request
		when(mockRequest.getHeader(HEADER_ACCEPT_ENCODING)).thenReturn("gzip, deflate");
		// call under test
		RequestDescription desc = FileControllerImpl.prepareResponse(mockRequest, mockResponse, mockConnection, pathPrefix);
		assertEquals("/pathStart/pathEnd",desc.getPath());
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
	
	@Test
	public void testIsRangeRequestFalse() throws Exception {
		long fileSize = 123;
		// default request is not a range request
		assertEquals(null, FileControllerImpl.isRangeRequest(mockRequest, fileSize));
	}
	
	@Test
	public void testIsRangeRequestTrue() throws Exception {
		// setup a range request
		String rangeString = "bytes=1-";
		when(mockRequest.getHeader(HEADER_RANGE)).thenReturn(rangeString);
		// Setup a range request
		long fileSize = 123;
		// default request is not a range request
		RangeValue exected = new RangeValue(rangeString);
		// call under test
		RangeValue result = FileControllerImpl.isRangeRequest(mockRequest, fileSize);
		//call under test.
		assertEquals(exected, result);
	}
	
	@Test
	public void testIsRangeRequestRangeNotSatisfiable(){
		// setup an invalid range
		String rangeString = "bytes=100-0";
		when(mockRequest.getHeader(HEADER_RANGE)).thenReturn(rangeString);
		// Setup a range request
		long fileSize = 123;
		try {
			// call under test
			FileControllerImpl.isRangeRequest(mockRequest, fileSize);
			fail("Should throw an exception");
		} catch (RangeNotSatisfiable e) {
			// expected
			assertEquals(rangeString, e.getMessage());
			assertEquals(fileSize, e.getFileSize());
		}
	}
	
	@Test
	public void testPrepareContentHeadersWithRangeUnbounded() throws Exception {
		// setup a range request
		String rangeString = "bytes=2-";
		long fileSize = 444;
		when(mockRequest.getHeader(HEADER_RANGE)).thenReturn(rangeString);
		RequestDescription desc = new RequestDescription();
		desc.setFileSize(fileSize);
		// call under test
		prepareContentHeaders(mockRequest, mockResponse, desc);
		// verify the headers are set as expected.
		verify(mockResponse).setHeader(HEADER_CONTENT_RANGE, "bytes 2-443/444");
		// The length is the size of the range.
		verify(mockResponse).setHeader(HEADER_CONTENT_LENGTH, ""+(443-2+1));
		assertNotNull(desc.getRange());
		assertEquals(2, desc.getRange().getFirstBytePosition());
		assertEquals(443, desc.getRange().getLastBytePosition());
	}
	
	@Test
	public void testPrepareContentHeadersWithRangeBoundex() throws Exception {
		// setup a range request
		String rangeString = "bytes=25-99";
		long fileSize = 444;
		when(mockRequest.getHeader(HEADER_RANGE)).thenReturn(rangeString);
		RequestDescription desc = new RequestDescription();
		desc.setFileSize(fileSize);
		// call under test
		prepareContentHeaders(mockRequest, mockResponse, desc);
		// verify the headers are set as expected.
		verify(mockResponse).setHeader(HEADER_CONTENT_RANGE, "bytes 25-99/444");
		// The length is the size of the range.
		verify(mockResponse).setHeader(HEADER_CONTENT_LENGTH, ""+(99-25+1));
		assertNotNull(desc.getRange());
		assertEquals(25, desc.getRange().getFirstBytePosition());
		assertEquals(99, desc.getRange().getLastBytePosition());
	}
	
	@Test
	public void testPrepareContentHeadersWithRangeFullFile() throws Exception {
		// setup a range request
		String rangeString = "bytes=0-";
		long fileSize = 444;
		when(mockRequest.getHeader(HEADER_RANGE)).thenReturn(rangeString);
		RequestDescription desc = new RequestDescription();
		desc.setFileSize(fileSize);
		// call under test
		prepareContentHeaders(mockRequest, mockResponse, desc);
		// verify the headers are set as expected.
		verify(mockResponse).setHeader(HEADER_CONTENT_RANGE, "bytes 0-443/444");
		// The length is the entire file size
		verify(mockResponse).setHeader(HEADER_CONTENT_LENGTH, ""+fileSize);
		assertNotNull(desc.getRange());
		assertEquals(0, desc.getRange().getFirstBytePosition());
		assertEquals(443, desc.getRange().getLastBytePosition());
	}
	
	@Test
	public void testPrepareContentHeadersWithRangeLastMax() throws Exception {
		// setup a range request
		String rangeString = "bytes=15-"+Long.MAX_VALUE;
		long fileSize = 444;
		when(mockRequest.getHeader(HEADER_RANGE)).thenReturn(rangeString);
		RequestDescription desc = new RequestDescription();
		desc.setFileSize(fileSize);
		// call under test
		prepareContentHeaders(mockRequest, mockResponse, desc);
		// verify the headers are set as expected.
		verify(mockResponse).setHeader(HEADER_CONTENT_RANGE, "bytes 15-443/444");
		// The length is the size of the range.
		verify(mockResponse).setHeader(HEADER_CONTENT_LENGTH, ""+(443-15+1));
		assertNotNull(desc.getRange());
		assertEquals(15, desc.getRange().getFirstBytePosition());
		assertEquals(443, desc.getRange().getLastBytePosition());
	}
	
	@Test(expected=RangeNotSatisfiable.class)
	public void testPrepareContentHeadersRangeNotSatisfiable() throws Exception {
		// range with invalid units
		String rangeString = "meters=15-16";
		long fileSize = 444;
		when(mockRequest.getHeader(HEADER_RANGE)).thenReturn(rangeString);
		RequestDescription desc = new RequestDescription();
		desc.setFileSize(fileSize);
		// call under test
		prepareContentHeaders(mockRequest, mockResponse, desc);
	}
	
	@Test
	public void testPrepareContentHeadersNoRange() throws Exception {
		// setup a request without a range.
		String rangeString = null;
		long fileSize = 444;
		when(mockRequest.getHeader(HEADER_RANGE)).thenReturn(rangeString);
		RequestDescription desc = new RequestDescription();
		desc.setFileSize(fileSize);
		// call under test
		prepareContentHeaders(mockRequest, mockResponse, desc);
		// verify the headers are set as expected.
		verify(mockResponse, never()).setHeader(eq(HEADER_CONTENT_RANGE), anyString());
		// The length is the size of the file
		verify(mockResponse).setHeader(HEADER_CONTENT_LENGTH, ""+fileSize);
		// range should not be set for this case.
		assertEquals(null, desc.getRange());
	}
	
	@Test
	public void testGetServerTime(){
		// call under test
		String timeString = FileControllerImpl.getServerTime(123000);
		assertEquals("Thu, 01 Jan 1970 00:02:03 GMT", timeString);
	}
}
