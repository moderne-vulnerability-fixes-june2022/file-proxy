package org.sagebionetworks.file.proxy.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.file.proxy.sftp.SftpManager;
import org.sagebionetworks.url.UrlData;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * This servlet bridges HTTP requests to SFTP requests.
 * 
 */
@Singleton
public class HttpToSftpServlet extends HttpServlet {

	private static final Logger log = LogManager
			.getLogger(HttpToSftpServlet.class);

	public static final String HEADER_CONTENT_TYPE = "Content-Type";

	public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";

	public static final String HEADER_CONTENT_LENGTH = "Content-Length";

	public static final String KEY_CONTENT_TYPE = "contentType";

	public static final String KEY_FILE_NAME = "fileName";

	public static final String KEY_CONTENT_SIZE = "contentSize";

	public static final long serialVersionUID = 1L;

	public static final String PATH_PREFIX = "/sftp/";

	public static String CONTENT_DISPOSITION_PATTERN = "attachment; filename=\"%1$s\"";

	final SftpManager sftpManager;

	@Inject
	public HttpToSftpServlet(SftpManager sftpManager) {
		this.sftpManager = sftpManager;
	}

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		try {
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
			String contentSize = queryParameters.get(KEY_CONTENT_SIZE);
			String fileName = queryParameters.get(KEY_FILE_NAME);
			String contentType = queryParameters.get(KEY_CONTENT_TYPE);

			// Setup the headers as needed
			if (contentSize != null) {
				response.setHeader(HEADER_CONTENT_LENGTH, contentSize);
			}
			if (fileName != null) {
				response.setHeader(HEADER_CONTENT_DISPOSITION,
						String.format(CONTENT_DISPOSITION_PATTERN, fileName));
			}
			if (contentType != null) {
				response.setHeader(HEADER_CONTENT_TYPE, contentType);
			}
			// Write the entire file to the stream
			OutputStream stream = response.getOutputStream();
			// Path excludes /sftp/
			int index = urlData.getPath().indexOf(PATH_PREFIX);
			if(index < 0){
				throw new IllegalArgumentException("Path does not contain: "+PATH_PREFIX);
			}
			String path = urlData.getPath().substring(index+PATH_PREFIX.length());
			// the manger writes to the stream
			sftpManager.getFile(path, stream);
			response.setStatus(HttpServletResponse.SC_OK);
		} catch (Exception e) {
			log.error("Request failed", e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					e.getMessage());
		}
	}

}
