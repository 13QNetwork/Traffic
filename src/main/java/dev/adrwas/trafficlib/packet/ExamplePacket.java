package dev.adrwas.trafficlib.packet;

import dev.adrwas.trafficlib.client.SocketClient;
import dev.adrwas.trafficlib.server.SocketServer;
import dev.adrwas.trafficlib.server.SocketServerRequestHandler;

public class ExamplePacket extends ClientPacket {

    @Override
    public void onRecievedByThisServer(SocketServerRequestHandler server) {
        System.out.println("Example packet recieved by server!!");
    }

    @Override
    public void onRecievedByRemoteServer(SocketClient client) {

    }

    @Override
    public void onProcessedByRemoteServer(SocketClient client) {

    }
}
