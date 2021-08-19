package dev.adrwas.trafficlib.packet.v_0_1_R1;

import dev.adrwas.trafficlib.client.SocketClient;
import dev.adrwas.trafficlib.packet.ServerPacket;
import dev.adrwas.trafficlib.packet.player.Player;
import dev.adrwas.trafficlib.server.SocketServerRequestHandler;

public class PacketServerFetchPlayerResponse extends ServerPacket {

    public final Player player;

    public PacketServerFetchPlayerResponse(long packetId, Player player) {
        super(packetId);
        this.player = player;
    }

    @Override
    public void onRecievedByThisClient(SocketClient client) {

    }

    @Override
    public void onRecievedByRemoteClient(SocketServerRequestHandler server) {

    }

    @Override
    public void onProcessedByRemoteClient(SocketServerRequestHandler server) {

    }
}
