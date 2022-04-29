package org.emeraldcraft.djitello4j.components;

/**
 * Tello response. The exception field is usually null, but is thrown if an exception is caught
 */
public final class TelloResponse {
    private final TelloCommand command;
    private final boolean wasSuccessful;
    private final Exception exception;

    /**
     * @param command
     * @param wasSuccessful
     * @param exception
     */
    public TelloResponse(TelloCommand command, boolean wasSuccessful, Exception exception) {
        this.command = command;
        this.wasSuccessful = wasSuccessful;
        this.exception = exception;
    }

    public TelloCommand command() {
        return command;
    }

    public boolean wasSuccessful() {
        return wasSuccessful;
    }

    public Exception getException() {
        return exception;
    }



}
