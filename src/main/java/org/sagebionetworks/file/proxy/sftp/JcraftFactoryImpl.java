package org.sagebionetworks.file.proxy.sftp;


import com.google.inject.Singleton;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Actual implementation of the JcraftFactory
 *
 */
@Singleton
public class JcraftFactoryImpl implements JcraftFactory {

	@Override
	public Session openNewSession(String userName, String password,
			String host, int port) throws JSchException {
		JSch jsch = new JSch();
		Session session = jsch.getSession(userName, host, port);
		session.setPassword(password);
		session.setConfig("StrictHostKeyChecking", "no");
		session.connect();
		return session;
	}

}
