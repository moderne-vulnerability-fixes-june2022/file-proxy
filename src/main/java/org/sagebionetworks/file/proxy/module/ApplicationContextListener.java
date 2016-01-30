package org.sagebionetworks.file.proxy.module;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.*;

/**
 * Main entry-point for the application.
 *
 */
public class ApplicationContextListener extends GuiceServletContextListener {

	@Override
	protected Injector getInjector() {
		// injection of all modules.
		return Guice.createInjector(new ApplicationServletModule());
	}

}
