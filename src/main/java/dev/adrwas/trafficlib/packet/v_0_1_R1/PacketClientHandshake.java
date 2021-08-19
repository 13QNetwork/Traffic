package dev.adrwas.trafficlib.packet.v_0_1_R1;

import dev.adrwas.trafficlib.TrafficLib;
import dev.adrwas.trafficlib.client.SocketClient;
import dev.adrwas.trafficlib.packet.ClientPacket;
import dev.adrwas.trafficlib.packet.PacketServerHandshakeResponse;
import dev.adrwas.trafficlib.packet.TrafficHandshakeException;
import dev.adrwas.trafficlib.server.SocketServerRequestHandler;

import java.io.IOException;

public class PacketClientHandshake extends ClientPacket {

    public final String serverId;
    public final String serverType;

    public final String trafficLibVersion;

    public final String[] plugins;

    public PacketClientHandshake(long packetId, String serverId, String serverType, String trafficLibVersion, String[] plugins) {
        super(packetId);
        this.serverId = serverId;
        this.serverType = serverType;
        this.trafficLibVersion = trafficLibVersion;
        this.plugins = plugins;
    }

    @Override
    public void onRecievedByThisServer(SocketServerRequestHandler server) {
        try {
            if (server.server.requestHandlers.containsKey(serverId)) {
                server.sendPacket(new PacketServerHandshakeResponse(packetId, false, new TrafficHandshakeException("Server id " + serverId + " is a duplicate of an existing connection")), PacketOperationTiming.SYNC_FINISH_WHEN_SENT);
                server.socket.close();
            } else if (!this.trafficLibVersion.equals(TrafficLib.getInstance().trafficLibVersion)) {
                server.sendPacket(new PacketServerHandshakeResponse(packetId, false, new TrafficHandshakeException("Incompatible versions: Master server uses Traffic " + TrafficLib.getInstance().trafficLibVersion)), PacketOperationTiming.SYNC_FINISH_WHEN_SENT);
                server.socket.close();
            } else {
                server.sendPacket(new PacketServerHandshakeResponse(packetId, true, null));
                server.recievedHandshake = true;
                server.server.requestHandlers.put(serverId, server);
                server.log("server successfully recieved handshake");
            }
        } catch (Exception exception) {
            server.recievedHandshake = false;
            exception.printStackTrace();
            try {
                server.socket.close();
            } catch (IOException exception2) {
                exception2.printStackTrace();
            }
        }
    }

    @Override
    public void onRecievedByRemoteServer(SocketClient client) {

    }

    @Override
    public void onProcessedByRemoteServer(SocketClient client) {

    }
}
