package dev.adrwas.trafficlib.spigot;

import dev.adrwas.trafficlib.TrafficLib;
import dev.adrwas.trafficlib.client.SocketClient;
import dev.adrwas.trafficlib.client.SocketClientAPI;
import dev.adrwas.trafficlib.packet.Packet;
import dev.adrwas.trafficlib.packet.PacketClientPlayerUpdate;
import dev.adrwas.trafficlib.packet.PacketTransmissionException;
import dev.adrwas.trafficlib.packet.player.Player;
import dev.adrwas.trafficlib.packet.player.SpigotPlayer;
import dev.adrwas.trafficlib.server.SocketServer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

public class SpigotPlugin extends JavaPlugin implements Listener {

    private String server;
    private int port;
    private boolean autoUpdate;
    private String password;

    private SocketServer socketServer;

    @Override
    public void onEnable() {
        getLogger().info("TrafficLib for Spigot enabled");
        Bukkit.getPluginManager().registerEvents(this, this);

        TrafficLib.setInstance(new TrafficLib("CLIENT_SPIGOT", "0.1"));
        loadConfig();

        if(!server.trim().equals("")) {
            SocketClientAPI.setMainClient(
                    SocketClientAPI.startSocketClient(this.server, this.port, this.password)
            );
        } else {
            System.out.println("Hosting master server on port " + this.port + "...");

            this.socketServer = new SocketServer(this.port, this.password);
            socketServer.startServer();

            SocketClientAPI.setMainClient(
                    SocketClientAPI.startSocketClient("localhost", this.port, this.password)
            );
        }

        // Save players into list of SpigotPlayers
        ArrayList<SpigotPlayer> players = new ArrayList<SpigotPlayer>();
        for(org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            players.add(new SpigotPlayer(player.getUniqueId(), player.getName()));
        }
        // Send player data to server
        try {
            SocketClientAPI.getMainClient().sendPacket(new PacketClientPlayerUpdate(Packet.generateId(), new PacketClientPlayerUpdate.PlayerUpdateDataAllPlayersSpigot(players)), Packet.PacketOperationTiming.SYNC_FINISH_AFTER_PROCESSED);
        } catch (PacketTransmissionException e) {
            e.printStackTrace();
        }
    }

    private void loadConfig() {
        saveDefaultConfig();

        FileConfiguration configuration = getConfig();

        if(configuration.isString("server")) {
            this.server = configuration.getString("server");
        } else {
            configuration.set("server", "");
            this.server = "";
        }

        if(configuration.isInt("port")) {
            this.port = configuration.getInt("port");
        } else {
            configuration.set("port", 4444);
            this.port = 4444;
        }

        if(configuration.isBoolean("auto-update")) {
            this.autoUpdate = configuration.getBoolean("auto-update");
        } else {
            configuration.set("auto-update", true);
            this.autoUpdate = true;
        }

        if(configuration.isString("password")) {
            this.password = configuration.getString("password");
        } else {
            String password = "(auto-generated) " + Long.toHexString(new Random().nextLong());
            configuration.set("password", password);
            this.password = password;
        }
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Send player update to server
        {
            org.bukkit.entity.Player player = event.getPlayer();
            try {
                SocketClientAPI.getMainClient().sendPacket(new PacketClientPlayerUpdate(Packet.generateId(), new PacketClientPlayerUpdate.PlayerUpdateDataPlayerConnectSpigot(new SpigotPlayer(player.getUniqueId(), player.getName()))), Packet.PacketOperationTiming.ASYNC);
            } catch (PacketTransmissionException e) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        // Send player update to server
        {
            org.bukkit.entity.Player player = event.getPlayer();
            try {
                SocketClientAPI.getMainClient().sendPacket(new PacketClientPlayerUpdate(Packet.generateId(), new PacketClientPlayerUpdate.PlayerUpdateDataPlayerDisconnectSpigot(new SpigotPlayer(player.getUniqueId(), player.getName()))), Packet.PacketOperationTiming.ASYNC);
            } catch (PacketTransmissionException e) {
                e.printStackTrace();
            }
        }
    }
}
