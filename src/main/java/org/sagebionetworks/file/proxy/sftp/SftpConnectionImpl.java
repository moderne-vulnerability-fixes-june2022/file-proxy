package org.sagebionetworks.file.proxy.sftp;

import java.io.OutputStream;

import org.sagebionetworks.file.proxy.NotFoundException;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpProgressMonitor;

public class SftpConnectionImpl implements SftpConnection {

	public static final String NO_SUCH_FILE = "No such file";

	ChannelSftp sftpChannel;

	/**
	 * Create a new connection with an established Channel.
	 * 
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
	 * 
	 * @param path
	 * @param e
	 * @throws NotFoundException
	 */
	static void handleNotFound(String path, Exception e)
			throws NotFoundException {
		if (e.getMessage().contains(NO_SUCH_FILE)) {
			throw new NotFoundException(path);
		}
	}

	@Override
	public boolean getFileRange(final String path, final OutputStream stream,
			final long startByteIndex, final long endByteIndex) throws NotFoundException {

		try {
			CountingMonitor monitor = new CountingMonitor(endByteIndex);
			sftpChannel.get(path, stream, monitor, ChannelSftp.RESUME, startByteIndex);
			return monitor.wasEndReached();
		} catch (Exception e) {
			handleNotFound(path, e);
			// convert to a runtime
			throw new RuntimeException(e);
		}
	}

	/**
	 * A simple monitor that tracks the current read index. When the current
	 * index exceeds the maximum index, the {@link #count(long)} will return
	 * false signaling the end of the current read.
	 * 
	 */
	private static class CountingMonitor implements SftpProgressMonitor {

		long currentIndex;
		long maxIndex;
		long fileSize;

		public CountingMonitor(long maxIndex) {
			super();
			this.maxIndex = maxIndex;
			this.currentIndex = 0;
		}

		@Override
		public boolean count(long count) {
			this.currentIndex += count;
			// Return false when the limit has been read.
			return currentIndex < this.maxIndex;
		}

		/**
		 * Did the current index reach the end of the file?
		 * 
		 * @return
		 */
		public boolean wasEndReached() {
			return this.currentIndex >= this.fileSize;
		}

		@Override
		public void init(int op, String src, String dest, long max) {
			// capture the file size
			this.fileSize = max;
		}

		@Override
		public void end() {
		}

	}

}
