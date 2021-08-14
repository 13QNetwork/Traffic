package dev.adrwas.trafficlib.bungee;

import dev.adrwas.trafficlib.TrafficLib;
import net.md_5.bungee.api.plugin.Plugin;

public class BungeePlugin extends Plugin {

    @Override
    public void onEnable() {
        getLogger().info("TrafficLib for Bungeecord enabled");
        TrafficLib.setInstance(new TrafficLib("CLIENT_BUNGEECORD", "0.1"));
    }
}
