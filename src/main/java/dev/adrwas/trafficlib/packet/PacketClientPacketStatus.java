package dev.adrwas.trafficlib.packet;

import dev.adrwas.trafficlib.client.SocketClient;
import dev.adrwas.trafficlib.packet.PendingPacket.PendingPacketStatus;
import dev.adrwas.trafficlib.server.SocketServer;
import dev.adrwas.trafficlib.server.SocketServerRequestHandler;

public class PacketClientPacketStatus extends ClientPacket implements NoTransitUpdates {

    public final long relevantPacketId;
    public final PendingPacketStatus status;

    public PacketClientPacketStatus(long relevantPacketId, PendingPacketStatus status) {
        super(0); // NoTransitUpdates means no unique id needed
        this.relevantPacketId = relevantPacketId;
        this.status = status;
    }

    @Override
    public void onRecievedByThisServer(SocketServerRequestHandler server) {
        if(server.transitPackets.containsKey(relevantPacketId)) {
            PendingPacket<?> packet = server.transitPackets.get(relevantPacketId);
            if(!(packet.packet instanceof ServerPacket)) return;
            packet.status = this.status;

            if(status.equals(PendingPacketStatus.PROCESSING)) {
                packet.fireEvent(PendingPacket.PendingPacketEvent.PRE_RECEIVED);
                ((ServerPacket) packet.packet).onRecievedByRemoteClient(server);
                packet.fireEvent(PendingPacket.PendingPacketEvent.POST_RECEIVED);
            } else if(status.equals(PendingPacketStatus.DONE)) {
                packet.fireEvent(PendingPacket.PendingPacketEvent.PRE_PROCESSED);
                ((ServerPacket) packet.packet).onProcessedByRemoteClient(server);
                server.transitPackets.remove(relevantPacketId);
                packet.fireEvent(PendingPacket.PendingPacketEvent.POST_PROCESSED);
            }
        }
    }

    @Override
    public void onRecievedByRemoteServer(SocketClient client) {

    }

    @Override
    public void onProcessedByRemoteServer(SocketClient client) {

    }
}
