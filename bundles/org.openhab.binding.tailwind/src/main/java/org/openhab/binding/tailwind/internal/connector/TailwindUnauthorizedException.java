package org.openhab.binding.tailwind.internal.connector;

public class TailwindUnauthorizedException extends TailwindCommunicationException {
    private static final long serialVersionUID = 1L;

    public TailwindUnauthorizedException(int statusCode, Exception e) {
        super(statusCode, e);
    }

    public TailwindUnauthorizedException(int statusCode) {
        super(statusCode);
    }

    public TailwindUnauthorizedException(int statusCode, String message) {
        super(statusCode, message);
    }
}
