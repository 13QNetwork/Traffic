package dev.adrwas.trafficlib.packet;

import dev.adrwas.trafficlib.client.SocketClient;
import dev.adrwas.trafficlib.server.SocketServer;
import dev.adrwas.trafficlib.server.SocketServerRequestHandler;

import java.io.Serializable;

public abstract class ClientPacket extends Packet implements Serializable {

    public abstract void onRecievedByThisServer(SocketServerRequestHandler server);

    public abstract void onRecievedByRemoteServer(SocketClient client);

    public abstract void onProcessedByRemoteServer(SocketClient client);

}
