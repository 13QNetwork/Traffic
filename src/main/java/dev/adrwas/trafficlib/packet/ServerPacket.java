package dev.adrwas.trafficlib.packet;

import dev.adrwas.trafficlib.client.SocketClient;
import dev.adrwas.trafficlib.server.SocketServer;
import dev.adrwas.trafficlib.server.SocketServerRequestHandler;

import java.io.Serializable;

public abstract class ServerPacket extends Packet implements Serializable {

    public abstract void onRecievedByThisClient(SocketClient client);

    public abstract void onRecievedByRemoteClient(SocketServerRequestHandler server);

    public abstract void onProcessedByRemoteClient(SocketServerRequestHandler server);

}