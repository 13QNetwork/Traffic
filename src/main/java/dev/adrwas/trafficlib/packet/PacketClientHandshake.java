package dev.adrwas.trafficlib.packet;

import dev.adrwas.trafficlib.client.SocketClient;
import dev.adrwas.trafficlib.server.SocketServerRequestHandler;

public class PacketClientHandshake extends ClientPacket {

    public final String serverId;
    public final String serverType;

    public final String trafficLibVersion;

    public final Class[] plugins;

    public PacketClientHandshake(long packetId, String serverId, String serverType, String trafficLibVersion, Class[] plugins) {
        super(packetId);
        this.serverId = serverId;
        this.serverType = serverType;
        this.trafficLibVersion = trafficLibVersion;
        this.plugins = plugins;
    }

    @Override
    public void onRecievedByThisServer(SocketServerRequestHandler server) {

    }

    @Override
    public void onRecievedByRemoteServer(SocketClient client) {

    }

    @Override
    public void onProcessedByRemoteServer(SocketClient client) {

    }
}
