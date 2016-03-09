package org.sagebionetworks.file.proxy.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class HttpToLocalServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	FileController controller;

	@Inject
	public HttpToLocalServlet(FileController controller) {
		this.controller = controller;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		controller.doGet(request, response);
	}

	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		controller.doHead(request, response);
	}


}
