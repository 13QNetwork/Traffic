package dev.adrwas.trafficlib.packet.player;

import java.util.UUID;

public class SpigotPlayer implements Player {

    private final UUID uuid;
    private final String playerName;

    public SpigotPlayer(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
    }

    @Override
    public String getConnectionType() {
        return "spigot";
    }

    @Override
    public UUID getUUID() {
        return this.uuid;
    }

    @Override
    public String getName() {
        return this.playerName;
    }

    @Override
    public void sendMessage(String message) {

    }
}
