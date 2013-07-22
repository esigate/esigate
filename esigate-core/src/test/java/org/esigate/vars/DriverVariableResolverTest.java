package org.esigate.vars;

import java.io.IOException;
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

public class DriverVariableResolverTest extends AbstractDriverTestCase {

	/**
	 * 0000246: ESI variables are not available / replaced
	 * https://sourceforge.net/apps/mantisbt/webassembletool/view.php?id=246
	 * 
	 * @throws IOException
	 * @throws HttpErrorPage
	 */
	@Test
	public void testEsiVariables() throws IOException, HttpErrorPage {
		// Configuration
		Properties properties = new Properties();
		properties.put(Parameters.REMOTE_URL_BASE.name, "http://localhost.mydomain.fr/");

		// Setup remote server (provider) response.
		HttpRequestExecutor mockExecutor = new HttpRequestExecutor() {
			@Override
			public HttpResponse execute(HttpRequest request, HttpClientConnection conn, HttpContext context)
					throws IOException, HttpException {
				StringBuilder content = new StringBuilder();
				content.append("<esi:vars>");
				content.append("<p>HTTP_ACCEPT_LANGUAGE: $(HTTP_ACCEPT_LANGUAGE)</p>");
				content.append("<p>HTTP_ACCEPT_LANGUAGE{en}: $(HTTP_ACCEPT_LANGUAGE{en})</p>");
				content.append("<p>QUERY_STRING{test}: $(QUERY_STRING{test})</p>");
				content.append("<p>QUERY_STRING: $(QUERY_STRING)</p>");
				content.append("<p>HTTP_REFERER: $(HTTP_REFERER)</p>");
				content.append("<p>PROVIDER{tested}: $(PROVIDER{tested})</p>");
				content.append("</esi:vars>");

				return createHttpResponse().entity(new StringEntity(content.toString(), ContentType.TEXT_HTML)).build();
			}
		};

		// Build driver and request.
		Driver driver = createMockDriver(properties, mockExecutor);
		HttpEntityEnclosingRequest request = createHttpRequest().uri("http://test.mydomain.fr/foobar/?test=esigate&test2=esigate2")
				.header("Referer", "http://www.esigate.org").header("Accept-Language", "da, en-gb;q=0.8, en;q=0.7")
				.mockMediator().build();

		driver.proxy("/foobar/", request, new EsiRenderer());

		HttpResponse response = TestUtils.getResponse(request);

		StringBuilder expected = new StringBuilder();
		expected.append("<p>HTTP_ACCEPT_LANGUAGE: da, en-gb;q=0.8, en;q=0.7</p>");
		expected.append("<p>HTTP_ACCEPT_LANGUAGE{en}: true</p>");
		expected.append("<p>QUERY_STRING{test}: esigate</p>");
		expected.append("<p>QUERY_STRING: test=esigate&test2=esigate2</p>");
		expected.append("<p>HTTP_REFERER: http://www.esigate.org</p>");
		expected.append("<p>PROVIDER{tested}: http://localhost.mydomain.fr/</p>");

		String[] expectedArray = StringUtils.splitByWholeSeparator(expected.toString(), "<p>");
		String[] resultArray = StringUtils.splitByWholeSeparator(EntityUtils.toString(response.getEntity()), "<p>");

		for (int i = 0; i < expectedArray.length; i++) {
			String varName = expectedArray[i].substring(0, expectedArray[i].indexOf(":"));
			Assert.assertEquals(varName, expectedArray[i],
					resultArray[i]);
		}

	}

}
