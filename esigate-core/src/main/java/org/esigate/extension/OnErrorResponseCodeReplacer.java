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

package org.esigate.extension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.http.HttpStatus;
import org.esigate.Driver;
import org.esigate.events.Event;
import org.esigate.events.EventDefinition;
import org.esigate.events.EventManager;
import org.esigate.events.IEventListener;
import org.esigate.events.impl.FragmentEvent;
import org.esigate.util.Parameter;
import org.esigate.util.ParameterCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This extension replaces response code when an error was returned during process
 * <p>
 * Be sure to put this extension last.
 * 
 * <p>
 * Variables:
 * <ul>
 * <li>onerror_response_code: pattern:int (response code used as replacement)</li>
 * </ul>
 * </p>
 * 
 * Note: Special response code value '0' means that the response code will be unchanged.
 * 
 * @author VeekeeFr
 * 
 */
public class OnErrorResponseCodeReplacer implements Extension, IEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(OnErrorResponseCodeReplacer.class);
    public static final Parameter<Collection<String>> ON_ERROR_RESPONSE_CODE = new ParameterCollection(
            "onerror_response_code");
    private Driver driver;
    private List<OnErrorResponseCodeReplacerMatcher> responseCodeMapping;

    @Override
    public void init(Driver pDriver, Properties properties) {
        this.driver = pDriver;
		this.responseCodeMapping = new ArrayList<>();
		ON_ERROR_RESPONSE_CODE.getValue(properties)
				.forEach(value -> {
					String[] kv = value.split(":", 2);
					this.responseCodeMapping.add(new OnErrorResponseCodeReplacerMatcher(kv[0], Integer.parseInt(kv[1])));
				});
        if (this.responseCodeMapping.isEmpty()) {
            LOG.warn(
                    "No onerror_response_code mapping could be retrieved for instance '{}'... Extension will be disabled!",
                    this.driver.getConfiguration().getInstanceName());
            return;
        } else {
            LOG.warn("Enabling onerror_response_code extension for instance '{}'", this.driver.getConfiguration()
                    .getInstanceName());
        }

        pDriver.getEventManager().register(EventManager.EVENT_FRAGMENT_POST, this);
    }

    @Override
    public boolean event(EventDefinition id, Event event) {
        FragmentEvent e = (FragmentEvent) event;

        int statusCode = e.getHttpResponse().getStatusLine().getStatusCode();

		if(LOG.isDebugEnabled()) {
			LOG.debug("Return code '{}' detected for '{}' (instance '{}')...", statusCode, e.getHttpRequest().getBaseUrl(),
					this.driver.getConfiguration().getInstanceName());
		}
        if (statusCode >= HttpStatus.SC_BAD_REQUEST) {
			String sStatusCode = String.valueOf(statusCode);
			int newResponseCode = this.responseCodeMapping
					.stream()
					.filter(rule -> rule.matches(sStatusCode))
					.findFirst()
					.map(rule -> rule.responseCode)
					.orElseGet(() -> 0);
			if(newResponseCode <= 0) {
				newResponseCode = statusCode;
			}

			if(LOG.isDebugEnabled()) {
				LOG.debug("Replacing with '{}'!", newResponseCode);
			}
            e.getHttpResponse().setStatusCode(newResponseCode);
        }

        // Continue processing
        return true;
    }
}

class OnErrorResponseCodeReplacerMatcher {
    private final Pattern regex;
    public final int responseCode;

    public OnErrorResponseCodeReplacerMatcher(String rgx, int rc) {
        this.regex = Pattern.compile(rgx);
        this.responseCode = rc;
    }

    public boolean matches(String value) {
        return this.regex.matcher(value).matches();
    }
}
