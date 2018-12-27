package org.esigate.http;

import java.util.Properties;

import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.esigate.Driver;
import org.esigate.events.Event;
import org.esigate.events.EventDefinition;
import org.esigate.events.EventManager;
import org.esigate.events.IEventListener;
import org.esigate.events.impl.FetchEvent;
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
        } else if (EventManager.EVENT_FETCH_PRE.equals(id)) {
            FetchEvent e = (FetchEvent) event;
            int attemptNumber = getAttemptNumber(e);
            setAttemptNumber(attemptNumber + 1, e);
            System.out.println("Attempt " + attemptNumber);
        } else if (EventManager.EVENT_FETCH_POST.equals(id)) {
            FetchEvent e = (FetchEvent) event;
            int attemptNumber = getAttemptNumber(e);
            if (attemptNumber < 2)
                e.setExit(false); // let's retry in case it failed
        }

        return true;
    }

    public void init(Driver driver, Properties properties) {
        driver.getEventManager().register(EventManager.EVENT_HTTP_BUILDER_INITIALIZATION, this);
        driver.getEventManager().register(EventManager.EVENT_FETCH_PRE, this);
        driver.getEventManager().register(EventManager.EVENT_FETCH_POST, this);
    }

    private final static String KEY = "RetryExtension.attemptNumber";

    private int getAttemptNumber(FetchEvent e) {
        Object number = e.getHttpContext().getAttribute(KEY);
        return number == null ? 0 : (Integer) number;
    }

    private void setAttemptNumber(int number, FetchEvent e) {
        e.getHttpContext().setAttribute(KEY, number);
    }
}
