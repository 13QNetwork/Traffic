package dev.adrwas.trafficlib.packet;

import dev.adrwas.trafficlib.client.SocketClient;
import dev.adrwas.trafficlib.server.SocketServerRequestHandler;

public class PacketClientPlayerUpdate extends ClientPacket {

    public PacketClientPlayerUpdate(long packetId) {
        super(packetId);
    }

    @Override
    public void onRecievedByThisServer(SocketServerRequestHandler server) {
        server.log("server recieved packet client player update");
    }

    @Override
    public void onRecievedByRemoteServer(SocketClient client) {

    }

    @Override
    public void onProcessedByRemoteServer(SocketClient client) {
        client.log("PacketClientPlayerUpdate processed by remote server");
    }
}
