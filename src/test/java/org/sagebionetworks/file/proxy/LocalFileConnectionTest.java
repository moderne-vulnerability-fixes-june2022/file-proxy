package org.sagebionetworks.file.proxy;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LocalFileConnectionTest {
	
	String fileData;
	File tempFile;
	String pathPrefix;
	LocalFileConnection connection;

	@Before
	public void before() throws IOException{
		// Use the temp directory as the pathPrefix
		pathPrefix = System.getProperty("java.io.tmpdir");
		connection = new LocalFileConnection(pathPrefix);
		
		// Create a tempFile to use.
		tempFile = File.createTempFile("LocalFileConnectionTest", ".txt");
		fileData = "This is the data that in the temp file";
		FileUtils.write(tempFile, fileData, "UTF-8");
	}
	
	@After
	public void after(){
		if(tempFile != null){
			tempFile.delete();
		}
	}
	
	@Test
	public void testGetFileForPathHappy() throws NotFoundException{
		// method under test.
		File file = connection.getFileForPath(tempFile.getName());
		assertNotNull(file);
		assertEquals(tempFile, file);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetFileForPathNotFound() throws NotFoundException{
		// method under test.
		connection.getFileForPath("ShouldNotExist.text");
	}
	
	@Test
	public void testFileSize() throws NotFoundException{
		// method under test.
		long size = connection.getFileSize(tempFile.getName());
		assertEquals(tempFile.length(), size);
	}
	
	@Test
	public void testGetFile() throws NotFoundException, IOException{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		// method under test
		connection.getFile(tempFile.getName(), out);
		// Is the file content copied?
		String fromOut = new String(out.toByteArray(), "UTF-8");
		assertEquals(fileData, fromOut);
	}
	
	@Test
	public void testGetFileRange() throws NotFoundException, IOException{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		// read the file in parts
		long fileSize = tempFile.length();
		// Read the file 3 bytes at a time.
		long bytesPerRead = 3;
		for(long startByteIndex = 0; startByteIndex < fileSize; startByteIndex+=bytesPerRead){
			long endByteIndex = startByteIndex + bytesPerRead;
			// method under test.
			connection.getFileRange(tempFile.getName(), out, startByteIndex, endByteIndex);
		}
		// Is the file content copied?
		String fromOut = new String(out.toByteArray(), "UTF-8");
		assertEquals(fileData, fromOut);
	}
	
}
