package dev.adrwas.trafficlib.packet;

import dev.adrwas.trafficlib.client.SocketClient;
import dev.adrwas.trafficlib.packet.PendingPacket.PendingPacketStatus;
import dev.adrwas.trafficlib.server.SocketServer;
import dev.adrwas.trafficlib.server.SocketServerRequestHandler;
import org.bukkit.Server;

public class PacketServerPacketStatus extends ServerPacket implements NoTransitUpdates {

    public final long relevantPacketId;
    public final PendingPacketStatus status;

    public PacketServerPacketStatus(long relevantPacketId, PendingPacketStatus status) {
        super(0); // NoTransitUpdates means no unique id needed
        this.relevantPacketId = relevantPacketId;
        this.status = status;
    }

    @Override
    public void onRecievedByThisClient(SocketClient client) {
        if(client.transitPackets.containsKey(relevantPacketId)) {
            PendingPacket<?> packet = client.transitPackets.get(relevantPacketId);
            if(!(packet.packet instanceof ClientPacket)) return;
            packet.status = this.status;

            if(status.equals(PendingPacketStatus.PROCESSING)) {
                ((ClientPacket) packet.packet).onRecievedByRemoteServer(client);
            } else if(status.equals(PendingPacketStatus.DONE)) {
                ((ClientPacket) packet.packet).onProcessedByRemoteServer(client);
                client.transitPackets.remove(relevantPacketId);
            }
        }
    }

    @Override
    public void onRecievedByRemoteClient(SocketServerRequestHandler server) {

    }

    @Override
    public void onProcessedByRemoteClient(SocketServerRequestHandler server) {

    }
}
