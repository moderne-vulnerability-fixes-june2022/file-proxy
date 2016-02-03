package org.sagebionetworks.file.proxy.config;

public interface Configuration {
	
	/**
	 * The secret key used to sign all pre-signed URLs.
	 * 
	 * @return
	 */
	public String getUrlSignerSecretKey();

	/**
	 * The SFTP service username.
	 * @return
	 */
	public String getSftpUsername();

	/**
	 * The SFTP service password.
	 * @return
	 */
	public String getSftpPassword();

	/**
	 * The SFTP server host.
	 * @return
	 */
	public String getSftpHost();

	/**
	 * The SFTP server port.
	 * @return
	 */
	public int getSftpPort();

}
