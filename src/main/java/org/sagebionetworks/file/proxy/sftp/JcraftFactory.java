package org.sagebionetworks.file.proxy.sftp;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Abstraction for creating Jcraft Objects
 *
 */
public interface JcraftFactory {
	
	/**
	 * Open a new SFTP session
	 * @param userName
	 * @param password
	 * @param host
	 * @param port
	 * @return
	 * @throws JSchException 
	 */
	Session openNewSession(String userName, String password, String host, int port) throws JSchException;

}
