package dev.adrwas.trafficlib.packet;

import dev.adrwas.trafficlib.client.SocketClient;
import dev.adrwas.trafficlib.server.SocketServerRequestHandler;

public class PacketServerRequestPlayers extends ServerPacket {

    public PacketServerRequestPlayers(long packetId) {
        super(packetId);
    }

    @Override
    public void onRecievedByThisClient(SocketClient client) {
        try {
            System.out.println("PacketClientPlayerUpdate sending");
            client.sendPacket(new PacketClientPlayerUpdate(Packet.generateId()), PacketOperationTiming.SYNC_FINISH_AFTER_PROCESSED);
            System.out.println("PacketClientPlayerUpdate done processing ");
        } catch (PacketTransmissionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRecievedByRemoteClient(SocketServerRequestHandler server) {

    }

    @Override
    public void onProcessedByRemoteClient(SocketServerRequestHandler server) {

    }
}
