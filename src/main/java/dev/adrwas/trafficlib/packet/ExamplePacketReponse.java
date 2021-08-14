package dev.adrwas.trafficlib.packet;

import dev.adrwas.trafficlib.client.SocketClient;
import dev.adrwas.trafficlib.server.SocketServerRequestHandler;

public class ExamplePacketReponse extends ServerPacket {

    public ExamplePacketReponse(long packetId) {
        super(packetId);
    }

    @Override
    public void onRecievedByThisClient(SocketClient client) {
        System.out.println("Example packet repsonse recieved by client");
    }

    @Override
    public void onRecievedByRemoteClient(SocketServerRequestHandler server) {

    }

    @Override
    public void onProcessedByRemoteClient(SocketServerRequestHandler server) {

    }
}
