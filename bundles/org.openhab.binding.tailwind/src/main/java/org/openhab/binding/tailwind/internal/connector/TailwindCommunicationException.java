package org.openhab.binding.tailwind.internal.connector;

import java.io.IOException;

import org.eclipse.jdt.annotation.Nullable;

public class TailwindCommunicationException extends IOException {

    private static final long serialVersionUID = 1L;
    private int statusCode = -1;

    public TailwindCommunicationException(Exception e) {
        super(e);
    }

    public TailwindCommunicationException(int statusCode, Exception e) {
        super(e);
        this.statusCode = statusCode;
    }

    public TailwindCommunicationException(int statusCode) {
        this.statusCode = statusCode;
    }

    public TailwindCommunicationException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public TailwindCommunicationException(String message, Exception e) {
        super(message, e);
    }

    public TailwindCommunicationException(String message) {
        super(message);
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public @Nullable String getMessage() {
        String message = super.getMessage();
        return message == null ? null : "Rest call failed: statusCode=" + statusCode + ", message=" + message;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": statusCode=" + statusCode + ", message=" + super.getMessage()
                + ", cause: " + getCause();
    }
}
