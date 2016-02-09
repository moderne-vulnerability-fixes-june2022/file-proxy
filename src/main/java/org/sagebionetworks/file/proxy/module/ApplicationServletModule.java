package org.sagebionetworks.file.proxy.module;

import org.sagebionetworks.file.proxy.filter.CorsFilter;
import org.sagebionetworks.file.proxy.filter.HealthCheckFilter;
import org.sagebionetworks.file.proxy.filter.PreSignedUrlFilter;
import org.sagebionetworks.file.proxy.servlet.HttpToSftpServlet;

import com.google.inject.servlet.ServletModule;

public class ApplicationServletModule extends ServletModule {


	@Override
	protected void configureServlets() {
		super.configureServlets();
		filter("/health-check").through(HealthCheckFilter.class);
		// Need a CORS filter to enable javascript to callers
		filter("/*").through(CorsFilter.class);
		// All calls must go through the pre-signed URL Filter.
		filter("/*").through(PreSignedUrlFilter.class);
		// HTTP to SFTP calls.
		serve("/sftp/*").with(HttpToSftpServlet.class);
	}

}
