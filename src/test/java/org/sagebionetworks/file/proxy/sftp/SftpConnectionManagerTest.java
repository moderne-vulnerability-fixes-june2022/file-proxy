package org.sagebionetworks.file.proxy.sftp;

import static org.junit.Assert.fail;
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
import org.sagebionetworks.file.proxy.FileConnection;
import org.sagebionetworks.file.proxy.NotFoundException;
import org.sagebionetworks.file.proxy.config.Configuration;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class SftpConnectionManagerTest {
	@Mock
	Configuration mockConfig;
	@Mock
	JcraftFactory mockJcraftFactory;
	@Mock
	Session mockSession;
	@Mock
	ChannelSftp mockChannel;
	@Mock
	OutputStream mockOutStream;
	@Mock
	ConnectionHandler mockHandler;

	SftpConnectionManager manager;

	String userName;
	String password;
	String host;
	int port;

	@Before
	public void before() throws JSchException {
		MockitoAnnotations.initMocks(this);

		userName = "userName";
		password = "password";
		host = "host.org";
		port = 22;

		when(mockConfig.getSftpUsername()).thenReturn(userName);
		when(mockConfig.getSftpPassword()).thenReturn(password);
		when(mockConfig.getSftpHost()).thenReturn(host);
		when(mockConfig.getSftpPort()).thenReturn(port);

		when(mockJcraftFactory.openNewSession(userName, password, host, port))
				.thenReturn(mockSession);
		when(mockSession.openChannel(anyString())).thenReturn(mockChannel);
		manager = new SftpConnectionManager(mockConfig, mockJcraftFactory);
	}

	@Test
	public void testConnectHappy() throws Exception {
		// call under test
		manager.connect(mockHandler);
		// the channel should be opened
		verify(mockSession).openChannel("sftp");
		verify(mockChannel).connect();
		verify(mockChannel).disconnect();
		verify(mockSession).disconnect();
		verify(mockHandler).execute(any(FileConnection.class));
	}

	@Test
	public void testConnectHandlerError() throws NotFoundException, Exception {
		// Setup a failure
		doThrow(new SftpException(22, "Something went wrong"))
				.when(mockHandler).execute(any(FileConnection.class));
		// call under test
		try {
			manager.connect(mockHandler);
			fail("Should have failed");
		} catch (Exception e) {
			// expected
		}
		// both the channel and session must be disconnected even though there
		// was an errors
		verify(mockChannel).disconnect();
		verify(mockSession).disconnect();
	}

	@Test
	public void testGetFileEarlyException() throws JSchException, SftpException {
		// Setup a failure
		when(mockJcraftFactory.openNewSession(userName, password, host, port))
				.thenThrow(new JSchException("Cannot connect"));
		String path = "somePath";
		// call under test
		try {
			manager.connect(mockHandler);
			fail("Should have failed");
		} catch (Exception e) {
			// expected
		}
		verify(mockSession, never()).openChannel("sftp");
		verify(mockChannel, never()).connect();
		verify(mockChannel, never()).get(path, mockOutStream);
	}


}
