package dev.adrwas.trafficlib.packet;

public class TrafficHandshakeException extends Exception {

    public TrafficHandshakeException(String errorMessage) {
        super(errorMessage);
    }

    public TrafficHandshakeException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
