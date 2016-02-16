package org.sagebionetworks.file.proxy.sftp;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.OutputStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.file.proxy.NotFoundException;
import org.sagebionetworks.file.proxy.config.Configuration;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class SftpConnectionImplTest {
	
	@Mock
	ChannelSftp mockSftpChannel;
	
	SftpConnectionImpl connection;


	@Before
	public void before() throws JSchException {
		MockitoAnnotations.initMocks(this);
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
			assertEquals(path, e.getMessage());
		}
	}
}
