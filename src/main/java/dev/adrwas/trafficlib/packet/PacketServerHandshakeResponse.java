package dev.adrwas.trafficlib.packet;

import dev.adrwas.trafficlib.client.SocketClient;
import dev.adrwas.trafficlib.server.SocketServerRequestHandler;

public class PacketServerHandshakeResponse extends ServerPacket implements NoTransitUpdates /* connection is closed once this packet is sent */ {

    public final boolean success;
    public final Object data;

    public PacketServerHandshakeResponse(long packetId, boolean success, Object data) {
        super(packetId);
        this.success = success;
        this.data = data;
    }

    @Override
    public void onRecievedByThisClient(SocketClient client) {
        if(!success) {
            client.log("Handshake error when connecting to master server:");
            if(data instanceof Exception) {
                ((Exception) data).printStackTrace();
            } else if(data != null){
                System.out.println(data);
            }
        }
    }

    @Override
    public void onRecievedByRemoteClient(SocketServerRequestHandler server) {

    }

    @Override
    public void onProcessedByRemoteClient(SocketServerRequestHandler server) {

    }
}
