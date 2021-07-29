package dev.adrwas.trafficlib.packet;

import dev.adrwas.trafficlib.client.SocketClient;
import dev.adrwas.trafficlib.server.SocketServer;
import dev.adrwas.trafficlib.server.SocketServerRequestHandler;

public class ExamplePacket extends ClientPacket {

    public ExamplePacket(long packetId) {
        super(packetId);
    }

    @Override
    public void onRecievedByThisServer(SocketServerRequestHandler server) {
        server.log("Example packet recieved by server");
    }

    @Override
    public void onRecievedByRemoteServer(SocketClient client) {
        client.log("Remote server recieved example packet");
    }

    @Override
    public void onProcessedByRemoteServer(SocketClient client) {
        client.log("Remote server processed example packet");
    }
}
