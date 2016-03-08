package org.sagebionetworks.file.proxy.module;

import org.sagebionetworks.file.proxy.LocalConnectionManager;
import org.sagebionetworks.file.proxy.filter.CorsFilter;
import org.sagebionetworks.file.proxy.filter.HealthCheckFilter;
import org.sagebionetworks.file.proxy.filter.PreSignedUrlFilter;
import org.sagebionetworks.file.proxy.servlet.FileControllerImpl;
import org.sagebionetworks.file.proxy.servlet.HttpToLocalServlet;
import org.sagebionetworks.file.proxy.servlet.HttpToSftpServlet;
import org.sagebionetworks.file.proxy.sftp.SftpConnectionManager;

import com.google.inject.Provides;
import com.google.inject.servlet.ServletModule;

public class ApplicationServletModule extends ServletModule {
	
	public static final String SFTP_PATH_PREFIX = "/sftp/";
	public static final String LOCAL_PATH_PREFIX = "/proxylocal/";


	@Override
	protected void configureServlets() {
		super.configureServlets();
		filter("/health-check").through(HealthCheckFilter.class);
		// Need a CORS filter to enable javascript to callers
		filter("/*").through(CorsFilter.class);
		// All calls must go through the pre-signed URL Filter.
		filter("/*").through(PreSignedUrlFilter.class);
		// HTTP to SFTP calls.
		serve(SFTP_PATH_PREFIX+"*").with(HttpToSftpServlet.class);
		// HTTP to local calls
		serve(LOCAL_PATH_PREFIX+"*").with(HttpToLocalServlet.class);
	}
	
	/**
	 * The HTTP to SFTP servlet depends on sftp connection manager.
	 * @param manager
	 * @return
	 */
	@Provides
	HttpToSftpServlet provideHttpToSftpServlet(SftpConnectionManager manager){
		return new HttpToSftpServlet(new FileControllerImpl(manager, SFTP_PATH_PREFIX));
	}
	
	/**
	 * HTTP to local servlet depends on local connection manager.
	 * @param manager
	 * @return
	 */
	@Provides
	HttpToLocalServlet provideHttpToLocalServlet(LocalConnectionManager manager){
		return new HttpToLocalServlet(new FileControllerImpl(manager, LOCAL_PATH_PREFIX));
	}

}
