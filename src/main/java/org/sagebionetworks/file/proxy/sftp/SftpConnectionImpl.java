package org.sagebionetworks.file.proxy.sftp;

import java.io.OutputStream;

import org.sagebionetworks.file.proxy.NotFoundException;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;

public class SftpConnectionImpl implements SftpConnection {

	public static final String NO_SUCH_FILE = "No such file";
	
	ChannelSftp sftpChannel;
	
	/**
	 * Create a new connection with an established Channel.
	 * @param sftpChannel
	 */
	public SftpConnectionImpl(ChannelSftp sftpChannel) {
		this.sftpChannel = sftpChannel;
	}

	@Override
	public void getFile(String path, OutputStream stream)
			throws NotFoundException {
		
		try {
			// write the file the the passed stream.
			sftpChannel.get(path, stream);
		} catch (Exception e) {
			handleNotFound(path, e);
			// convert to a runtime
			throw new RuntimeException(e);
		}
	}

	@Override
	public long getFileSize(String path) throws NotFoundException {
		try {
			// Get the attributes of this file.
			SftpATTRS atts = this.sftpChannel.lstat(path);
			return atts.getSize();
		} catch (Exception e) {
			handleNotFound(path, e);
			// convert to a runtime
			throw new RuntimeException(e);
		}
	}

	/**
	 * Throws NotFoundException when the message contains 'No Such File'
	 * @param path
	 * @param e
	 * @throws NotFoundException
	 */
	static void handleNotFound(String path, Exception e)
			throws NotFoundException {
		if(e.getMessage().contains(NO_SUCH_FILE)){
			throw new NotFoundException(path);
		}
	}

}
