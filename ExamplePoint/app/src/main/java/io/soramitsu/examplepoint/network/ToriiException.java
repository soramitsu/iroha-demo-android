package io.soramitsu.examplepoint.network;

/**
 * Indicates an HTTP or protocol level failure while talking to the Torii API.
 */
public class ToriiException extends Exception {
    public ToriiException(String message) {
        super(message);
    }

    public ToriiException(String message, Throwable cause) {
        super(message, cause);
    }
}
