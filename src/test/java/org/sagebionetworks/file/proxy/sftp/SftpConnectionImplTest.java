package org.sagebionetworks.file.proxy.sftp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.file.proxy.NotFoundException;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;

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
			assertEquals(path, e1.getMessage());
		}
	}
}
