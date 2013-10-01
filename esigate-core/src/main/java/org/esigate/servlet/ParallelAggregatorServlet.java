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

package org.esigate.servlet;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.tuple.Pair;
import org.esigate.Driver;
import org.esigate.DriverFactory;
import org.esigate.HttpErrorPage;
import org.esigate.aggregator.AggregateRenderer;
import org.esigate.extension.parallelesi.EsiRenderer;
import org.esigate.impl.UriMapping;
import org.esigate.servlet.impl.DriverSelector;
import org.esigate.servlet.impl.RequestUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet used to proxy requests from a remote application.
 * <p>
 * ALPHA: This servlet uses the parallel ESI renderer, which is more resource
 * intensive but can be faster in some cases, where a lot of <esi:include> are
 * used AND include results cannot be cached AND remote provider is slow.
 * <p>
 * Unless you know exactly what you are doing, it is recommended to use
 * {@link AggregatorServlet} instead.
 * 
 * <p>
 * Parameters are :
 * <ul>
 * <li>provider : single provider name</li>
 * <li>providers : comma-separated list of provider mappings based on host
 * requested. Format is: host1=provider,host2=provider2</li>
 * <li>useMappings (optional - BETA, testing only): true or false : use mappings
 * from esigate.properties</li>
 * </ul>
 * 
 * 
 * @author Nicolas Richeton
 */
public class ParallelAggregatorServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(ParallelAggregatorServlet.class);
	private DriverSelector driverSelector = new DriverSelector();

	/**
	 * Get current Driver selector. This is mainly used for unit testing.
	 * 
	 * @return
	 */
	public DriverSelector getDriverSelector() {
		return this.driverSelector;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {

		HttpServletMediator mediator = new HttpServletMediator(request, response, getServletContext());

		try {
			Pair<Driver, UriMapping> dm = this.driverSelector.selectProvider(request);
			String relUrl = RequestUrl.getRelativeUrl(request, dm.getRight());
			LOG.debug("Proxying {}", relUrl);
			// dm.getLeft().proxy(relUrl, mediator.getHttpRequest(), new
			// AggregateRenderer(), new EsiRenderer());
			dm.getLeft().proxy(relUrl, mediator.getHttpRequest(), new AggregateRenderer(), new EsiRenderer());
		} catch (HttpErrorPage e) {
			mediator.sendResponse(e.getHttpResponse());
		}

	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		// get selected provided from web.xml (deprecated)
		this.driverSelector.setWebXmlProvider(config.getInitParameter("provider"));

		// Load mappings from web.xml (deprecated)
		this.driverSelector.setWebXmlProviders(config.getInitParameter("providers"));

		this.driverSelector.setUseMappings("true".equalsIgnoreCase(config.getInitParameter("useMappings")));

		// Force esigate configuration parsing to trigger errors right away (if
		// any) and prevent delay on first call.
		DriverFactory.ensureConfigured();

	}
}
