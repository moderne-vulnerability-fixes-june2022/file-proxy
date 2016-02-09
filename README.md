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
The example below shows how to start the proxy confiugred with these values.

## Example
In the following example, the goal is to setup a new ProxyFileHandle to represent a file that resides on a SFTP server.  After everything is setup correctly, users should be able to download the file from any Synapse client in the same way as any other file in Synapse.

#### SFTP Server
The SFTP server for this example has the following relevant data:
host = ec2-123.us-west-2.compute.amazonaws.com
port = 22
The proxy will use the following service credentials to connect to the SFTP server:
username = <service_username>
password = <service_password>

The file to proxy resides on the SFTP server with the following path:
filePath = /public/downloads/Before.png

#### Proxy Server
In this example proxy server will be launched locally.  The proxy server must be configured with the both the SFTP parameters and the secret key used to sign URLs generated from Synapse.
secretKey =  ed29688c-cae2-11e5-9956-625662870761
```
git clone https://github.com/Sage-Bionetworks/file-proxy.git
cd file-proxy
```
The proxy server can then be started by passing all of the relevant configuration information as system properties (-Dkey=value) as follows
```
mvn tomcat:run -D"org.sagebionetworks.url.signer.secret.key=ed29688c-cae2-11e5-9956-625662870761" -Dorg.sagebionetworks.sftp.username=<service_username> -Dorg.sagebionetworks.sftp.password=<service_password> -Dorg.sagebionetworks.sftp.host=ec2-123.us-west-2.compute.amazonaws.com -Dorg.sagebionetworks.sftp.port=22
```
console output:
```
[INFO] --- tomcat-maven-plugin:1.1:run (default-cli) @ file-proxy ---
[INFO] Running war on http://localhost:8080/file-proxy
[INFO] Using existing Tomcat server configuration at C:\cygwin64\home\jhill\git\file-proxy\target\tomcat
Feb 08, 2016 4:16:18 PM org.apache.catalina.startup.Embedded start
INFO: Starting tomcat server
Feb 08, 2016 4:16:19 PM org.apache.catalina.core.StandardEngine start
INFO: Starting Servlet Engine: Apache Tomcat/6.0.29
Feb 08, 2016 4:16:19 PM org.apache.coyote.http11.Http11Protocol init
INFO: Initializing Coyote HTTP/1.1 on http-8080
Feb 08, 2016 4:16:19 PM org.apache.coyote.http11.Http11Protocol start
INFO: Starting Coyote HTTP/1.1 on http-8080
```
The console shows the proxy server URL as http://localhost:8080/file-proxy

#### ProxyStorageLocationSettings
Before we can start creating ProxyFileHandles for the above setups we must first register a new StorageLocation with Synapse.  The ProxyStorageLocationSettings contains all of
the parameters Synapse will need to generated ProxyFileHandles and to issue pre-signed URL for ProxyFileHandles.
The following curl call was used to create the ProxyStorageLocationSettings:
```
curl -k -H "sessionToken:<session_token>" -H "Content-Type: application/json" -X POST -d '{"uploadType":"SFTP","secretKey":"ed29688c-cae2-11e5-9956-625662870761", "proxyUrl":"https://localhost:8080/file-proxy", "concreteType":"org.sagebionetworks.repo.model.project.ProxyStorageLocationSettings"}' https://repo-prod.prod.sagebase.org/repo/v1/storageLocation
```
response:
```
{"createdOn":"2016-02-09T00:42:06.942Z","uploadType":"SFTP","concreteType":"org.sagebionetworks.repo.model.project.ProxyStorageLocationSettings","secretKey":"ed29688c-cae2-11e5-9956-625662870761","etag":"251fbeeb-3004-47dc-86af-56a11d2a8b3f","createdBy":273991,"proxyUrl":"https://localhost:8080/file-proxy","storageLocationId":4170}
```

#### ProxyFileHandle
Once the SFTP and proxy servers are started and the ProxyStorageLocationSettings is created in Synapse, we can start creating ProxyFileHandles for each file.
```
curl -k -H "sessionToken:<session_token>" -H "Content-Type: application/json" -X POST -d '{"storageLocationId":"4170", "filePath":"/public/downloads/Before.png", "fileName":"Before.png", "contentType":"image/png", "contentMd5":"md5", "contentSize":"29486", "concreteType":"org.sagebionetworks.repo.model.file.ProxyFileHandle"}' https://repo-prod.prod.sagebase.org/file/v1/externalFileHandle/proxy
```
Response:
```
{"id":"7512151","createdOn":"2016-02-09T00:46:32.000Z","concreteType":"org.sagebionetworks.repo.model.file.ProxyFileHandle","etag":"8421db63-5167-438c-a7df-15534d9290de","createdBy":"273991","contentSize":29486,"filePath":"/public/downloads/Before.png","fileName":"Before.png","contentType":"image/png","contentMd5":"md5","storageLocationId":4170}
```

#### File download
The final step is to test downloading the file through the proxy by fetching a pre-signed URL for the ProxyFileHandle from Synapse:
```
curl -k -H "sessionToken:<session_token>" "https://repo-prod.prod.sagebase.org/file/v1/fileHandle/7512151/url?redirect=false"
```
Response:
```
https://localhost:8080/file-proxy/sftp/public/downloads/Before.png?fileName=Before.png&contentType=image%2Fpng&contentMD5=md5&contentSize=29486&expiration=1454979041142&hmacSignature=f1289a824901a6fc1af3286cb404fb94adb667b6
```

