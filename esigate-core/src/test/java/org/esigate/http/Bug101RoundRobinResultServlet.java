package org.esigate.http;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bug101RoundRobinResultServlet extends HttpServlet {
	private final static Logger LOG = LoggerFactory
			.getLogger(Bug101RoundRobinResultServlet.class);

	static private int pos = 0;

	protected synchronized int getAndCycle() {

		int result = pos;

		pos++;
		if (pos > 5)
			pos = 0;

		return result;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		int result = getAndCycle();

		switch (result) {
		case 0:

			render200(req, resp);
			break;

		case 1:

			render404(req, resp);
			break;

		case 2:

			render500(req, resp);
			break;

		case 3:

			render200(req, resp);
			break;

		case 4:

			render200(req, resp);
			break;

		}
	}

	protected void render200(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setStatus(200);
		OutputStream out = resp.getOutputStream();

		out.write("OK".getBytes());
		out.close();

		LOG.info("Handle with 200");
	}

	protected void render404(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setStatus(404);

		OutputStream out = resp.getOutputStream();

		out.write("NOT FOUND".getBytes());
		out.close();
		LOG.info("Handle with 404");

	}

	protected void render500(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setStatus(500);

		OutputStream out = resp.getOutputStream();

		out.write("ERROR".getBytes());
		out.close();
		LOG.info("Handle with 500");

	}

}
