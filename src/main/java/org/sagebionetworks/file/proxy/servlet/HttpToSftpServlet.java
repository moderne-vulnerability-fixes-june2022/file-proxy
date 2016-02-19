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
import org.sagebionetworks.file.proxy.RangeNotSatisfiable;
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
	public static final String HEADER_ACCEPT_RANGES = "Accept-Ranges";
	public static final String HEADER_RANGE = "Range";
	public static final String HEADER_CONTENT_RANGE = "Content-Range";

	public static final String KEY_CONTENT_MD5 = "contentMD5";
	public static final String KEY_CONTENT_TYPE = "contentType";
	public static final String KEY_FILE_NAME = "fileName";
	public static final String KEY_CONTENT_SIZE = "contentSize";
	
	public static final String RANGE_NOT_SATISFIABLE_PREFIX = "bytes */";
	public static final String BYTES = "bytes";
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
			public void execute(SftpConnection connection) throws NotFoundException, IOException, RangeNotSatisfiable {
				// prepare the response based on what the caller is asking for.
				final RequestDescription desc = prepareResponse(request, response, connection);
				// Write the file to the HTTP output stream
				OutputStream out = response.getOutputStream();
				if(desc.isUseGZIP()){
					// Send compressed results.
					out = new GZIPOutputStream(out);
					log.info("Using compression for: "+desc.getPath());
				}
				
				// The request can either be a full file request or a range request
				if(desc.getRange() != null){
					// range download
					connection.getFileRange(desc.getPath(), out, desc.getRange().getFirstBytePosition(), desc.getRange().getLastBytePosition());
					// partial content response
					response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
				}else{
					// Full file download.
					connection.getFile(desc.getPath(), out);
					// done
					response.setStatus(HttpServletResponse.SC_OK);
				}

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
		} catch (RangeNotSatisfiable e) {
			log.error("RangeNotSatisfiable: "+e.getMessage());
			// Add a header that tells the caller the types or range requests allowed
			response.setHeader(HEADER_CONTENT_RANGE, RANGE_NOT_SATISFIABLE_PREFIX+e.getFileSize());
			response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
					e.getMessage());
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
	 * @throws RangeNotSatisfiable 
	 */
	static RequestDescription prepareResponse(HttpServletRequest request,
			HttpServletResponse response, SftpConnection connection) throws MalformedURLException, NotFoundException, RangeNotSatisfiable{
		
		RequestDescription description = new RequestDescription();
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
				String md5Base64 = Base64.encodeBase64String(Hex.decodeHex(md5.toCharArray()));
				response.setHeader(HEADER_CONTENT_MD5, md5Base64);
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
			description.setUseGZIP(true);
		}
		// Notify clients that byte serving is supported (https://en.wikipedia.org/wiki/Byte_serving)
		response.setHeader(HEADER_ACCEPT_RANGES, BYTES);
		
		// The path of the file on the SFTP server.
		String path = urlData.getPath().substring(index+PATH_PREFIX.length()-1);
		description.setPath(path);
		// get the file size.
		long fileSize = connection.getFileSize(path);
		description.setFileSize(fileSize);
		// Setup content size and content range.
		prepareContentHeaders(request, response, description);
	
		return description;
	}

	/**
	 * Sets two headers "Content-Length" and "Content-Range".
	 * 
	 * @param request
	 * @param response
	 * @param fileSize
	 * @throws RangeNotSatisfiable
	 */
	static void prepareContentHeaders(HttpServletRequest request,
			HttpServletResponse response, RequestDescription description)
			throws RangeNotSatisfiable {
		long fileSize = description.getFileSize();
		// Start with a content length equal to the files size.
		long contentLength = fileSize;
		// Is this a range request?
		RangeValue range = isRangeRequest(request, fileSize);
		if(range != null){
			long firstBytePosition = range.getFirstBytePosition();
			long lastBytePosition = fileSize-1;
			// if given a last byte position then use it.
			if(range.getLastBytePosition() != null){
				lastBytePosition = Math.min(range.getLastBytePosition(), fileSize-1);
			}
			// when given a valid range the content length is delta of the range.
			long delta = lastBytePosition-firstBytePosition+1;
			contentLength = Math.min(fileSize, delta);
			// Can now prepare the Content-Range header
			StringBuilder contentRange = new StringBuilder();
			contentRange.append(RangeUnits.bytes.name());
			contentRange.append(" ");
			contentRange.append(firstBytePosition);
			contentRange.append("-");
			contentRange.append(lastBytePosition);
			contentRange.append("/");
			contentRange.append(fileSize);
			response.setHeader(HEADER_CONTENT_RANGE, contentRange.toString());
			// pass along the 
			description.setRange(new ByteRange(firstBytePosition, lastBytePosition));
		}
		// Always set a content length.
		response.setHeader(HEADER_CONTENT_LENGTH, Long.toString(contentLength));
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
	
	/**
	 * Is this a range request?
	 * @param request
	 * @return returns null if this is not a range request, else the range.
	 * @throws RangeNotSatisfiable 
	 */
	static RangeValue isRangeRequest(HttpServletRequest request, long fileSize) throws RangeNotSatisfiable{
		String rangeValue = request.getHeader(HEADER_RANGE);
		if(rangeValue != null){
			try {
				return new RangeValue(rangeValue);
			} catch (IllegalArgumentException e) {
				// If we cannot read the range send a RangeNotSatisfiable error.
				throw new RangeNotSatisfiable(rangeValue, fileSize);
			}
		}
		return null;
	}
	
}
