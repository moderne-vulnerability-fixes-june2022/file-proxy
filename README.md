# file-proxy
Proxy for file upload and download.

## Configuration
The following properties must be provided to the application
|Property Name| Description |
| --- | --- |
|org.sagebionetworks.url.signer.secret.key| The credentials used to sign pre-signed URLs.  The credentials should match the value of the 'ProxyStorageLocationSettings.secretKey' set in Synapse.|