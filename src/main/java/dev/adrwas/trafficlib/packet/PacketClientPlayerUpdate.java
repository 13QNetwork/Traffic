package dev.adrwas.trafficlib.packet;

import dev.adrwas.trafficlib.client.SocketClient;
import dev.adrwas.trafficlib.packet.player.SpigotPlayer;
import dev.adrwas.trafficlib.server.SocketServerRequestHandler;
import org.bukkit.World;

import java.io.Serializable;
import java.util.ArrayList;

public class PacketClientPlayerUpdate extends ClientPacket {

    public PlayerUpdateData data;

    public PacketClientPlayerUpdate(long packetId, PlayerUpdateData data) {
        super(packetId);
        this.data = data;
    }

    @Override
    public void onRecievedByThisServer(SocketServerRequestHandler server) {
        if(data instanceof PlayerUpdateDataAllPlayersSpigot) {
            PlayerUpdateDataAllPlayersSpigot converted = (PlayerUpdateDataAllPlayersSpigot) data;
            server.players.clear();
            for(SpigotPlayer player : converted.allPlayers) {
                server.players.add(player);
            }
        } else if(data instanceof PlayerUpdateDataPlayerConnectSpigot) {
            PlayerUpdateDataPlayerConnectSpigot converted = (PlayerUpdateDataPlayerConnectSpigot) data;
            server.players.add(converted.spigotPlayer);
        } else if(data instanceof PlayerUpdateDataPlayerDisconnectSpigot) {
            PlayerUpdateDataPlayerDisconnectSpigot converted = (PlayerUpdateDataPlayerDisconnectSpigot) data;
            server.players.remove(converted.spigotPlayer);
        }
    }

    @Override
    public void onRecievedByRemoteServer(SocketClient client) {

    }

    @Override
    public void onProcessedByRemoteServer(SocketClient client) {
    }

    public interface PlayerUpdateData {

    }

    public static class PlayerUpdateDataAllPlayersSpigot implements PlayerUpdateData, Serializable {
        ArrayList<SpigotPlayer> allPlayers;

        public PlayerUpdateDataAllPlayersSpigot(ArrayList<SpigotPlayer> allPlayers) {
            this.allPlayers = allPlayers;
        }
    }

    public static class PlayerUpdateDataPlayerConnectSpigot implements PlayerUpdateData, Serializable {
        SpigotPlayer spigotPlayer;

        public PlayerUpdateDataPlayerConnectSpigot(SpigotPlayer spigotPlayer) {
            this.spigotPlayer = spigotPlayer;
        }
    }

    public static class PlayerUpdateDataPlayerDisconnectSpigot implements PlayerUpdateData, Serializable {
        SpigotPlayer spigotPlayer;

        public PlayerUpdateDataPlayerDisconnectSpigot(SpigotPlayer spigotPlayer) {
            this.spigotPlayer = spigotPlayer;
        }
    }
}
