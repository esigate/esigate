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

public class DriverEsiVariablesTest extends AbstractDriverTestCase {
	private static final Logger LOG = LoggerFactory.getLogger(DriverEsiVariablesTest.class);

	/**
	 * 0000246: ESI variables are not available / replaced
	 * https://sourceforge.net/apps/mantisbt/webassembletool/view.php?id=246
	 * 
	 * @throws IOException
	 * @throws HttpErrorPage
	 * @throws URISyntaxException
	 */
	@Test
	public void testEsiVariables() throws IOException, HttpErrorPage, URISyntaxException {
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
		addExpression(expected, "HTTP_ACCEPT_LANGUAGE", "da, en-gb;q=0.8, en;q=0.7");
		addExpression(expected, "HTTP_ACCEPT_LANGUAGE{en}", "true");
		addExpression(expected, "HTTP_ACCEPT_LANGUAGE{fr}", "false");
		addExpression(expected, "QUERY_STRING{test}", "esigate");
		addExpression(expected, "QUERY_STRING", "test=esigate&test2=esigate2");
		addExpression(expected, "HTTP_REFERER", "http://www.esigate.org");
		addExpression(expected, "PROVIDER{tested}", "http://localhost.mydomain.fr/");
		addExpression(expected, "PROVIDER{missing}", "");
		addExpression(expected, "PROVIDER", "");
		addExpression(expected, "HTTP_HOST", "test.mydomain.fr");
		addExpression(expected, "HTTP_USER_AGENT",
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) AppleWebKit/536.30.1 (KHTML, like Gecko) Version/6.0.5 Safari/536.30.1");
		addExpression(expected, "HTTP_COOKIE{test-cookie}", "test-cookie-value");
		// addExpression(expected, "HTTP_COOKIE",
		// "test-cookie=test-cookie-value; test-cookie2=test-cookie-value2");
		// addExpression(expected, "HTTP_COOKIE{missing}", "");
		// addExpression(expected, "QUERY_STRING{missing}|default-value",
		// "default-value");
		// addExpression(expected, "QUERY_STRING{missing}|'default value'",
		// "default value");
		// addExpression(expected, "QUERY_STRING{missing}", "");

		// Setup remote server (provider) response.
		HttpRequestExecutor mockExecutor = new HttpRequestExecutor() {
			@Override
			public HttpResponse execute(HttpRequest request, HttpClientConnection conn, HttpContext context)
					throws IOException, HttpException {

				StringBuilder content = new StringBuilder();
				content.append("<esi:vars>");

				String[] expectedArray = StringUtils.splitByWholeSeparator(expected.toString(), "<p>");

				for (String expr : expectedArray) {
					addExpression(content, expr.substring(0, expr.indexOf(":")));
				}

				content.append("</esi:vars>");

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

	static void addExpression(StringBuilder sb, String variable) {
		sb.append("<p>" + variable + ": $(" + variable + ")</p>");
		LOG.info("Adding {} for evaluation", variable);
	}

	static void addExpression(StringBuilder sb, String variable, String value) {
		sb.append("<p>" + variable + ": " + value + "</p>");
		LOG.info("Adding {} with expected result {}", variable, value);

	}

}
