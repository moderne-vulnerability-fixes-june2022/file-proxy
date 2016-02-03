# file-proxy
Proxy for file upload and download.

## Configuration
The following properties must be provided to the application to run:

| Property Name | Description |
| --- | --- |
| org.sagebionetworks.url.signer.secret.key | The credentials used to sign pre-signed URLs.  The credentials should match the value of the 'ProxyStorageLocationSettings.secretKey' set in Synapse. |
| org.sagebionetworks.sftp.username | The SFTP service username. |
| org.sagebionetworks.sftp.password | The SFTP service password. |
| org.sagebionetworks.sftp.host | The SFTP server host. |
| org.sagebionetworks.sftp.port | The SFTP server port. |
