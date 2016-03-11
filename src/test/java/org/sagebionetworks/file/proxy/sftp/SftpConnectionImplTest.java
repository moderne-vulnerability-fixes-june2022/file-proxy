package org.sagebionetworks.file.proxy.sftp;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import java.io.OutputStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.file.proxy.NotFoundException;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

public class SftpConnectionImplTest {
	
	@Mock
	ChannelSftp mockSftpChannel;
	@Mock
	OutputStream mockOut;
	@Mock
	SftpATTRS mockAtts;
	
	long fileSize;
	long lastModifiedOnSec;
	SftpATTRS atts;
	String path;
	// for range read track the current range index.
	long currentRangeIndex;
	long readBufferSize;
	
	SftpConnectionImpl connection;


	@Before
	public void before() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		fileSize = 123L;
		when(mockAtts.getSize()).thenReturn(fileSize);
		lastModifiedOnSec = 8888L;
		when(mockAtts.getMTime()).thenReturn((int) lastModifiedOnSec);
		
		path = "/root/folder/child.txt";
		
		when(mockSftpChannel.lstat(path)).thenReturn(mockAtts);
		
		readBufferSize = 10;
		// Simulate a range read.
		doAnswer(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				// capture the monitor and start index.
				String pathArg = (String) invocation.getArguments()[0];
				SftpProgressMonitor monitor = (SftpProgressMonitor) invocation.getArguments()[2];
				int mode = (Integer) invocation.getArguments()[3];
				long startIndex = (Long) invocation.getArguments()[4];
				// File size will be passed to the monitor.
				monitor.init(mode, pathArg, "??", fileSize);
				// move the monitor to the start index
				monitor.count(startIndex);
				currentRangeIndex = startIndex;
				// simulate a read
				while(monitor.count(readBufferSize)){
					if(currentRangeIndex >= fileSize){
						break;
					}
					currentRangeIndex += readBufferSize;
				}
				return null;
			}
		}).when(mockSftpChannel).get(anyString(), any(OutputStream.class), any(SftpProgressMonitor.class), anyInt(), anyLong());
		
		connection = new SftpConnectionImpl(mockSftpChannel);
	}
	
	@Test
	public void testHandleNotFound() throws NotFoundException{
		String path = "/path/child";
		RuntimeException e = new RuntimeException(SftpConnectionImpl.NO_SUCH_FILE);
		try {
			SftpConnectionImpl.handleNotFound(path, e);
			fail("Should throw NotFoundException");
		} catch (NotFoundException e1) {
			assertEquals(path, e1.getMessage());
		}
	}
	
	@Test
	public void testGetFile() throws NotFoundException, SftpException{
		// call under test
		connection.getFile(path, mockOut);
		verify(mockSftpChannel).get(path, mockOut);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetFileNotFound() throws NotFoundException, SftpException{
		// setup an exaction that should be translated to a NotFoundException
		RuntimeException e = new RuntimeException(SftpConnectionImpl.NO_SUCH_FILE);
		doThrow(e).when(mockSftpChannel).get(anyString(), any(OutputStream.class));
		// call under test
		connection.getFile(path, mockOut);
		verify(mockSftpChannel).get(path, mockOut);
	}
	
	@Test
	public void testGetFileSize() throws NotFoundException, SftpException{
		// call under test
		long size = connection.getFileSize(path);
		assertEquals(fileSize, size);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetFileSizeNotFound() throws NotFoundException, SftpException{
		// setup an exaction that should be translated to a NotFoundException
		RuntimeException e = new RuntimeException(SftpConnectionImpl.NO_SUCH_FILE);
		doThrow(e).when(mockSftpChannel).lstat(anyString());
		// call under test
		long size = connection.getFileSize(path);
		assertEquals(fileSize, size);
	}

	@Test
	public void testGetFileRangeFullRange() throws NotFoundException{
		long startByteIndex = 0;
		long endByteIndex = Long.MAX_VALUE;
		// call under test
		boolean fullRead = connection.getFileRange(path, mockOut, startByteIndex, endByteIndex);
		assertTrue("The entire file should have been read",fullRead);
		assertTrue(currentRangeIndex >= fileSize);
	}
	
	@Test
	public void testGetFileRangePartialRange() throws NotFoundException{
		long startByteIndex = 0;
		long endByteIndex = fileSize-readBufferSize-2;
		// call under test
		boolean fullRead = connection.getFileRange(path, mockOut, startByteIndex, endByteIndex);
		assertFalse("Expected a partial read", fullRead);
		assertTrue(currentRangeIndex < fileSize);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetFileRangeNotFound() throws Exception {
		// setup an exaction that should be translated to a NotFoundException
		RuntimeException e = new RuntimeException(SftpConnectionImpl.NO_SUCH_FILE);
		doThrow(e).when(mockSftpChannel).get(anyString(), any(OutputStream.class), any(SftpProgressMonitor.class), anyInt(), anyLong());
		
		long startByteIndex = 0;
		long endByteIndex = Long.MAX_VALUE;
		// call under test
		connection.getFileRange(path, mockOut, startByteIndex, endByteIndex);
	}
	
	@Test
	public void testGetLastMod() throws NotFoundException{
		// call under test
		long lastMod = connection.getLastModifiedDate(path);
		// value should be returned in MS.
		long expectedLastModifiedOnMS = this.lastModifiedOnSec*1000;
		assertEquals(expectedLastModifiedOnMS, lastMod);
	}
	
}
