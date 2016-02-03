package org.sagebionetworks.file.proxy.sftp;

import java.io.OutputStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.mockito.MockitoAnnotations;
import org.sagebionetworks.file.proxy.config.Configuration;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class SftpManagerImplTest {
	
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
	
	SftpManagerImpl manager;
	
	String userName;
	String password;
	String host;
	int port;
	
	@Before 
	public void before() throws JSchException{
		MockitoAnnotations.initMocks(this);
		
		userName = "userName";
		password = "password";
		host = "host.org";
		port = 22;
		
		when(mockConfig.getSftpUsername()).thenReturn(userName);
		when(mockConfig.getSftpPassword()).thenReturn(password);
		when(mockConfig.getSftpHost()).thenReturn(host);
		when(mockConfig.getSftpPort()).thenReturn(port);
		
		when(mockJcraftFactory.openNewSession(userName, password, host, port)).thenReturn(mockSession);
		when(mockSession.openChannel(anyString())).thenReturn(mockChannel);
		manager = new SftpManagerImpl(mockConfig, mockJcraftFactory);
	}
	
	@Test
	public void testGetFileHappy() throws JSchException, SftpException{
		String path = "somePath";
		//call under test
		manager.getFile(path, mockOutStream);
		// the channel should be opened
		verify(mockSession).openChannel("sftp");
		verify(mockChannel).connect();
		verify(mockChannel).get(path, mockOutStream);
		verify(mockChannel).disconnect();
		verify(mockSession).disconnect();
	}
	
	@Test
	public void testGetFileError() throws JSchException, SftpException{
		// Setup a failure
		doThrow(new SftpException(22, "Something went wrong")).when(mockChannel).get(anyString(), any(OutputStream.class));
		String path = "somePath";
		//call under test
		try {
			manager.getFile(path, mockOutStream);
			fail("Should have failed");
		} catch (Exception e) {
			//expected
		}
		// both the channel and session must be disconnected even though there was an errors
		verify(mockChannel).disconnect();
		verify(mockSession).disconnect();
	}
	
	@Test
	public void testGetFileEarlyException() throws JSchException, SftpException{
		// Setup a failure
		when(mockJcraftFactory.openNewSession(userName, password, host, port)).thenThrow(new JSchException("Cannot connect"));
		String path = "somePath";
		//call under test
		try {
			manager.getFile(path, mockOutStream);
			fail("Should have failed");
		} catch (Exception e) {
			//expected
		}
		verify(mockSession, never()).openChannel("sftp");
		verify(mockChannel, never()).connect();
		verify(mockChannel, never()).get(path, mockOutStream);
	}

}
