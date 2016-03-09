package org.sagebionetworks.file.proxy;

import org.sagebionetworks.file.proxy.module.ApplicationModule;
import org.sagebionetworks.file.proxy.module.ApplicationServletModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class TestContext {

	/**
	 * Create a Guice Injector that can be used for testing.
	 * @return
	 */
	public static Injector createTestConext() {
		// injection of all modules.
		return Guice.createInjector(new ApplicationServletModule(), new ApplicationModule());
	}
}
