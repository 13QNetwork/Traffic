package dev.adrwas.trafficlib.packet.player;

import java.util.UUID;

public interface Player {

    String getConnectionType();

    UUID getUUID();
    String getName();

    void sendMessage(String message);
}
