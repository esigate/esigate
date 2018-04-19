/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.esigate;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.esigate.extension.Esi;
import org.esigate.extension.FetchLogging;
import org.esigate.http.IncomingRequest;
import org.esigate.http.RetryExtension;
import org.esigate.test.PropertiesBuilder;
import org.esigate.test.TestUtils;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Esigate tests using a real http connection.
 * 
 * @author Nicolas Richeton
 * 
 */
public class DriverHttpTest extends TestCase {

    /**
     * Ensure Http client retry behavior can be enabled.
     * <p>
     * Was broken in esigate &lt; 5.3
     * <p>
     * 
     * @see https://github.com/esigate/esigate/issues/185
     * 
     * @throws Exception
     */
    public void testHttpClientRetry() throws Exception {

        // Conf
        Properties properties = new PropertiesBuilder()//
                .set(Parameters.REMOTE_URL_BASE, "http://localhost:9999/") //
                // Enable retry
                .set(Parameters.EXTENSIONS, Esi.class, RetryExtension.class, FetchLogging.class) //
                .set(Parameters.MAX_CONNECTIONS_PER_HOST, 200) //
                .build();

        Driver driver = Driver.builder().setName("tested").setProperties(properties).build();

        // Setup remote server (provider) response.
        Server server = new Server(9999);
        server.setStopAtShutdown(true);

        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String arg0, Request baseRequest, HttpServletRequest arg2, HttpServletResponse response)
                    throws IOException, ServletException {
                response.setContentType("text/html;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);
                response.setHeader("Cache-Control", "private");
                response.getWriter().print("OK");
                baseRequest.setHandled(true);
            }

        });
        server.start();

        // Request
        IncomingRequest requestWithSurrogate = TestUtils.createRequest("http://localhost/foobar/").build();

        HttpResponse response = TestUtils.driverProxy(driver, requestWithSurrogate);
        Assert.assertEquals("OK", EntityUtils.toString(response.getEntity()));

        // Restart server.
        // This breaks closes the socket and will cause an IO error, as Http Client only
        // tests
        // connections which are idle for more than 2s
        server.stop();
        server.start();

        // Request
        requestWithSurrogate = TestUtils.createRequest("http://localhost/foobar/").build();

        response = TestUtils.driverProxy(driver, requestWithSurrogate);
        Assert.assertEquals("OK", EntityUtils.toString(response.getEntity()));

        server.stop();
    }

}
