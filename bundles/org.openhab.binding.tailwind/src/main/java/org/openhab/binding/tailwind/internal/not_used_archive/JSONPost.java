/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.tailwind.internal.not_used_archive;

import static org.openhab.binding.tailwind.internal.TailwindBindingConstants.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.tailwind.internal.connector.TailwindCommunicationException;
import org.openhab.binding.tailwind.internal.connector.TailwindUnauthorizedException;
import org.openhab.binding.tailwind.internal.dto.TailwindControllerData;
import org.openhab.core.thing.Thing;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 *
 */
public class JSONPost {

    private Gson gson = new Gson();

    /**
     * Posts a JSON string to a specified HTTP end point.
     *
     * @param thing The TailWind device thing to retrieve properties from.
     * @param jsonBody The JSON string to post.
     * @return The response body as a String.
     * @throws Exception
     * @throws IOException If an I/O error occurs when sending or receiving.
     * @throws InterruptedException If the operation is interrupted.
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public TailwindControllerData postJson(Thing thing, String jsonBody, String authToken) throws Exception {

        // @NonNull
        // ContentResponse response = null;
        HttpClient httpClient = new HttpClient();
        httpClient.start();
        // String stringResponse = "";
        Request request = httpClient.newRequest(getTailwindServerUrl(thing));
        request.method(HttpMethod.POST);
        request.header("TOKEN", authToken);
        request.content(new StringContentProvider(jsonBody));
        request.header(HttpHeader.CONTENT_TYPE, "application/json");
        request.method(HttpMethod.POST);
        request.timeout(10, TimeUnit.SECONDS);

        ContentResponse result = request.send();
        httpClient.stop();
        return parseResponse(result, TailwindControllerData.class);
    }

    /**
     * Method used to extract the device/observations from the BloomSky API response from a string to
     * a JSON object.
     *
     * @param <T> - Placeholder for return type (DTO class used to parse the response from the BloomSky rest API)
     * @param response - from the HTTP GET request to the BloomSky API
     * @param type - name of DTO class to use for the gson.fromJson method call
     * @return A parsed response in JSON format if no errors, otherwise throw an exception with error code
     * @throws BloomSkyCommunicationException indicating the type of communication error in the API response header
     */
    private <T> T parseResponse(ContentResponse response, Class<T> type) throws TailwindCommunicationException {
        int statusCode = response.getStatus();

        checkForError(response, statusCode);
        try {
            return gson.fromJson(response.getContentAsString(), type);
        } catch (JsonSyntaxException e) {
            throw new TailwindCommunicationException(e);
        }
    }

    /**
     * Method used to determine if the BloomSky API response is valid, if not, provide additional details
     * in the log explaining what the problem might be.
     *
     * @param response - header/body information from the GET request to the BloomSKy API
     * @param statusCode - from the header to catch typical errors and report them to the log
     * @throws BloomSkyCommunicationException if there is an error requesting information from the BloomSKy API
     */
    private void checkForError(ContentResponse response, int statusCode) throws TailwindCommunicationException {
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }

        switch (statusCode) {
            case HttpStatus.NOT_FOUND_404:
                throw new TailwindCommunicationException(statusCode, "Target '" + response.getRequest().getURI()
                        + "' seems to be not available: " + response.getContentAsString());

            case HttpStatus.FORBIDDEN_403:
            case HttpStatus.UNAUTHORIZED_401:
                throw new TailwindUnauthorizedException(statusCode, response.getContentAsString());

            default:
                throw new TailwindCommunicationException(statusCode, response.getContentAsString());
        }
    }

    private String getTailwindServerUrl(Thing thing) throws TailwindCommunicationException {
        String serverURL = "";
        Map<@NonNull String, @NonNull String> thingPropertiesMap = thing.getProperties();
        if (thingPropertiesMap.containsKey(TAILWIND_HTTP_SERVER_URL)) {
            String server = thingPropertiesMap.get(TAILWIND_HTTP_SERVER_URL);
            if (server != null && !server.isBlank()) {
                serverURL = TAILWIND_BASE_URL_PART_1 + server + TAILWIND_BASE_URL_PART_2;
            } else {
                serverURL = NOT_FOUND_ERROR;
                throw new TailwindCommunicationException("The Tailwind Thing URL for the Http server was " + serverURL
                        + ". Check thing properties for a valid httpServerUrl!!");
            } // If server URL has a value
        } // If server URL was found in the properties for this thing

        ;
        return serverURL;
    }

}
