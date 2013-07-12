/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.esigate.http;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.DeflateDecompressingEntity;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.util.EntityUtils;
import org.esigate.HttpErrorPage;
import org.esigate.util.UriUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for HttpClient's Request and Response objects.
 * 
 * @author Francois-Xavier Bonnet
 * @author Nicolas Richeton
 * 
 */
public class HttpResponseUtils {
	private static final Logger LOG = LoggerFactory.getLogger(HttpResponseUtils.class);

	/**
	 * Check if httpResponse has an error status.
	 * 
	 * @param httpResponse
	 * @return true if status code >= 400
	 */
	public static boolean isError(HttpResponse httpResponse) {
		return httpResponse.getStatusLine().getStatusCode() >= 400;
	}

	/**
	 * Get the value of the first header matching "headerName".
	 * 
	 * @param headerName
	 * @param httpResponse
	 * @return value of the first header or null if it doesn't exist.
	 */
	public static String getFirstHeader(String headerName, HttpResponse httpResponse) {
		Header header = httpResponse.getFirstHeader(headerName);
		if (header != null)
			return header.getValue();
		return null;
	}

	/**
	 * Returns the charset of the entity of "httpResponse".
	 * 
	 * @param httpResponse
	 * @return charset as string or null if no charset defined.
	 */
	public static String getContentCharset(HttpResponse httpResponse) {
		ContentType contentType = ContentType.get(httpResponse.getEntity());
		if (contentType != null) {
			Charset charset = contentType.getCharset();
			if (charset != null)
				return charset.name();
		}
		return null;
	}

	/**
	 * Removes ";jsessionid=&lt;id&gt;" from the url, if the session id is also
	 * set in "httpResponse".
	 * <p>
	 * This methods first looks for the following header :
	 * 
	 * <pre>
	 * Set-Cookie: JSESSIONID=
	 * </pre>
	 * 
	 * . If found and perfectly matches the jsessionid value in url, the
	 * complete jsessionid definition is removed from the url.
	 * 
	 * @param uri
	 *            original uri, may contains a jsessionid.
	 * @param httpResponse
	 *            the response which set the jsessionId
	 * @return uri, without jsession
	 */
	public static String removeSessionId(String uri, HttpResponse httpResponse) {
		CookieSpec cookieSpec = new BrowserCompatSpec();
		// Dummy origin, used only by CookieSpec for setting the domain for the
		// cookie but we don't need it
		CookieOrigin cookieOrigin = new CookieOrigin("dummy", 80, "/", false);
		Header[] responseHeaders = httpResponse.getHeaders("Set-cookie");
		String jsessionid = null;
		for (int i = 0; i < responseHeaders.length; i++) {
			Header header = responseHeaders[i];
			try {
				List<Cookie> cookies = cookieSpec.parse(header, cookieOrigin);
				for (Cookie cookie : cookies) {
					if ("JSESSIONID".equalsIgnoreCase(cookie.getName()))
						jsessionid = cookie.getValue();
					break;
				}
			} catch (MalformedCookieException ex) {
				LOG.warn("Malformed header: " + header.getName() + ": " + header.getValue());
			}
			if (jsessionid != null)
				break;
		}
		if (jsessionid == null) {
			return uri;
		}

		return UriUtils.removeSessionId(jsessionid, uri);
	}

	/**
	 * Returns the response body as a string or the reason phrase if body is
	 * empty.
	 * <p>
	 * This methods uses EntityUtils#toString() internally, but uncompress the
	 * entity first if necessary.
	 * 
	 * 
	 * @param httpResponse
	 * @return The body as string or the reason phrase if body was empty.
	 * @throws HttpErrorPage
	 */
	public static String toString(HttpResponse httpResponse) throws HttpErrorPage {
		HttpEntity httpEntity = httpResponse.getEntity();
		String result;
		if (httpEntity == null) {
			result = httpResponse.getStatusLine().getReasonPhrase();
		} else {
			// Unzip the stream if necessary
			Header contentEncoding = httpEntity.getContentEncoding();
			if (contentEncoding != null) {
				String contentEncodingValue = contentEncoding.getValue();
				if ("gzip".equalsIgnoreCase(contentEncodingValue) || "x-gzip".equalsIgnoreCase(contentEncodingValue)) {
					httpEntity = new GzipDecompressingEntity(httpEntity);
				} else if ("deflate".equalsIgnoreCase(contentEncodingValue)) {
					httpEntity = new DeflateDecompressingEntity(httpEntity);
				} else {
					throw new UnsupportedContentEncodingException("Content-encoding \"" + contentEncoding
							+ "\" is not supported");
				}
			}
			try {
				result = EntityUtils.toString(httpEntity);
			} catch (IOException e) {
				throw new HttpErrorPage(IOExceptionHandler.toHttpResponse(e));
			}
		}
		return removeSessionId(result, httpResponse);
	}
}
