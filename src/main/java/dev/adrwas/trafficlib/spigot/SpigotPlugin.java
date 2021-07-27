package dev.adrwas.trafficlib.spigot;

import dev.adrwas.trafficlib.client.SocketClientAPI;
import org.bukkit.plugin.java.JavaPlugin;

public class SpigotPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("TrafficLib for Spigot enabled");

        SocketClientAPI.setMainClient(
                SocketClientAPI.startSocketClient("localhost", 6000, "Pass1word")
        );
    }
}
