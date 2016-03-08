package org.sagebionetworks.file.proxy.servlet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.file.proxy.servlet.FileControllerImpl.HEADER_CONTENT_LENGTH;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.file.proxy.TestContext;
import org.sagebionetworks.file.proxy.filter.TestServletOutputStream;
import org.sagebionetworks.file.proxy.module.ApplicationServletModule;

/**
 * An integration test for the HttpToLocalServlet.  The HttpServletRequest and HttpServletResponse are mocked but
 * a Guice inject servlet is used.
 * 
 *
 */
public class HttpToLocalServletTest {
	
	@Mock
	HttpServletRequest mockRequest;
	@Mock
	HttpServletResponse mockResponse;
	
	TestServletOutputStream outputStream;
	String fileData;
	File tempFile;
	String pathPrefix;
	
	String url;

	
	HttpToLocalServlet servlet;
	
	@Before
	public void before() throws IOException{
		MockitoAnnotations.initMocks(this);
		
		// Use the temp directory as the pathPrefix
		pathPrefix = System.getProperty("java.io.tmpdir");
		// Set the configuration property for local path
		System.setProperty("org.sagebionetworks.local.path.prefix", pathPrefix);
		
		// Create a tempFile to use.
		tempFile = File.createTempFile("HttpToLocalServletTest", ".txt");
		fileData = "This is the data that in the temp file";
		FileUtils.write(tempFile, fileData, "UTF-8");
		
		url = "https://domain.org"+ApplicationServletModule.LOCAL_PATH_PREFIX+tempFile.getName();
		when(mockRequest.getRequestURL()).thenReturn(new StringBuffer(url));
		
		outputStream = new TestServletOutputStream();
		when(mockResponse.getOutputStream()).thenReturn(outputStream);
		
		// Create a Guice injected servlet.
		servlet = TestContext.createTestConext().getInstance(HttpToLocalServlet.class);
	}
	
	@After
	public void after(){
		if(tempFile != null){
			tempFile.delete();
		}
	}
	
	@Test
	public void testDoHead() throws ServletException, IOException{
		// call under test
		servlet.doHead(mockRequest, mockResponse);
		verify(mockResponse).setHeader(HEADER_CONTENT_LENGTH, ""+tempFile.length());
		verify(mockResponse).setStatus(HttpServletResponse.SC_OK);
	}
	
	@Test
	public void testDoGet() throws ServletException, IOException{
		// call under test
		servlet.doGet(mockRequest, mockResponse);
		verify(mockResponse).setHeader(HEADER_CONTENT_LENGTH, ""+tempFile.length());
		verify(mockResponse).setStatus(HttpServletResponse.SC_OK);
		
		// validate the file was written to the stream
		String fromOut = new String(outputStream.toByteArray(), "UTF-8");
		assertEquals(fileData, fromOut);
	}

}
