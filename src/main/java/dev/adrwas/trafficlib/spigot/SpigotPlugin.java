package dev.adrwas.trafficlib.spigot;

import org.bukkit.plugin.java.JavaPlugin;

public class SpigotPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("TrafficLib for Spigot enabled");
    }
}
