package org.sagebionetworks.file.proxy.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Abstraction for a controller that bridges between HTTP call and file
 * connections.
 * 
 */
public interface FileController {

	/**
	 * See: {@link HttpServlet#doGet(HttpServletRequest, HttpServletResponse)}
	 * 
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException;

	/**
	 * See: {@link HttpServlet#doHead(HttpServletRequest, HttpServletResponse)}
	 * 
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	void doHead(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException;

}
