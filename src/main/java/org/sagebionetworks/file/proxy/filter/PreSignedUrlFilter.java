package org.sagebionetworks.file.proxy.filter;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.file.proxy.config.Configuration;
import org.sagebionetworks.url.HttpMethod;
import org.sagebionetworks.url.SignatureExpiredException;
import org.sagebionetworks.url.SignatureMismatchException;
import org.sagebionetworks.url.UrlSignerUtils;

import com.google.inject.Inject;

/**
 * Filter used for all requests to validate pre-signed URLs.
 *
 */
public class PreSignedUrlFilter implements Filter{
	
	 private static final Logger log = LogManager.getLogger(PreSignedUrlFilter.class);
	
	private static final String TEXT_PLAIN = "text/plain";
	final Configuration config;
	
	@Inject
	public PreSignedUrlFilter(final Configuration config){
		this.config = config;
	}


	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		// validate the call
		HttpMethod method = HttpMethod.valueOf(httpRequest.getMethod());
		StringBuffer urlBuffer = httpRequest.getRequestURL();
		if(httpRequest.getQueryString() != null){
			urlBuffer.append("?");
			urlBuffer.append(httpRequest.getQueryString());
		}
		String url = urlBuffer.toString();
		try {
			// This method will throw exceptions if the signature is not valid.
			UrlSignerUtils.validatePresignedURL(method, url, config.getUrlSignerSecretKey());
			// signature is valid so proceed.
			chain.doFilter(httpRequest, httpResponse);
		} catch (SignatureMismatchException e) {
			// Signature is not valid
			prepareErrorResponse(HttpServletResponse.SC_UNAUTHORIZED, url, httpResponse, e);
		} catch (SignatureExpiredException e) {
			// Signature is not valid
			prepareErrorResponse(HttpServletResponse.SC_UNAUTHORIZED, url, httpResponse, e);
		}
	}
	
	/**
	 * Helper to write an error response.
	 * @param statusCode
	 * @param httpResponse
	 * @param e
	 * @throws IOException
	 */
	private static void prepareErrorResponse(int statusCode, String url, HttpServletResponse httpResponse, Exception e) throws IOException{
		httpResponse.setStatus(statusCode);
		httpResponse.setContentType(TEXT_PLAIN);
		PrintWriter writer = httpResponse.getWriter();
		writer.write(e.getMessage());
		writer.flush();
		if(log.isInfoEnabled()){
			log.info("Status code: "+statusCode+" message: "+e.getMessage()+" url: "+url);
		}
	}

	@Override
	public void destroy() {
	}

}
