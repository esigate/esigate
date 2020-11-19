package org.esigate;

import junit.framework.TestCase;
import org.apache.http.HttpStatus;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;

public class HttpErrorPageTest extends TestCase {
    /**
     * Make sure that {@link NoHttpResponseException} is handled with a specific error message rather than triggering
     * the default exception handling that would pollute the logs with a stack trace.
     */
    public void testNoHttpResponseException() {
        CloseableHttpResponse response =
                HttpErrorPage.generateHttpResponse(new NoHttpResponseException("Something bad happened"));
        assertEquals(HttpStatus.SC_BAD_GATEWAY, response.getStatusLine().getStatusCode());
        assertEquals("The target server failed to respond", response.getStatusLine().getReasonPhrase());
    }
}
