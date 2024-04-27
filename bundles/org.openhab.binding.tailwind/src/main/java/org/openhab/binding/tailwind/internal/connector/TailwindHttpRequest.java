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

import javax.ws.rs.ProcessingException;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpResponse;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class TailwindHttpRequest implements AutoCloseable {
    /** the logger */
    private final Logger logger = LoggerFactory.getLogger(TailwindHttpRequest.class);

    /** The httpClient to use */
    private final HttpClient httpClient;

    /**
     * Instantiates a new request
     */
    public TailwindHttpRequest(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Send post JSON command using the body
     *
     * @param uri the non empty uri
     * @param body the non-null, possibly empty body
     * @return the {@link HttpResponse}
     */
    public @NonNull TailwindHttpResponse sendPostJsonCommand(String uri, String body, String authToken) {
        // NeeoUtil.requireNotEmpty(uri, "uri cannot be empty");
        // Objects.requireNonNull(body, "body cannot be null");

        try {
            final org.eclipse.jetty.client.api.Request request = httpClient.newRequest(uri);
            request.content(new StringContentProvider(body));
            request.header(HttpHeader.CONTENT_TYPE, "application/json");
            request.header("TOKEN", authToken);
            request.method(HttpMethod.POST);
            request.timeout(10, TimeUnit.SECONDS);
            ContentResponse refreshResponse = request.send();
            return new TailwindHttpResponse(refreshResponse);
        } catch (IOException | IllegalStateException | ProcessingException e) {
            String message = e.getMessage();
            return new TailwindHttpResponse(HttpStatus.SERVICE_UNAVAILABLE_503, message != null ? message : "");
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            logger.debug("An exception occurred while invoking a HTTP request: '{}'", e.getMessage());
            String message = e.getMessage();
            return new TailwindHttpResponse(HttpStatus.SERVICE_UNAVAILABLE_503, message != null ? message : "");
        }
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub

    }

}
