package dev.adrwas.trafficlib.spigot;

import dev.adrwas.trafficlib.client.SocketClientAPI;
import dev.adrwas.trafficlib.server.SocketServer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class SpigotPlugin extends JavaPlugin {

    private String server;
    private int port;
    private boolean autoUpdate;
    private String password;

    @Override
    public void onEnable() {
        getLogger().info("TrafficLib for Spigot enabled");

        loadConfig();

        if(!server.trim().equals("")) {
            SocketClientAPI.setMainClient(
                    SocketClientAPI.startSocketClient(this.server, this.port, this.password)
            );
        } else {
            System.out.println("Hosting master server on port " + this.port + "...");

            SocketServer server = new SocketServer(this.port, this.password);
            server.startServer();

            SocketClientAPI.setMainClient(
                    SocketClientAPI.startSocketClient("localhost", this.port, this.password)
            );
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
}
