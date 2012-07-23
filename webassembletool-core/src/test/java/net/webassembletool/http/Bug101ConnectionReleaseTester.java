package net.webassembletool.http;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.Assert;

import net.webassembletool.Driver;
import net.webassembletool.DriverFactory;
import net.webassembletool.HttpErrorPage;

import org.easymock.EasyMock;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class Bug101ConnectionReleaseTester {

	static Server server = null;
	static int SERVER_PORT = 16009;

	/**
	 * This method while return immediately if no connections are leaked of will
	 * return after 20 or 30 seconds, (pool timeout)
	 * 
	 * @throws IOException
	 * @throws HttpErrorPage
	 */
	@Test
	public void testConnectionLeak() throws IOException, HttpErrorPage {

		long start = System.currentTimeMillis();

		Writer out = new StringWriter();
		Driver d = DriverFactory.getInstance("bug101");

		for (int i = 0; i < 5; i++) {
			HttpServletRequest request = EasyMock
					.createMock(HttpServletRequest.class);
			EasyMock.expect(request.getMethod()).andReturn("POST").anyTimes();
			EasyMock.expect(request.getCharacterEncoding()).andReturn(null)
					.anyTimes();
			EasyMock.expect(request.getRemoteUser()).andReturn(null).anyTimes();
			EasyMock.expect(request.getHeaderNames()).andReturn(null)
					.anyTimes();
			EasyMock.expect(request.getAttribute((String) EasyMock.anyObject()))
					.andReturn(null).anyTimes();
			request.setAttribute((String) EasyMock.anyObject(),
					EasyMock.anyObject());
			request.setAttribute((String) EasyMock.anyObject(),
					EasyMock.anyObject());
			request.setAttribute((String) EasyMock.anyObject(),
					EasyMock.anyObject());
			request.setAttribute((String) EasyMock.anyObject(),
					EasyMock.anyObject());
			EasyMock.expect(request.getHeader((String) EasyMock.anyObject()))
					.andReturn(null).anyTimes();
			EasyMock.expect(request.getSession(EasyMock.anyBoolean()))
					.andReturn(null).anyTimes();
			EasyMock.replay(request);

			d.renderBlock("/test", null, out, request, null,
					new HashMap<String, String>(), null, false);

		}
		
		Assert.assertTrue("Connection pool timeout : ressource leaked", System.currentTimeMillis() - start < 1000);
	}

	@BeforeClass
	public static void setup() throws Exception {

		final Bug101RoundRobinResultServlet servlet = new Bug101RoundRobinResultServlet();

		Handler handler = new AbstractHandler() {

			public void handle(String arg0, Request arg1,
					HttpServletRequest request, HttpServletResponse response)
					throws IOException, ServletException {
				servlet.doGet(request, response);

				((Request) request).setHandled(true);
			}
		};
		server = new Server(SERVER_PORT);
		server.setHandler(handler);
		server.start();

	}

	@AfterClass
	public static void teardown() throws Exception {
		server.stop();
	}

}
