package org.sagebionetworks.file.proxy.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Singleton;

/**
 * Filter to allow Cross-Origin Resource Sharing (CORS).
 * This type of filter is required to allow direct javascript calls.
 * 
 * See: http://enable-cors.org/index.html
 * 
 *
 */
@Singleton
public class CorsFilter implements Filter {
	
	public static final String MAX_AGE_SECONDS = "300";
	public static final String WILD_CARD = "*";
	public static final String OPTIONS = "OPTIONS";
	public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
	public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
	public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		// Add this header to the response of every call.
		response.addHeader(ACCESS_CONTROL_ALLOW_ORIGIN, WILD_CARD);
		// Is this a pre-flight request?
		if(isPreFlightRequest(request)){
			// header indicates how long the results of a pre-flight request can be cached in seconds
			response.addHeader(ACCESS_CONTROL_MAX_AGE, MAX_AGE_SECONDS);
		} else {
			// pass along all non-pre-flight requests.
			chain.doFilter(request, response);
		}
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}
	
	/**
	 * Is this a pre-flight request.
	 * @param request
	 * @return
	 */
	public static boolean isPreFlightRequest(HttpServletRequest request){
		return request.getHeader(ACCESS_CONTROL_REQUEST_METHOD) != null && 
				request.getMethod() != null &&
				request.getMethod().equals(OPTIONS);
	}

}
