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
        super(5);
        this.relevantPacketId = relevantPacketId;
        this.status = status;
    }

    @Override
    public void onRecievedByThisClient(SocketClient client) {
        System.out.println("PacketServerPacketStatus packet recieved, transit packets length = " + client.transitPackets.size());
        for (Long aLong : client.transitPackets.keySet()) {
            System.out.println("Transit packet --> " + aLong);
        }
        System.out.println("Relevant packet id: " + relevantPacketId);

        if(client.transitPackets.containsKey(relevantPacketId)) {
            System.out.println("Transit packet found");

            PendingPacket<?> packet = client.transitPackets.get(relevantPacketId);
            if(!(packet.packet instanceof ClientPacket)) return;
            System.out.println("Transit packet is client packet");
            packet.status = this.status;

            if(status.equals(PendingPacketStatus.PROCESSING)) {
                System.out.println("Exectuing #onRecievedByRemoteServer");
                ((ClientPacket) packet.packet).onRecievedByRemoteServer(client);
            } else if(status.equals(PendingPacketStatus.DONE)) {
                System.out.println("Exectuing #onProcessedByRemoteServer");
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
