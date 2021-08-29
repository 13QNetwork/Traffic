package dev.adrwas.trafficlib.packet;

import dev.adrwas.trafficlib.TrafficLib;
import dev.adrwas.trafficlib.client.SocketClient;
import dev.adrwas.trafficlib.packet.player.Player;
import dev.adrwas.trafficlib.packet.player.SpigotPlayer;
import dev.adrwas.trafficlib.server.SocketServerRequestHandler;
import dev.adrwas.trafficlib.packet.PacketClientMessagePlayerResponse.*;
import org.bukkit.Bukkit;

public class PacketServerMessagePlayer extends ServerPacket {

    public final Player player;
    public final String message;

    public PacketServerMessagePlayer(long packetId, Player player, String message) {
        super(packetId);
        this.player = player;
        this.message = message;
    }

    @Override
    public void onRecievedByThisClient(SocketClient client) {
        PacketClientMessagePlayerResponse response;

        if (TrafficLib.getInstance().environment.equals("CLIENT_SPIGOT")) {
            if(player instanceof SpigotPlayer) {
                if(Bukkit.getPlayer(player.getUUID()) != null) {
                    Bukkit.getPlayer(player.getUUID()).sendMessage(message);
                    response = new PacketClientMessagePlayerResponse(packetId, new MessagePlayerResult(MessagePlayerResult.MessagePlayerResultEnum.SUCCESS, null));
                } else {
                    response = new PacketClientMessagePlayerResponse(packetId, new MessagePlayerResult(MessagePlayerResult.MessagePlayerResultEnum.PLAYER_OFFLINE, null));
                }
            } else {
                response = new PacketClientMessagePlayerResponse(packetId, new MessagePlayerResult(MessagePlayerResult.MessagePlayerResultEnum.INCOMPATIBLE_TYPE, null));
            }
        } else {
            response = new PacketClientMessagePlayerResponse(packetId, new MessagePlayerResult(MessagePlayerResult.MessagePlayerResultEnum.INCOMPATIBLE_TYPE, null));
        }

        if(response != null) {
            try {
                client.sendPacket(response);
            } catch (PacketTransmissionException e) {
                e.printStackTrace();
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
