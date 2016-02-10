package org.sagebionetworks.file.proxy.sftp;

import java.io.OutputStream;

import org.sagebionetworks.file.proxy.NotFoundException;
import org.sagebionetworks.file.proxy.config.Configuration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

@Singleton
public class SftpManagerImpl implements SftpManager {
	
	public static final String NO_SUCH_FILE = "No such file";

	private static final String TYPE_SFTP = "sftp";
	
	final Configuration config;
	final JcraftFactory jcraftFactory;
	
	@Inject
	public SftpManagerImpl(final Configuration config, JcraftFactory jcraftFactory){
		this.config = config;
		this.jcraftFactory = jcraftFactory;
	}

	@Override
	public void getFile(String path, OutputStream stream) throws NotFoundException {
		// create a connection
		Session session = null;
		Channel channel = null;
		try {
			session = createNewConnection();
			channel = session.openChannel(TYPE_SFTP);
			channel.connect();
			ChannelSftp sftpChannel = (ChannelSftp) channel;
			sftpChannel.get(path, stream);
		} catch (Exception e) {
			if(e.getMessage().contains(NO_SUCH_FILE)){
				throw new NotFoundException(path);
			}
			// convert to a runtime
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
