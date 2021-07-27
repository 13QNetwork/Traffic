package dev.adrwas.trafficlib.client;

import java.net.Socket;

public class SocketClientAPI {

    private static SocketClient mainClient;

    public static SocketClient startSocketClient(String address, int port, String encryptionPassword) {
        return new SocketClient(address, port, encryptionPassword);
    }

    public static SocketClient getMainClient() {
        return mainClient;
    }

    public static void setMainClient(SocketClient newMainClient) {
        mainClient = newMainClient;
    }
}
