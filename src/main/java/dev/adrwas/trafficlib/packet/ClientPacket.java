package dev.adrwas.trafficlib.packet;

import dev.adrwas.trafficlib.client.SocketClient;
import dev.adrwas.trafficlib.server.SocketServerRequestHandler;

import java.io.Serializable;

public abstract class ClientPacket implements Packet, Serializable {

    public final long packetId;

    public ClientPacket(long packetId) {
        this.packetId = packetId;
    }

    public abstract void onRecievedByThisServer(SocketServerRequestHandler server);

    public abstract void onRecievedByRemoteServer(SocketClient client);

    public abstract void onProcessedByRemoteServer(SocketClient client);

}
