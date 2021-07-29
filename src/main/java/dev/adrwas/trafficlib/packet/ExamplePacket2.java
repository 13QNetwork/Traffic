package dev.adrwas.trafficlib.packet;

import dev.adrwas.trafficlib.client.SocketClient;
import dev.adrwas.trafficlib.server.SocketServerRequestHandler;

public class ExamplePacket2 extends ClientPacket implements NoTransitUpdates {

    final String name;

    public ExamplePacket2(String name) {
        super(12345);
        this.name = name;
    }

    @Override
    public void onRecievedByThisServer(SocketServerRequestHandler server) {
        System.out.println("Example packet recieved by server with name " + name);
    }

    @Override
    public void onRecievedByRemoteServer(SocketClient client) {

    }

    @Override
    public void onProcessedByRemoteServer(SocketClient client) {

    }
}
