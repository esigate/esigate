package org.esigate.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.Assert;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.esigate.Driver;
import org.esigate.DriverFactory;
import org.esigate.HttpErrorPage;
import org.esigate.api.Cookie;
import org.esigate.api.HttpRequest;
import org.esigate.api.HttpSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bug101ConnectionReleaseTester {

	static Server server = null;
	static int SERVER_PORT = 16009;
	private final static Logger LOG = LoggerFactory
			.getLogger(Bug101ConnectionReleaseTester.class);

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

		Appendable out = new StringBuffer();
		Driver d = DriverFactory.getInstance("bug101");

		for (int i = 0; i < 20; i++) {

			HttpRequest request = new HttpRequest() {

				public String getQueryString() {
					return null;
				}

				public String getParameter(String name) {
					return null;
				}

				public String getHeader(String name) {
					return null;
				}

				public Collection<String> getHeaderNames() {
					return new ArrayList<String>();
				}

				public Cookie[] getCookies() {
					return null;
				}

				public String getMethod() {
					return null;
				}

				public int getServerPort() {
					return 0;
				}

				public String getServerName() {
					return null;
				}

				public String getScheme() {
					return null;
				}

				public String getRemoteAddr() {
					return null;
				}

				public InputStream getInputStream() throws IOException {
					return null;
				}

				public String getContentType() {
					return null;
				}

				public String getRequestURI() {
					return null;
				}

				public String getRequestURL() {
					return null;
				}

				public boolean isSecure() {
					return false;
				}

				public String getCharacterEncoding() {
					return null;
				}

				public void setCharacterEncoding(String env)
						throws UnsupportedEncodingException {

				}

				public String getRemoteUser() {
					return null;
				}

				public Object getAttribute(String name) {
					return null;
				}

				public void setAttribute(String name, Object o) {

				}

				public Principal getUserPrincipal() {
					return null;
				}

				public HttpSession getSession(boolean create) {
					return null;
				}

				public Long getResourceTtl() {
					return null;
				}

				public Boolean isNoStoreResource() {
					return false;
				}

				public Integer getFetchMaxWait() {
					return null;
				}

				public void setResourceTtl(Long ttl) {
				}

				public void setNoStoreResource(boolean noStore) {
				}

				public void setFetchMaxWait(Integer maxWait) {
				}

			};

			try {
				d.renderBlock("/test" + i, null, out, request, null,
						new HashMap<String, String>(), null, false);
			} catch (Exception e) {
				LOG.warn("Error while processing renderBlock", e);
			}

		}

		Assert.assertTrue("Connection pool timeout : ressource leaked",
				System.currentTimeMillis() - start < 2000);
	}

	@BeforeClass
	public static void setup() throws Exception {

		Properties p = new Properties();
		p.setProperty("bug101.maxConnectionsPerHost", "1");
		p.setProperty("bug101.timeout", "10000");
		p.setProperty("bug101.cacheRefreshDelay", "-1");
		p.setProperty("bug101.remoteUrlBase", "http://localhost:16009/tester/");
		
		final Bug101RoundRobinResultServlet servlet = new Bug101RoundRobinResultServlet();

		DriverFactory.configure(p);
		
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
