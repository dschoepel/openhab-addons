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
package org.openhab.binding.tailwind.internal.connector;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.tailwind.internal.dto.TailwindControllerData;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 *
 */
public class JSONPost {

    private Gson gson = new Gson();

    /**
     * Posts a JSON string to a specified HTTP endpoint.
     *
     * @param uriString The URI of the endpoint as a String.
     * @param jsonBody The JSON string to post.
     * @return The response body as a String.
     * @throws Exception
     * @throws IOException If an I/O error occurs when sending or receiving.
     * @throws InterruptedException If the operation is interrupted.
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public TailwindControllerData postJson(String uriString, String jsonBody, String authToken) throws Exception {

        // @NonNull
        // ContentResponse response = null;
        HttpClient httpClient = new HttpClient();
        httpClient.start();
        // String stringResponse = "";
        Request request = httpClient.newRequest(uriString);
        request.method(HttpMethod.POST);
        request.header("TOKEN", authToken);
        request.content(new StringContentProvider(jsonBody));
        request.header(HttpHeader.CONTENT_TYPE, "application/json");
        request.header("TOKEN", authToken);
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

}
