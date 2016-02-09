package org.sagebionetworks.file.proxy.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Singleton;

@Singleton
public class HealthCheckFilter implements Filter{

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		// A simple health check that just returns OK.
		HttpServletResponse httpResonse = (HttpServletResponse) response;
		httpResonse.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	public void destroy() {	
	}


}
