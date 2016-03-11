package org.sagebionetworks.file.proxy.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.file.proxy.FileConnection;
import org.sagebionetworks.file.proxy.NotFoundException;
import org.sagebionetworks.file.proxy.RangeNotSatisfiable;
import org.sagebionetworks.file.proxy.sftp.ConnectionHandler;
import org.sagebionetworks.file.proxy.sftp.FileConnectionManager;
import org.sagebionetworks.url.UrlData;

import com.google.inject.Inject;

/**
 * This controller bridges HTTP requests to file requests.
 * 
 */
public class FileControllerImpl implements FileController {

	public static final int MIN_COMPRESSION_FILE_SIZE_BYES = 5000;

	public static final String TEXT_PREFIX = "text/";

	public static final long serialVersionUID = 1L;

	private static final Logger log = LogManager
			.getLogger(FileControllerImpl.class);

	public static final String HEADER_CONTENT_TYPE = "Content-Type";
	public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
	public static final String HEADER_CONTENT_LENGTH = "Content-Length";
	public static final String HEADER_CONTENT_MD5 = "Content-MD5";
	public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";
	public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
	public static final String HEADER_ACCEPT_RANGES = "Accept-Ranges";
	public static final String HEADER_RANGE = "Range";
	public static final String HEADER_CONTENT_RANGE = "Content-Range";
	public static final String HEADER_E_TAG = "ETag";
	public static final String HEADER_CONTENT_LOCATION = "Content-Location";
	public static final String HEADER_DATE = "Date";
	public static final String HEADER_LAST_MODIFIED = "Last-Modified";

	public static final String KEY_CONTENT_MD5 = "contentMD5";
	public static final String KEY_CONTENT_TYPE = "contentType";
	public static final String KEY_FILE_NAME = "fileName";
	public static final String KEY_CONTENT_SIZE = "contentSize";
	
	public static final String RANGE_NOT_SATISFIABLE_PREFIX = "bytes */";
	public static final String BYTES = "bytes";
	public static final String GZIP = "gzip";

	public static String CONTENT_DISPOSITION_PATTERN = "attachment; filename=\"%1$s\"";

	final FileConnectionManager connectionManager;
	final String pathPrefix;

	@Inject
	public FileControllerImpl(FileConnectionManager connectionManager, String pathPrefix) {
		this.connectionManager = connectionManager;
		this.pathPrefix = pathPrefix;
	}

	@Override
	public void doGet(final HttpServletRequest request,
			final HttpServletResponse response) throws ServletException, IOException {
		// Connect to file server.
		doConnection(request, response, new ConnectionHandler() {
			
			@Override
			public void execute(FileConnection connection) throws NotFoundException, IOException, RangeNotSatisfiable {
				// prepare the response based on what the caller is asking for.
				final RequestDescription desc = prepareResponse(request, response, connection, pathPrefix);
				// Write the file to the HTTP output stream
				OutputStream out = response.getOutputStream();
				if(desc.isGZIP()){
					// Send compressed results.
					out = new GZIPOutputStream(out);
					log.info("Using compression for: "+desc.getPath());
				}
				
				// The request can either be a full file request or a range request
				if(desc.getRange() != null){
					// partial content response
					// Note: Getting the file will close the connection, so the status must be set before the file is fetched.
					response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
					// range download
					connection.getFileRange(desc.getPath(), out, desc.getRange().getFirstBytePosition(), desc.getRange().getLastBytePosition());
				}else{
					// Note: Getting the file will close the connection, so the status must be set before the file is fetched.
					response.setStatus(HttpServletResponse.SC_OK);
					// Full file download.
					connection.getFile(desc.getPath(), out);
				}

			}
		});
	}

	@Override
	public void doHead(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		// make a connection and get the size
		doConnection(request, response, new ConnectionHandler() {
			
			@Override
			public void execute(FileConnection connection) throws Exception {
				// This will just setup the response without downloading the file.
				prepareResponse(request, response, connection, pathPrefix);
				// done
				response.setStatus(HttpServletResponse.SC_OK);
			}
		});
	}
	
	/**
	 * General file connection with error handling.
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
			HttpServletResponse response, FileConnection connection, String pathPrefix) throws MalformedURLException, NotFoundException, RangeNotSatisfiable{
		
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
			// When given an md5 use it as an etag.  Firefox requires an ETage for partial content (406)
			response.setHeader(HEADER_E_TAG, md5);
		}
		// Path excludes pathPrefix
		int index = urlData.getPath().indexOf(pathPrefix);
		if(index < 0){
			throw new IllegalArgumentException("Path does not contain: "+pathPrefix);
		}
	
		// The path of the file on the SFTP server.
		String path = urlData.getPath().substring(index+pathPrefix.length()-1);
		description.setPath(path);
		// Including Content-Location as it is recommended for partial content (406).
		response.setHeader(HEADER_CONTENT_LOCATION, path);
		
		// get the file size.
		long fileSize = connection.getFileSize(path);
		description.setFileSize(fileSize);
		
		// Include the last modified date for the file.
		long lastModifiedOn = connection.getLastModifiedDate(path);
		response.setHeader(HEADER_LAST_MODIFIED, getServerTime(lastModifiedOn));
		
		// Notify clients that byte serving is supported (https://en.wikipedia.org/wiki/Byte_serving)
		response.setHeader(HEADER_ACCEPT_RANGES, BYTES);
		
		// Date field should be returned for all responses and is required for partial content (406).
		response.setHeader(HEADER_DATE, getServerTime());
				
		// Is the client requesting compression?
		if(isGZIPRequest(request, contentType, fileSize)){
			// Will respond with GZIP result.
			response.setHeader(HEADER_CONTENT_ENCODING, GZIP);
			description.setIsGZIP(true);
		}
		
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
	static boolean isGZIPRequest(HttpServletRequest request, String contentType, long fileSize){
		String acceptEncoding = request.getHeader(HEADER_ACCEPT_ENCODING);
		if(acceptEncoding != null && contentType != null){
			if(contentType.contains(TEXT_PREFIX) && fileSize > MIN_COMPRESSION_FILE_SIZE_BYES){
				// text file over 5K bytes.
				return acceptEncoding.contains(GZIP);
			}
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
	
	/**
	 * Get the server time in HTTP-date format
	 * 
	 * @param time
	 * @return
	 */
	static String getServerTime(long time){
	    SimpleDateFormat dateFormat = new SimpleDateFormat(
	        "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
	    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	    return dateFormat.format(time);
	}
	
	/**
	 * Get the current server time in HTTP-date format
	 * (https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html) 14.18 Date.
	 * 
	 * @return
	 */
	static String getServerTime() {
		return getServerTime(System.currentTimeMillis());
	}
}
