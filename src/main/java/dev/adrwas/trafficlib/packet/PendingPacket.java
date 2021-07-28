package dev.adrwas.trafficlib.packet;

public class PendingPacket<T extends Packet> {

    public enum PendingPacketStatus {
        SENDING,
        PROCESSING,
        DONE
    }

    public T packet;
    public PendingPacketStatus status;

    public PendingPacket(T packet) {
        this.packet = packet;
        this.status = PendingPacketStatus.SENDING;
    }

    public PendingPacket(T packet, PendingPacketStatus status) {
        this.packet = packet;
        this.status = status;
    }
}
