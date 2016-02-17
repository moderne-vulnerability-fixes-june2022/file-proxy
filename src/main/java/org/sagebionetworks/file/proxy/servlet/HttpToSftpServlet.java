package org.sagebionetworks.file.proxy.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.LinkedHashMap;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.file.proxy.NotFoundException;
import org.sagebionetworks.file.proxy.sftp.ConnectionHandler;
import org.sagebionetworks.file.proxy.sftp.SftpConnection;
import org.sagebionetworks.file.proxy.sftp.SftpConnectionManager;
import org.sagebionetworks.url.UrlData;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * This servlet bridges HTTP requests to SFTP requests.
 * 
 */
@Singleton
public class HttpToSftpServlet extends HttpServlet {

	public static final long serialVersionUID = 1L;

	private static final Logger log = LogManager
			.getLogger(HttpToSftpServlet.class);

	public static final String HEADER_CONTENT_TYPE = "Content-Type";
	public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
	public static final String HEADER_CONTENT_LENGTH = "Content-Length";
	public static final String HEADER_CONTENT_MD5 = "Content-MD5";
	public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";
	public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";

	public static final String KEY_CONTENT_MD5 = "contentMD5";
	public static final String KEY_CONTENT_TYPE = "contentType";
	public static final String KEY_FILE_NAME = "fileName";
	public static final String KEY_CONTENT_SIZE = "contentSize";
	
	public static final String GZIP = "gzip";

	public static final String PATH_PREFIX = "/sftp/";

	public static String CONTENT_DISPOSITION_PATTERN = "attachment; filename=\"%1$s\"";

	final SftpConnectionManager connectionManager;

	@Inject
	public HttpToSftpServlet(SftpConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	@Override
	protected void doGet(final HttpServletRequest request,
			final HttpServletResponse response) throws ServletException, IOException {
		// Connect to SFTP server.
		doConnection(request, response, new ConnectionHandler() {
			
			@Override
			public void execute(SftpConnection connection) throws NotFoundException, IOException {
				// prepare the response based on what the caller is asking for.
				final String path = prepareResponse(request, response, connection);
				// Write the file to the HTTP output stream
				OutputStream out = response.getOutputStream();
				if(isGZIPRequest(request)){
					// Send compressed results.
					out = new GZIPOutputStream(out);
					log.info("Using compression for: "+path);
				}
				// proxy the file...
				connection.getFile(path, out);
				// done
				response.setStatus(HttpServletResponse.SC_OK);
			}
		});
	}

	@Override
	protected void doHead(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		// make a connection and get the size
		doConnection(request, response, new ConnectionHandler() {
			
			@Override
			public void execute(SftpConnection connection) throws Exception {
				// This will just setup the response without downloading the file.
				prepareResponse(request, response, connection);
				// done
				response.setStatus(HttpServletResponse.SC_OK);
			}
		});
	}
	
	/**
	 * General SFTP connection with error handling.
	 * @param request
	 * @param response
	 * @param handler
	 * @throws IOException
	 */
	void doConnection(final HttpServletRequest request, final HttpServletResponse response, ConnectionHandler handler) throws ServletException, IOException{
		try{
			connectionManager.connect(handler);
		} catch (NotFoundException e) {
			log.error("Not Found: "+e.getMessage());
			response.sendError(HttpServletResponse.SC_NOT_FOUND,
					e.getMessage());
		} catch (Exception e) {
			log.error("Request failed", e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					e.getMessage());
		}
	}
	
	/**
	 * The basic response is prepared the same for both GET and HEAD calls.
	 * 
	 * @param request
	 * @param response
	 * @return The path of the file.
	 * @throws MalformedURLException
	 * @throws NotFoundException 
	 */
	static String prepareResponse(HttpServletRequest request,
			HttpServletResponse response, SftpConnection connection) throws MalformedURLException, NotFoundException{
		// Read the URL from the request
		StringBuffer urlBuffer = request.getRequestURL();
		if (request.getQueryString() != null) {
			urlBuffer.append("?");
			urlBuffer.append(request.getQueryString());
		}
		// parse the URL
		UrlData urlData = new UrlData(urlBuffer.toString());
		LinkedHashMap<String, String> queryParameters = urlData
				.getQueryParameters();
		String fileName = queryParameters.get(KEY_FILE_NAME);
		String contentType = queryParameters.get(KEY_CONTENT_TYPE);
		String md5 = queryParameters.get(KEY_CONTENT_MD5);

		// Setup the headers as needed
		if (fileName != null) {
			response.setHeader(HEADER_CONTENT_DISPOSITION,
					String.format(CONTENT_DISPOSITION_PATTERN, fileName));
		}
		if (contentType != null) {
			response.setHeader(HEADER_CONTENT_TYPE, contentType);
		}
		if(md5 != null){
			// The MD5 provide is in Hex and needs to be converted to base64
			// https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
			try {
				String md5Base65 = Base64.encodeBase64String(Hex.decodeHex(md5.toCharArray()));
				response.setHeader(HEADER_CONTENT_MD5, md5Base65);
			} catch (DecoderException e) {
				log.info("Failed to convert MD5 hex :"+md5+" message: "+e.getMessage());
			}

		}
		// Path excludes /sftp/
		int index = urlData.getPath().indexOf(PATH_PREFIX);
		if(index < 0){
			throw new IllegalArgumentException("Path does not contain: "+PATH_PREFIX);
		}
		// Is the client requesting compression?
		if(isGZIPRequest(request)){
			// Will respond with GZIP result.
			response.setHeader(HEADER_CONTENT_ENCODING, GZIP);
		}
		// The path of the file on the SFTP server.
		String path = urlData.getPath().substring(index+PATH_PREFIX.length()-1);
		// get the file size.
		long contentLength = connection.getFileSize(path);
		// add the size header
		response.setHeader(HEADER_CONTENT_LENGTH, Long.toString(contentLength));
		return path;
	}
	
	/**
	 * Is this a GZIP request?
	 * @param request
	 * @return
	 */
	static boolean isGZIPRequest(HttpServletRequest request){
		String acceptEncoding = request.getHeader(HEADER_ACCEPT_ENCODING);
		if(acceptEncoding != null){
			return acceptEncoding.contains(GZIP);
		}
		return false;
	}
	
}
