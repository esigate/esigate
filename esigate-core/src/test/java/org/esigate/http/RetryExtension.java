package org.esigate.http;

import java.util.Properties;

import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.esigate.Driver;
import org.esigate.events.Event;
import org.esigate.events.EventDefinition;
import org.esigate.events.EventManager;
import org.esigate.events.IEventListener;
import org.esigate.events.impl.HttpClientBuilderEvent;
import org.esigate.extension.Extension;

/**
 * Enable Retry handler in http client.
 * 
 * @author Nicolas Richeton
 * 
 */
public class RetryExtension implements Extension, IEventListener {

    public boolean event(EventDefinition id, Event event) {

        if (EventManager.EVENT_HTTP_BUILDER_INITIALIZATION.equals(id)) {
            HttpClientBuilderEvent e = (HttpClientBuilderEvent) event;
            e.getHttpClientBuilder().setRetryHandler(new DefaultHttpRequestRetryHandler(1, true));
        }

        return true;
    }

    public void init(Driver driver, Properties properties) {
        driver.getEventManager().register(EventManager.EVENT_HTTP_BUILDER_INITIALIZATION, this);
    }

}
