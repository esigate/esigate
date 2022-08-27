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

import java.util.Properties;

import org.apache.http.HttpStatus;
import org.esigate.Driver;
import org.esigate.events.Event;
import org.esigate.events.EventDefinition;
import org.esigate.events.EventManager;
import org.esigate.events.IEventListener;
import org.esigate.events.impl.FragmentEvent;
import org.esigate.util.Parameter;
import org.esigate.util.ParameterInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This extension replaces response code when an error was returned during process
 * <p>
 * Be sure to put this extension last
 * 
 * <p>
 * Variables:
 * <ul>
 * <li>onerror_response_code: integer (response code used as replacement)</li>
 * </ul>
 * </p>
 * 
 * @author VeekeeFr
 * 
 */
public class OnErrorResponseCodeReplacer implements Extension, IEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(OnErrorResponseCodeReplacer.class);
    public static final Parameter<Integer> ON_ERROR_RESPONSE_CODE = new ParameterInteger("onerror_response_code", 0);
    private int newResponseCode;

    @Override
    public void init(Driver pDriver, Properties properties) {
        this.newResponseCode = ON_ERROR_RESPONSE_CODE.getValue(properties);
        if (newResponseCode == 0) {
            LOG.warn(
                    "No onerror_response_code value could be retrieved for instance '{}'... Extension will be disabled!",
                    pDriver.getConfiguration().getInstanceName());
            return;
        }

		pDriver.getEventManager().register(EventManager.EVENT_FRAGMENT_POST, this);
    }

    @Override
    public boolean event(EventDefinition id, Event event) {
        FragmentEvent e = (FragmentEvent) event;

        int statusCode = e.getHttpResponse().getStatusLine().getStatusCode();

        if (statusCode >= HttpStatus.SC_BAD_REQUEST) {
            e.getHttpResponse().setStatusCode(newResponseCode);
        }

        // Continue processing
        return true;
    }

}
