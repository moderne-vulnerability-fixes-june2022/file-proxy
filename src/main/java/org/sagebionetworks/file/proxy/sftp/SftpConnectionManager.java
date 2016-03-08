package org.sagebionetworks.file.proxy.sftp;

import java.io.IOException;

import org.sagebionetworks.file.proxy.NotFoundException;
import org.sagebionetworks.file.proxy.config.Configuration;

import com.google.inject.Inject;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
/**
 * 
 * Manages a connection to SFTP server.
 *
 */
public class SftpConnectionManager implements FileConnectionManager {
	
	public static final String NO_SUCH_FILE = "No such file";

	private static final String TYPE_SFTP = "sftp";
	
	final Configuration config;
	final JcraftFactory jcraftFactory;
	
	@Inject
	public SftpConnectionManager(final Configuration config, JcraftFactory jcraftFactory){
		this.config = config;
		this.jcraftFactory = jcraftFactory;
	}

	/**
	 * Make an SFTP connection and provide it to the passed handler.
	 * 
	 * @param handler
	 * @throws NotFoundException
	 * @throws IOException 
	 */
	@Override
	public void connect(ConnectionHandler handler) throws NotFoundException {
		Session session = null;
		Channel channel = null;
		try {
			session = createNewConnection();
			channel = session.openChannel(TYPE_SFTP);
			channel.connect();
			ChannelSftp sftpChannel = (ChannelSftp) channel;
			// Create the connection and pass it to the handler
			handler.execute(new SftpConnectionImpl(sftpChannel));
		} catch (NotFoundException e) {
			// just re-throw
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}finally{
			if(channel != null){
				try {
					channel.disconnect();
				} catch (Exception e) {}
			}
			if(session != null){
				try {
					session.disconnect();
				} catch (Exception e) {}
			}
		}
	}
	
	/**
	 * Create a new connection.
	 * @return
	 * @throws JSchException
	 */
	private Session createNewConnection() throws JSchException{
		return jcraftFactory.openNewSession(config.getSftpUsername(), config.getSftpPassword(), config.getSftpHost(), config.getSftpPort());
	}

}
