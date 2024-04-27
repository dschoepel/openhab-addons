package org.openhab.binding.tailwind.internal.connector;

import org.eclipse.jetty.client.HttpClient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public abstract class TailwindApi {
    private final HttpClient httpClient;
    protected final Gson gson;

    /**
     * Constructor for TailWind rest API requests using Gson API's to parse JSON responses.
     *
     * @param httpClient - common client used for API GET requests
     */
    protected TailwindApi(HttpClient httpClient) {
        this.httpClient = httpClient;

        GsonBuilder gsonBuilder = new GsonBuilder();
        gson = gsonBuilder.create();
    }

    /**
     * Method to retrieve the shared HTTP client used by the Handlers to retrieve device/observations from the API.
     *
     * @return Common shared HTTP client to use for API requests
     */
    protected HttpClient getHttpClient() {
        return httpClient;
    }
}
