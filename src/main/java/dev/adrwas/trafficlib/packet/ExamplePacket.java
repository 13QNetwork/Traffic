package dev.adrwas.trafficlib.packet;

import dev.adrwas.trafficlib.server.SocketServer;

public class ExamplePacket extends ClientPacket {
    @Override
    public void onRecievedByServer(SocketServer server) {
        System.out.println("Example packet recieved by server!!");
    }
}
