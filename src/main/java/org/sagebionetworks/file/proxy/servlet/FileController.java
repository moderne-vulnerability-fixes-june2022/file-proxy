package org.sagebionetworks.file.proxy.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Abstraction for a controller that bridges between HTTP call and manager calls.
 *
 */
public interface FileController {

	void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException;

	void doHead(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException;

}
