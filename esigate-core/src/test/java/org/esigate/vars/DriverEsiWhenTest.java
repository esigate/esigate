package org.esigate.vars;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.util.EntityUtils;
import org.esigate.Driver;
import org.esigate.HttpErrorPage;
import org.esigate.Parameters;
import org.esigate.esi.EsiRenderer;
import org.esigate.test.TestUtils;
import org.esigate.test.driver.AbstractDriverTestCase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DriverEsiWhenTest extends AbstractDriverTestCase {
	private static final Logger LOG = LoggerFactory.getLogger(DriverEsiWhenTest.class);

	@Test
	public void testEsiWhenCase1() throws IOException, HttpErrorPage, URISyntaxException {
		// Configuration
		Properties properties = new Properties();
		properties.put(Parameters.REMOTE_URL_BASE.name, "http://localhost.mydomain.fr/");

		// Test case
		HttpEntityEnclosingRequest request = createHttpRequest()
				.uri("http://test.mydomain.fr/foobar/?test=esigate&test2=esigate2")
				.header("Referer", "http://www.esigate.org")
				.header("User-Agent",
						"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) AppleWebKit/536.30.1 (KHTML, like Gecko) Version/6.0.5 Safari/536.30.1")
				.header("Accept-Language", "da, en-gb;q=0.8, en;q=0.7").cookie("test-cookie", "test-cookie-value")
				.cookie("test-cookie2", "test-cookie-value2").mockMediator().build();

		final StringBuilder expected = new StringBuilder();
		addExpression(expected, "!(1==1)", false);
		addExpression(expected, "1==1", true);
		addExpression(expected, "$(HTTP_COOKIE{test-cookie})==test-cookie-value", true);
		addExpression(expected, "$(HTTP_COOKIE{test-cookie})=='test-cookie-value'", true);

		addExpression(expected, "$(HTTP_REFERER)==http://www.esigate.org", true);
		addExpression(expected, "$(HTTP_HOST)=='test.mydomain.fr'", true);
		addExpression(expected, "$(HTTP_HOST)==test.mydomain.fr", true);

		// Setup remote server (provider) response.
		HttpRequestExecutor mockExecutor = new HttpRequestExecutor() {
			@Override
			public HttpResponse execute(HttpRequest request, HttpClientConnection conn, HttpContext context)
					throws IOException, HttpException {

				StringBuilder content = new StringBuilder();

				String[] expectedArray = StringUtils.splitByWholeSeparator(expected.toString(), "<p>");

				for (String expr : expectedArray) {
					addExpression(content, expr.substring(0, expr.indexOf(": ")));
				}

				return createHttpResponse().entity(new StringEntity(content.toString(), ContentType.TEXT_HTML)).build();
			}
		};

		// Build driver and request.
		Driver driver = createMockDriver(properties, mockExecutor);

		driverProxy(driver, request, new EsiRenderer());

		HttpResponse response = TestUtils.getResponse(request);

		String[] expectedArray = StringUtils.splitByWholeSeparator(expected.toString(), "<p>");
		String[] resultArray = StringUtils.splitByWholeSeparator(EntityUtils.toString(response.getEntity()), "<p>");

		for (int i = 0; i < expectedArray.length; i++) {
			String varName = expectedArray[i].substring(0, expectedArray[i].indexOf(":"));
			Assert.assertEquals(varName, expectedArray[i], resultArray[i]);
			LOG.info("Success with variable {}", varName);
		}

	}

	static void addExpression(StringBuilder sb, String expr) {
		sb.append("<p>" + expr + ": " + "<esi:choose>" + "<esi:when test=\"" + expr + "\">true</esi:when>"
				+ "<esi:otherwise>false</esi:otherwise>" + "</esi:choose>" + "</p>");

		LOG.info("Adding {} for evaluation", expr);
	}

	static void addExpression(StringBuilder sb, String expr, boolean value) {
		sb.append("<p>" + expr + ": " + value + "</p>");
		LOG.info("Adding {} with expected result {}", expr, value);

	}

}
