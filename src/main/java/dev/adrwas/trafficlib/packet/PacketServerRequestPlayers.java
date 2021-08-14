package dev.adrwas.trafficlib.packet;

import com.google.common.collect.Lists;
import dev.adrwas.trafficlib.TrafficLib;
import dev.adrwas.trafficlib.client.SocketClient;
import dev.adrwas.trafficlib.packet.player.SpigotPlayer;
import dev.adrwas.trafficlib.server.SocketServerRequestHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class PacketServerRequestPlayers extends ServerPacket {

    public PacketServerRequestPlayers(long packetId) {
        super(packetId);
    }

    @Override
    public void onRecievedByThisClient(SocketClient client) {
        try {
            if(TrafficLib.getInstance().environment.equals("CLIENT_SPIGOT")) {
                ArrayList<SpigotPlayer> players = new ArrayList<SpigotPlayer>();
                for(Player player : Bukkit.getOnlinePlayers()) {
                    players.add(new SpigotPlayer(player.getUniqueId(), player.getName()));
                }

                client.sendPacket(new PacketClientPlayerUpdate(this.packetId, new PacketClientPlayerUpdate.PlayerUpdateDataAllPlayersSpigot(players)), PacketOperationTiming.SYNC_FINISH_AFTER_PROCESSED);
            }
        } catch (PacketTransmissionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRecievedByRemoteClient(SocketServerRequestHandler server) {

    }

    @Override
    public void onProcessedByRemoteClient(SocketServerRequestHandler server) {

    }
}
