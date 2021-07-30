package dev.adrwas.trafficlib.packet;

public class PacketTransmissionException extends Exception {

    public PacketTransmissionException(String errorMessage) {
        super(errorMessage);
    }

    public PacketTransmissionException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
