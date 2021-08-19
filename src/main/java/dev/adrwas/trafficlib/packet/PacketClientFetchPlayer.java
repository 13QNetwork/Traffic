package dev.adrwas.trafficlib.packet;

import dev.adrwas.trafficlib.client.SocketClient;
import dev.adrwas.trafficlib.packet.player.Player;
import dev.adrwas.trafficlib.packet.v_0_1_R1.PacketServerFetchPlayerResponse;
import dev.adrwas.trafficlib.server.SocketServerRequestHandler;

import java.util.UUID;

public class PacketClientFetchPlayer extends ClientPacket {

    public final String nameQuery;
    public final UUID uuidQuery;

    public PacketClientFetchPlayer(long packetId, String nameQuery) {
        super(packetId);
        this.nameQuery = nameQuery;
        this.uuidQuery = null;
    }

    public PacketClientFetchPlayer(long packetId, UUID uuidQuery) {
        super(packetId);
        this.nameQuery = null;
        this.uuidQuery = uuidQuery;
    }

    @Override
    public void onRecievedByThisServer(SocketServerRequestHandler server) {
        Player nonExact = null;

        try {
            for(SocketServerRequestHandler handler : server.server.requestHandlers.values()) {
                for(Player player : handler.players) {
                    if(nameQuery != null && player.getName().equalsIgnoreCase(nameQuery)) {
                        server.sendPacket(new PacketServerFetchPlayerResponse(packetId, player));
                        return;
                    }

                    else if(uuidQuery != null && player.getUUID().equals(uuidQuery)) {
                        server.sendPacket(new PacketServerFetchPlayerResponse(packetId, player));
                        return;
                    }

                    else if(player.getName().toLowerCase().startsWith(nameQuery.toLowerCase())) {
                        nonExact = player;
                    }
                }
            }

            server.sendPacket(new PacketServerFetchPlayerResponse(packetId, nonExact));
        } catch (PacketTransmissionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRecievedByRemoteServer(SocketClient client) {

    }

    @Override
    public void onProcessedByRemoteServer(SocketClient client) {

    }
}
